/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.hocon

import com.typesafe.config.*
import kotlin.time.*
import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.modules.*

@ExperimentalSerializationApi
internal abstract class AbstractHoconEncoder(
    private val hocon: Hocon,
    private val valueConsumer: (ConfigValue) -> Unit,
) : NamedValueEncoder() {

    override val serializersModule: SerializersModule
        get() = hocon.serializersModule

    private var writeDiscriminator: Boolean = false

    override fun elementName(descriptor: SerialDescriptor, index: Int): String {
        return descriptor.getConventionElementName(index, hocon.useConfigNamingConvention)
    }

    override fun composeName(parentName: String, childName: String): String = childName

    protected abstract fun encodeTaggedConfigValue(tag: String, value: ConfigValue)
    protected abstract fun getCurrent(): ConfigValue

    override fun encodeTaggedValue(tag: String, value: Any) = encodeTaggedConfigValue(tag, configValueOf(value))
    override fun encodeTaggedNull(tag: String) = encodeTaggedConfigValue(tag, configValueOf(null))
    override fun encodeTaggedChar(tag: String, value: Char) = encodeTaggedString(tag, value.toString())

    override fun encodeTaggedEnum(tag: String, enumDescriptor: SerialDescriptor, ordinal: Int) {
        encodeTaggedString(tag, enumDescriptor.getElementName(ordinal))
    }

    override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean = hocon.encodeDefaults

    override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
        when {
            serializer.descriptor == Duration.serializer().descriptor -> encodeDuration(value as Duration)
            serializer !is AbstractPolymorphicSerializer<*> || hocon.useArrayPolymorphism -> serializer.serialize(this, value)
            else -> {
                @Suppress("UNCHECKED_CAST")
                val casted = serializer as AbstractPolymorphicSerializer<Any>
                val actualSerializer = casted.findPolymorphicSerializer(this, value as Any)
                writeDiscriminator = true

                actualSerializer.serialize(this, value)
            }
        }
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
        val consumer =
            if (currentTagOrNull == null) valueConsumer
            else { value -> encodeTaggedConfigValue(currentTag, value) }
        val kind = descriptor.hoconKind(hocon.useArrayPolymorphism)

        return when {
            kind.listLike -> HoconConfigListEncoder(hocon, consumer)
            kind.objLike -> HoconConfigEncoder(hocon, consumer)
            kind == StructureKind.MAP -> HoconConfigMapEncoder(hocon, consumer)
            else -> this
        }.also { encoder ->
            if (writeDiscriminator) {
                encoder.encodeTaggedString(hocon.classDiscriminator, descriptor.serialName)
                writeDiscriminator = false
            }
        }
    }

    override fun endEncode(descriptor: SerialDescriptor) {
        valueConsumer(getCurrent())
    }

    private fun configValueOf(value: Any?) = ConfigValueFactory.fromAnyRef(value)

    private fun encodeDuration(value: Duration) {
        val result = value.toComponents { seconds, nanoseconds ->
            when {
                nanoseconds == 0 -> {
                    if (seconds % 60 == 0L) { // minutes
                        if (seconds % 3600 == 0L) { // hours
                            if (seconds % 86400 == 0L) { // days
                                "${seconds / 86400} d"
                            } else {
                                "${seconds / 3600} h"
                            }
                        } else {
                            "${seconds / 60} m"
                        }
                    } else {
                        "$seconds s"
                    }
                }
                nanoseconds % 1_000_000 == 0 -> "${seconds * 1_000 + nanoseconds / 1_000_000} ms"
                nanoseconds % 1_000 == 0 -> "${seconds * 1_000_000 + nanoseconds / 1_000} us"
                else -> "${value.inWholeNanoseconds} ns"
            }
        }
        encodeString(result)
    }
}

@ExperimentalSerializationApi
internal class HoconConfigEncoder(hocon: Hocon, configConsumer: (ConfigValue) -> Unit) :
    AbstractHoconEncoder(hocon, configConsumer) {

    private val configMap = mutableMapOf<String, ConfigValue>()

    override fun encodeTaggedConfigValue(tag: String, value: ConfigValue) {
        configMap[tag] = value
    }

    override fun getCurrent(): ConfigValue = ConfigValueFactory.fromMap(configMap)
}

@ExperimentalSerializationApi
internal class HoconConfigListEncoder(hocon: Hocon, configConsumer: (ConfigValue) -> Unit) :
    AbstractHoconEncoder(hocon, configConsumer) {

    private val values = mutableListOf<ConfigValue>()

    override fun elementName(descriptor: SerialDescriptor, index: Int): String = index.toString()

    override fun encodeTaggedConfigValue(tag: String, value: ConfigValue) {
        values.add(tag.toInt(), value)
    }

    override fun getCurrent(): ConfigValue = ConfigValueFactory.fromIterable(values)
}

@ExperimentalSerializationApi
internal class HoconConfigMapEncoder(hocon: Hocon, configConsumer: (ConfigValue) -> Unit) :
    AbstractHoconEncoder(hocon, configConsumer) {

    private val configMap = mutableMapOf<String, ConfigValue>()

    private lateinit var key: String
    private var isKey: Boolean = true

    override fun encodeTaggedConfigValue(tag: String, value: ConfigValue) {
        if (isKey) {
            key = when (value.valueType()) {
                ConfigValueType.OBJECT, ConfigValueType.LIST -> throw InvalidKeyKindException(value)
                else -> value.unwrappedNullable().toString()
            }
            isKey = false
        } else {
            configMap[key] = value
            isKey = true
        }
    }

    override fun getCurrent(): ConfigValue = ConfigValueFactory.fromMap(configMap)

    // Without cast to `Any?` Kotlin will assume unwrapped value as non-nullable by default
    // and will call `Any.toString()` instead of extension-function `Any?.toString()`.
    // We can't cast value in place using `(value.unwrapped() as Any?).toString()` because of warning "No cast needed".
    private fun ConfigValue.unwrappedNullable(): Any? = unwrapped()
}
