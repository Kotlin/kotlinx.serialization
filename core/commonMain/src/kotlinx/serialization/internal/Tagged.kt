/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:OptIn(ExperimentalSerializationApi::class)

package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*

/*
 * These classes are intended to be used only within the kotlinx.serialization.
 * They neither do have stable API, nor internal invariants and are changed without any warnings.
 */
@InternalSerializationApi
public abstract class TaggedEncoder<Tag : Any?> : Encoder, CompositeEncoder {

    /**
     * Provides a tag object for given serial descriptor and index.
     * Tag object allows associating given user information with a particular element of composite serializable entity.
     */
    protected abstract fun SerialDescriptor.getTag(index: Int): Tag

    override val serializersModule: SerializersModule
        get() = EmptySerializersModule

    // ---- API ----
    protected open fun encodeTaggedValue(tag: Tag, value: Any): Unit =
        throw SerializationException("Non-serializable ${value::class} is not supported by ${this::class} encoder")

    protected open fun encodeTaggedNull(tag: Tag): Unit = throw SerializationException("null is not supported")
    protected open fun encodeTaggedInt(tag: Tag, value: Int): Unit = encodeTaggedValue(tag, value)
    protected open fun encodeTaggedByte(tag: Tag, value: Byte): Unit = encodeTaggedValue(tag, value)
    protected open fun encodeTaggedShort(tag: Tag, value: Short): Unit = encodeTaggedValue(tag, value)
    protected open fun encodeTaggedLong(tag: Tag, value: Long): Unit = encodeTaggedValue(tag, value)
    protected open fun encodeTaggedFloat(tag: Tag, value: Float): Unit = encodeTaggedValue(tag, value)
    protected open fun encodeTaggedDouble(tag: Tag, value: Double): Unit = encodeTaggedValue(tag, value)
    protected open fun encodeTaggedBoolean(tag: Tag, value: Boolean): Unit = encodeTaggedValue(tag, value)
    protected open fun encodeTaggedChar(tag: Tag, value: Char): Unit = encodeTaggedValue(tag, value)
    protected open fun encodeTaggedString(tag: Tag, value: String): Unit = encodeTaggedValue(tag, value)

    protected open fun encodeTaggedEnum(
        tag: Tag,
        enumDescriptor: SerialDescriptor,
        ordinal: Int
    ): Unit = encodeTaggedValue(tag, ordinal)

    protected open fun encodeTaggedInline(tag: Tag, inlineDescriptor: SerialDescriptor): Encoder =
        this.apply { pushTag(tag) }

    final override fun encodeInline(inlineDescriptor: SerialDescriptor): Encoder =
        encodeTaggedInline(popTag(), inlineDescriptor)

    // ---- Implementation of low-level API ----

    private fun encodeElement(desc: SerialDescriptor, index: Int): Boolean {
        val tag = desc.getTag(index)
        pushTag(tag)
        return true
    }

    final override fun encodeNotNullMark() {} // Does nothing, open because is not really required
    open override fun encodeNull(): Unit = encodeTaggedNull(popTag())
    final override fun encodeBoolean(value: Boolean): Unit = encodeTaggedBoolean(popTag(), value)
    final override fun encodeByte(value: Byte): Unit = encodeTaggedByte(popTag(), value)
    final override fun encodeShort(value: Short): Unit = encodeTaggedShort(popTag(), value)
    final override fun encodeInt(value: Int): Unit = encodeTaggedInt(popTag(), value)
    final override fun encodeLong(value: Long): Unit = encodeTaggedLong(popTag(), value)
    final override fun encodeFloat(value: Float): Unit = encodeTaggedFloat(popTag(), value)
    final override fun encodeDouble(value: Double): Unit = encodeTaggedDouble(popTag(), value)
    final override fun encodeChar(value: Char): Unit = encodeTaggedChar(popTag(), value)
    final override fun encodeString(value: String): Unit = encodeTaggedString(popTag(), value)

    final override fun encodeEnum(
        enumDescriptor: SerialDescriptor,
        index: Int
    ): Unit = encodeTaggedEnum(popTag(), enumDescriptor, index)

    override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder = this

    final override fun endStructure(descriptor: SerialDescriptor) {
        if (tagStack.isNotEmpty()) {
            popTag()
        }
        endEncode(descriptor)
    }

    /**
     * Format-specific replacement for [endStructure], because latter is overridden to manipulate tag stack.
     */
    protected open fun endEncode(descriptor: SerialDescriptor) {}

    final override fun encodeBooleanElement(descriptor: SerialDescriptor, index: Int, value: Boolean): Unit =
        encodeTaggedBoolean(descriptor.getTag(index), value)

