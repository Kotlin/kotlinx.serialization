@file:OptIn(ExperimentalUnsignedTypes::class)

package kotlinx.serialization.cbor

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlin.test.*

class CborNumberEncodingTest {

    // 0-23 packs into a single byte
    @Test
    fun testEncodingLengthOfTinyNumbers() {
        val tinyNumbers = listOf(0, 1, 23)
        for (number in tinyNumbers) {
            assertEquals(
                expected = 1,
                actual = Cbor.encodeToByteArray(number).size,
                "when encoding value '$number'"
            )
        }
    }

    // 24..(2^8-1) packs into 2 bytes
    @Test
    fun testEncodingLengthOf8BitNumbers() {
        val tinyNumbers = listOf(24, 127, 128, 255)
        for (number in tinyNumbers) {
            assertEquals(
                expected = 2,
                actual = Cbor.encodeToByteArray(number).size,
                "when encoding value '$number'"
            )
        }
    }

    // 2^8..(2^16-1) packs into 3 bytes
    @Test
    fun testEncodingLengthOf16BitNumbers() {
        val tinyNumbers = listOf(256, 32767, 32768, 65535)
        for (number in tinyNumbers) {
            assertEquals(
                expected = 3,
                actual = Cbor.encodeToByteArray(number).size,
                "when encoding value '$number'"
            )
        }
    }

    // 2^16..(2^32-1) packs into 5 bytes
    @Test
    fun testEncodingLengthOf32BitNumbers() {
        val tinyNumbers = listOf(65536, 2147483647, 2147483648, 4294967295)
        for (number in tinyNumbers) {
            assertEquals(
                expected = 5,
                actual = Cbor.encodeToByteArray(number).size,
                "when encoding value '$number'"
            )
        }
    }

    // 2^32+ packs into 9 bytes
    @Test
    fun testEncodingLengthOfLargeNumbers() {
        val tinyNumbers = listOf(4294967296, 8589934592)
        for (number in tinyNumbers) {
            assertEquals(
                expected = 9,
                actual = Cbor.encodeToByteArray(number).size,
                "when encoding value '$number'"
            )
        }
    }

    @Test
    fun testEncodingLargestPositiveTinyNumber() {
        assertEquals(
            expected = byteArrayOf(23).toList(),
            actual = Cbor.encodeToByteArray(23).toList(),
        )
    }

    @Test
    fun testDecodingLargestPositiveTinyNumber() {
        assertEquals(
            expected = 23,
            actual = Cbor.decodeFromByteArray(byteArrayOf(23)),
        )
    }


    @Test
    fun testEncodingLargestNegativeTinyNumber() {
        assertEquals(
            expected = byteArrayOf(55).toList(),
            actual = Cbor.encodeToByteArray(-24).toList(),
        )
    }

    @Test
    fun testDecodingLargestNegativeTinyNumber() {
        assertEquals(
            expected = -24,
            actual = Cbor.decodeFromByteArray(byteArrayOf(55)),
        )
    }

    @Test
    fun testEncodingLargestPositive8BitNumber() {
        val bytes = listOf(24, 255).map { it.toByte() }
        assertEquals(
            expected = bytes,
            actual = Cbor.encodeToByteArray(255).toList(),
        )
    }

    @Test
    fun testDecodingLargestPositive8BitNumber() {
        val bytes = listOf(24, 255).map { it.toByte() }.toByteArray()
        assertEquals(
            expected = 255,
            actual = Cbor.decodeFromByteArray(bytes),
        )
    }

    @Test
    fun testEncodingLargestNegative8BitNumber() {
        val bytes = listOf(56, 255).map { it.toByte() }
        assertEquals(
            expected = bytes,
            actual = Cbor.encodeToByteArray(-256).toList(),
        )
    }

    @Test
    fun testDecodingLargestNegative8BitNumber() {
        val bytes = listOf(56, 255).map { it.toByte() }.toByteArray()
        assertEquals(
            expected = -256,
            actual = Cbor.decodeFromByteArray(bytes),
        )
    }

    @Test
    fun testEncodingLargestPositive16BitNumber() {
        val bytes = listOf(25, 255, 255).map { it.toByte() }
        assertEquals(
            expected = bytes,
            actual = Cbor.encodeToByteArray(65535).toList(),
        )
    }

    @Test
    fun testDecodingLargestPositive16BitNumber() {
        val bytes = listOf(25, 255, 255).map { it.toByte() }.toByteArray()
        assertEquals(
            expected = 65535,
            actual = Cbor.decodeFromByteArray(bytes),
        )
    }

    @Test
    fun testEncodingLargestNegative16BitNumber() {
        val bytes = listOf(57, 255, 255).map { it.toByte() }
        assertEquals(
            expected = bytes,
            actual = Cbor.encodeToByteArray(-65536).toList(),
        )
    }

