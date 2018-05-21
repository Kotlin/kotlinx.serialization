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
import kotlinx.serialization.internal.KEY_INDEX

private val SerialKind.listLike get() = this == SerialKind.LIST || this == SerialKind.SET || this == SerialKind.POLYMORPHIC
private val SerialKind.objLike get() = this == SerialKind.CLASS || this == SerialKind.OBJECT || this == SerialKind.SEALED

class ConfigParser(val context: SerialContext? = null) {
    inline fun <reified T : Any> parse(conf: Config): T = parse(conf, context.klassSerializer(T::class))
    fun <T> parse(conf: Config, loader: DeserializationStrategy<T>): T = ConfigReader(conf).decode(loader)


    private abstract inner class ConfigConverter<T> : TaggedInput<T>() {
        init {
            this.context = this@ConfigParser.context
        }

        abstract fun getTaggedConfigValue(tag: T): ConfigValue

        private inline fun <reified E : Any> validateAndCast(tag: T, wrappedType: ConfigValueType): E {
            val cfValue = getTaggedConfigValue(tag)
            if (cfValue.valueType() != wrappedType) throw SerializationException("${cfValue.origin().description()} required to be a $wrappedType")
            return cfValue.unwrapped() as E
        }

        private fun getTaggedNumber(tag: T) = validateAndCast<Number>(tag, ConfigValueType.NUMBER)

        override fun readTaggedString(tag: T) = validateAndCast<String>(tag, ConfigValueType.STRING)

        override fun readTaggedByte(tag: T): Byte = getTaggedNumber(tag).toByte()
        override fun readTaggedShort(tag: T): Short = getTaggedNumber(tag).toShort()
        override fun readTaggedInt(tag: T): Int = getTaggedNumber(tag).toInt()
        override fun readTaggedLong(tag: T): Long = getTaggedNumber(tag).toLong()
        override fun readTaggedFloat(tag: T): Float = getTaggedNumber(tag).toFloat()
        override fun readTaggedDouble(tag: T): Double = getTaggedNumber(tag).toDouble()

        override fun readTaggedUnit(tag: T) = Unit

        override fun readTaggedChar(tag: T): Char {
            val s = validateAndCast<String>(tag, ConfigValueType.STRING)
            if (s.length != 1) throw SerializationException("String \"$s\" is not convertible to Char")
            return s[0]
        }

        override fun readTaggedValue(tag: T): Any = getTaggedConfigValue(tag).unwrapped()

        override fun readTaggedNotNullMark(tag: T) = getTaggedConfigValue(tag).valueType() != ConfigValueType.NULL

        override fun <E : Enum<E>> readTaggedEnum(tag: T, enumCreator: EnumCreator<E>): E {
            val s = validateAndCast<String>(tag, ConfigValueType.STRING)
            return enumCreator.createFromName(s)
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

        override fun readTaggedNotNullMark(tag: String): Boolean {
            return !conf.getIsNull(tag)
        }

        override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder = when {
            desc.kind.listLike -> ListConfigReader(conf.getList(currentTag))
            desc.kind == SerialKind.MAP -> MapConfigReader(conf.getObject(currentTag))
            else -> this
        }
    }

    private inner class ListConfigReader(private val list: ConfigList) : ConfigConverter<Int>() {
        private var ind = 0

        override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder = when {
            desc.kind.listLike -> ListConfigReader(list[currentTag] as ConfigList)
            desc.kind.objLike -> ConfigReader((list[currentTag] as ConfigObject).toConfig())
            desc.kind == SerialKind.MAP -> MapConfigReader(list[currentTag] as ConfigObject)
            else -> this
        }

        override fun SerialDescriptor.getTag(index: Int) = index - 1

        override fun decodeElementIndex(desc: SerialDescriptor): Int {
            ind++
            return if (ind > list.size) READ_DONE else ind
        }

        override fun getTaggedConfigValue(tag: Int): ConfigValue = list[tag]
    }

    private inner class MapConfigReader(map: ConfigObject) : ConfigConverter<Int>() {
        private var ind = 0
        private val entries = map.entries.toList()

        override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
            return when (desc.kind) {
                SerialKind.ENTRY -> MapEntryReader(entries[currentTag])
                else -> throw IllegalStateException("Map not from entries")
            }
        }

        override fun SerialDescriptor.getTag(index: Int) = index - 1

        override fun decodeElementIndex(desc: SerialDescriptor): Int {
            ind++
            return if (ind > entries.size) READ_DONE else ind
        }

        override fun getTaggedConfigValue(tag: Int): ConfigValue = throw IllegalStateException("Should read as entries")
    }

    private inner class MapEntryReader(val e: Map.Entry<String, ConfigValue>) : ConfigConverter<Int>() {
        override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder = when {
            desc.kind.listLike -> ListConfigReader(e.value as ConfigList)
            desc.kind.objLike -> ConfigReader((e.value as ConfigObject).toConfig())
            desc.kind == SerialKind.MAP -> MapConfigReader(e.value as ConfigObject)
            else -> this
        }

        override fun SerialDescriptor.getTag(index: Int) = index

        override fun getTaggedConfigValue(tag: Int): ConfigValue {
            return if (tag == KEY_INDEX) ConfigValueFactory.fromAnyRef(e.key)
            else e.value
        }
    }

    companion object {
        fun <T> parse(conf: Config, serial: DeserializationStrategy<T>) = ConfigParser().parse(conf, serial)
        inline fun <reified T : Any> parse(conf: Config) = ConfigParser().parse(conf, T::class.serializer())
    }
}
