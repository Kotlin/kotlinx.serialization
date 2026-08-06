/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*

// kotlinx-serialization-cbor keeps the same set in its Encoding.kt; keep both in sync.
private val unsignedNumberDescriptors = setOf(
    UInt.serializer().descriptor,
    ULong.serializer().descriptor,
    UByte.serializer().descriptor,
    UShort.serializer().descriptor
)

internal val SerialDescriptor.isUnsignedNumber: Boolean
    get() = this.isInline && this in unsignedNumberDescriptors

internal val SerialDescriptor.isUnquotedLiteral: Boolean
    get() = this.isInline && this == jsonUnquotedLiteralDescriptor

@OptIn(ExperimentalSerializationApi::class)
internal class StreamingJsonEncoder(
    private val composer: Composer,
    override val json: Json,
    private val mode: LexerMode,
    private val modeReuseCache: Array<JsonEncoder?>?
) : JsonEncoder, AbstractEncoder() {

    internal constructor(
        output: InternalJsonWriter, json: Json, mode: LexerMode,
        modeReuseCache: Array<JsonEncoder?>
    ) : this(Composer(output, json), json, mode, modeReuseCache)

    override val serializersModule: SerializersModule = json.serializersModule
    private val configuration = json.configuration

    // Forces serializer to wrap all values into quotes
    private var forceQuoting: Boolean = false
    private var polymorphicDiscriminator: String? = null
    private var polymorphicSerialName: String? = null

    // Cache of the @JsonExtraKeys index for the most recently queried descriptor.
    // The sentinel `lookupDescriptor !== descriptor` triggers a refresh when the
    // descriptor changes. This avoids hitting the per-Json schema cache on every
    // encoded element.
    private var lookupDescriptor: SerialDescriptor? = null
    private var cachedExtraKeysIndex: Int = -1

    private fun extraKeysIndexFor(descriptor: SerialDescriptor): Int {
        // Flag check first: when the feature is disabled this is a single
        // predictable branch, before any cache machinery is touched.
        if (!configuration.useExtraKeys) return -1
        if (lookupDescriptor !== descriptor) {
            lookupDescriptor = descriptor
            cachedExtraKeysIndex = descriptor.jsonExtraKeysIndex(json)
        }
        return cachedExtraKeysIndex
    }

    // State for @JsonExtraKeys write-back. The bucket value is stashed when its
    // element comes through encodeSerializableElement and written out as the
    // last members of the enclosing JSON object in endStructure. Because this
    // encoder instance is reused across same-mode nested structures
    // (modeReuseCache), the state is saved/restored via a stack — but only for
    // descriptors that actually declare a bucket, so bucket-free encoding pays
    // nothing beyond the extraKeysIndexFor check in beginStructure.
    private var pendingExtraKeys: Map<String, JsonElement>? = null
    private var pendingDiscriminator: String? = null

    private class ExtraKeysFrame(val extras: Map<String, JsonElement>?, val discriminator: String?)

    private var extraKeysStack: MutableList<ExtraKeysFrame>? = null

    private fun pushExtraKeysFrame(discriminator: String?) {
        val stack = extraKeysStack ?: mutableListOf<ExtraKeysFrame>().also { extraKeysStack = it }
        stack.add(ExtraKeysFrame(pendingExtraKeys, pendingDiscriminator))
        pendingExtraKeys = null
        pendingDiscriminator = discriminator
    }

    private fun flushExtraKeys(descriptor: SerialDescriptor, bucketIndex: Int) {
        val extras = pendingExtraKeys
        if (extras != null && extras.isNotEmpty()) {
            val collisionNames = descriptor.jsonExtraKeysCollisionNames(json, bucketIndex)
            for ((key, element) in extras) {
                validateNoExtraKeyCollision(descriptor, key, collisionNames)
                if (!composer.writingFirst) composer.print(COMMA)
                composer.nextItem()
                encodeString(key)
                composer.print(COLON)
                composer.space()
                encodeSerializableValue(JsonElementSerializer, element)
            }
        }
        val stack = extraKeysStack!!
        val frame = stack.removeAt(stack.lastIndex)
        pendingExtraKeys = frame.extras
        pendingDiscriminator = frame.discriminator
    }

    private fun validateNoExtraKeyCollision(descriptor: SerialDescriptor, key: String, collisionNames: Set<String>) {
        if (key in collisionNames) {
            throw JsonEncodingException(
                "@JsonExtraKeys property of '${descriptor.serialName}' contains key '$key' " +
                    "which conflicts with a declared property name.",
                classSerialName = descriptor.serialName
            )
        }
        if (key == pendingDiscriminator) {
            throw JsonEncodingException(
                "@JsonExtraKeys property of '${descriptor.serialName}' contains key '$key' " +
                    "which conflicts with the class discriminator.",
                classSerialName = descriptor.serialName
            )
        }
    }

    init {
        val i = mode.ordinal
        if (modeReuseCache != null) {
            if (modeReuseCache[i] !== null || modeReuseCache[i] !== this)
                modeReuseCache[i] = this
        }
    }

    override fun encodeJsonElement(element: JsonElement) {
        if (polymorphicDiscriminator != null && element !is JsonObject) {
            throwJsonElementPolymorphicException(polymorphicSerialName, element)
        }
        encodeSerializableValue(JsonElementSerializer, element)
    }

    override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean {
        return configuration.encodeDefaults
    }

    override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
        encodePolymorphically(serializer, value) { discriminatorName, serialName ->
            polymorphicDiscriminator = discriminatorName
            polymorphicSerialName = serialName
        }
    }

    private fun encodeTypeInfo(discriminator: String, serialName: String) {
        composer.nextItem()
        encodeString(discriminator)
        composer.print(COLON)
        composer.space()
        encodeString(serialName)
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        val newMode = json.modeFor(descriptor)
        if (newMode.begin != INVALID) { // entry
            composer.print(newMode.begin)
            composer.indent()
        }

        val discriminator = polymorphicDiscriminator
        if (discriminator != null) {
            encodeTypeInfo(discriminator, polymorphicSerialName ?: descriptor.serialName)
            polymorphicDiscriminator = null
            polymorphicSerialName = null
        }

        val result = if (mode == newMode) {
            this
        } else {
            (modeReuseCache?.get(newMode.ordinal) ?: StreamingJsonEncoder(composer, json, newMode, modeReuseCache))
        }
        if (newMode == LexerMode.OBJ && extraKeysIndexFor(descriptor) != -1) {
            (result as StreamingJsonEncoder).pushExtraKeysFrame(discriminator)
        }
        return result
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        if (mode == LexerMode.OBJ) {
            val bucketIndex = extraKeysIndexFor(descriptor)
            if (bucketIndex != -1) flushExtraKeys(descriptor, bucketIndex)
        }
        if (mode.end != INVALID) {
            composer.unIndent()
            composer.nextItemIfNotFirst()
            composer.print(mode.end)
        }
    }

    override fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean {
        when (mode) {
            LexerMode.LIST -> {
                if (!composer.writingFirst)
                    composer.print(COMMA)
                composer.nextItem()
            }
            LexerMode.MAP -> {
                if (!composer.writingFirst) {
                    forceQuoting = if (index % 2 == 0) {
                        composer.print(COMMA)
                        composer.nextItem() // indent should only be put after commas in map
                        true
                    } else {
                        composer.print(COLON)
                        composer.space()
                        false
                    }
                } else {
                    forceQuoting = true
                    composer.nextItem()
                }
            }
            LexerMode.POLY_OBJ -> {
                if (index == 0)
                    forceQuoting = true
                if (index == 1) {
                    composer.print(COMMA)
                    composer.space()
                    forceQuoting = false
                }
            }
            else -> {
                if (!composer.writingFirst)
                    composer.print(COMMA)
                composer.nextItem()
                encodeString(descriptor.getJsonElementName(json, index))
                composer.print(COLON)
                composer.space()
            }
        }
        return true
    }

    override fun <T> encodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T
    ) {
        // The @JsonExtraKeys bucket is not written as a regular property: it is
        // stashed here and spread as the last members of the enclosing object
        // in endStructure. The check lives here rather than in encodeElement
        // because a bucket is always a Map and can only arrive through this
        // method — primitive properties never pay for it. The mode check
        // short-circuits map/list entries before the descriptor lookup.
        if (mode == LexerMode.OBJ && extraKeysIndexFor(descriptor) == index) {
            // Validation in JsonNamesMap guarantees the declared type is
            // JsonObject or Map<String, JsonElement>.
            @Suppress("UNCHECKED_CAST")
            pendingExtraKeys = value as Map<String, JsonElement>
            return
        }
        super.encodeSerializableElement(descriptor, index, serializer, value)
    }

    override fun <T : Any> encodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T?
    ) {
        if (mode == LexerMode.OBJ && extraKeysIndexFor(descriptor) == index) {
            @Suppress("UNCHECKED_CAST")
            pendingExtraKeys = value as Map<String, JsonElement>?
            return
        }
        if (value != null || configuration.explicitNulls) {
            super.encodeNullableSerializableElement(descriptor, index, serializer, value)
        }
    }

    override fun encodeInline(descriptor: SerialDescriptor): Encoder =
        when {
            descriptor.isUnsignedNumber -> StreamingJsonEncoder(composerAs(::ComposerForUnsignedNumbers), json, mode, null)
            descriptor.isUnquotedLiteral -> StreamingJsonEncoder(composerAs(::ComposerForUnquotedLiterals), json, mode, null)
            polymorphicDiscriminator != null -> apply { polymorphicSerialName = descriptor.serialName }
            else                        -> super.encodeInline(descriptor)
        }

    private inline fun <reified T: Composer> composerAs(composerCreator: (writer: InternalJsonWriter, forceQuoting: Boolean) -> T): T {
        // If we're inside encodeInline().encodeSerializableValue, we should preserve the forceQuoting state
        // inside the composer, but not in the encoder (otherwise we'll get into `if (forceQuoting) encodeString(value.toString())` part
        // and unsigned numbers would be encoded incorrectly)
        return if (composer is T) composer
        else composerCreator(composer.writer, forceQuoting)
    }

    override fun encodeNull() {
        composer.print(NULL)
    }

    override fun encodeBoolean(value: Boolean) {
        if (forceQuoting) encodeString(value.toString()) else composer.print(value)
    }

    override fun encodeByte(value: Byte) {
        if (forceQuoting) encodeString(value.toString()) else composer.print(value)
    }

    override fun encodeShort(value: Short) {
        if (forceQuoting) encodeString(value.toString()) else composer.print(value)
    }

    override fun encodeInt(value: Int) {
        if (forceQuoting) encodeString(value.toString()) else composer.print(value)
    }

    override fun encodeLong(value: Long) {
        if (forceQuoting) encodeString(value.toString()) else composer.print(value)
    }

    override fun encodeFloat(value: Float) {
        // First encode value, then check, to have a prettier error message
        if (forceQuoting) encodeString(value.toString()) else composer.print(value)
        if (!configuration.allowSpecialFloatingPointValues && !value.isFinite()) {
            throw InvalidFloatingPointEncoded(value)
        }
    }

    override fun encodeDouble(value: Double) {
        // First encode value, then check, to have a prettier error message
        if (forceQuoting) encodeString(value.toString()) else composer.print(value)
        if (!configuration.allowSpecialFloatingPointValues && !value.isFinite()) {
            throw InvalidFloatingPointEncoded(value)
        }
    }

    override fun encodeChar(value: Char) {
        encodeString(value.toString())
    }

    override fun encodeString(value: String) = composer.printQuoted(value)

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
        encodeString(enumDescriptor.getElementName(index))
    }
}