    final override fun encodeByteElement(descriptor: SerialDescriptor, index: Int, value: Byte): Unit =
        encodeTaggedByte(descriptor.getTag(index), value)

    final override fun encodeShortElement(descriptor: SerialDescriptor, index: Int, value: Short): Unit =
        encodeTaggedShort(descriptor.getTag(index), value)

    final override fun encodeIntElement(descriptor: SerialDescriptor, index: Int, value: Int): Unit =
        encodeTaggedInt(descriptor.getTag(index), value)

    final override fun encodeLongElement(descriptor: SerialDescriptor, index: Int, value: Long): Unit =
        encodeTaggedLong(descriptor.getTag(index), value)

    final override fun encodeFloatElement(descriptor: SerialDescriptor, index: Int, value: Float): Unit =
        encodeTaggedFloat(descriptor.getTag(index), value)

    final override fun encodeDoubleElement(descriptor: SerialDescriptor, index: Int, value: Double): Unit =
        encodeTaggedDouble(descriptor.getTag(index), value)

    final override fun encodeCharElement(descriptor: SerialDescriptor, index: Int, value: Char): Unit =
        encodeTaggedChar(descriptor.getTag(index), value)

    final override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String): Unit =
        encodeTaggedString(descriptor.getTag(index), value)

    final override fun encodeInlineElement(
        descriptor: SerialDescriptor,
        index: Int
    ): Encoder {
        return encodeTaggedInline(descriptor.getTag(index), descriptor.getElementDescriptor(index))
    }

    override fun <T : Any?> encodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T
    ) {
        if (encodeElement(descriptor, index))
            encodeSerializableValue(serializer, value)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun <T : Any> encodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T?
    ) {
        if (encodeElement(descriptor, index))
            encodeNullableSerializableValue(serializer, value)
    }

    private val tagStack = arrayListOf<Tag>()
    protected val currentTag: Tag
        get() = tagStack.last()
    protected val currentTagOrNull: Tag?
        get() = tagStack.lastOrNull()

    protected fun pushTag(name: Tag) {
        tagStack.add(name)
    }

    protected fun popTag(): Tag =
        if (tagStack.isNotEmpty())
            tagStack.removeAt(tagStack.lastIndex)
        else
            throw SerializationException("No tag in stack for requested element")
}

@InternalSerializationApi
@OptIn(ExperimentalSerializationApi::class)
public abstract class NamedValueEncoder : TaggedEncoder<String>() {
    final override fun SerialDescriptor.getTag(index: Int): String = nested(elementName(this, index))
    protected fun nested(nestedName: String): String = composeName(currentTagOrNull ?: "", nestedName)
    protected open fun elementName(descriptor: SerialDescriptor, index: Int): String = descriptor.getElementName(index)
    protected open fun composeName(parentName: String, childName: String): String =
        if (parentName.isEmpty()) childName else "$parentName.$childName"
}

@InternalSerializationApi
public abstract class TaggedDecoder<Tag : Any?> : Decoder, CompositeDecoder {
    override val serializersModule: SerializersModule
        get() = EmptySerializersModule

    protected abstract fun SerialDescriptor.getTag(index: Int): Tag

    // ---- API ----
    protected open fun decodeTaggedValue(tag: Tag): Any =
        throw SerializationException("${this::class} can't retrieve untyped values")

    protected open fun decodeTaggedNotNullMark(tag: Tag): Boolean = true
    protected open fun decodeTaggedNull(tag: Tag): Nothing? = null

    protected open fun decodeTaggedBoolean(tag: Tag): Boolean = decodeTaggedValue(tag) as Boolean
    protected open fun decodeTaggedByte(tag: Tag): Byte = decodeTaggedValue(tag) as Byte
    protected open fun decodeTaggedShort(tag: Tag): Short = decodeTaggedValue(tag) as Short
    protected open fun decodeTaggedInt(tag: Tag): Int = decodeTaggedValue(tag) as Int
    protected open fun decodeTaggedLong(tag: Tag): Long = decodeTaggedValue(tag) as Long
    protected open fun decodeTaggedFloat(tag: Tag): Float = decodeTaggedValue(tag) as Float
    protected open fun decodeTaggedDouble(tag: Tag): Double = decodeTaggedValue(tag) as Double
    protected open fun decodeTaggedChar(tag: Tag): Char = decodeTaggedValue(tag) as Char
    protected open fun decodeTaggedString(tag: Tag): String = decodeTaggedValue(tag) as String
    protected open fun decodeTaggedEnum(tag: Tag, enumDescriptor: SerialDescriptor): Int =
        decodeTaggedValue(tag) as Int

