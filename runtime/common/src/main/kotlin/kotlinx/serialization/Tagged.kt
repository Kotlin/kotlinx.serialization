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
import kotlinx.serialization.context.EmptyContext
import kotlinx.serialization.context.SerialContext
import kotlinx.serialization.internal.EnumDescriptor

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class SerialId(val id: Int)

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class SerialTag(val tag: String)


abstract class TaggedEncoder<Tag : Any?> : Encoder, CompositeEncoder {

    /**
     * Provides a tag object for given serial descriptor and index.
     * Tag object allows to associate given user information with particular element of composite serializable entity..
     */
    protected abstract fun SerialDescriptor.getTag(index: Int): Tag

    override var context: SerialContext = EmptyContext

    // ---- API ----
    open fun encodeTaggedValue(tag: Tag, value: Any): Unit
            = throw SerializationException("Non-serializable ${value::class} is not supported by ${this::class} encoder")

    open fun encodeTaggedNotNullMark(tag: Tag) {}
    open fun encodeTaggedNull(tag: Tag): Unit = throw SerializationException("null is not supported")

    private fun encodeTaggedNullable(tag: Tag, value: Any?) {
        if (value == null) {
            encodeTaggedNull(tag)
        } else {
            encodeTaggedNotNullMark(tag)
            encodeTaggedValue(tag, value)
        }
    }

    open fun encodeTaggedUnit(tag: Tag) = encodeTaggedValue(tag, Unit)
    open fun encodeTaggedInt(tag: Tag, value: Int) = encodeTaggedValue(tag, value)
    open fun encodeTaggedByte(tag: Tag, value: Byte) = encodeTaggedValue(tag, value)
    open fun encodeTaggedShort(tag: Tag, value: Short) = encodeTaggedValue(tag, value)
    open fun encodeTaggedLong(tag: Tag, value: Long) = encodeTaggedValue(tag, value)
    open fun encodeTaggedFloat(tag: Tag, value: Float) = encodeTaggedValue(tag, value)
    open fun encodeTaggedDouble(tag: Tag, value: Double) = encodeTaggedValue(tag, value)
    open fun encodeTaggedBoolean(tag: Tag, value: Boolean) = encodeTaggedValue(tag, value)
    open fun encodeTaggedChar(tag: Tag, value: Char) = encodeTaggedValue(tag, value)
    open fun encodeTaggedString(tag: Tag, value: String) = encodeTaggedValue(tag, value)

    open fun encodeTaggedEnum(
        tag: Tag,
        enumDescription: EnumDescriptor,
        ordinal: Int
    ) = encodeTaggedValue(tag, ordinal)

    // ---- Implementation of low-level API ----

    fun encodeElement(desc: SerialDescriptor, index: Int): Boolean {
        val tag = desc.getTag(index)
        val shouldWriteElement = shouldWriteElement(desc, tag, index)
        if (shouldWriteElement) {
            pushTag(tag)
        }
        return shouldWriteElement
    }

    // For format-specific behaviour, invoked only on
    open fun shouldWriteElement(desc: SerialDescriptor, tag: Tag, index: Int) = true

    final override fun encodeNotNullMark() = encodeTaggedNotNullMark(currentTag)
    final override fun encodeNull() = encodeTaggedNull(popTag())

    final override fun encodeUnit() = encodeTaggedUnit(popTag())
    final override fun encodeBoolean(value: Boolean) = encodeTaggedBoolean(popTag(), value)
    final override fun encodeByte(value: Byte) = encodeTaggedByte(popTag(), value)
    final override fun encodeShort(value: Short) = encodeTaggedShort(popTag(), value)
    final override fun encodeInt(value: Int) = encodeTaggedInt(popTag(), value)
    final override fun encodeLong(value: Long) = encodeTaggedLong(popTag(), value)
    final override fun encodeFloat(value: Float) = encodeTaggedFloat(popTag(), value)
    final override fun encodeDouble(value: Double) = encodeTaggedDouble(popTag(), value)
    final override fun encodeChar(value: Char) = encodeTaggedChar(popTag(), value)
    final override fun encodeString(value: String) = encodeTaggedString(popTag(), value)

    final override fun encodeEnum(
        enumDescription: EnumDescriptor,
        ordinal: Int
    ) = encodeTaggedEnum(popTag(), enumDescription, ordinal)

    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeEncoder {
        return this
    }

    final override fun endStructure(desc: SerialDescriptor) {
        if (tagStack.isNotEmpty()) popTag(); endEncode(desc)
    }

    /**
     * Format-specific replacement for [endStructure], because latter is overridden to manipulate tag stack.
     */
    open fun endEncode(desc: SerialDescriptor) {}

    final override fun encodeNonSerializableElement(desc: SerialDescriptor, index: Int, value: Any) = encodeTaggedValue(desc.getTag(index), value)

