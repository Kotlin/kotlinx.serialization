/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlinx.serialization.modules.*

import kotlinx.serialization.modules.*

internal const val unitDeprecated =
    "This method is deprecated with no replacement. Unit is encoded as an empty object and does not require a dedicated method. " +
            "To migrate, just remove your own implementation of this method"

/*
 * These classes are intended to be used only within the kotlinx.serialization.
 * They neither do have stable API, not internal invariants and are changed without any warnings.
 */

@InternalSerializationApi
abstract class TaggedEncoder<Tag : Any?> : Encoder, CompositeEncoder {

    /**
     * Provides a tag object for given serial descriptor and index.
     * Tag object allows to associate given user information with particular element of composite serializable entity..
     */
    protected abstract fun SerialDescriptor.getTag(index: Int): Tag

    override val context: SerialModule
        get() = EmptyModule

    // ---- API ----
    open fun encodeTaggedValue(tag: Tag, value: Any): Unit = throw SerializationException("Non-serializable ${value::class} is not supported by ${this::class} encoder")

    open fun encodeTaggedNotNullMark(tag: Tag) {}
    open fun encodeTaggedNull(tag: Tag): Unit = throw SerializationException("null is not supported")
    @Deprecated(message = unitDeprecated, level = DeprecationLevel.ERROR)
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
        enumDescription: SerialDescriptor,
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
    @Suppress("DEPRECATION_ERROR")
    final override fun encodeUnit() = UnitSerializer.serialize(this, Unit)
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
        enumDescriptor: SerialDescriptor,
        index: Int
    ) = encodeTaggedEnum(popTag(), enumDescriptor, index)

    override fun beginStructure(descriptor: SerialDescriptor, vararg typeSerializers: KSerializer<*>): CompositeEncoder {
        return this
    }

    final override fun endStructure(descriptor: SerialDescriptor) {
        if (tagStack.isNotEmpty()) popTag(); endEncode(descriptor)
    }

    /**
     * Format-specific replacement for [endStructure], because latter is overridden to manipulate tag stack.
     */
    open fun endEncode(descriptor: SerialDescriptor) {}

    @Suppress("DEPRECATION_ERROR")
    final override fun encodeUnitElement(desc: SerialDescriptor, index: Int) = encodeTaggedUnit(desc.getTag(index))
    final override fun encodeBooleanElement(descriptor: SerialDescriptor, index: Int, value: Boolean) = encodeTaggedBoolean(descriptor.getTag(index), value)
    final override fun encodeByteElement(descriptor: SerialDescriptor, index: Int, value: Byte) = encodeTaggedByte(descriptor.getTag(index), value)
    final override fun encodeShortElement(descriptor: SerialDescriptor, index: Int, value: Short) = encodeTaggedShort(descriptor.getTag(index), value)
    final override fun encodeIntElement(descriptor: SerialDescriptor, index: Int, value: Int) = encodeTaggedInt(descriptor.getTag(index), value)
    final override fun encodeLongElement(descriptor: SerialDescriptor, index: Int, value: Long) = encodeTaggedLong(descriptor.getTag(index), value)
    final override fun encodeFloatElement(descriptor: SerialDescriptor, index: Int, value: Float) = encodeTaggedFloat(descriptor.getTag(index), value)
    final override fun encodeDoubleElement(descriptor: SerialDescriptor, index: Int, value: Double) = encodeTaggedDouble(descriptor.getTag(index), value)
    final override fun encodeCharElement(descriptor: SerialDescriptor, index: Int, value: Char) = encodeTaggedChar(descriptor.getTag(index), value)
    final override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String) = encodeTaggedString(descriptor.getTag(index), value)

    final override fun <T : Any?> encodeSerializableElement(descriptor: SerialDescriptor, index: Int, serializer: SerializationStrategy<T>, value: T) {
        if (encodeElement(descriptor, index))
            encodeSerializableValue(serializer, value)
    }

    final override fun <T : Any> encodeNullableSerializableElement(descriptor: SerialDescriptor, index: Int, serializer: SerializationStrategy<T>, value: T?) {
        if (encodeElement(descriptor, index))
            encodeNullableSerializableValue(serializer, value)
    }

    private val tagStack = arrayListOf<Tag>()
    protected val currentTag: Tag
        get() = tagStack.last()
    protected val currentTagOrNull
        get() = tagStack.lastOrNull()

    protected fun pushTag(name: Tag) {
        tagStack.add(name)
    }

    protected fun popTag() =
        if (tagStack.isNotEmpty())
            tagStack.removeAt(tagStack.lastIndex)
        else
            throw SerializationException("No tag in stack for requested element")
}

