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

package kotlinx.serialization

import kotlinx.serialization.StructureDecoder.Companion.READ_ALL
import kotlin.reflect.KClass

open class ValueTransformer {
    // ------- top-level API (use it) -------

    fun <T> transform(serializer: KSerializer<T>, obj: T): T {
        val output = Output()
        output.encode(serializer, obj)
        val input = Input(output.list)
        return input.decode(serializer)
    }

    inline fun <reified T : Any> transform(obj: T): T = transform(T::class.serializer(), obj)

    // ------- override to define transformations -------

    open fun transformBooleanValue(desc: SerialDescriptor, index: Int, value: Boolean) = value
    open fun transformByteValue(desc: SerialDescriptor, index: Int, value: Byte) = value
    open fun transformShortValue(desc: SerialDescriptor, index: Int, value: Short) = value
    open fun transformIntValue(desc: SerialDescriptor, index: Int, value: Int) = value
    open fun transformLongValue(desc: SerialDescriptor, index: Int, value: Long) = value
    open fun transformFloatValue(desc: SerialDescriptor, index: Int, value: Float) = value
    open fun transformDoubleValue(desc: SerialDescriptor, index: Int, value: Double) = value
    open fun transformCharValue(desc: SerialDescriptor, index: Int, value: Char) = value
    open fun transformStringValue(desc: SerialDescriptor, index: Int, value: String) = value

    @Deprecated("Not supported in Native", replaceWith = ReplaceWith("transformEnumValue(enumLoader)"))
    open fun <T : Enum<T>> transformEnumValue(desc: SerialDescriptor, index: Int, enumClass: KClass<T>, value: T): T = value

    open fun <T : Enum<T>> transformEnumValue(desc: SerialDescriptor, index: Int, enumCreator: EnumCreator<T>, value: T): T = value

    open fun isRecursiveTransform(): Boolean = true

    // ---------------

    private inner class Output : StructureEncoder {
        override var context: SerialContext? = null

        internal val list = arrayListOf<Any?>()

        override fun encodeNullableValue(value: Any?) {
            list.add(value)
        }

        override fun encodeElement(desc: SerialDescriptor, index: Int) = true
        override fun encodeNotNullMark() {}
        override fun encodeNullValue() { encodeNullableValue(null) }
        override fun encodeAnyValue(value: Any) { encodeNullableValue(value) }
        override fun encodeUnitValue() { encodeNullableValue(Unit) }
        override fun encodeBooleanValue(value: Boolean) { encodeNullableValue(value) }
        override fun encodeByteValue(value: Byte) { encodeNullableValue(value) }
        override fun encodeShortValue(value: Short) { encodeNullableValue(value) }
        override fun encodeIntValue(value: Int) { encodeNullableValue(value) }
        override fun encodeLongValue(value: Long) { encodeNullableValue(value) }
        override fun encodeFloatValue(value: Float) { encodeNullableValue(value) }
        override fun encodeDoubleValue(value: Double) { encodeNullableValue(value) }
        override fun encodeCharValue(value: Char) { encodeNullableValue(value) }
        override fun encodeStringValue(value: String) { encodeNullableValue(value) }
        override fun <T : Enum<T>> encodeEnumValue(enumClass: KClass<T>, value: T) { encodeNullableValue(value) }
        override fun <T : Enum<T>> encodeEnumValue(value: T) {
            encodeNullableValue(value)
        }

        override fun <T : Any?> encodeSerializableValue(saver: SerializationStrategy<T>, value: T) {
            if (isRecursiveTransform()) {
                saver.serialize(this, value)
            } else
                encodeNullableValue(value)
        }

        // ---------------

        override fun encodeNonSerializableElementValue(desc: SerialDescriptor, index: Int, value: Any) { encodeNullableValue(value) }
        override fun encodeNullableElementValue(desc: SerialDescriptor, index: Int, value: Any?) = encodeNullableValue(value)
        override fun encodeUnitElementValue(desc: SerialDescriptor, index: Int) = encodeNullableValue(Unit)
        override fun encodeBooleanElementValue(desc: SerialDescriptor, index: Int, value: Boolean) = encodeNullableValue(value)
        override fun encodeByteElementValue(desc: SerialDescriptor, index: Int, value: Byte) = encodeNullableValue(value)
        override fun encodeShortElementValue(desc: SerialDescriptor, index: Int, value: Short) = encodeNullableValue(value)
        override fun encodeIntElementValue(desc: SerialDescriptor, index: Int, value: Int) = encodeNullableValue(value)
        override fun encodeLongElementValue(desc: SerialDescriptor, index: Int, value: Long) = encodeNullableValue(value)
        override fun encodeFloatElementValue(desc: SerialDescriptor, index: Int, value: Float) = encodeNullableValue(value)
        override fun encodeDoubleElementValue(desc: SerialDescriptor, index: Int, value: Double) = encodeNullableValue(value)
        override fun encodeCharElementValue(desc: SerialDescriptor, index: Int, value: Char) = encodeNullableValue(value)
        override fun encodeStringElementValue(desc: SerialDescriptor, index: Int, value: String) = encodeNullableValue(value)

        override fun <T : Enum<T>> encodeEnumElementValue(desc: SerialDescriptor, index: Int, enumClass: KClass<T>, value: T) =
            encodeNullableValue(value)

        override fun <T : Enum<T>> encodeEnumElementValue(desc: SerialDescriptor, index: Int, value: T) {
            encodeNullableValue(value)
        }
    }

