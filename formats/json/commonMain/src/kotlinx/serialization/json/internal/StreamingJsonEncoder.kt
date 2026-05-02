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
    private var activeDiscriminator: String? = null
    private val activeDiscriminatorStack = mutableListOf<String?>()

    // Cache of the @JsonExtraKeys index for the most recently queried descriptor.
    // The sentinel `lookupDescriptor !== descriptor` triggers a refresh when the
    // descriptor changes. This avoids hitting the per-Json schema cache on every
    // encoded element.
    private var lookupDescriptor: SerialDescriptor? = null
    private var cachedExtraKeysIndex: Int = -1

    private fun extraKeysIndexFor(descriptor: SerialDescriptor): Int {
        if (lookupDescriptor !== descriptor) {
            lookupDescriptor = descriptor
            cachedExtraKeysIndex = descriptor.jsonExtraKeysIndex(json)
        }
        return cachedExtraKeysIndex
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

        if (mode == newMode) {
            activeDiscriminatorStack.add(activeDiscriminator)
            activeDiscriminator = null
        }

        val discriminator = polymorphicDiscriminator
        if (discriminator != null) {
            encodeTypeInfo(discriminator, polymorphicSerialName ?: descriptor.serialName)
            activeDiscriminator = discriminator
            polymorphicDiscriminator = null
            polymorphicSerialName = null
        }

        if (mode == newMode) {
            return this
        }

        val cached = modeReuseCache?.get(newMode.ordinal)
        if (cached != null) {
            (cached as StreamingJsonEncoder).activeDiscriminator = null
            return cached
        }
        return StreamingJsonEncoder(composer, json, newMode, modeReuseCache)
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        if (mode.end != INVALID) {
            composer.unIndent()
            composer.nextItemIfNotFirst()
            composer.print(mode.end)
        }
        if (activeDiscriminatorStack.isNotEmpty()) {
            activeDiscriminator = activeDiscriminatorStack.removeAt(activeDiscriminatorStack.lastIndex)
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
        // Bucket-spread only applies in OBJ mode. The descriptor-validation in
        // JsonNamesMap.kt already filters out non-class descriptors, but the
        // explicit mode check documents intent and is robust to future
        // loosening of that validation.
        if (mode != WriteMode.OBJ || extraKeysIndexFor(descriptor) != index) {
            super.encodeSerializableElement(descriptor, index, serializer, value)
            return
        }
        // Drive the user's MapSerializer through a wrapper that intercepts the
        // alternating key/value calls and emits each entry as a sibling pair of
        // the parent JSON object. This works for arbitrary V: the wrapper
        // delegates value-encoding to the parent encoder via encodeSerializableValue,
        // which preserves polymorphic-discriminator routing and the JsonElement
        // short-circuit.
        val wrapper = JsonExtraKeysSpreadingEncoder(this, composer, descriptor, activeDiscriminator, json)
        serializer.serialize(wrapper, value)
    }

    override fun <T : Any> encodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T?
    ) {
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

/**
 * Encoder driven by the user's `MapSerializer<String, V>` to spread bucket
 * entries as sibling key/value pairs of the parent JSON object.
 *
 * Extends [AbstractEncoder] so unexpected primitive calls fail loud via the
 * default `encodeValue` (a non-standard custom map serializer that doesn't
 * follow the alternating key/value protocol is unsupported by design).
 */
@OptIn(ExperimentalSerializationApi::class)
private class JsonExtraKeysSpreadingEncoder(
    private val parent: StreamingJsonEncoder,
    private val composer: Composer,
    private val parentDescriptor: SerialDescriptor,
    private val activeDiscriminator: String?,
    private val json: Json,
) : AbstractEncoder() {

    override val serializersModule: SerializersModule get() = parent.serializersModule

    private var pendingKey: String? = null
    private val declaredNames: Set<String> = parentDescriptor.getJsonDecodingNames(json)

    override fun <T> encodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T
    ) {
        if (index % 2 == 0) {
            // Even index = key. Validation in JsonNamesMap guarantees the key
            // serializer is exactly String.serializer(), so the runtime type
            // is String.
            pendingKey = value as String
        } else {
            val k = pendingKey!!
            validateNoCollision(k)
            if (!composer.writingFirst) composer.print(COMMA)
            composer.nextItem()
            parent.encodeString(k)
            composer.print(COLON); composer.space()
            // Route value-encoding through the parent's encodeSerializableValue
            // so polymorphic-discriminator setup and the JsonElement short-circuit
            // both apply.
            parent.encodeSerializableValue(serializer, value)
            pendingKey = null
        }
    }

    private fun validateNoCollision(k: String) {
        if (k in declaredNames) {
            throw JsonEncodingException(
                "@JsonExtraKeys map in '${parentDescriptor.serialName}' contains key '$k' " +
                    "which conflicts with a declared property name.",
                classSerialName = parentDescriptor.serialName
            )
        }
        if (k == activeDiscriminator) {
            throw JsonEncodingException(
                "@JsonExtraKeys map in '${parentDescriptor.serialName}' contains key '$k' " +
                    "which conflicts with the active class discriminator '$activeDiscriminator'.",
                classSerialName = parentDescriptor.serialName
            )
        }
    }
}
