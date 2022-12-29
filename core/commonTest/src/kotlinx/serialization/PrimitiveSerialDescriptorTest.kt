package kotlinx.serialization

import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.internal.PrimitiveSerialDescriptor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame

class PrimitiveSerialDescriptorTest {

    @Test
    fun testEqualsImplemented() {
        val first = PrimitiveSerialDescriptor("test_name", PrimitiveKind.LONG)
        val second = PrimitiveSerialDescriptor("test_name", PrimitiveKind.LONG)

        assertNotSame(first, second)
        assertEquals(first, second)
    }

    @Test
    fun testHashCodeStability() {
        val first = PrimitiveSerialDescriptor("test_name", PrimitiveKind.LONG)
        val second = PrimitiveSerialDescriptor("test_name", PrimitiveKind.LONG)

        assertNotSame(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }

}
