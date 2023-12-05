/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.hocon

import com.typesafe.config.*
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.encoding.CompositeDecoder.Companion.DECODE_DONE
import kotlinx.serialization.hocon.internal.SuppressAnimalSniffer
import kotlinx.serialization.hocon.internal.*
import kotlinx.serialization.hocon.serializers.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.modules.*
import kotlin.time.*

/**
 * Allows [deserialization][decodeFromConfig]
 * of [Config] object from popular Lightbend/config library into Kotlin objects.
 *
 * [Config] object represents "Human-Optimized Config Object Notation" —
 * [HOCON][https://github.com/lightbend/config#using-hocon-the-json-superset].
 *
 * [Duration] objects are encoded/decoded using "HOCON duration format" -
 * [Duration format][https://github.com/lightbend/config/blob/main/HOCON.md#duration-format]
 * [Duration] objects encoded using time unit short names: d, h, m, s, ms, us, ns.
 * Encoding use the largest time unit.
 * Example:
 *      120.seconds -> 2 m
 *      121.seconds -> 121 s
 *      120.minutes -> 2 h
 *      122.minutes -> 122 m
 *      24.hours -> 1 d
 * All restrictions on the maximum and minimum duration are specified in [Duration].
 *
 * It is also possible to encode and decode [java.time.Duration] and [com.typesafe.config.ConfigMemorySize]
 * with provided serializers: [JavaDurationSerializer] and [ConfigMemorySizeSerializer].
 * Because these types are not @[Serializable] by default,
 * one has to apply these serializers manually — either via @Serializable(with=...) / @file:UseSerializers
 * or using [Contextual] and [SerializersModule] mechanisms.
 *
 * @param [useConfigNamingConvention] switches naming resolution to config naming convention (hyphen separated).
 * @param serializersModule A [SerializersModule] which should contain registered serializers
 * for [Contextual] and [Polymorphic] serialization, if you have any.
 */
