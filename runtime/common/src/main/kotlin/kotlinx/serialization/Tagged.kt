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

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class SerialId(val id: Int)

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class SerialTag(val tag: String)


abstract class TaggedOutput<T : Any?> : KOutput() {
    protected abstract fun KSerialClassDesc.getTag(index: Int): T


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
    @Deprecated("Not supported in Native", replaceWith = ReplaceWith("writeTaggedEnum(tag, value)"))
    open fun <E : Enum<E>> writeTaggedEnum(tag: T, enumClass: KClass<E>, value: E) = writeTaggedEnum(tag, value)

    open fun <E : Enum<E>> writeTaggedEnum(tag: T, value: E) = writeTaggedValue(tag, value)

    // ---- Implementation of low-level API ----

    final override fun writeElement(desc: KSerialClassDesc, index: Int): Boolean {
        val tag = desc.getTag(index)
        val shouldWriteElement = shouldWriteElement(desc, tag, index)
        if (shouldWriteElement) {
            pushTag(tag)
        }
        return shouldWriteElement
    }

    // For format-specific behaviour
    open fun shouldWriteElement(desc: KSerialClassDesc, tag: T, index: Int) = true

    final override fun writeNotNullMark() {
        writeTaggedNotNullMark(currentTag)
    }

    final override fun writeNullValue() {
        writeTaggedNull(popTag())
    }

    final override fun writeNonSerializableValue(value: Any) {
        writeTaggedValue(popTag(), value)
    }

    final override fun writeNullableValue(value: Any?) {
        writeTaggedNullable(popTag(), value)
    }

    final override fun writeUnitValue() {
        writeTaggedUnit(popTag())
    }

    final override fun writeBooleanValue(value: Boolean) {
        writeTaggedBoolean(popTag(), value)
    }

    final override fun writeByteValue(value: Byte) {
        writeTaggedByte(popTag(), value)
    }

    final override fun writeShortValue(value: Short) {
        writeTaggedShort(popTag(), value)
    }

    final override fun writeIntValue(value: Int) {
        writeTaggedInt(popTag(), value)
    }

    final override fun writeLongValue(value: Long) {
        writeTaggedLong(popTag(), value)
    }

    final override fun writeFloatValue(value: Float) {
        writeTaggedFloat(popTag(), value)
    }

    final override fun writeDoubleValue(value: Double) {
        writeTaggedDouble(popTag(), value)
    }

    final override fun writeCharValue(value: Char) {
        writeTaggedChar(popTag(), value)
    }

    final override fun writeStringValue(value: String) {
        writeTaggedString(popTag(), value)
    }

    @Deprecated("Not supported in Native", replaceWith = ReplaceWith("readEnumValue(enumLoader)"))
    final override fun <E : Enum<E>> writeEnumValue(enumClass: KClass<E>, value: E) {
        writeEnumValue(value)
    }

    final override fun <T : Enum<T>> writeEnumValue(value: T) {
        writeTaggedEnum(popTag(), value)
    }

    final override fun writeEnd(desc: KSerialClassDesc) {
        if (tagStack.isNotEmpty()) popTag(); writeFinished(desc)
    }

    // For format-specific behaviour
    open fun writeFinished(desc: KSerialClassDesc) {}

    final override fun writeNonSerializableElementValue(desc: KSerialClassDesc, index: Int, value: Any) = writeTaggedValue(desc.getTag(index), value)


