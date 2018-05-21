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

import kotlinx.serialization.CompositeDecoder.Companion.READ_ALL
import kotlin.reflect.KClass

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class SerialId(val id: Int)

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class SerialTag(val tag: String)


abstract class TaggedOutput<T : Any?> : CompositeEncoder {
    protected abstract fun SerialDescriptor.getTag(index: Int): T

    override var context: SerialContext? = null

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

    final override fun encodeElement(desc: SerialDescriptor, index: Int): Boolean {
        val tag = desc.getTag(index)
        val shouldWriteElement = shouldWriteElement(desc, tag, index)
        if (shouldWriteElement) {
            pushTag(tag)
        }
        return shouldWriteElement
    }

    // For format-specific behaviour
    open fun shouldWriteElement(desc: SerialDescriptor, tag: T, index: Int) = true

    final override fun encodeNotNullMark() {
        writeTaggedNotNullMark(currentTag)
    }

    final override fun encodeNull() {
        writeTaggedNull(popTag())
    }

    final override fun encodeNonSerializableValue(value: Any) {
        writeTaggedValue(popTag(), value)
    }

    final override fun encodeNullableValue(value: Any?) {
        writeTaggedNullable(popTag(), value)
    }

    final override fun encodeUnit() {
        writeTaggedUnit(popTag())
    }

    final override fun encodeBoolean(value: Boolean) {
        writeTaggedBoolean(popTag(), value)
    }

    final override fun encodeByte(value: Byte) {
        writeTaggedByte(popTag(), value)
    }

    final override fun encodeShort(value: Short) {
        writeTaggedShort(popTag(), value)
    }

    final override fun encodeInt(value: Int) {
        writeTaggedInt(popTag(), value)
    }

    final override fun encodeLong(value: Long) {
        writeTaggedLong(popTag(), value)
    }

    final override fun encodeFloat(value: Float) {
        writeTaggedFloat(popTag(), value)
    }

    final override fun encodeDouble(value: Double) {
        writeTaggedDouble(popTag(), value)
    }

    final override fun encodeChar(value: Char) {
        writeTaggedChar(popTag(), value)
    }

    final override fun encodeString(value: String) {
        writeTaggedString(popTag(), value)
    }

    @Deprecated("Not supported in Native", replaceWith = ReplaceWith("decodeEnum(enumLoader)"))
    final override fun <E : Enum<E>> encodeEnum(enumClass: KClass<E>, value: E) {
        encodeEnum(value)
    }

    final override fun <T : Enum<T>> encodeEnum(value: T) {
        writeTaggedEnum(popTag(), value)
    }

    final override fun endStructure(desc: SerialDescriptor) {
        if (tagStack.isNotEmpty()) popTag(); writeFinished(desc)
    }

    // For format-specific behaviour
    open fun writeFinished(desc: SerialDescriptor) {}

    final override fun encodeNonSerializableElement(desc: SerialDescriptor, index: Int, value: Any) = writeTaggedValue(desc.getTag(index), value)


    final override fun encodeNullableElementValue(desc: SerialDescriptor, index: Int, value: Any?) = writeTaggedNullable(desc.getTag(index), value)
    final override fun encodeUnitElement(desc: SerialDescriptor, index: Int) = writeTaggedUnit(desc.getTag(index))
    final override fun encodeBooleanElement(desc: SerialDescriptor, index: Int, value: Boolean) = writeTaggedBoolean(desc.getTag(index), value)
    final override fun encodeByteElement(desc: SerialDescriptor, index: Int, value: Byte) = writeTaggedByte(desc.getTag(index), value)
    final override fun encodeShortElement(desc: SerialDescriptor, index: Int, value: Short) = writeTaggedShort(desc.getTag(index), value)
    final override fun encodeIntElement(desc: SerialDescriptor, index: Int, value: Int) = writeTaggedInt(desc.getTag(index), value)
    final override fun encodeLongElement(desc: SerialDescriptor, index: Int, value: Long) = writeTaggedLong(desc.getTag(index), value)
    final override fun encodeFloatElement(desc: SerialDescriptor, index: Int, value: Float) = writeTaggedFloat(desc.getTag(index), value)
    final override fun encodeDoubleElement(desc: SerialDescriptor, index: Int, value: Double) = writeTaggedDouble(desc.getTag(index), value)
    final override fun encodeCharElement(desc: SerialDescriptor, index: Int, value: Char) = writeTaggedChar(desc.getTag(index), value)
    final override fun encodeStringElement(desc: SerialDescriptor, index: Int, value: String) = writeTaggedString(desc.getTag(index), value)