@ExperimentalSerializationApi
public sealed class Hocon(
    internal val encodeDefaults: Boolean,
    internal val useConfigNamingConvention: Boolean,
    internal val useArrayPolymorphism: Boolean,
    internal val classDiscriminator: String,
    override val serializersModule: SerializersModule,
) : SerialFormat {

    /**
     * Decodes the given [config] into a value of type [T] using the given serializer.
     */
    @ExperimentalSerializationApi
    public fun <T> decodeFromConfig(deserializer: DeserializationStrategy<T>, config: Config): T =
        ConfigReader(config).decodeSerializableValue(deserializer)

    /**
     * Encodes the given [value] into a [Config] using the given [serializer].
     * @throws SerializationException If list or primitive type passed as a [value].
     */
    @ExperimentalSerializationApi
    public fun <T> encodeToConfig(serializer: SerializationStrategy<T>, value: T): Config {
        lateinit var configValue: ConfigValue
        val encoder = HoconConfigEncoder(this) { configValue = it }
        encoder.encodeSerializableValue(serializer, value)

        if (configValue !is ConfigObject) {
            throw SerializationException(
                "Value of type '${configValue.valueType()}' can't be used at the root of HOCON Config. " +
                        "It should be either object or map."
            )
        }
        return (configValue as ConfigObject).toConfig()
    }

    /**
     * The default instance of Hocon parser.
     */
    @ExperimentalSerializationApi
    public companion object Default : Hocon(false, false, false, "type", EmptySerializersModule())

    private abstract inner class ConfigConverter<T> : TaggedDecoder<T>(), HoconDecoder {
        override val serializersModule: SerializersModule
            get() = this@Hocon.serializersModule

        abstract fun <E> getValueFromTaggedConfig(tag: T, valueResolver: (Config, String) -> E): E

        private inline fun <reified E : Any> validateAndCast(tag: T): E {
            return try {
                when (E::class) {
                    Number::class -> getValueFromTaggedConfig(tag) { config, path -> config.getNumber(path) } as E
                    Boolean::class -> getValueFromTaggedConfig(tag) { config, path -> config.getBoolean(path) } as E
                    String::class -> getValueFromTaggedConfig(tag) { config, path -> config.getString(path) } as E
                    else -> getValueFromTaggedConfig(tag) { config, path -> config.getAnyRef(path) } as E
                }
            } catch (e: ConfigException) {
                val configOrigin = e.origin()
                throw ConfigValueTypeCastException<E>(configOrigin)
            }
        }

        private fun getTaggedNumber(tag: T) = validateAndCast<Number>(tag)

        @SuppressAnimalSniffer
        protected fun <E> decodeDuration(tag: T): E {
            @Suppress("UNCHECKED_CAST")
            return getValueFromTaggedConfig(tag) { conf, path -> conf.decodeJavaDuration(path).toKotlinDuration() } as E
        }

        override fun decodeTaggedString(tag: T) = validateAndCast<String>(tag)

        override fun decodeTaggedBoolean(tag: T) = validateAndCast<Boolean>(tag)
        override fun decodeTaggedByte(tag: T): Byte = getTaggedNumber(tag).toByte()
        override fun decodeTaggedShort(tag: T): Short = getTaggedNumber(tag).toShort()
        override fun decodeTaggedInt(tag: T): Int = getTaggedNumber(tag).toInt()
        override fun decodeTaggedLong(tag: T): Long = getTaggedNumber(tag).toLong()
        override fun decodeTaggedFloat(tag: T): Float = getTaggedNumber(tag).toFloat()
        override fun decodeTaggedDouble(tag: T): Double = getTaggedNumber(tag).toDouble()

        override fun decodeTaggedChar(tag: T): Char {
            val s = validateAndCast<String>(tag)
            if (s.length != 1) throw SerializationException("String \"$s\" is not convertible to Char")
            return s[0]
        }

        override fun decodeTaggedValue(tag: T): Any = getValueFromTaggedConfig(tag) { c, s -> c.getAnyRef(s) }

        override fun decodeTaggedNotNullMark(tag: T) = getValueFromTaggedConfig(tag) { c, s -> !c.getIsNull(s) }

        override fun decodeTaggedEnum(tag: T, enumDescriptor: SerialDescriptor): Int {
            val s = validateAndCast<String>(tag)
            return enumDescriptor.getElementIndexOrThrow(s)
        }

        override fun <E> decodeConfigValue(extractValueAtPath: (Config, String) -> E): E =
            getValueFromTaggedConfig(currentTag, extractValueAtPath)

    }

    private inner class ConfigReader(val conf: Config, private val isPolymorphic: Boolean = false) : ConfigConverter<String>() {
        private var ind = -1

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            while (++ind < descriptor.elementsCount) {
                val name = descriptor.getTag(ind)
                if (conf.hasPathOrNull(name)) {
                    return ind
                }
            }
            return DECODE_DONE
        }

        private fun composeName(parentName: String, childName: String) =
            if (parentName.isEmpty()) childName else "$parentName.$childName"

        override fun SerialDescriptor.getTag(index: Int): String {
            val conventionName = getConventionElementName(index, useConfigNamingConvention)
            return if (!isPolymorphic) composeName(currentTagOrNull.orEmpty(), conventionName) else conventionName
        }

        override fun decodeNotNullMark(): Boolean {
            // Tag might be null for top-level deserialization
            val currentTag = currentTagOrNull ?: return !conf.isEmpty
            return decodeTaggedNotNullMark(currentTag)
        }

        override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
            return when {
                deserializer.descriptor.isDuration -> decodeDuration(currentTag)
                deserializer !is AbstractPolymorphicSerializer<*> || useArrayPolymorphism -> deserializer.deserialize(this)
                else -> {
                    val config = if (currentTagOrNull != null) conf.getConfig(currentTag) else conf

                    val reader = ConfigReader(config)
                    val type = reader.decodeTaggedString(classDiscriminator)
                    val actualSerializer = deserializer.findPolymorphicSerializerOrNull(reader, type)
                        ?: throw SerializerNotFoundException(type)

                    @Suppress("UNCHECKED_CAST")
                    (actualSerializer as DeserializationStrategy<T>).deserialize(reader)
                }
            }
        }

        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
            val kind = descriptor.hoconKind(useArrayPolymorphism)

            return when {
                kind.listLike -> ListConfigReader(conf.getList(currentTag))
                kind.objLike -> if (ind > -1) ConfigReader(conf.getConfig(currentTag)) else this
                kind == StructureKind.MAP ->
                    // if current tag is null - map in the root of config
                    MapConfigReader(if (currentTagOrNull != null) conf.getObject(currentTag) else conf.root())
                else -> this
            }
        }

        override fun <E> getValueFromTaggedConfig(tag: String, valueResolver: (Config, String) -> E): E {
            return valueResolver(conf, tag)
        }
    }

    private inner class PolymorphConfigReader(private val conf: Config) : ConfigConverter<String>() {
        private var ind = -1

        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
            when {
                descriptor.kind.objLike -> ConfigReader(conf, isPolymorphic = true)
                else -> this
            }

        override fun SerialDescriptor.getTag(index: Int): String = getElementName(index)

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            ind++
            return if (ind >= descriptor.elementsCount) DECODE_DONE else ind
        }

        override fun <E> getValueFromTaggedConfig(tag: String, valueResolver: (Config, String) -> E): E {
            return valueResolver(conf, tag)
        }
    }

    private inner class ListConfigReader(private val list: ConfigList) : ConfigConverter<Int>() {
        private var ind = -1

        override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T = when {
            deserializer.descriptor.isDuration -> decodeDuration(ind)
            else -> super.decodeSerializableValue(deserializer)
        }

        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
            when {
                descriptor.kind is PolymorphicKind -> PolymorphConfigReader((list[currentTag] as ConfigObject).toConfig())
                descriptor.kind.listLike -> ListConfigReader(list[currentTag] as ConfigList)
                descriptor.kind.objLike -> ConfigReader((list[currentTag] as ConfigObject).toConfig())
                descriptor.kind == StructureKind.MAP -> MapConfigReader(list[currentTag] as ConfigObject)
                else -> this
            }

        override fun SerialDescriptor.getTag(index: Int) = index

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            ind++
            return if (ind > list.size - 1) DECODE_DONE else ind
        }

        override fun <E> getValueFromTaggedConfig(tag: Int, valueResolver: (Config, String) -> E): E {
            val tagString = tag.toString()
            val configValue = valueResolver(list[tag].atKey(tagString), tagString)
            return configValue
        }
    }

    private inner class MapConfigReader(map: ConfigObject) : ConfigConverter<Int>() {
        private var ind = -1
        private val keys: List<String>
        private val values: List<ConfigValue>

        init {
            val entries = map.entries.toList() // to fix traversal order
            keys = entries.map(MutableMap.MutableEntry<String, ConfigValue>::key)
            values = entries.map(MutableMap.MutableEntry<String, ConfigValue>::value)
        }

        private val indexSize = values.size * 2

        override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T = when {
            deserializer.descriptor.isDuration -> decodeDuration(ind)
            else -> super.decodeSerializableValue(deserializer)
        }

        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
            when {
                descriptor.kind is PolymorphicKind -> PolymorphConfigReader((values[currentTag / 2] as ConfigObject).toConfig())
                descriptor.kind.listLike -> ListConfigReader(values[currentTag / 2] as ConfigList)
                descriptor.kind.objLike -> ConfigReader((values[currentTag / 2] as ConfigObject).toConfig())
                descriptor.kind == StructureKind.MAP -> MapConfigReader(values[currentTag / 2] as ConfigObject)
                else -> this
            }

        override fun SerialDescriptor.getTag(index: Int) = index

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            ind++
            return if (ind >= indexSize) DECODE_DONE else ind
        }

        override fun <E> getValueFromTaggedConfig(tag: Int, valueResolver: (Config, String) -> E): E {
            val idx = tag / 2
            val tagString = tag.toString()
            val configValue = if (tag % 2 == 0) { // entry as string
                ConfigValueFactory.fromAnyRef(keys[idx]).atKey(tagString)
            } else {
                val configValue = values[idx]
                configValue.atKey(tagString)
            }
            return valueResolver(configValue, tagString)
        }
    }

    private fun SerialDescriptor.getElementIndexOrThrow(name: String): Int {
        val index = getElementIndex(name)
        if (index == CompositeDecoder.UNKNOWN_NAME)
            throw SerializationException("$serialName does not contain element with name '$name'")
        return index
    }
}

