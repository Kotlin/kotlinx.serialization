package kotlinx.serialization.cbor

import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
