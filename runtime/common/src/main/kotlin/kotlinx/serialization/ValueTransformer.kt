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

    open fun transformBooleanValue(desc: KSerialClassDesc, index: Int, value: Boolean) = value
    open fun transformByteValue(desc: KSerialClassDesc, index: Int, value: Byte) = value
    open fun transformShortValue(desc: KSerialClassDesc, index: Int, value: Short) = value
    open fun transformIntValue(desc: KSerialClassDesc, index: Int, value: Int) = value
    open fun transformLongValue(desc: KSerialClassDesc, index: Int, value: Long) = value
    open fun transformFloatValue(desc: KSerialClassDesc, index: Int, value: Float) = value
    open fun transformDoubleValue(desc: KSerialClassDesc, index: Int, value: Double) = value
    open fun transformCharValue(desc: KSerialClassDesc, index: Int, value: Char) = value
    open fun transformStringValue(desc: KSerialClassDesc, index: Int, value: String) = value

    @Deprecated("Not supported in Native", replaceWith = ReplaceWith("transformEnumValue(enumLoader)"))
    open fun <T : Enum<T>> transformEnumValue(desc: KSerialClassDesc, index: Int, enumClass: KClass<T>, value: T): T = value

    open fun <T : Enum<T>> transformEnumValue(desc: KSerialClassDesc, index: Int, enumLoader: EnumLoader<T>, value: T): T = value

    open fun isRecursiveTransform(): Boolean = true

    // ---------------

    private inner class Output : KOutput() {
        internal val list = arrayListOf<Any?>()

        override fun writeNullableValue(value: Any?) {
            list.add(value)
        }

        override fun writeElement(desc: KSerialClassDesc, index: Int) = true
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

        override fun <T : Any?> writeSerializableValue(saver: KSerialSaver<T>, value: T) {
            if (isRecursiveTransform()) {
                saver.save(this, value)
            } else
                writeNullableValue(value)
        }

        // ---------------

        override fun writeNonSerializableElementValue(desc: KSerialClassDesc, index: Int, value: Any) { writeNullableValue(value) }
        override fun writeNullableElementValue(desc: KSerialClassDesc, index: Int, value: Any?) = writeNullableValue(value)
        override fun writeUnitElementValue(desc: KSerialClassDesc, index: Int) = writeNullableValue(Unit)
        override fun writeBooleanElementValue(desc: KSerialClassDesc, index: Int, value: Boolean) = writeNullableValue(value)
        override fun writeByteElementValue(desc: KSerialClassDesc, index: Int, value: Byte) = writeNullableValue(value)
        override fun writeShortElementValue(desc: KSerialClassDesc, index: Int, value: Short) = writeNullableValue(value)
        override fun writeIntElementValue(desc: KSerialClassDesc, index: Int, value: Int) = writeNullableValue(value)
        override fun writeLongElementValue(desc: KSerialClassDesc, index: Int, value: Long) = writeNullableValue(value)
        override fun writeFloatElementValue(desc: KSerialClassDesc, index: Int, value: Float) = writeNullableValue(value)
        override fun writeDoubleElementValue(desc: KSerialClassDesc, index: Int, value: Double) = writeNullableValue(value)
        override fun writeCharElementValue(desc: KSerialClassDesc, index: Int, value: Char) = writeNullableValue(value)
        override fun writeStringElementValue(desc: KSerialClassDesc, index: Int, value: String) = writeNullableValue(value)

        override fun <T : Enum<T>> writeEnumElementValue(desc: KSerialClassDesc, index: Int, enumClass: KClass<T>, value: T) =
            writeNullableValue(value)

        override fun <T : Enum<T>> writeEnumElementValue(desc: KSerialClassDesc, index: Int, value: T) {
            writeNullableValue(value)
        }
    }

    private inner class Input(private val list: List<Any?>) : KInput() {
        private var index = 0
        private var curDesc: KSerialClassDesc? = null
        private var curIndex: Int = 0

        private fun cur(desc: KSerialClassDesc, index: Int) {
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
        override fun <T : Enum<T>> readEnumValue(enumLoader: EnumLoader<T>): T =
            transformEnumValue(curDesc!!, curIndex, enumLoader, readValue() as T)

        @Suppress("UNCHECKED_CAST")
        override fun <T : Any?> readSerializableValue(loader: KSerialLoader<T>): T {
            if (isRecursiveTransform())
                return loader.load(this)
            else
                return readValue() as T
        }

        // ---------------

        override fun readElement(desc: KSerialClassDesc): Int = READ_ALL

        override fun readElementValue(desc: KSerialClassDesc, index: Int): Any {
            cur(desc, index); return readValue()
        }

        override fun readNullableElementValue(desc: KSerialClassDesc, index: Int): Any? {
            cur(desc, index); return readNullableValue()
        }
        override fun readUnitElementValue(desc: KSerialClassDesc, index: Int) { cur(desc, index); return readUnitValue() }
        override fun readBooleanElementValue(desc: KSerialClassDesc, index: Int): Boolean { cur(desc, index); return readBooleanValue() }
        override fun readByteElementValue(desc: KSerialClassDesc, index: Int): Byte { cur(desc, index); return readByteValue() }
        override fun readShortElementValue(desc: KSerialClassDesc, index: Int): Short { cur(desc, index); return readShortValue() }
        override fun readIntElementValue(desc: KSerialClassDesc, index: Int): Int { cur(desc, index); return readIntValue() }
        override fun readLongElementValue(desc: KSerialClassDesc, index: Int): Long { cur(desc, index); return readLongValue() }
        override fun readFloatElementValue(desc: KSerialClassDesc, index: Int): Float { cur(desc, index); return readFloatValue() }
        override fun readDoubleElementValue(desc: KSerialClassDesc, index: Int): Double { cur(desc, index); return readDoubleValue() }
        override fun readCharElementValue(desc: KSerialClassDesc, index: Int): Char { cur(desc, index); return readCharValue() }
        override fun readStringElementValue(desc: KSerialClassDesc, index: Int): String { cur(desc, index); return readStringValue() }

        override fun <T : Enum<T>> readEnumElementValue(desc: KSerialClassDesc, index: Int, enumClass: KClass<T>): T {
            cur(desc, index)
            return readEnumValue(enumClass)
        }

        override fun <T : Enum<T>> readEnumElementValue(desc: KSerialClassDesc, index: Int, enumLoader: EnumLoader<T>): T {
            cur(desc, index)
            return readEnumValue(enumLoader)
        }

        override fun <T: Any?> readSerializableElementValue(desc: KSerialClassDesc, index: Int, loader: KSerialLoader<T>): T {
            cur(desc, index)
            return readSerializableValue(loader)
        }

        override fun <T: Any> readNullableSerializableElementValue(desc: KSerialClassDesc, index: Int, loader: KSerialLoader<T?>): T? {
            cur(desc, index)
            return readNullableSerializableValue(loader)
        }
    }
}