/**
 * Decodes the given [config] into a value of type [T] using a deserializer retrieved
 * from the reified type parameter.
 */
@ExperimentalSerializationApi
public inline fun <reified T> Hocon.decodeFromConfig(config: Config): T =
    decodeFromConfig(serializersModule.serializer(), config)

/**
 * Encodes the given [value] of type [T] into a [Config] using a serializer retrieved
 * from the reified type parameter.
 */
@ExperimentalSerializationApi
public inline fun <reified T> Hocon.encodeToConfig(value: T): Config =
    encodeToConfig(serializersModule.serializer(), value)

/**
 * Creates an instance of [Hocon] configured from the optionally given [Hocon instance][from]
 * and adjusted with [builderAction].
 */
@ExperimentalSerializationApi
public fun Hocon(from: Hocon = Hocon, builderAction: HoconBuilder.() -> Unit): Hocon {
    return HoconImpl(HoconBuilder(from).apply(builderAction))
}

/**
 * Builder of the [Hocon] instance provided by `Hocon` factory function.
 */
@ExperimentalSerializationApi
public class HoconBuilder internal constructor(hocon: Hocon) {
    /**
     * Module with contextual and polymorphic serializers to be used in the resulting [Hocon] instance.
     */
    public var serializersModule: SerializersModule = hocon.serializersModule

    /**
     * Specifies whether default values of Kotlin properties should be encoded.
     * `false` by default.
     */
    public var encodeDefaults: Boolean = hocon.encodeDefaults

    /**
     * Switches naming resolution to config naming convention: hyphen separated.
     */
    public var useConfigNamingConvention: Boolean = hocon.useConfigNamingConvention

    /**
     * Switches polymorphic serialization to the default array format.
     * This is an option for legacy polymorphism format and should not be generally used.
     * `false` by default.
     */
    public var useArrayPolymorphism: Boolean = hocon.useArrayPolymorphism

    /**
     * Name of the class descriptor property for polymorphic serialization.
     * "type" by default.
     */
    public var classDiscriminator: String = hocon.classDiscriminator
}

@OptIn(ExperimentalSerializationApi::class)
private class HoconImpl(hoconBuilder: HoconBuilder) : Hocon(
    encodeDefaults = hoconBuilder.encodeDefaults,
    useConfigNamingConvention = hoconBuilder.useConfigNamingConvention,
    useArrayPolymorphism = hoconBuilder.useArrayPolymorphism,
    classDiscriminator = hoconBuilder.classDiscriminator,
    serializersModule = hoconBuilder.serializersModule
)
