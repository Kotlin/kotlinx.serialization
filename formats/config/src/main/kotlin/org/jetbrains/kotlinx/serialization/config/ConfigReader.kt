/*
 * Copyright 2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlinx.serialization.config

import com.typesafe.config.*
import kotlinx.serialization.*
import kotlinx.serialization.CompositeDecoder.Companion.READ_DONE
import kotlinx.serialization.modules.*
import kotlinx.serialization.internal.EnumDescriptor

private val SerialKind.listLike get() = this == StructureKind.LIST || this == UnionKind.POLYMORPHIC
private val SerialKind.objLike get() = this == StructureKind.CLASS || this == UnionKind.OBJECT || this == UnionKind.SEALED

class ConfigParser(context: SerialModule = EmptyModule): AbstractSerialFormat(context) {
    @ImplicitReflectionSerializer
    inline fun <reified T : Any> parse(conf: Config): T = parse(conf, context.getContextualOrDefault(T::class))

    fun <T> parse(conf: Config, deserializer: DeserializationStrategy<T>): T = ConfigReader(conf).decode(deserializer)


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

        override fun decodeTaggedEnum(tag: T, enumDescription: EnumDescriptor): Int {
            val s = validateAndCast<String>(tag, ConfigValueType.STRING)
            return enumDescription.getElementIndexOrThrow(s)
        }
    }

    private inner class ConfigReader(val conf: Config) : ConfigConverter<String>() {
        private fun composeName(parentName: String, childName: String) =
            if (parentName.isEmpty()) childName else parentName + "." + childName

        override fun SerialDescriptor.getTag(index: Int): String = composeName(
            currentTagOrNull
                    ?: "", getElementName(index)
        )

        override fun getTaggedConfigValue(tag: String): ConfigValue {
            return conf.getValue(tag)
        }

        override fun decodeTaggedNotNullMark(tag: String): Boolean {
            return !conf.getIsNull(tag)
        }

        override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder = when {
            desc.kind.listLike -> ListConfigReader(conf.getList(currentTag))
            desc.kind == StructureKind.MAP -> MapConfigReader(conf.getObject(currentTag))
            else -> this
        }
    }

    private inner class ListConfigReader(private val list: ConfigList) : ConfigConverter<Int>() {
        private var ind = -1

        override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder = when {
            desc.kind.listLike -> ListConfigReader(list[currentTag] as ConfigList)
            desc.kind.objLike -> ConfigReader((list[currentTag] as ConfigObject).toConfig())
            desc.kind == StructureKind.MAP -> MapConfigReader(list[currentTag] as ConfigObject)
            else -> this
        }

        override fun SerialDescriptor.getTag(index: Int) = index

        override fun decodeElementIndex(desc: SerialDescriptor): Int {
            ind++
            return if (ind > list.size - 1) READ_DONE else ind
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

        override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder =
            when {
                desc.kind.listLike -> ListConfigReader(values[currentTag / 2] as ConfigList)
                desc.kind.objLike -> ConfigReader((values[currentTag / 2] as ConfigObject).toConfig())
                desc.kind == StructureKind.MAP -> MapConfigReader(values[currentTag / 2] as ConfigObject)
                else -> this
        }

        override fun SerialDescriptor.getTag(index: Int) = index

        override fun decodeElementIndex(desc: SerialDescriptor): Int {
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

    companion object {
        fun <T> parse(conf: Config, serial: DeserializationStrategy<T>) = ConfigParser().parse(conf, serial)

        @ImplicitReflectionSerializer
        inline fun <reified T : Any> parse(conf: Config) = ConfigParser().parse(conf, T::class.serializer())
    }
}