    @Deprecated("Not supported in Native", replaceWith = ReplaceWith("decodeEnum(enumLoader)"))
    final override fun <E : Enum<E>> encodeEnumElement(desc: SerialDescriptor, index: Int, enumClass: KClass<E>, value: E) {
        encodeEnumElement(desc, index, value)
    }


    final override fun <T : Enum<T>> encodeEnumElement(desc: SerialDescriptor, index: Int, value: T) {
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
    final override fun SerialDescriptor.getTag(index: Int): Int? = getSerialId(this, index)
}

abstract class StringTaggedOutput : TaggedOutput<String?>() {
    final override fun SerialDescriptor.getTag(index: Int): String? = getSerialTag(this, index)
}

abstract class NamedValueOutput(val rootName: String = "") : TaggedOutput<String>() {
    final override fun SerialDescriptor.getTag(index: Int): String = composeName(currentTagOrNull ?: rootName, elementName(this, index))

    open fun elementName(desc: SerialDescriptor, index: Int) = desc.getElementName(index)
    open fun composeName(parentName: String, childName: String) = if (parentName.isEmpty()) childName else parentName + "." + childName
}

// =====

expect fun getSerialId(desc: SerialDescriptor, index: Int): Int?
expect fun getSerialTag(desc: SerialDescriptor, index: Int): String?

// =================================================================

abstract class TaggedInput<T : Any?> : CompositeDecoder {
    override var context: SerialContext? = null
    override val updateMode: UpdateMode = UpdateMode.UPDATE

    protected abstract fun SerialDescriptor.getTag(index: Int): T


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
    @Deprecated("Not supported in Native", replaceWith = ReplaceWith("decodeEnum(enumLoader)"))
    fun <E : Enum<E>> readTaggedEnum(tag: T, enumClass: KClass<E>): E = readTaggedEnum(tag, LegacyEnumCreator(enumClass))
    @Suppress("UNCHECKED_CAST")
    open fun <E : Enum<E>> readTaggedEnum(tag: T, enumCreator: EnumCreator<E>): E = readTaggedValue(tag) as E


    // ---- Implementation of low-level API ----

    final override fun decodeNotNullMark(): Boolean = readTaggedNotNullMark(currentTag)
    final override fun decodeNull(): Nothing? = null

    final override fun decodeValue(): Any = readTaggedValue(popTag())
    final override fun decodeNullableValue(): Any? = readTaggedNullable(popTag())
    final override fun decodeUnit() = readTaggedUnit(popTag())
    final override fun decodeBoolean(): Boolean = readTaggedBoolean(popTag())
    final override fun decodeByte(): Byte = readTaggedByte(popTag())
    final override fun decodeShort(): Short = readTaggedShort(popTag())
    final override fun decodeInt(): Int = readTaggedInt(popTag())
    final override fun decodeLong(): Long = readTaggedLong(popTag())
    final override fun decodeFloat(): Float = readTaggedFloat(popTag())
    final override fun decodeDouble(): Double = readTaggedDouble(popTag())
    final override fun decodeChar(): Char = readTaggedChar(popTag())
    final override fun decodeString(): String = readTaggedString(popTag())
    @Deprecated("Not supported in Native", replaceWith = ReplaceWith("decodeEnum(enumLoader)"))
    final override fun <T : Enum<T>> decodeEnum(enumClass: KClass<T>): T = decodeEnum(LegacyEnumCreator(enumClass))