    final override fun encodeUnitElement(desc: SerialDescriptor, index: Int) = encodeTaggedUnit(desc.getTag(index))
    final override fun encodeBooleanElement(desc: SerialDescriptor, index: Int, value: Boolean) = encodeTaggedBoolean(desc.getTag(index), value)
    final override fun encodeByteElement(desc: SerialDescriptor, index: Int, value: Byte) = encodeTaggedByte(desc.getTag(index), value)
    final override fun encodeShortElement(desc: SerialDescriptor, index: Int, value: Short) = encodeTaggedShort(desc.getTag(index), value)
    final override fun encodeIntElement(desc: SerialDescriptor, index: Int, value: Int) = encodeTaggedInt(desc.getTag(index), value)
    final override fun encodeLongElement(desc: SerialDescriptor, index: Int, value: Long) = encodeTaggedLong(desc.getTag(index), value)
    final override fun encodeFloatElement(desc: SerialDescriptor, index: Int, value: Float) = encodeTaggedFloat(desc.getTag(index), value)
    final override fun encodeDoubleElement(desc: SerialDescriptor, index: Int, value: Double) = encodeTaggedDouble(desc.getTag(index), value)
    final override fun encodeCharElement(desc: SerialDescriptor, index: Int, value: Char) = encodeTaggedChar(desc.getTag(index), value)
    final override fun encodeStringElement(desc: SerialDescriptor, index: Int, value: String) = encodeTaggedString(desc.getTag(index), value)

    final override fun <T : Any?> encodeSerializableElement(desc: SerialDescriptor, index: Int, serializer: SerializationStrategy<T>, value: T) {
        if (encodeElement(desc, index))
            encodeSerializableValue(serializer, value)
    }

    final override fun <T : Any> encodeNullableSerializableElement(desc: SerialDescriptor, index: Int, serializer: SerializationStrategy<T>, value: T?) {
        if (encodeElement(desc, index))
            encodeNullableSerializableValue(serializer, value)
    }

    private val tagStack = arrayListOf<Tag>()
    protected val currentTag: Tag
        get() = tagStack.last()
    protected val currentTagOrNull
        get() = tagStack.lastOrNull()

    private fun pushTag(name: Tag) {
        tagStack.add(name)
    }

    private fun popTag() =
        if (tagStack.isNotEmpty())
            tagStack.removeAt(tagStack.lastIndex)
        else
            throw SerializationException("No tag in stack for requested element")
}

abstract class IntTaggedEncoder : TaggedEncoder<Int?>() {
    final override fun SerialDescriptor.getTag(index: Int): Int? = getSerialId(this, index)
}

abstract class StringTaggedEncoder : TaggedEncoder<String?>() {
    final override fun SerialDescriptor.getTag(index: Int): String? = getSerialTag(this, index)
}

abstract class NamedValueEncoder(val rootName: String = "") : TaggedEncoder<String>() {
    final override fun SerialDescriptor.getTag(index: Int): String = nested(elementName(this, index))

    protected fun nested(nestedName: String) = composeName(currentTagOrNull ?: rootName, nestedName)
    open fun elementName(desc: SerialDescriptor, index: Int) = desc.getElementName(index)
    open fun composeName(parentName: String, childName: String) = if (parentName.isEmpty()) childName else parentName + "." + childName
}

// Helpers for native

expect fun getSerialId(desc: SerialDescriptor, index: Int): Int?
expect fun getSerialTag(desc: SerialDescriptor, index: Int): String?

abstract class TaggedDecoder<Tag : Any?> : Decoder, CompositeDecoder {
    override var context: SerialContext = EmptyContext
    override val updateMode: UpdateMode = UpdateMode.UPDATE

    protected abstract fun SerialDescriptor.getTag(index: Int): Tag


    // ---- API ----
    open fun decodeTaggedValue(tag: Tag): Any
            = throw SerializationException("${this::class} can't retrieve untyped values")

    open fun decodeTaggedNotNullMark(tag: Tag): Boolean = true
    open fun decodeTaggedNull(tag: Tag): Nothing? = null

    private fun decodeTaggedNullable(tag: Tag): Any? {
        return if (decodeTaggedNotNullMark(tag)) {
            decodeTaggedValue(tag)
        } else {
            decodeTaggedNull(tag)
        }
    }

    open fun decodeTaggedUnit(tag: Tag): Unit = decodeTaggedValue(tag) as Unit
    open fun decodeTaggedBoolean(tag: Tag): Boolean = decodeTaggedValue(tag) as Boolean
    open fun decodeTaggedByte(tag: Tag): Byte = decodeTaggedValue(tag) as Byte
    open fun decodeTaggedShort(tag: Tag): Short = decodeTaggedValue(tag) as Short
    open fun decodeTaggedInt(tag: Tag): Int = decodeTaggedValue(tag) as Int
    open fun decodeTaggedLong(tag: Tag): Long = decodeTaggedValue(tag) as Long
    open fun decodeTaggedFloat(tag: Tag): Float = decodeTaggedValue(tag) as Float
    open fun decodeTaggedDouble(tag: Tag): Double = decodeTaggedValue(tag) as Double
    open fun decodeTaggedChar(tag: Tag): Char = decodeTaggedValue(tag) as Char
    open fun decodeTaggedString(tag: Tag): String = decodeTaggedValue(tag) as String
    open fun decodeTaggedEnum(tag: Tag, enumDescription: EnumDescriptor): Int = decodeTaggedValue(tag) as Int


