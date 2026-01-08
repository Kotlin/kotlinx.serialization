package kotlinx.serialization.cbor.internal

import kotlinx.serialization.*
import kotlin.test.*

class StreamsTest {

    @Test
    fun powerOfTwoCapacity_negativeValue() {
        assertEquals(0, ByteArrayOutput.nextPowerOfTwoCapacity(-1))
        assertEquals(0, ByteArrayOutput.nextPowerOfTwoCapacity(-17))
    }

    @Test
    fun powerOfTwoCapacity_zeroValue() {
        assertEquals(0, ByteArrayOutput.nextPowerOfTwoCapacity(0))
    }

    @Test
    fun powerOfTwoCapacity_exactPowerOfTwo() {
        assertEquals(16, ByteArrayOutput.nextPowerOfTwoCapacity(8))
        assertEquals(32, ByteArrayOutput.nextPowerOfTwoCapacity(16))
        assertEquals(64, ByteArrayOutput.nextPowerOfTwoCapacity(32))
    }

    @Test
    fun powerOfTwoCapacity_nonPowerOfTwo() {
        assertEquals(16, ByteArrayOutput.nextPowerOfTwoCapacity(9))
        assertEquals(64, ByteArrayOutput.nextPowerOfTwoCapacity(33))
        assertEquals(128, ByteArrayOutput.nextPowerOfTwoCapacity(65))
    }

    @Test
    fun powerOfTwoCapacity_smallValues() {
        assertEquals(2, ByteArrayOutput.nextPowerOfTwoCapacity(1))
        assertEquals(4, ByteArrayOutput.nextPowerOfTwoCapacity(2))
        assertEquals(4, ByteArrayOutput.nextPowerOfTwoCapacity(3))
    }

    @Test
    fun powerOfTwoCapacity_boundaryValues() {
        assertEquals(0, ByteArrayOutput.nextPowerOfTwoCapacity(0))
        assertEquals(2, ByteArrayOutput.nextPowerOfTwoCapacity(1))
        assertEquals(4, ByteArrayOutput.nextPowerOfTwoCapacity(3))
        assertEquals(8, ByteArrayOutput.nextPowerOfTwoCapacity(5))
    }

    @Test
    fun powerOfTwoCapacity_largeValues() {
        assertEquals(1073741824, ByteArrayOutput.nextPowerOfTwoCapacity(536870912))
        assertEquals(1073741824, ByteArrayOutput.nextPowerOfTwoCapacity(1073741823))
        assertEquals(Integer.MAX_VALUE, ByteArrayOutput.nextPowerOfTwoCapacity(1073741824))
        assertEquals(Integer.MAX_VALUE, ByteArrayOutput.nextPowerOfTwoCapacity(1073741825))
        assertEquals(Integer.MAX_VALUE, ByteArrayOutput.nextPowerOfTwoCapacity(Integer.MAX_VALUE-1))
        assertEquals(Integer.MAX_VALUE, ByteArrayOutput.nextPowerOfTwoCapacity(Integer.MAX_VALUE))
    }
}
