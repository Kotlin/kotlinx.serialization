/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.encoding

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.modules.*


/**
 * Encodes elements in a user-defined order managed by [mapElementIndex].
 *
 * This encoder will replicate the behavior of a standard encoding, but calling the `encode*Element` methods in
 * the order defined by [mapElementIndex]. It first buffers each `encode*Element` calls in an array following
 * the given indexes using [mapElementIndex], then when [endStructure] is called, it encodes the buffered calls
 * in the expected order by replaying the previous calls on the given [compositeEncoderDelegate].
 *
 * This encoder is stateful and not designed to be reused.
 *
 * @param compositeEncoderDelegate the [CompositeEncoder] to be used to encode the given descriptor's elements in the expected order.
 * @param encodedElementsCount The final number of elements to encode, which could be smaller than the original descriptor when [mapElementIndex] returns [SKIP_ELEMENT_INDEX] or when the index mapper has returned the same index twice.
 * @param mapElementIndex maps the element index to a new positional zero-based index.
 *                          The mapped index just helps to reorder the elements,
 *                          but the reordered `encode*Element` method calls will still pass the original element index.
 *                          If this mapper returns [SKIP_ELEMENT_INDEX] or -1, the element will be ignored and not encoded.
 *                          If this mapper provides the same index for multiple elements,
 *                          only the last one will be encoded as the previous ones will be overridden.
 */
