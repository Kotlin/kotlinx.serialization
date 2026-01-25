package kotlinx.serialization.cbor

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.cbor.internal.CborDecodingException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CborElementSerializerBehaviorTest {

    private object NonCborEncoder : Encoder {
        override val serializersModule: SerializersModule = EmptySerializersModule()
        override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder = error("unused")
        override fun encodeBoolean(value: Boolean) = error("unused")
        override fun encodeByte(value: Byte) = error("unused")
        override fun encodeChar(value: Char) = error("unused")
        override fun encodeDouble(value: Double) = error("unused")
        override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) = error("unused")
        override fun encodeFloat(value: Float) = error("unused")
        override fun encodeInline(descriptor: SerialDescriptor): Encoder = error("unused")
        override fun encodeInt(value: Int) = error("unused")
        override fun encodeLong(value: Long) = error("unused")
        override fun encodeNotNullMark() = error("unused")
        override fun encodeNull() = error("unused")
        override fun encodeShort(value: Short) = error("unused")
        override fun encodeString(value: String) = error("unused")
        override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) = error("unused")
    }

    private object NonCborDecoder : Decoder {
        override val serializersModule: SerializersModule = EmptySerializersModule()
        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder = error("unused")
        override fun decodeBoolean(): Boolean = error("unused")
        override fun decodeByte(): Byte = error("unused")
        override fun decodeChar(): Char = error("unused")
        override fun decodeDouble(): Double = error("unused")
        override fun decodeEnum(enumDescriptor: SerialDescriptor): Int = error("unused")
        override fun decodeFloat(): Float = error("unused")
        override fun decodeInline(descriptor: SerialDescriptor): Decoder = error("unused")
        override fun decodeInt(): Int = error("unused")
        override fun decodeLong(): Long = error("unused")
        override fun decodeNotNullMark(): Boolean = error("unused")
        override fun decodeNull(): Nothing? = error("unused")
        override fun decodeShort(): Short = error("unused")
        override fun decodeString(): String = error("unused")
        override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T = error("unused")
    }

    @Test
    fun cborElementSerializerRequiresCborEncoder() {
        val ex = assertFailsWith<IllegalStateException> {
            CborElement.serializer().serialize(NonCborEncoder, CborInteger(1))
        }
        assertTrue(ex.message?.contains("This serializer can be used only with Cbor format") == true)
    }

    @Test
    fun cborElementSerializerRequiresCborDecoder() {
        val ex = assertFailsWith<IllegalStateException> {
            CborElement.serializer().deserialize(NonCborDecoder)
        }
        assertTrue(ex.message?.contains("This serializer can be used only with Cbor format") == true)
    }

    @Test
    fun typedCborArrayDeserializationFailsOnNonListInput() {
        val cbor = Cbor {}
        val bytes = cbor.encodeToByteArray(CborElement.serializer(), CborInteger(1))
        assertFailsWith<CborDecodingException> {
            cbor.decodeFromByteArray(CborArray.serializer(), bytes)
        }
        assertFailsWith<CborDecodingException> {
            cbor.decodeFromCborElement(CborArray.serializer(), CborInteger(1))
        }
    }

    @Test
    fun typedCborIntDeserializationFailsOnNonIntInput() {
        val cbor = Cbor {}
        val bytes = cbor.encodeToByteArray(CborElement.serializer(), CborArray(listOf(CborInteger(1))))
        assertFailsWith<CborDecodingException> {
            cbor.decodeFromByteArray(CborInteger.serializer(), bytes)
        }
        assertFailsWith<CborDecodingException> {
            cbor.decodeFromCborElement(CborInteger.serializer(), CborArray(listOf(CborInteger(1))))
        }
    }

    @Test
    fun structuredByteStringDecodesToByteArray() {
        val cbor = Cbor { alwaysUseByteString = true }
        val element = cbor.encodeToCborElement(byteArrayOf(1, 2, 3))
        assertTrue(element is CborByteString)
        assertTrue(cbor.decodeFromCborElement<ByteArray>(element).contentEquals(byteArrayOf(1, 2, 3)))
    }
}