    private inner class Input(private val list: List<Any?>) : StructureDecoder {
        override var context: SerialContext? = null
        override val updateMode: UpdateMode = UpdateMode.BANNED

        private var index = 0
        private var curDesc: SerialDescriptor? = null
        private var curIndex: Int = 0

        private fun cur(desc: SerialDescriptor, index: Int) {
            curDesc = desc
            curIndex = index
        }

        override fun decodeNotNullMark(): Boolean = list[index] != null
        override fun decodeNullValue(): Nothing? { index++; return null }
        override fun decodeAnyValue(): Any = list[index++]!!
        override fun decodeNullableValue(): Any? = list[index++]
        override fun decodeUnitValue(): Unit { index++ }

        override fun decodeBooleanValue(): Boolean = transformBooleanValue(curDesc!!, curIndex, decodeAnyValue() as Boolean)
        override fun decodeByteValue(): Byte = transformByteValue(curDesc!!, curIndex, decodeAnyValue() as Byte)
        override fun decodeShortValue(): Short = transformShortValue(curDesc!!, curIndex, decodeAnyValue() as Short)
        override fun decodeIntValue(): Int = transformIntValue(curDesc!!, curIndex, decodeAnyValue() as Int)
        override fun decodeLongValue(): Long = transformLongValue(curDesc!!, curIndex, decodeAnyValue() as Long)
        override fun decodeFloatValue(): Float = transformFloatValue(curDesc!!, curIndex, decodeAnyValue() as Float)
        override fun decodeDoubleValue(): Double = transformDoubleValue(curDesc!!, curIndex, decodeAnyValue() as Double)
        override fun decodeCharValue(): Char = transformCharValue(curDesc!!, curIndex, decodeAnyValue() as Char)
        override fun decodeStringValue(): String = transformStringValue(curDesc!!, curIndex, decodeAnyValue() as String)

        @Suppress("UNCHECKED_CAST")
        @Deprecated("Not supported in Native", replaceWith = ReplaceWith("decodeEnumValue(enumLoader)"))
        override fun <T : Enum<T>> decodeEnumValue(enumClass: KClass<T>): T =
            transformEnumValue(curDesc!!, curIndex, enumClass, decodeAnyValue() as T)

        @Suppress("UNCHECKED_CAST")
        override fun <T : Enum<T>> decodeEnumValue(enumCreator: EnumCreator<T>): T =
            transformEnumValue(curDesc!!, curIndex, enumCreator, decodeAnyValue() as T)

        @Suppress("UNCHECKED_CAST")
        override fun <T : Any?> decodeSerializableValue(loader: DeserializationStrategy<T>): T {
            if (isRecursiveTransform())
                return loader.deserialize(this)
            else
                return decodeAnyValue() as T
        }

        // ---------------

        override fun decodeElement(desc: SerialDescriptor): Int = READ_ALL

        override fun decodeAnyElementValue(desc: SerialDescriptor, index: Int): Any {
            cur(desc, index); return decodeAnyValue()
        }

        override fun decodeNullableElementValue(desc: SerialDescriptor, index: Int): Any? {
            cur(desc, index); return decodeNullableValue()
        }
        override fun decodeUnitElementValue(desc: SerialDescriptor, index: Int) { cur(desc, index); return decodeUnitValue() }
        override fun decodeBooleanElementValue(desc: SerialDescriptor, index: Int): Boolean { cur(desc, index); return decodeBooleanValue() }
        override fun decodeByteElementValue(desc: SerialDescriptor, index: Int): Byte { cur(desc, index); return decodeByteValue() }
        override fun decodeShortElementValue(desc: SerialDescriptor, index: Int): Short { cur(desc, index); return decodeShortValue() }
        override fun decodeIntElementValue(desc: SerialDescriptor, index: Int): Int { cur(desc, index); return decodeIntValue() }
        override fun decodeLongElementValue(desc: SerialDescriptor, index: Int): Long { cur(desc, index); return decodeLongValue() }
        override fun decodeFloatElementValue(desc: SerialDescriptor, index: Int): Float { cur(desc, index); return decodeFloatValue() }
        override fun decodeDoubleElementValue(desc: SerialDescriptor, index: Int): Double { cur(desc, index); return decodeDoubleValue() }
        override fun decodeCharElementValue(desc: SerialDescriptor, index: Int): Char { cur(desc, index); return decodeCharValue() }
        override fun decodeStringElementValue(desc: SerialDescriptor, index: Int): String { cur(desc, index); return decodeStringValue() }

        override fun <T : Enum<T>> decodeEnumElementValue(desc: SerialDescriptor, index: Int, enumClass: KClass<T>): T {
            cur(desc, index)
            return decodeEnumValue(enumClass)
        }

        override fun <T : Enum<T>> decodeEnumElementValue(desc: SerialDescriptor, index: Int, enumCreator: EnumCreator<T>): T {
            cur(desc, index)
            return decodeEnumValue(enumCreator)
        }

        override fun <T: Any?> decodeSerializableElementValue(desc: SerialDescriptor, index: Int, loader: DeserializationStrategy<T>): T {
            cur(desc, index)
            return decodeSerializableValue(loader)
        }

        override fun <T: Any> decodeNullableSerializableElementValue(desc: SerialDescriptor, index: Int, loader: DeserializationStrategy<T?>): T? {
            cur(desc, index)
            return decodeNullableSerializableValue(loader)
        }
    }
}
