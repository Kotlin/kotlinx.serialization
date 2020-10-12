/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.hocon

import com.typesafe.config.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.encoding.CompositeDecoder.Companion.DECODE_DONE
import kotlinx.serialization.internal.*
import kotlinx.serialization.modules.*

/**
 * Allows [deserialization][decodeFromConfig]
 * of [Config] object from popular Lightbend/config library into Kotlin objects.
 *
 * [Config] object represents "Human-Optimized Config Object Notation" —
 * [HOCON][https://github.com/lightbend/config#using-hocon-the-json-superset].
 *
 * @param [useConfigNamingConvention] switches naming resolution to config naming convention (hyphen separated).
 * @param serializersModule A [SerializersModule] which should contain registered serializers
 * for [Contextual] and [Polymorphic] serialization, if you have any.
 */
@ExperimentalSerializationApi
public sealed class Hocon(
        internal val useConfigNamingConvention: Boolean,
        internal val useArrayPolymorphism: Boolean,
        internal val classDiscriminator: String,
        override val serializersModule: SerializersModule
) : SerialFormat {

    @ExperimentalSerializationApi
    public fun <T> decodeFromConfig(deserializer: DeserializationStrategy<T>, config: Config): T =
        ConfigReader(config).decodeSerializableValue(deserializer)

    /**
     * The default instance of Hocon parser.
     */
    @ExperimentalSerializationApi
    public companion object Default : Hocon(false, false, "type", EmptySerializersModule) {
        private val NAMING_CONVENTION_REGEX by lazy { "[A-Z]".toRegex() }
    }

    private abstract inner class ConfigConverter<T> : TaggedDecoder<T>() {
        override val serializersModule: SerializersModule
            get() = this@Hocon.serializersModule

        abstract fun getTaggedConfigValue(tag: T): ConfigValue

        private inline fun <reified E : Any> validateAndCast(tag: T, wrappedType: ConfigValueType): E {
            val cfValue = getTaggedConfigValue(tag)
            if (cfValue.valueType() != wrappedType) throw SerializationException("${cfValue.origin().description()} required to be a $wrappedType")
            return cfValue.unwrapped() as E
        }

        private fun getTaggedNumber(tag: T) = validateAndCast<Number>(tag, ConfigValueType.NUMBER)

        override fun decodeTaggedString(tag: T) = validateAndCast<String>(tag, ConfigValueType.STRING)

        override fun decodeTaggedByte(tag: T): Byte = getTaggedNumber(tag).toByte()
        override fun decodeTaggedShort(tag: T): Short = getTaggedNumber(tag).toShort()
        override fun decodeTaggedInt(tag: T): Int = getTaggedNumber(tag).toInt()
        override fun decodeTaggedLong(tag: T): Long = getTaggedNumber(tag).toLong()
        override fun decodeTaggedFloat(tag: T): Float = getTaggedNumber(tag).toFloat()
        override fun decodeTaggedDouble(tag: T): Double = getTaggedNumber(tag).toDouble()

        override fun decodeTaggedChar(tag: T): Char {
            val s = validateAndCast<String>(tag, ConfigValueType.STRING)
            if (s.length != 1) throw SerializationException("String \"$s\" is not convertible to Char")
            return s[0]
        }

        override fun decodeTaggedValue(tag: T): Any = getTaggedConfigValue(tag).unwrapped()

        override fun decodeTaggedNotNullMark(tag: T) = getTaggedConfigValue(tag).valueType() != ConfigValueType.NULL

        override fun decodeTaggedEnum(tag: T, enumDescriptor: SerialDescriptor): Int {
            val s = validateAndCast<String>(tag, ConfigValueType.STRING)
            return enumDescriptor.getElementIndexOrThrow(s)
        }
    }

    private inner class ConfigReader(val conf: Config) : ConfigConverter<String>() {
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

        override fun SerialDescriptor.getTag(index: Int): String =
            composeName(currentTagOrNull ?: "", getConventionElementName(index))

        private fun SerialDescriptor.getConventionElementName(index: Int): String {
            val originalName = getElementName(index)
            return if (!useConfigNamingConvention) originalName
            else originalName.replace(NAMING_CONVENTION_REGEX) { "-${it.value.toLowerCase()}" }
        }

        override fun getTaggedConfigValue(tag: String): ConfigValue {
            return conf.getValue(tag)
        }

        override fun decodeTaggedNotNullMark(tag: String): Boolean {
            return !conf.getIsNull(tag)
        }

        override fun decodeNotNullMark(): Boolean {
            // Tag might be null for top-level deserialization
            val currentTag = currentTagOrNull ?: return !conf.isEmpty
            return decodeTaggedNotNullMark(currentTag)
        }

        override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T {
            if (deserializer !is AbstractPolymorphicSerializer<*> || useArrayPolymorphism) {
                return deserializer.deserialize(this)
            }

            val config = if (currentTagOrNull != null) conf.getConfig(currentTag) else conf

            val reader = ConfigReader(config)
            val type = reader.decodeTaggedString(classDiscriminator)
            val actualSerializer = deserializer.findPolymorphicSerializerOrNull(reader, type)
                    ?: throwSerializerNotFound(type)

            @Suppress("UNCHECKED_CAST")
            return (actualSerializer as DeserializationStrategy<T>).deserialize(reader)
        }

        private fun throwSerializerNotFound(type: String?): Nothing {
            val suffix = if (type == null) "missing class discriminator ('null')" else "class discriminator '$type'"
            throw SerializationException("Polymorphic serializer was not found for $suffix")
        }

        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
            val kind = when (descriptor.kind) {
                is PolymorphicKind -> {
                    if (useArrayPolymorphism) StructureKind.LIST else StructureKind.MAP
                }
                else -> descriptor.kind
            }

            return when {
                kind.listLike -> ListConfigReader(conf.getList(currentTag))
                kind.objLike -> if (ind > -1) ConfigReader(conf.getConfig(currentTag)) else this
                kind == StructureKind.MAP ->
                    // if current tag is null - map in the root of config
                    MapConfigReader(if (currentTagOrNull != null) conf.getObject(currentTag) else conf.root())
                else -> this
            }
        }
    }

    private inner class ListConfigReader(private val list: ConfigList) : ConfigConverter<Int>() {
        private var ind = -1

        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
            when {
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

        override fun getTaggedConfigValue(tag: Int): ConfigValue = list[tag]
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

        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
            when {
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

        override fun getTaggedConfigValue(tag: Int): ConfigValue {
            val idx = tag / 2
            return if (tag % 2 == 0) { // entry as string
                ConfigValueFactory.fromAnyRef(keys[idx])
            } else {
                values[idx]
            }
        }
    }

    private fun SerialDescriptor.getElementIndexOrThrow(name: String): Int {
        val index = getElementIndex(name)
        if (index == CompositeDecoder.UNKNOWN_NAME)
            throw SerializationException("$serialName does not contain element with name '$name'")
        return index
    }

    private val SerialKind.listLike get() = this == StructureKind.LIST || this is PolymorphicKind
    private val SerialKind.objLike get() = this == StructureKind.CLASS || this == StructureKind.OBJECT
}

/**
 * Decodes the given [config] into a value of type [T] using a deserialize retrieved
 * from reified type parameter.
 */
@ExperimentalSerializationApi
public inline fun <reified T> Hocon.decodeFromConfig(config: Config): T =
    decodeFromConfig(serializersModule.serializer(), config)

/**
 * Creates an instance of [Hocon] configured from the optionally given [Hocon instance][from]
 * and adjusted with [builderAction].
 */
@ExperimentalSerializationApi
public fun Hocon(from: Hocon = Hocon, builderAction: HoconBuilder.() -> Unit): Hocon {
    val builder = HoconBuilder(from)
    builder.builderAction()
    return HoconImpl(builder.useConfigNamingConvention, builder.useArrayPolymorphism, builder.classDiscriminator, builder.serializersModule)
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
private class HoconImpl(
        useConfigNamingConvention: Boolean,
        useArrayPolymorphism: Boolean,
        classDiscriminator: String,
        serializersModule: SerializersModule
) : Hocon(useConfigNamingConvention, useArrayPolymorphism, classDiscriminator, serializersModule)