    // ---- Implementation of low-level API ----

    final override fun decodeNotNullMark(): Boolean = decodeTaggedNotNullMark(currentTag)
    final override fun decodeNull(): Nothing? = null

    final override fun decodeUnit() = decodeTaggedUnit(popTag())
    final override fun decodeBoolean(): Boolean = decodeTaggedBoolean(popTag())
    final override fun decodeByte(): Byte = decodeTaggedByte(popTag())
    final override fun decodeShort(): Short = decodeTaggedShort(popTag())
    final override fun decodeInt(): Int = decodeTaggedInt(popTag())
    final override fun decodeLong(): Long = decodeTaggedLong(popTag())
    final override fun decodeFloat(): Float = decodeTaggedFloat(popTag())
    final override fun decodeDouble(): Double = decodeTaggedDouble(popTag())
    final override fun decodeChar(): Char = decodeTaggedChar(popTag())
    final override fun decodeString(): String = decodeTaggedString(popTag())

    final override fun decodeEnum(enumDescription: EnumDescriptor): Int = decodeTaggedEnum(popTag(), enumDescription)

    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
        return this
    }

    /**
     * Assumes that all elements go in order by default.
     */
    override fun decodeElementIndex(desc: SerialDescriptor): Int = READ_ALL

    final override fun decodeUnitElement(desc: SerialDescriptor, index: Int) = decodeTaggedUnit(desc.getTag(index))
    final override fun decodeBooleanElement(desc: SerialDescriptor, index: Int): Boolean = decodeTaggedBoolean(desc.getTag(index))
    final override fun decodeByteElement(desc: SerialDescriptor, index: Int): Byte = decodeTaggedByte(desc.getTag(index))
    final override fun decodeShortElement(desc: SerialDescriptor, index: Int): Short = decodeTaggedShort(desc.getTag(index))
    final override fun decodeIntElement(desc: SerialDescriptor, index: Int): Int = decodeTaggedInt(desc.getTag(index))
    final override fun decodeLongElement(desc: SerialDescriptor, index: Int): Long = decodeTaggedLong(desc.getTag(index))
    final override fun decodeFloatElement(desc: SerialDescriptor, index: Int): Float = decodeTaggedFloat(desc.getTag(index))
    final override fun decodeDoubleElement(desc: SerialDescriptor, index: Int): Double = decodeTaggedDouble(desc.getTag(index))
    final override fun decodeCharElement(desc: SerialDescriptor, index: Int): Char = decodeTaggedChar(desc.getTag(index))
    final override fun decodeStringElement(desc: SerialDescriptor, index: Int): String = decodeTaggedString(desc.getTag(index))

    final override fun <T : Any?> decodeSerializableElement(desc: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T>): T =
        tagBlock(desc.getTag(index)) { decodeSerializableValue(deserializer) }

    final override fun <T : Any> decodeNullableSerializableElement(desc: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T?>): T? =
        tagBlock(desc.getTag(index)) { decodeNullableSerializableValue(deserializer) }

    override fun <T> updateSerializableElement(desc: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T>, old: T): T =
        tagBlock(desc.getTag(index)) { updateSerializableValue(deserializer, old) }

    override fun <T : Any> updateNullableSerializableElement(desc: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T?>, old: T?): T? =
        tagBlock(desc.getTag(index)) { updateNullableSerializableValue(deserializer, old) }

    private fun <E> tagBlock(tag: Tag, block: () -> E): E {
        pushTag(tag)
        val r = block()
        if (!flag) {
            popTag()
        }
        flag = false
        return r
    }

    private val tagStack = arrayListOf<Tag>()
    protected val currentTag: Tag
        get() = tagStack.last()
    protected val currentTagOrNull
        get() = tagStack.lastOrNull()

    private fun pushTag(name: Tag) {
        tagStack.add(name)
    }

    private var flag = false

    private fun popTag(): Tag {
        val r = tagStack.removeAt(tagStack.lastIndex)
        flag = true
        return r
    }
}

abstract class IntTaggedDecoder: TaggedDecoder<Int?>() {
    final override fun SerialDescriptor.getTag(index: Int): Int? = getSerialId(this, index)
}

abstract class StringTaggedDecoder : TaggedDecoder<String?>() {
    final override fun SerialDescriptor.getTag(index: Int): String? = getSerialTag(this, index)
}

abstract class NamedValueDecoder(val rootName: String = "") : TaggedDecoder<String>() {
    final override fun SerialDescriptor.getTag(index: Int): String = nested(elementName(this, index))

    protected fun nested(nestedName: String) = composeName(currentTagOrNull ?: rootName, nestedName)
    open fun elementName(desc: SerialDescriptor, index: Int) = desc.getElementName(index)
    open fun composeName(parentName: String, childName: String) = if (parentName.isEmpty()) childName else parentName + "." + childName
}