    final override fun writeNullableElementValue(desc: KSerialClassDesc, index: Int, value: Any?) = writeTaggedNullable(desc.getTag(index), value)
    final override fun writeUnitElementValue(desc: KSerialClassDesc, index: Int) = writeTaggedUnit(desc.getTag(index))
    final override fun writeBooleanElementValue(desc: KSerialClassDesc, index: Int, value: Boolean) = writeTaggedBoolean(desc.getTag(index), value)
    final override fun writeByteElementValue(desc: KSerialClassDesc, index: Int, value: Byte) = writeTaggedByte(desc.getTag(index), value)
    final override fun writeShortElementValue(desc: KSerialClassDesc, index: Int, value: Short) = writeTaggedShort(desc.getTag(index), value)
    final override fun writeIntElementValue(desc: KSerialClassDesc, index: Int, value: Int) = writeTaggedInt(desc.getTag(index), value)
    final override fun writeLongElementValue(desc: KSerialClassDesc, index: Int, value: Long) = writeTaggedLong(desc.getTag(index), value)
    final override fun writeFloatElementValue(desc: KSerialClassDesc, index: Int, value: Float) = writeTaggedFloat(desc.getTag(index), value)
    final override fun writeDoubleElementValue(desc: KSerialClassDesc, index: Int, value: Double) = writeTaggedDouble(desc.getTag(index), value)
    final override fun writeCharElementValue(desc: KSerialClassDesc, index: Int, value: Char) = writeTaggedChar(desc.getTag(index), value)
    final override fun writeStringElementValue(desc: KSerialClassDesc, index: Int, value: String) = writeTaggedString(desc.getTag(index), value)

    @Deprecated("Not supported in Native", replaceWith = ReplaceWith("readEnumValue(enumLoader)"))
    final override fun <E : Enum<E>> writeEnumElementValue(desc: KSerialClassDesc, index: Int, enumClass: KClass<E>, value: E) {
        writeEnumElementValue(desc, index, value)
    }


    final override fun <T : Enum<T>> writeEnumElementValue(desc: KSerialClassDesc, index: Int, value: T) {
        writeTaggedEnum(desc.getTag(index), value)
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
    final override fun KSerialClassDesc.getTag(index: Int): Int? = getSerialId(this, index)
}

abstract class StringTaggedOutput : TaggedOutput<String?>() {
    final override fun KSerialClassDesc.getTag(index: Int): String? = getSerialTag(this, index)
}

abstract class NamedValueOutput(val rootName: String = "") : TaggedOutput<String>() {
    final override fun KSerialClassDesc.getTag(index: Int): String = composeName(currentTagOrNull ?: rootName, elementName(this, index))

    open fun elementName(desc: KSerialClassDesc, index: Int) = desc.getElementName(index)
    open fun composeName(parentName: String, childName: String) = if (parentName.isEmpty()) childName else parentName + "." + childName
}

// =====

expect fun getSerialId(desc: KSerialClassDesc, index: Int): Int?
expect fun getSerialTag(desc: KSerialClassDesc, index: Int): String?

// =================================================================

abstract class TaggedInput<T : Any?> : KInput() {
    protected abstract fun KSerialClassDesc.getTag(index: Int): T


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
    @Deprecated("Not supported in Native", replaceWith = ReplaceWith("readEnumValue(enumLoader)"))
    fun <E : Enum<E>> readTaggedEnum(tag: T, enumClass: KClass<E>): E = readTaggedEnum(tag, LegacyEnumLoader(enumClass))
    @Suppress("UNCHECKED_CAST")
    open fun <E : Enum<E>> readTaggedEnum(tag: T, enumLoader: EnumLoader<E>): E = readTaggedValue(tag) as E


    // ---- Implementation of low-level API ----

    final override fun readNotNullMark(): Boolean = readTaggedNotNullMark(currentTag)
    final override fun readNullValue(): Nothing? = null

    final override fun readValue(): Any = readTaggedValue(popTag())
    final override fun readNullableValue(): Any? = readTaggedNullable(popTag())
    final override fun readUnitValue() = readTaggedUnit(popTag())
    final override fun readBooleanValue(): Boolean = readTaggedBoolean(popTag())
    final override fun readByteValue(): Byte = readTaggedByte(popTag())
    final override fun readShortValue(): Short = readTaggedShort(popTag())
    final override fun readIntValue(): Int = readTaggedInt(popTag())
    final override fun readLongValue(): Long = readTaggedLong(popTag())
    final override fun readFloatValue(): Float = readTaggedFloat(popTag())
    final override fun readDoubleValue(): Double = readTaggedDouble(popTag())
    final override fun readCharValue(): Char = readTaggedChar(popTag())
    final override fun readStringValue(): String = readTaggedString(popTag())
    @Deprecated("Not supported in Native", replaceWith = ReplaceWith("readEnumValue(enumLoader)"))
    final override fun <T : Enum<T>> readEnumValue(enumClass: KClass<T>): T = readEnumValue(LegacyEnumLoader(enumClass))