    @Test
    fun testDecodingLargestNegative16BitNumber() {
        val bytes = listOf(57, 255, 255).map { it.toByte() }.toByteArray()
        assertEquals(
            expected = -65536,
            actual = Cbor.decodeFromByteArray(bytes),
        )
    }

    @Test
    fun testEncodingLargestPositive32BitNumber() {
        val bytes = listOf(26, 255, 255, 255, 255).map { it.toByte() }
        assertEquals(
            expected = bytes,
            actual = Cbor.encodeToByteArray(4294967295).toList(),
        )
    }

    @Test
    fun testDecodingLargestPositive32BitNumber() {
        val bytes = listOf(26, 255, 255, 255, 255).map { it.toByte() }.toByteArray()
        assertEquals(
            expected = 4294967295,
            actual = Cbor.decodeFromByteArray(bytes),
        )
    }

    @Test
    fun testEncodingLargestNegative32BitNumber() {
        val bytes = listOf(58, 255, 255, 255, 255).map { it.toByte() }
        assertEquals(
            expected = bytes,
            actual = Cbor.encodeToByteArray(-4294967296).toList(),
        )
    }

    @Test
    fun testDecodingLargestNegative32BitNumber() {
        val bytes = listOf(58, 255, 255, 255, 255).map { it.toByte() }.toByteArray()
        assertEquals(
            expected = -4294967296,
            actual = Cbor.decodeFromByteArray(bytes),
        )
    }

    @Test
    fun testEncodingUnsignedValuesAsPositiveInteger() {
        assertEquals(expected = "18c8", actual = Cbor.encodeToHexString(UByte.serializer(), 200u))
        assertEquals(expected = "197d00", actual = Cbor.encodeToHexString(UShort.serializer(), 32000u))
        assertEquals(expected = "1a80000000", actual = Cbor.encodeToHexString(UInt.serializer(), 2147483648u))
        assertEquals(
            expected = "1b8000000000000000",
            actual = Cbor.encodeToHexString(ULong.serializer(), 9223372036854775808uL)
        )
        assertEquals(expected = "9f18d0ff", actual = Cbor.encodeToHexString(UByteArraySerializer(), ubyteArrayOf(208u)))
        assertEquals(
            expected = "9f198000ff",
            actual = Cbor.encodeToHexString(UShortArraySerializer(), ushortArrayOf(32768u))
        )
        assertEquals(
            expected = "9f1a80000000ff",
            actual = Cbor.encodeToHexString(UIntArraySerializer(), uintArrayOf(2147483648u))
        )
        assertEquals(
            expected = "9f1b8000000000000000ff",
            actual = Cbor.encodeToHexString(ULongArraySerializer(), ulongArrayOf(9223372036854775808uL))
        )
    }

    @Test
    fun testDecodingLegacySignedEncodingOfUnsignedValues() {
        assertEquals(expected = 200u, actual = Cbor.decodeFromHexString(UByte.serializer(), "3837"))
        assertEquals(expected = 32768u, actual = Cbor.decodeFromHexString(UShort.serializer(), "397fff"))
        assertEquals(expected = 2147483648u, actual = Cbor.decodeFromHexString(UInt.serializer(), "3a7fffffff"))
        assertEquals(
            expected = 9223372036854775808uL,
            actual = Cbor.decodeFromHexString(ULong.serializer(), "3b7fffffffffffffff")
        )
        assertContentEquals(expected = ubyteArrayOf(208u), actual = Cbor.decodeFromHexString("9f382fff"))
        assertContentEquals(expected = ushortArrayOf(32768u), actual = Cbor.decodeFromHexString("9f397fffff"))
        assertContentEquals(expected = uintArrayOf(2147483648u), actual = Cbor.decodeFromHexString("9f3a7fffffffff"))
        assertContentEquals(
            expected = ulongArrayOf(9223372036854775808uL),
            actual = Cbor.decodeFromHexString("9f3b7fffffffffffffffff")
        )
    }

    @Test
    fun testUnsignedArrayMixedDecoding() {
        assertContentEquals(expected = ubyteArrayOf(208u, 208u), actual = Cbor.decodeFromHexString("9f382f18d0ff"))
        assertContentEquals(
            expected = ushortArrayOf(32768u, 32768u),
            actual = Cbor.decodeFromHexString("9f397fff198000ff")
        )
        assertContentEquals(
            expected = uintArrayOf(2147483648u, 2147483648u),
            actual = Cbor.decodeFromHexString("9f3a7fffffff1a80000000ff")
        )
        assertContentEquals(
            expected = ulongArrayOf(9223372036854775808uL, 9223372036854775808uL),
            actual = Cbor.decodeFromHexString("9f3b7fffffffffffffff1b8000000000000000ff")
        )
    }
}
