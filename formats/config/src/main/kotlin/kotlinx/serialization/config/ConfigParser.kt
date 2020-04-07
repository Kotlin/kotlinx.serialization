/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.config

import com.typesafe.config.*
import kotlinx.serialization.*
import kotlinx.serialization.CompositeDecoder.Companion.READ_DONE
import kotlinx.serialization.internal.*
import kotlinx.serialization.modules.*

private val SerialKind.listLike get() = this == StructureKind.LIST || this is PolymorphicKind
private val SerialKind.objLike get() = this == StructureKind.CLASS || this == StructureKind.OBJECT

public class ConfigParser(
    private val configuration: ConfigParserConfiguration = ConfigParserConfiguration(),
    override val context: SerialModule = EmptyModule
) : SerialFormat {
    @ImplicitReflectionSerializer
    public inline fun <reified T : Any> parse(conf: Config): T = parse(conf, context.getContextualOrDefault(T::class))

    public fun <T> parse(conf: Config, deserializer: DeserializationStrategy<T>): T =
        ConfigDecoder(conf).decode(deserializer)

    public interface ConfigInput : Decoder, CompositeDecoder {}


    private abstract inner class ConfigConverter<T> : TaggedDecoder<T>() {
        override val context: SerialModule
            get() = this@ConfigParser.context

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

        override fun decodeTaggedUnit(tag: T) = Unit

        override fun decodeTaggedChar(tag: T): Char {
            val s = validateAndCast<String>(tag, ConfigValueType.STRING)
            if (s.length != 1) throw SerializationException("String \"$s\" is not convertible to Char")
            return s[0]
        }

        override fun decodeTaggedValue(tag: T): Any = getTaggedConfigValue(tag).unwrapped()

        override fun decodeTaggedNotNullMark(tag: T) = getTaggedConfigValue(tag).valueType() != ConfigValueType.NULL

        override fun decodeTaggedEnum(tag: T, enumDescription: SerialDescriptor): Int {
            val s = validateAndCast<String>(tag, ConfigValueType.STRING)
            return enumDescription.getElementIndexOrThrow(s)
        }
    }

    private inner class ConfigDecoder(val conf: Config) : ConfigConverter<String>(), ConfigInput {
        private var ind = -1

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            while (++ind < descriptor.elementsCount) {
                val name = descriptor.getTag(ind)
                if (conf.hasPathOrNull(name)) {
                    return ind
                }
            }
            return READ_DONE
        }

        private fun composeName(parentName: String, childName: String) =
            if (parentName.isEmpty()) childName else parentName + "." + childName

        override fun SerialDescriptor.getTag(index: Int): String =
            composeName(currentTagOrNull ?: "", getConventionElementName(index))

        private fun SerialDescriptor.getConventionElementName(index: Int): String {
            val originalName = getElementName(index)
            return if (!configuration.useConfigNamingConvention) originalName
            else originalName.replace(NAMING_CONVENTION_REGEX) { "-${it.value.toLowerCase()}" }
        }

        override fun getTaggedConfigValue(tag: String): ConfigValue {
            return conf.getValue(tag)
        }

        override fun decodeTaggedNotNullMark(tag: String): Boolean {
            return !conf.getIsNull(tag)
        }

        override fun beginStructure(descriptor: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder =
            when {
                descriptor.kind.listLike -> ListConfigDecoder(conf.getList(currentTag))
                descriptor.kind.objLike -> if (ind > -1) ConfigDecoder(conf.getConfig(currentTag)) else this
                descriptor.kind == StructureKind.MAP -> MapConfigDecoder(conf.getObject(currentTag))
                else -> this
            }
    }

    private inner class ListConfigDecoder(private val list: ConfigList) : ConfigConverter<Int>() {
        private var ind = -1

        override fun beginStructure(descriptor: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder =
            when {
                descriptor.kind.listLike -> ListConfigDecoder(list[currentTag] as ConfigList)
                descriptor.kind.objLike -> ConfigDecoder((list[currentTag] as ConfigObject).toConfig())
                descriptor.kind == StructureKind.MAP -> MapConfigDecoder(list[currentTag] as ConfigObject)
                else -> this
            }

        override fun SerialDescriptor.getTag(index: Int) = index

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            ind++
            return if (ind > list.size - 1) READ_DONE else ind
        }

        override fun getTaggedConfigValue(tag: Int): ConfigValue = list[tag]
    }

    private inner class MapConfigDecoder(map: ConfigObject) : ConfigConverter<Int>() {

        private var ind = -1
        private val keys: List<String>
        private val values: List<ConfigValue>

        init {
            val entries = map.entries.toList() // to fix traversal order
            keys = entries.map(MutableMap.MutableEntry<String, ConfigValue>::key)
            values = entries.map(MutableMap.MutableEntry<String, ConfigValue>::value)
        }

        private val indexSize = values.size * 2

        override fun beginStructure(descriptor: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder =
            when {
                descriptor.kind.listLike -> ListConfigDecoder(values[currentTag / 2] as ConfigList)
                descriptor.kind.objLike -> ConfigDecoder((values[currentTag / 2] as ConfigObject).toConfig())
                descriptor.kind == StructureKind.MAP -> MapConfigDecoder(values[currentTag / 2] as ConfigObject)
                else -> this
            }

        override fun SerialDescriptor.getTag(index: Int) = index

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            ind++
            return if (ind >= indexSize) READ_DONE else ind
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

    @Suppress("UNUSED")
    companion object {
        public fun <T> parse(conf: Config, serial: DeserializationStrategy<T>): T = ConfigParser().parse(conf, serial)

        @ImplicitReflectionSerializer
        public inline fun <reified T : Any> parse(conf: Config): T = ConfigParser().parse(conf, T::class.serializer())

        private val NAMING_CONVENTION_REGEX by lazy { "[A-Z]".toRegex() }
    }
}
