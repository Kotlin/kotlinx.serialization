package kotlinx.serialization

import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.internal.ElementMarker
import kotlin.test.Test
import kotlin.test.assertEquals

class ElementMarkerTest {
    @Test
    fun testNothingWasRead() {
        val size = 5
        val descriptor = createClassDescriptor(size)
        val reader = ElementMarker(descriptor) { _, _ -> true }

        for (i in 0 until size) {
            assertEquals(i, reader.nextUnmarkedIndex())
        }
        assertEquals(CompositeDecoder.DECODE_DONE, reader.nextUnmarkedIndex())
    }

    @Test
    fun testAllWasRead() {
        val size = 5
        val descriptor = createClassDescriptor(size)
        val reader = ElementMarker(descriptor) { _, _ -> true }
        for (i in 0 until size) {
            reader.mark(i)
        }

        assertEquals(CompositeDecoder.DECODE_DONE, reader.nextUnmarkedIndex())
    }

    @Test
    fun testFilteredRead() {
        val size = 10
        val readIndex = 4

        val predicate: (Any?, Int) -> Boolean = { _, i -> i % 2 == 0 }
        val descriptor = createClassDescriptor(size)
        val reader = ElementMarker(descriptor, predicate)
        reader.mark(readIndex)

        for (i in 0 until size) {
            if (predicate(descriptor, i) && i != readIndex) {
                //`readIndex` already read and only filtered elements must be read
                assertEquals(i, reader.nextUnmarkedIndex())
            }
        }
        assertEquals(CompositeDecoder.DECODE_DONE, reader.nextUnmarkedIndex())
    }

    @Test
    fun testSmallPartiallyRead() {
        testPartiallyRead(Long.SIZE_BITS / 3)
    }

    @Test
    fun test64PartiallyRead() {
        testPartiallyRead(Long.SIZE_BITS)
    }

    @Test
    fun test128PartiallyRead() {
        testPartiallyRead(Long.SIZE_BITS * 2)
    }

    @Test
    fun testLargePartiallyRead() {
        testPartiallyRead(Long.SIZE_BITS * 2 + Long.SIZE_BITS / 3)
    }

    private fun testPartiallyRead(size: Int) {
        val descriptor = createClassDescriptor(size)
        val reader = ElementMarker(descriptor) { _, _ -> true }
        for (i in 0 until size) {
            if (i % 2 == 0) {
                reader.mark(i)
            }
        }

        for (i in 0 until size) {
            if (i % 2 != 0) {
                assertEquals(i, reader.nextUnmarkedIndex())
            }
        }
        assertEquals(CompositeDecoder.DECODE_DONE, reader.nextUnmarkedIndex())
    }

    private fun createClassDescriptor(size: Int): SerialDescriptor {
        return buildClassSerialDescriptor("descriptor") {
            for (i in 0 until size) {
                element("element$i", buildSerialDescriptor("int", PrimitiveKind.INT))
            }
        }
    }
}