@ExperimentalSerializationApi
public class ReorderingCompositeEncoder(
    encodedElementsCount: Int,
    private val compositeEncoderDelegate: CompositeEncoder,
    private val mapElementIndex: (SerialDescriptor, Int) -> Int,
) : CompositeEncoder {
    private val bufferedCalls = Array<BufferedCall?>(encodedElementsCount) { null }

    public companion object {
        @ExperimentalSerializationApi
        public const val SKIP_ELEMENT_INDEX: Int = -1
    }

    override val serializersModule: SerializersModule
        // No need to return a serializers module as it's not used during buffering
        get() = EmptySerializersModule()

    private data class BufferedCall(
        val originalElementIndex: Int,
        val encoder: () -> Unit,
    )

    private fun bufferEncoding(
        descriptor: SerialDescriptor,
        index: Int,
        encoder: () -> Unit
    ) {
        val newIndex = mapElementIndex(descriptor, index)
        if (newIndex != SKIP_ELEMENT_INDEX) {
            bufferedCalls[newIndex] = BufferedCall(index, encoder)
        }
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        bufferedCalls.forEach { fieldToEncode ->
            // In case of skipped fields, overridden fields (mapped to same index) or too big [encodedElementsCount],
            // the fieldToEncode may be null as no element was encoded for that index
            fieldToEncode?.encoder?.invoke()
        }
        compositeEncoderDelegate.endStructure(descriptor)
    }

    override fun encodeBooleanElement(descriptor: SerialDescriptor, index: Int, value: Boolean) {
        bufferEncoding(descriptor, index) {
            compositeEncoderDelegate.encodeBooleanElement(descriptor, index, value)
        }
    }

    override fun encodeByteElement(descriptor: SerialDescriptor, index: Int, value: Byte) {
        bufferEncoding(descriptor, index) {
            compositeEncoderDelegate.encodeByteElement(descriptor, index, value)
        }
    }

    override fun encodeCharElement(descriptor: SerialDescriptor, index: Int, value: Char) {
        bufferEncoding(descriptor, index) {
            compositeEncoderDelegate.encodeCharElement(descriptor, index, value)
        }
    }

    override fun encodeDoubleElement(descriptor: SerialDescriptor, index: Int, value: Double) {
        bufferEncoding(descriptor, index) {
            compositeEncoderDelegate.encodeDoubleElement(descriptor, index, value)
        }
    }

    override fun encodeFloatElement(descriptor: SerialDescriptor, index: Int, value: Float) {
        bufferEncoding(descriptor, index) {
            compositeEncoderDelegate.encodeFloatElement(descriptor, index, value)
        }
    }

    override fun encodeIntElement(descriptor: SerialDescriptor, index: Int, value: Int) {
        bufferEncoding(descriptor, index) {
            compositeEncoderDelegate.encodeIntElement(descriptor, index, value)
        }
    }

    override fun encodeLongElement(descriptor: SerialDescriptor, index: Int, value: Long) {
        bufferEncoding(descriptor, index) {
            compositeEncoderDelegate.encodeLongElement(descriptor, index, value)
        }
    }

    override fun <T : Any> encodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T?
    ) {
        bufferEncoding(descriptor, index) {
            compositeEncoderDelegate.encodeNullableSerializableElement(descriptor, index, serializer, value)
        }
    }

    override fun <T> encodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        serializer: SerializationStrategy<T>,
        value: T
    ) {
        bufferEncoding(descriptor, index) {
            compositeEncoderDelegate.encodeSerializableElement(descriptor, index, serializer, value)
        }
    }

    override fun encodeShortElement(descriptor: SerialDescriptor, index: Int, value: Short) {
        bufferEncoding(descriptor, index) {
            compositeEncoderDelegate.encodeShortElement(descriptor, index, value)
        }
    }

    override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String) {
        bufferEncoding(descriptor, index) {
            compositeEncoderDelegate.encodeStringElement(descriptor, index, value)
        }
    }

    override fun encodeInlineElement(descriptor: SerialDescriptor, index: Int): Encoder {
        return BufferingInlineEncoder(descriptor, index)
    }

    override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean {
        return compositeEncoderDelegate.shouldEncodeElementDefault(descriptor, index)
    }

    private inner class BufferingInlineEncoder(
        private val descriptor: SerialDescriptor,
        private val elementIndex: Int,
    ) : Encoder {
        private var encodeNotNullMarkCalled = false

        override val serializersModule: SerializersModule
            get() = this@ReorderingCompositeEncoder.serializersModule

        private fun bufferEncoding(encoder: Encoder.() -> Unit) {
            bufferEncoding(descriptor, elementIndex) {
                compositeEncoderDelegate.encodeInlineElement(descriptor, elementIndex).apply {
                    if (encodeNotNullMarkCalled) {
                        encodeNotNullMark()
                    }
                    encoder()
                }
            }
        }

        override fun encodeNotNullMark() {
            encodeNotNullMarkCalled = true
        }

        override fun <T : Any> encodeNullableSerializableValue(serializer: SerializationStrategy<T>, value: T?) {
            bufferEncoding { encodeNullableSerializableValue(serializer, value) }
        }

        override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
            bufferEncoding { encodeSerializableValue(serializer, value) }
        }

        override fun encodeBoolean(value: Boolean) {
            bufferEncoding { encodeBoolean(value) }
        }

        override fun encodeByte(value: Byte) {
            bufferEncoding { encodeByte(value) }
        }

        override fun encodeChar(value: Char) {
            bufferEncoding { encodeChar(value) }
        }

        override fun encodeDouble(value: Double) {
            bufferEncoding { encodeDouble(value) }
        }

        override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
            bufferEncoding { encodeEnum(enumDescriptor, index) }
        }

        override fun encodeFloat(value: Float) {
            bufferEncoding { encodeFloat(value) }
        }

        override fun encodeInt(value: Int) {
            bufferEncoding { encodeInt(value) }
        }

        override fun encodeLong(value: Long) {
            bufferEncoding { encodeLong(value) }
        }

        @ExperimentalSerializationApi
        override fun encodeNull() {
            bufferEncoding { encodeNull() }
        }

        override fun encodeShort(value: Short) {
            bufferEncoding { encodeShort(value) }
        }

        override fun encodeString(value: String) {
            bufferEncoding { encodeString(value) }
        }

        override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
            unexpectedCall(::beginStructure.name)
        }

        override fun encodeInline(descriptor: SerialDescriptor): Encoder {
            unexpectedCall(::encodeInline.name)
        }

        private fun unexpectedCall(methodName: String): Nothing {
            // This method is normally called from within encodeSerializableValue or encodeNullableSerializableValue which is buffered, so we should never go here during buffering as it will be delegated to the concrete CompositeEncoder
            throw UnsupportedOperationException("Non-standard usage of ${CompositeEncoder::class.simpleName}: $methodName should be called from within encodeSerializableValue or encodeNullableSerializableValue")
        }
    }
}