    final override fun <T : Enum<T>> readEnumValue(enumLoader: EnumLoader<T>): T = readTaggedEnum(popTag(), enumLoader)

    // Override for custom behaviour
    override fun readElement(desc: KSerialClassDesc): Int = READ_ALL

    final override fun readElementValue(desc: KSerialClassDesc, index: Int): Any = readTaggedValue(desc.getTag(index))
    final override fun readNullableElementValue(desc: KSerialClassDesc, index: Int): Any? = readTaggedNullable(desc.getTag(index))
    final override fun readUnitElementValue(desc: KSerialClassDesc, index: Int) = readTaggedUnit(desc.getTag(index))
    final override fun readBooleanElementValue(desc: KSerialClassDesc, index: Int): Boolean = readTaggedBoolean(desc.getTag(index))
    final override fun readByteElementValue(desc: KSerialClassDesc, index: Int): Byte = readTaggedByte(desc.getTag(index))
    final override fun readShortElementValue(desc: KSerialClassDesc, index: Int): Short = readTaggedShort(desc.getTag(index))
    final override fun readIntElementValue(desc: KSerialClassDesc, index: Int): Int = readTaggedInt(desc.getTag(index))
    final override fun readLongElementValue(desc: KSerialClassDesc, index: Int): Long = readTaggedLong(desc.getTag(index))
    final override fun readFloatElementValue(desc: KSerialClassDesc, index: Int): Float = readTaggedFloat(desc.getTag(index))
    final override fun readDoubleElementValue(desc: KSerialClassDesc, index: Int): Double = readTaggedDouble(desc.getTag(index))
    final override fun readCharElementValue(desc: KSerialClassDesc, index: Int): Char = readTaggedChar(desc.getTag(index))
    final override fun readStringElementValue(desc: KSerialClassDesc, index: Int): String = readTaggedString(desc.getTag(index))
    @Deprecated("Not supported in Native", replaceWith = ReplaceWith("readEnumValue(enumLoader)"))
    final override fun <T : Enum<T>> readEnumElementValue(desc: KSerialClassDesc, index: Int, enumClass: KClass<T>): T = readEnumElementValue(desc, index, LegacyEnumLoader(enumClass))

    final override fun <T : Enum<T>> readEnumElementValue(desc: KSerialClassDesc, index: Int, enumLoader: EnumLoader<T>): T = readTaggedEnum(desc.getTag(index), enumLoader)

    final override fun <T : Any?> readSerializableElementValue(desc: KSerialClassDesc, index: Int, loader: KSerialLoader<T>): T {
        return tagBlock(desc.getTag(index)) { readSerializableValue(loader) }
    }

    final override fun <T : Any> readNullableSerializableElementValue(desc: KSerialClassDesc, index: Int, loader: KSerialLoader<T?>): T? {
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
        }
        flag = false
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
    final override fun KSerialClassDesc.getTag(index: Int): Int? = getSerialId(this, index)
}

abstract class StringTaggedInput : TaggedInput<String?>() {
    final override fun KSerialClassDesc.getTag(index: Int): String? = getSerialTag(this, index)
}

abstract class NamedValueInput(val rootName: String = "") : TaggedInput<String>() {
    final override fun KSerialClassDesc.getTag(index: Int): String = composeName(currentTagOrNull ?: rootName, elementName(this, index))

    open fun elementName(desc: KSerialClassDesc, index: Int) = desc.getElementName(index)
    open fun composeName(parentName: String, childName: String) = if (parentName.isEmpty()) childName else parentName + "." + childName
}