    final override fun <T : Enum<T>> decodeEnum(enumCreator: EnumCreator<T>): T = readTaggedEnum(popTag(), enumCreator)

    // Override for custom behaviour
    override fun decodeElementIndex(desc: SerialDescriptor): Int = READ_ALL

    final override fun decodeElementValue(desc: SerialDescriptor, index: Int): Any = readTaggedValue(desc.getTag(index))
    final override fun decodeNullableElementValue(desc: SerialDescriptor, index: Int): Any? = readTaggedNullable(desc.getTag(index))
    final override fun decodeUnitElement(desc: SerialDescriptor, index: Int) = readTaggedUnit(desc.getTag(index))
    final override fun decodeBooleanElement(desc: SerialDescriptor, index: Int): Boolean = readTaggedBoolean(desc.getTag(index))
    final override fun decodeByteElement(desc: SerialDescriptor, index: Int): Byte = readTaggedByte(desc.getTag(index))
    final override fun decodeShortElement(desc: SerialDescriptor, index: Int): Short = readTaggedShort(desc.getTag(index))
    final override fun decodeIntElement(desc: SerialDescriptor, index: Int): Int = readTaggedInt(desc.getTag(index))
    final override fun decodeLongElement(desc: SerialDescriptor, index: Int): Long = readTaggedLong(desc.getTag(index))
    final override fun decodeFloatElement(desc: SerialDescriptor, index: Int): Float = readTaggedFloat(desc.getTag(index))
    final override fun decodeDoubleElement(desc: SerialDescriptor, index: Int): Double = readTaggedDouble(desc.getTag(index))
    final override fun decodeCharElement(desc: SerialDescriptor, index: Int): Char = readTaggedChar(desc.getTag(index))
    final override fun decodeStringElement(desc: SerialDescriptor, index: Int): String = readTaggedString(desc.getTag(index))
    @Deprecated("Not supported in Native", replaceWith = ReplaceWith("decodeEnum(enumLoader)"))
    final override fun <T : Enum<T>> decodeEnumElementValue(desc: SerialDescriptor, index: Int, enumClass: KClass<T>): T = decodeEnumElementValue(desc, index, LegacyEnumCreator(enumClass))

    final override fun <T : Enum<T>> decodeEnumElementValue(desc: SerialDescriptor, index: Int, enumCreator: EnumCreator<T>): T = readTaggedEnum(desc.getTag(index), enumCreator)

    final override fun <T : Any?> decodeSerializableElement(desc: SerialDescriptor, index: Int, loader: DeserializationStrategy<T>): T {
        return tagBlock(desc.getTag(index)) { decodeSerializableValue(loader) }
    }

    final override fun <T : Any> decodeNullableSerializableElement(desc: SerialDescriptor, index: Int, loader: DeserializationStrategy<T?>): T? {
        return tagBlock(desc.getTag(index)) { decodeNullableSerializableValue(loader) }
    }

    override fun <T> updateSerializableElementValue(desc: SerialDescriptor, index: Int, loader: DeserializationStrategy<T>, old: T): T {
        return tagBlock(desc.getTag(index)) { updateSerializableValue(loader, desc, old) }
    }

    override fun <T : Any> updateNullableSerializableElementValue(desc: SerialDescriptor, index: Int, loader: DeserializationStrategy<T?>, old: T?): T? {
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
    final override fun SerialDescriptor.getTag(index: Int): Int? = getSerialId(this, index)
}

abstract class StringTaggedInput : TaggedInput<String?>() {
    final override fun SerialDescriptor.getTag(index: Int): String? = getSerialTag(this, index)
}

abstract class NamedValueInput(val rootName: String = "") : TaggedInput<String>() {
    final override fun SerialDescriptor.getTag(index: Int): String = composeName(currentTagOrNull ?: rootName, elementName(this, index))

    open fun elementName(desc: SerialDescriptor, index: Int) = desc.getElementName(index)
    open fun composeName(parentName: String, childName: String) = if (parentName.isEmpty()) childName else parentName + "." + childName
}