@InternalSerializationApi
abstract class NamedValueEncoder(val rootName: String = "") : TaggedEncoder<String>() {
    final override fun SerialDescriptor.getTag(index: Int): String = nested(elementName(this, index))
    protected fun nested(nestedName: String) = composeName(currentTagOrNull ?: rootName, nestedName)
    open fun elementName(descriptor: SerialDescriptor, index: Int) = descriptor.getElementName(index)
    open fun composeName(parentName: String, childName: String) = if (parentName.isEmpty()) childName else "$parentName.$childName"
}

@InternalSerializationApi
abstract class TaggedDecoder<Tag : Any?> : Decoder,
    CompositeDecoder {
    override val context: SerialModule
        get() = EmptyModule

    override val updateMode: UpdateMode =
        UpdateMode.UPDATE

    protected abstract fun SerialDescriptor.getTag(index: Int): Tag


    // ---- API ----
    open fun decodeTaggedValue(tag: Tag): Any =
        throw SerializationException("${this::class} can't retrieve untyped values")

    open fun decodeTaggedNotNullMark(tag: Tag): Boolean = true
    open fun decodeTaggedNull(tag: Tag): Nothing? = null

    @Deprecated(message = unitDeprecated, level = DeprecationLevel.ERROR)
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
    open fun decodeTaggedEnum(tag: Tag, enumDescription: SerialDescriptor): Int = decodeTaggedValue(tag) as Int


    // ---- Implementation of low-level API ----

    final override fun decodeNotNullMark(): Boolean = decodeTaggedNotNullMark(currentTag)
    final override fun decodeNull(): Nothing? = null

    @Deprecated(message = unitDeprecated, level = DeprecationLevel.ERROR)
    @Suppress("DEPRECATION_ERROR")
    final override fun decodeUnit() = UnitSerializer.deserialize(this)
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

    override fun beginStructure(descriptor: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
        return this
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        // Nothing
    }

    @Deprecated(message = unitDeprecated, level = DeprecationLevel.ERROR)
    @Suppress("DEPRECATION_ERROR")
    final override fun decodeUnitElement(desc: SerialDescriptor, index: Int) = decodeTaggedUnit(desc.getTag(index))
    final override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int): Boolean = decodeTaggedBoolean(descriptor.getTag(index))
    final override fun decodeByteElement(descriptor: SerialDescriptor, index: Int): Byte = decodeTaggedByte(descriptor.getTag(index))
    final override fun decodeShortElement(descriptor: SerialDescriptor, index: Int): Short = decodeTaggedShort(descriptor.getTag(index))
    final override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int = decodeTaggedInt(descriptor.getTag(index))
    final override fun decodeLongElement(descriptor: SerialDescriptor, index: Int): Long = decodeTaggedLong(descriptor.getTag(index))
    final override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int): Float = decodeTaggedFloat(descriptor.getTag(index))
    final override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int): Double = decodeTaggedDouble(descriptor.getTag(index))
    final override fun decodeCharElement(descriptor: SerialDescriptor, index: Int): Char = decodeTaggedChar(descriptor.getTag(index))
    final override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String = decodeTaggedString(descriptor.getTag(index))

    final override fun <T : Any?> decodeSerializableElement(descriptor: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T>): T =
        tagBlock(descriptor.getTag(index)) { decodeSerializableValue(deserializer) }

    final override fun <T : Any> decodeNullableSerializableElement(descriptor: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T?>): T? =
        tagBlock(descriptor.getTag(index)) { decodeNullableSerializableValue(deserializer) }

    override fun <T> updateSerializableElement(descriptor: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T>, old: T): T =
        tagBlock(descriptor.getTag(index)) { updateSerializableValue(deserializer, old) }

    override fun <T : Any> updateNullableSerializableElement(descriptor: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T?>, old: T?): T? =
        tagBlock(descriptor.getTag(index)) { updateNullableSerializableValue(deserializer, old) }

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
abstract class NamedValueDecoder(val rootName: String = "") : TaggedDecoder<String>() {
    final override fun SerialDescriptor.getTag(index: Int): String = nested(elementName(this, index))

    protected fun nested(nestedName: String) = composeName(currentTagOrNull ?: rootName, nestedName)
    open fun elementName(desc: SerialDescriptor, index: Int) = desc.getElementName(index)
    open fun composeName(parentName: String, childName: String) = if (parentName.isEmpty()) childName else "$parentName.$childName"
}
