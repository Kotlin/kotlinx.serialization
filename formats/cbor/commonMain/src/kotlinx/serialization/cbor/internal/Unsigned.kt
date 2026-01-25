@file:OptIn(ExperimentalSerializationApi::class, ExperimentalUnsignedTypes::class)

package kotlinx.serialization.cbor.internal

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.CborInteger
import kotlinx.serialization.cbor.longOrNull
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal class UnsignedInlineEncoder(
    private val delegate: CborWriter,
) : Encoder by delegate {
    override fun encodeByte(value: Byte) {
        delegate.encodePositive(value.toUByte().toULong())
    }

    override fun encodeShort(value: Short) {
        delegate.encodePositive(value.toUShort().toULong())
    }

    override fun encodeInt(value: Int) {
        delegate.encodePositive(value.toUInt().toULong())
    }

    override fun encodeLong(value: Long) {
        delegate.encodePositive(value.toULong())
    }
}

internal class UnsignedInlineDecoder(
    private val delegate: CborReader,
) : Decoder by delegate {

    override fun decodeByte(): Byte {
        val value = delegate.parser.nextNumber(delegate.tags)
        if (value !in 0L..UByte.MAX_VALUE.toLong()) {
            throw CborDecodingException("Decoded number $value is not within the range for type UByte ([0..255])")
        }
        return value.toByte()
    }

    override fun decodeShort(): Short {
        val value = delegate.parser.nextNumber(delegate.tags)
        if (value !in 0L..UShort.MAX_VALUE.toLong()) {
            throw CborDecodingException("Decoded number $value is not within the range for type UShort ([0..65535])")
        }
        return value.toShort()
    }

    override fun decodeInt(): Int {
        val value = delegate.parser.nextNumber(delegate.tags)
        if (value !in 0L..UInt.MAX_VALUE.toLong()) {
            throw CborDecodingException(
                "Decoded number $value is not within the range for type UInt ([0..${UInt.MAX_VALUE.toLong()}])"
            )
        }
        return value.toInt()
    }

    override fun decodeLong(): Long {
        val parser = delegate.parser
        val tags = delegate.tags

        return when (parser) {
            is StructuredCborParser -> {
                parser.processTags(tags)
                val element = parser.layer.current
                val integer = element as? CborInteger
                    ?: throw CborDecodingException("Expected number, got ${element::class.simpleName}")

                if (!integer.isPositive) {
                    throw CborDecodingException("Expected unsigned integer, got $integer")
                }
                integer.value.toLong()
            }

            is CborParser -> {
                parser.processTags(tags)
                val header = parser.curByte
                if ((header and MAJOR_TYPE_MASK) != HEADER_POSITIVE.toInt()) {
                    throw CborDecodingException("unsigned integer", header)
                }
                parser.nextULong(null).toLong()
            }
        }
    }
}
