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

import kotlin.reflect.KClass

open class ValueTransformer {
    // ------- top-level API (use it) -------

    fun <T> transform(serializer: KSerializer<T>, obj: T): T {
        val output = Output()
        output.write(serializer, obj)
        val input = Input(output.list)
        return input.read(serializer)
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

    private inner class Output : KOutput() {
        internal val list = arrayListOf<Any?>()

        override fun writeNullableValue(value: Any?) {
            list.add(value)
        }

        override fun writeElement(desc: SerialDescriptor, index: Int) = true
        override fun writeNotNullMark() {}
        override fun writeNullValue() { writeNullableValue(null) }
        override fun writeNonSerializableValue(value: Any) { writeNullableValue(value) }
        override fun writeUnitValue() { writeNullableValue(Unit) }
        override fun writeBooleanValue(value: Boolean) { writeNullableValue(value) }
        override fun writeByteValue(value: Byte) { writeNullableValue(value) }
        override fun writeShortValue(value: Short) { writeNullableValue(value) }
        override fun writeIntValue(value: Int) { writeNullableValue(value) }
        override fun writeLongValue(value: Long) { writeNullableValue(value) }
        override fun writeFloatValue(value: Float) { writeNullableValue(value) }
        override fun writeDoubleValue(value: Double) { writeNullableValue(value) }
        override fun writeCharValue(value: Char) { writeNullableValue(value) }
        override fun writeStringValue(value: String) { writeNullableValue(value) }
        override fun <T : Enum<T>> writeEnumValue(enumClass: KClass<T>, value: T) { writeNullableValue(value) }
        override fun <T : Enum<T>> writeEnumValue(value: T) {
            writeNullableValue(value)
        }

        override fun <T : Any?> writeSerializableValue(saver: SerializationStrategy<T>, value: T) {
            if (isRecursiveTransform()) {
                saver.serialize(this, value)
            } else
                writeNullableValue(value)
        }

        // ---------------

        override fun writeNonSerializableElementValue(desc: SerialDescriptor, index: Int, value: Any) { writeNullableValue(value) }
        override fun writeNullableElementValue(desc: SerialDescriptor, index: Int, value: Any?) = writeNullableValue(value)
        override fun writeUnitElementValue(desc: SerialDescriptor, index: Int) = writeNullableValue(Unit)
        override fun writeBooleanElementValue(desc: SerialDescriptor, index: Int, value: Boolean) = writeNullableValue(value)
        override fun writeByteElementValue(desc: SerialDescriptor, index: Int, value: Byte) = writeNullableValue(value)
        override fun writeShortElementValue(desc: SerialDescriptor, index: Int, value: Short) = writeNullableValue(value)
        override fun writeIntElementValue(desc: SerialDescriptor, index: Int, value: Int) = writeNullableValue(value)
        override fun writeLongElementValue(desc: SerialDescriptor, index: Int, value: Long) = writeNullableValue(value)
        override fun writeFloatElementValue(desc: SerialDescriptor, index: Int, value: Float) = writeNullableValue(value)
        override fun writeDoubleElementValue(desc: SerialDescriptor, index: Int, value: Double) = writeNullableValue(value)
        override fun writeCharElementValue(desc: SerialDescriptor, index: Int, value: Char) = writeNullableValue(value)
        override fun writeStringElementValue(desc: SerialDescriptor, index: Int, value: String) = writeNullableValue(value)

        override fun <T : Enum<T>> writeEnumElementValue(desc: SerialDescriptor, index: Int, enumClass: KClass<T>, value: T) =
            writeNullableValue(value)

        override fun <T : Enum<T>> writeEnumElementValue(desc: SerialDescriptor, index: Int, value: T) {
            writeNullableValue(value)
        }
    }

    private inner class Input(private val list: List<Any?>) : KInput() {
        private var index = 0
        private var curDesc: SerialDescriptor? = null
        private var curIndex: Int = 0

        private fun cur(desc: SerialDescriptor, index: Int) {
            curDesc = desc
            curIndex = index
        }

        override fun readNotNullMark(): Boolean = list[index] != null
        override fun readNullValue(): Nothing? { index++; return null }
        override fun readValue(): Any = list[index++]!!
        override fun readNullableValue(): Any? = list[index++]
        override fun readUnitValue(): Unit { index++ }

        override fun readBooleanValue(): Boolean = transformBooleanValue(curDesc!!, curIndex, readValue() as Boolean)
        override fun readByteValue(): Byte = transformByteValue(curDesc!!, curIndex, readValue() as Byte)
        override fun readShortValue(): Short = transformShortValue(curDesc!!, curIndex, readValue() as Short)
        override fun readIntValue(): Int = transformIntValue(curDesc!!, curIndex, readValue() as Int)
        override fun readLongValue(): Long = transformLongValue(curDesc!!, curIndex, readValue() as Long)
        override fun readFloatValue(): Float = transformFloatValue(curDesc!!, curIndex, readValue() as Float)
        override fun readDoubleValue(): Double = transformDoubleValue(curDesc!!, curIndex, readValue() as Double)
        override fun readCharValue(): Char = transformCharValue(curDesc!!, curIndex, readValue() as Char)
        override fun readStringValue(): String = transformStringValue(curDesc!!, curIndex, readValue() as String)

        @Suppress("UNCHECKED_CAST")
        @Deprecated("Not supported in Native", replaceWith = ReplaceWith("readEnumValue(enumLoader)"))
        override fun <T : Enum<T>> readEnumValue(enumClass: KClass<T>): T =
            transformEnumValue(curDesc!!, curIndex, enumClass, readValue() as T)

        @Suppress("UNCHECKED_CAST")
        override fun <T : Enum<T>> readEnumValue(enumCreator: EnumCreator<T>): T =
            transformEnumValue(curDesc!!, curIndex, enumCreator, readValue() as T)

        @Suppress("UNCHECKED_CAST")
        override fun <T : Any?> readSerializableValue(loader: DeserializationStrategy<T>): T {
            if (isRecursiveTransform())
                return loader.deserialize(this)
            else
                return readValue() as T
        }

        // ---------------

        override fun readElement(desc: SerialDescriptor): Int = READ_ALL

        override fun readElementValue(desc: SerialDescriptor, index: Int): Any {
            cur(desc, index); return readValue()
        }

        override fun readNullableElementValue(desc: SerialDescriptor, index: Int): Any? {
            cur(desc, index); return readNullableValue()
        }
        override fun readUnitElementValue(desc: SerialDescriptor, index: Int) { cur(desc, index); return readUnitValue() }
        override fun readBooleanElementValue(desc: SerialDescriptor, index: Int): Boolean { cur(desc, index); return readBooleanValue() }
        override fun readByteElementValue(desc: SerialDescriptor, index: Int): Byte { cur(desc, index); return readByteValue() }
        override fun readShortElementValue(desc: SerialDescriptor, index: Int): Short { cur(desc, index); return readShortValue() }
        override fun readIntElementValue(desc: SerialDescriptor, index: Int): Int { cur(desc, index); return readIntValue() }
        override fun readLongElementValue(desc: SerialDescriptor, index: Int): Long { cur(desc, index); return readLongValue() }
        override fun readFloatElementValue(desc: SerialDescriptor, index: Int): Float { cur(desc, index); return readFloatValue() }
        override fun readDoubleElementValue(desc: SerialDescriptor, index: Int): Double { cur(desc, index); return readDoubleValue() }
        override fun readCharElementValue(desc: SerialDescriptor, index: Int): Char { cur(desc, index); return readCharValue() }
        override fun readStringElementValue(desc: SerialDescriptor, index: Int): String { cur(desc, index); return readStringValue() }

        override fun <T : Enum<T>> readEnumElementValue(desc: SerialDescriptor, index: Int, enumClass: KClass<T>): T {
            cur(desc, index)
            return readEnumValue(enumClass)
        }

        override fun <T : Enum<T>> readEnumElementValue(desc: SerialDescriptor, index: Int, enumCreator: EnumCreator<T>): T {
            cur(desc, index)
            return readEnumValue(enumCreator)
        }

        override fun <T: Any?> readSerializableElementValue(desc: SerialDescriptor, index: Int, loader: DeserializationStrategy<T>): T {
            cur(desc, index)
            return readSerializableValue(loader)
        }

        override fun <T: Any> readNullableSerializableElementValue(desc: SerialDescriptor, index: Int, loader: DeserializationStrategy<T?>): T? {
            cur(desc, index)
            return readNullableSerializableValue(loader)
        }
    }
}