    protected open fun decodeTaggedInline(tag: Tag, inlineDescriptor: SerialDescriptor): Decoder = this.apply { pushTag(tag) }

    protected open fun <T : Any?> decodeSerializableValue(deserializer: DeserializationStrategy<T>, previousValue: T?): T =
        decodeSerializableValue(deserializer)


    // ---- Implementation of low-level API ----

    final override fun decodeInline(inlineDescriptor: SerialDescriptor): Decoder =
        decodeTaggedInline(popTag(), inlineDescriptor)

    // TODO this method should be overridden by any sane format that supports top-level nulls
    override fun decodeNotNullMark(): Boolean {
        // Tag might be null for top-level deserialization
        val currentTag = currentTagOrNull ?: return false
        return decodeTaggedNotNullMark(currentTag)
    }

    final override fun decodeNull(): Nothing? = null

    final override fun decodeBoolean(): Boolean = decodeTaggedBoolean(popTag())
    final override fun decodeByte(): Byte = decodeTaggedByte(popTag())
    final override fun decodeShort(): Short = decodeTaggedShort(popTag())
    final override fun decodeInt(): Int = decodeTaggedInt(popTag())
    final override fun decodeLong(): Long = decodeTaggedLong(popTag())
    final override fun decodeFloat(): Float = decodeTaggedFloat(popTag())
    final override fun decodeDouble(): Double = decodeTaggedDouble(popTag())
    final override fun decodeChar(): Char = decodeTaggedChar(popTag())
    final override fun decodeString(): String = decodeTaggedString(popTag())

    final override fun decodeEnum(enumDescriptor: SerialDescriptor): Int = decodeTaggedEnum(popTag(), enumDescriptor)

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder = this

    override fun endStructure(descriptor: SerialDescriptor) {
        // Nothing
    }

    final override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int): Boolean =
        decodeTaggedBoolean(descriptor.getTag(index))

    final override fun decodeByteElement(descriptor: SerialDescriptor, index: Int): Byte =
        decodeTaggedByte(descriptor.getTag(index))

    final override fun decodeShortElement(descriptor: SerialDescriptor, index: Int): Short =
        decodeTaggedShort(descriptor.getTag(index))

    final override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int =
        decodeTaggedInt(descriptor.getTag(index))

    final override fun decodeLongElement(descriptor: SerialDescriptor, index: Int): Long =
        decodeTaggedLong(descriptor.getTag(index))

    final override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int): Float =
        decodeTaggedFloat(descriptor.getTag(index))

    final override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int): Double =
        decodeTaggedDouble(descriptor.getTag(index))

    final override fun decodeCharElement(descriptor: SerialDescriptor, index: Int): Char =
        decodeTaggedChar(descriptor.getTag(index))

    final override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String =
        decodeTaggedString(descriptor.getTag(index))

    final override fun decodeInlineElement(
        descriptor: SerialDescriptor,
        index: Int
    ): Decoder = decodeTaggedInline(descriptor.getTag(index), descriptor.getElementDescriptor(index))

    final override fun <T : Any?> decodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
        previousValue: T?
    ): T =
        tagBlock(descriptor.getTag(index)) { decodeSerializableValue(deserializer, previousValue) }

    final override fun <T : Any> decodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T?>,
        previousValue: T?
    ): T? =
        tagBlock(descriptor.getTag(index)) {
            if (decodeNotNullMark()) decodeSerializableValue(
                deserializer,
                previousValue
            ) else decodeNull()
        }

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
    protected val currentTagOrNull: Tag?
        get() = tagStack.lastOrNull()

    protected fun pushTag(name: Tag) {
        tagStack.add(name)
    }

    protected fun copyTagsTo(other: TaggedDecoder<Tag>) {
        other.tagStack.addAll(tagStack)
    }

    private var flag = false

    protected fun popTag(): Tag {
        val r = tagStack.removeAt(tagStack.lastIndex)
        flag = true
        return r
    }
}

@InternalSerializationApi
@OptIn(ExperimentalSerializationApi::class)
public abstract class NamedValueDecoder : TaggedDecoder<String>() {
    final override fun SerialDescriptor.getTag(index: Int): String = nested(elementName(this, index))

    protected fun nested(nestedName: String): String = composeName(currentTagOrNull ?: "", nestedName)
    protected open fun elementName(desc: SerialDescriptor, index: Int): String = desc.getElementName(index)
    protected open fun composeName(parentName: String, childName: String): String =
        if (parentName.isEmpty()) childName else "$parentName.$childName"
}
