/*
 * Copyright 2017 JetBrains s.r.o.
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

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class SerialId(val id: Int)

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class SerialTag(val tag: String)


abstract class TaggedOutput<T : Any?> : KOutput() {
    abstract protected fun KSerialClassDesc.getTag(index: Int): T


    // ---- API ----
    open fun writeTaggedValue(tag: T, value: Any): Unit = throw SerializationException("$value is not supported")

    open fun writeTaggedNotNullMark(tag: T) {}
    open fun writeTaggedNull(tag: T): Unit = throw SerializationException("null is not supported")

    private fun writeTaggedNullable(tag: T, value: Any?) {
        if (value == null) {
            writeTaggedNull(tag)
        } else {
            writeTaggedNotNullMark(tag)
            writeTaggedValue(tag, value)
        }
    }

    open fun writeTaggedUnit(tag: T) = writeTaggedValue(tag, Unit)
    open fun writeTaggedInt(tag: T, value: Int) = writeTaggedValue(tag, value)
    open fun writeTaggedByte(tag: T, value: Byte) = writeTaggedValue(tag, value)
    open fun writeTaggedShort(tag: T, value: Short) = writeTaggedValue(tag, value)
    open fun writeTaggedLong(tag: T, value: Long) = writeTaggedValue(tag, value)
    open fun writeTaggedFloat(tag: T, value: Float) = writeTaggedValue(tag, value)
    open fun writeTaggedDouble(tag: T, value: Double) = writeTaggedValue(tag, value)
    open fun writeTaggedBoolean(tag: T, value: Boolean) = writeTaggedValue(tag, value)
    open fun writeTaggedChar(tag: T, value: Char) = writeTaggedValue(tag, value)
    open fun writeTaggedString(tag: T, value: String) = writeTaggedValue(tag, value)
    open fun <E : Enum<E>> writeTaggedEnum(tag: T, enumClass: KClass<E>, value: E) = writeTaggedValue(tag, value)

    // ---- Implementation of low-level API ----

    override final fun writeElement(desc: KSerialClassDesc, index: Int): Boolean {
        val tag = desc.getTag(index)
        val shouldWriteElement = shouldWriteElement(desc, tag, index)
        if (shouldWriteElement) {
            pushTag(tag)
        }
        return shouldWriteElement
    }

    // For format-specific behaviour
    open fun shouldWriteElement(desc: KSerialClassDesc, tag: T, index: Int) = true

    override final fun writeNotNullMark() {
        writeTaggedNotNullMark(currentTag)
    }

    override final fun writeNullValue() {
        writeTaggedNull(popTag())
    }

    override final fun writeNonSerializableValue(value: Any) {
        writeTaggedValue(popTag(), value)
    }

    override final fun writeNullableValue(value: Any?) {
        writeTaggedNullable(popTag(), value)
    }

    override final fun writeUnitValue() {
        writeTaggedUnit(popTag())
    }

    override final fun writeBooleanValue(value: Boolean) {
        writeTaggedBoolean(popTag(), value)
    }

    override final fun writeByteValue(value: Byte) {
        writeTaggedByte(popTag(), value)
    }

    override final fun writeShortValue(value: Short) {
        writeTaggedShort(popTag(), value)
    }

    override final fun writeIntValue(value: Int) {
        writeTaggedInt(popTag(), value)
    }

    override final fun writeLongValue(value: Long) {
        writeTaggedLong(popTag(), value)
    }

    override final fun writeFloatValue(value: Float) {
        writeTaggedFloat(popTag(), value)
    }

    override final fun writeDoubleValue(value: Double) {
        writeTaggedDouble(popTag(), value)
    }

    override final fun writeCharValue(value: Char) {
        writeTaggedChar(popTag(), value)
    }

    override final fun writeStringValue(value: String) {
        writeTaggedString(popTag(), value)
    }

    override final fun <E : Enum<E>> writeEnumValue(enumClass: KClass<E>, value: E) {
        writeTaggedEnum(popTag(), enumClass, value)
    }

    override final fun writeEnd(desc: KSerialClassDesc) {
        if (tagStack.isNotEmpty()) popTag(); writeFinished(desc)
    }

    // For format-specific behaviour
    open fun writeFinished(desc: KSerialClassDesc) {}

    override final fun writeNonSerializableElementValue(desc: KSerialClassDesc, index: Int, value: Any) = writeTaggedValue(desc.getTag(index), value)


    override final fun writeNullableElementValue(desc: KSerialClassDesc, index: Int, value: Any?) = writeTaggedNullable(desc.getTag(index), value)
    override final fun writeUnitElementValue(desc: KSerialClassDesc, index: Int) = writeTaggedUnit(desc.getTag(index))
    override final fun writeBooleanElementValue(desc: KSerialClassDesc, index: Int, value: Boolean) = writeTaggedBoolean(desc.getTag(index), value)
    override final fun writeByteElementValue(desc: KSerialClassDesc, index: Int, value: Byte) = writeTaggedByte(desc.getTag(index), value)
    override final fun writeShortElementValue(desc: KSerialClassDesc, index: Int, value: Short) = writeTaggedShort(desc.getTag(index), value)
    override final fun writeIntElementValue(desc: KSerialClassDesc, index: Int, value: Int) = writeTaggedInt(desc.getTag(index), value)
    override final fun writeLongElementValue(desc: KSerialClassDesc, index: Int, value: Long) = writeTaggedLong(desc.getTag(index), value)
    override final fun writeFloatElementValue(desc: KSerialClassDesc, index: Int, value: Float) = writeTaggedFloat(desc.getTag(index), value)
    override final fun writeDoubleElementValue(desc: KSerialClassDesc, index: Int, value: Double) = writeTaggedDouble(desc.getTag(index), value)
    override final fun writeCharElementValue(desc: KSerialClassDesc, index: Int, value: Char) = writeTaggedChar(desc.getTag(index), value)
    override final fun writeStringElementValue(desc: KSerialClassDesc, index: Int, value: String) = writeTaggedString(desc.getTag(index), value)

    override final fun <E : Enum<E>> writeEnumElementValue(desc: KSerialClassDesc, index: Int, enumClass: KClass<E>, value: E) {
        writeTaggedEnum(desc.getTag(index), enumClass, value)
    }

    private val tagStack = arrayListOf<T>()
    protected val currentTag: T
        get() = tagStack.last()
    protected val currentTagOrNull
        get() = tagStack.lastOrNull()

    private fun pushTag(name: T) {
        tagStack.add(name)
    }

    private fun popTag() = tagStack.removeAt(tagStack.lastIndex)


}

abstract class IntTaggedOutput : TaggedOutput<Int?>() {
    override final fun KSerialClassDesc.getTag(index: Int): Int? = this.getAnnotationsForIndex(index).filterIsInstance<SerialId>().singleOrNull()?.id
}

abstract class StringTaggedOutput : TaggedOutput<String?>() {
    override final fun KSerialClassDesc.getTag(index: Int): String? = this.getAnnotationsForIndex(index).filterIsInstance<SerialTag>().singleOrNull()?.tag
}

abstract class NamedValueOutput(val rootName: String = "") : TaggedOutput<String>() {
    override final fun KSerialClassDesc.getTag(index: Int): String = composeName(currentTagOrNull ?: rootName, elementName(this, index))

    open fun elementName(desc: KSerialClassDesc, index: Int) = desc.getElementName(index)
    open fun composeName(parentName: String, childName: String) = if (parentName.isEmpty()) childName else parentName + "." + childName
}

// =================================================================

abstract class TaggedInput<T : Any?> : KInput() {
    abstract protected fun KSerialClassDesc.getTag(index: Int): T


    // ---- API ----
    open fun readTaggedValue(tag: T): Any = throw SerializationException("value is not supported for $tag")

    open fun readTaggedNotNullMark(tag: T): Boolean = true
    open fun readTaggedNull(tag: T): Nothing? = null

    private fun readTaggedNullable(tag: T): Any? {
        return if (readTaggedNotNullMark(tag)) {
            readTaggedValue(tag)
        } else {
            readTaggedNull(tag)
        }
    }

    open fun readTaggedUnit(tag: T): Unit = readTaggedValue(tag) as Unit
    open fun readTaggedBoolean(tag: T): Boolean = readTaggedValue(tag) as Boolean
    open fun readTaggedByte(tag: T): Byte = readTaggedValue(tag) as Byte
    open fun readTaggedShort(tag: T): Short = readTaggedValue(tag) as Short
    open fun readTaggedInt(tag: T): Int = readTaggedValue(tag) as Int
    open fun readTaggedLong(tag: T): Long = readTaggedValue(tag) as Long
    open fun readTaggedFloat(tag: T): Float = readTaggedValue(tag) as Float
    open fun readTaggedDouble(tag: T): Double = readTaggedValue(tag) as Double
    open fun readTaggedChar(tag: T): Char = readTaggedValue(tag) as Char
    open fun readTaggedString(tag: T): String = readTaggedValue(tag) as String
    @Suppress("UNCHECKED_CAST")
    open fun <E : Enum<E>> readTaggedEnum(tag: T, enumClass: KClass<E>): E = readTaggedValue(tag) as E

    // ---- Implementation of low-level API ----

    override final fun readNotNullMark(): Boolean = readTaggedNotNullMark(currentTag)
    override final fun readNullValue(): Nothing? = null

    override final fun readValue(): Any = readTaggedValue(popTag())
    override final fun readNullableValue(): Any? = readTaggedNullable(popTag())
    override final fun readUnitValue() = readTaggedUnit(popTag())
    override final fun readBooleanValue(): Boolean = readTaggedBoolean(popTag())
    override final fun readByteValue(): Byte = readTaggedByte(popTag())
    override final fun readShortValue(): Short = readTaggedShort(popTag())
    override final fun readIntValue(): Int = readTaggedInt(popTag())
    override final fun readLongValue(): Long = readTaggedLong(popTag())
    override final fun readFloatValue(): Float = readTaggedFloat(popTag())
    override final fun readDoubleValue(): Double = readTaggedDouble(popTag())
    override final fun readCharValue(): Char = readTaggedChar(popTag())
    override final fun readStringValue(): String = readTaggedString(popTag())
    override final fun <T : Enum<T>> readEnumValue(enumClass: KClass<T>): T = readTaggedEnum(popTag(), enumClass)

    // Override for custom behaviour
    override fun readElement(desc: KSerialClassDesc): Int = READ_ALL

    override final fun readElementValue(desc: KSerialClassDesc, index: Int): Any = readTaggedValue(desc.getTag(index))
    override final fun readNullableElementValue(desc: KSerialClassDesc, index: Int): Any? = readTaggedNullable(desc.getTag(index))
    override final fun readUnitElementValue(desc: KSerialClassDesc, index: Int) = readTaggedUnit(desc.getTag(index))
    override final fun readBooleanElementValue(desc: KSerialClassDesc, index: Int): Boolean = readTaggedBoolean(desc.getTag(index))
    override final fun readByteElementValue(desc: KSerialClassDesc, index: Int): Byte = readTaggedByte(desc.getTag(index))
    override final fun readShortElementValue(desc: KSerialClassDesc, index: Int): Short = readTaggedShort(desc.getTag(index))
    override final fun readIntElementValue(desc: KSerialClassDesc, index: Int): Int = readTaggedInt(desc.getTag(index))
    override final fun readLongElementValue(desc: KSerialClassDesc, index: Int): Long = readTaggedLong(desc.getTag(index))
    override final fun readFloatElementValue(desc: KSerialClassDesc, index: Int): Float = readTaggedFloat(desc.getTag(index))
    override final fun readDoubleElementValue(desc: KSerialClassDesc, index: Int): Double = readTaggedDouble(desc.getTag(index))
    override final fun readCharElementValue(desc: KSerialClassDesc, index: Int): Char = readTaggedChar(desc.getTag(index))
    override final fun readStringElementValue(desc: KSerialClassDesc, index: Int): String = readTaggedString(desc.getTag(index))
    override final fun <T : Enum<T>> readEnumElementValue(desc: KSerialClassDesc, index: Int, enumClass: KClass<T>): T = readTaggedEnum(desc.getTag(index), enumClass)

    override final fun <T : Any?> readSerializableElementValue(desc: KSerialClassDesc, index: Int, loader: KSerialLoader<T>): T {
        return tagBlock(desc.getTag(index)) { readSerializableValue(loader) }
    }

    override final fun <T : Any> readNullableSerializableElementValue(desc: KSerialClassDesc, index: Int, loader: KSerialLoader<T?>): T? {
        return tagBlock(desc.getTag(index)) { readNullableSerializableValue(loader) }
    }

    override fun <T> updateSerializableElementValue(desc: KSerialClassDesc, index: Int, loader: KSerialLoader<T>, old: T): T {
        return tagBlock(desc.getTag(index)) { updateSerializableValue(loader, desc, old) }
    }

    override fun <T : Any> updateNullableSerializableElementValue(desc: KSerialClassDesc, index: Int, loader: KSerialLoader<T?>, old: T?): T? {
        return tagBlock(desc.getTag(index)) { updateNullableSerializableValue(loader, desc, old) }
    }

    private fun <E> tagBlock(tag: T, block: () -> E): E {
        pushTag(tag)
        val r = block()
        if (!flag) {
            popTag()
            flag = false
        }
        return r
    }

    private val tagStack = arrayListOf<T>()
    protected val currentTag: T
        get() = tagStack.last()
    protected val currentTagOrNull
        get() = tagStack.lastOrNull()

    private fun pushTag(name: T) {
        tagStack.add(name)
    }

    private var flag = false

    private fun popTag(): T {
        val r = tagStack.removeAt(tagStack.lastIndex)
        flag = true
        return r
    }

}

abstract class IntTaggedInput : TaggedInput<Int?>() {
    override final fun KSerialClassDesc.getTag(index: Int): Int? = this.getAnnotationsForIndex(index).filterIsInstance<SerialId>().singleOrNull()?.id
}

abstract class StringTaggedInput : TaggedInput<String?>() {
    override final fun KSerialClassDesc.getTag(index: Int): String? = this.getAnnotationsForIndex(index).filterIsInstance<SerialTag>().singleOrNull()?.tag
}

abstract class NamedValueInput(val rootName: String = "") : TaggedInput<String>() {
    override final fun KSerialClassDesc.getTag(index: Int): String = composeName(currentTagOrNull ?: rootName, elementName(this, index))

    open fun elementName(desc: KSerialClassDesc, index: Int) = desc.getElementName(index)
    open fun composeName(parentName: String, childName: String) = if (parentName.isEmpty()) childName else parentName + "." + childName
}

// =========================================


object Mapper {

    class OutMapper : NamedValueOutput() {
        private var _map: MutableMap<String, Any> = mutableMapOf()

        val map: Map<String, Any>
            get() = _map

        override fun writeTaggedValue(tag: String, value: Any) {
            _map[tag] = value
        }

        override fun writeTaggedNull(tag: String) {
            throw SerializationException("null is not supported. use Mapper.mapNullable()/OutNullableMapper instead")
        }
    }

    class OutNullableMapper : NamedValueOutput() {
        private var _map: MutableMap<String, Any?> = mutableMapOf()

        val map: Map<String, Any?>
            get() = _map

        override fun writeTaggedValue(tag: String, value: Any) {
            _map[tag] = value
        }

        override fun writeTaggedNull(tag: String) {
            _map[tag] = null
        }
    }

    class InMapper(val map: Map<String, Any>) : NamedValueInput() {
        override fun readTaggedValue(tag: String): Any = map.getValue(tag)
    }

    class InNullableMapper(val map: Map<String, Any?>) : NamedValueInput() {
        override fun readTaggedValue(tag: String): Any = map.getValue(tag)!!

        override fun readTaggedNotNullMark(tag: String): Boolean = map.getValue(tag) != null
    }

    inline fun <reified T : Any> map(obj: T): Map<String, Any> {
        val m = OutMapper()
        m.write(obj)
        return m.map
    }

    inline fun <reified T : Any> mapNullable(obj: T): Map<String, Any?> {
        val m = OutNullableMapper()
        m.write(obj)
        return m.map
    }

    inline fun <reified T : Any> unmap(map: Map<String, Any>): T {
        val m = InMapper(map)
        return m.read()
    }

    inline fun <reified T : Any> unmapNullable(map: Map<String, Any?>): T {
        val m = InNullableMapper(map)
        return m.read()
    }
}