@file:OptIn(ExperimentalUnsignedTypes::class, ExperimentalSerializationApi::class)

package kotlinx.serialization.cbor

import kotlinx.serialization.*
import kotlin.test.*

class CborUnsignedInteroperabilityTest {

    @Test
    fun testUByteIsNotInteroperableWithCborUnsignedIntegerEncoding() {
        // Canonical CBOR encoding for unsigned integer 200 is 0x18 0xC8.
        val canonicalUnsigned200 = "18c8"

        val element = Cbor.decodeFromHexString<CborElement>(canonicalUnsigned200)
        val asInteger = element as? CborInteger ?: fail("Expected CborInteger, got ${element::class}")
        assertTrue(asInteger.isPositive)
        assertEquals(200uL, asInteger.value)

        // Kotlin UByte serializer is implemented via Byte serializer (two's-complement mapping),
        // so decoding a canonical unsigned integer > 127 fails due to Byte range checks.
        assertEquals(200.toUByte(), Cbor.decodeFromHexString<UByte>(canonicalUnsigned200))


        // Encoding UByte(200) does not produce the canonical unsigned integer representation.
        val encodedByCurrentSerializer = Cbor.encodeToHexString(200u.toUByte())
        assertEquals(canonicalUnsigned200, encodedByCurrentSerializer)

        // It round-trips within the library, but the CBOR representation is a negative integer.
        val encodedElement = Cbor.decodeFromHexString<CborElement>(encodedByCurrentSerializer)
        val encodedAsInteger =
            encodedElement as? CborInteger ?: fail("Expected CborInteger, got ${encodedElement::class}")
        //is actually -56, not 200
        assertEquals(200L, encodedAsInteger.long)
    }
}

