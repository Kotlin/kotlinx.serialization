@file:OptIn(ExperimentalUnsignedTypes::class, ExperimentalSerializationApi::class)

package kotlinx.serialization.cbor

import kotlinx.serialization.*
import kotlin.test.*

class CborUnsignedInteroperabilityTest {

    @Test
    fun testUnsignedInlineTypesUseCborUnsignedIntegerEncoding() {
        // Canonical CBOR encoding for unsigned integer 200 is 0x18 0xC8.
        val canonicalUnsigned200 = "18c8"

        val element = Cbor.decodeFromHexString<CborElement>(canonicalUnsigned200)
        val asInteger = element as? CborInteger ?: fail("Expected CborInteger, got ${element::class}")
        assertTrue(asInteger.isPositive)
        assertEquals(200uL, asInteger.absoluteValue)

        assertEquals(200u.toUByte(), Cbor.decodeFromHexString<UByte>(canonicalUnsigned200))
        assertEquals(canonicalUnsigned200, Cbor.encodeToHexString(200u.toUByte()))

        val canonicalUInt = "1aee6b2800" // 4_000_000_000u
        assertEquals(4_000_000_000u, Cbor.decodeFromHexString<UInt>(canonicalUInt))
        assertEquals(canonicalUInt, Cbor.encodeToHexString(4_000_000_000u))

        val canonicalULongMax = "1bffffffffffffffff"
        assertEquals(ULong.MAX_VALUE, Cbor.decodeFromHexString<ULong>(canonicalULongMax))
        assertEquals(canonicalULongMax, Cbor.encodeToHexString(ULong.MAX_VALUE))

        // Structured decoding from CborElement also supports unsigned values beyond Long range.
        assertEquals(ULong.MAX_VALUE, Cbor.decodeFromCborElement<ULong>(CborInteger(ULong.MAX_VALUE)))
    }
}
