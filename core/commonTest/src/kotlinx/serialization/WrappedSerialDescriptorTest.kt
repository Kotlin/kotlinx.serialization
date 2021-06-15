package kotlinx.serialization

import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.test.*
import kotlin.test.*

class WrappedSerialDescriptorTest {

    private fun checkWrapped(original: SerialDescriptor, wrappedName: String) {
        val wrapped = SerialDescriptor(wrappedName, original)

        assertEquals(wrappedName, wrapped.serialName)
        assertNotEquals(original.serialName, wrapped.serialName)

        assertEquals(original.elementsCount, wrapped.elementsCount)
        assertEquals(original.isNullable, wrapped.isNullable)
        assertEquals(original.annotations, wrapped.annotations)
        assertEquals(original.kind, wrapped.kind)

        for (i in 0 until original.elementsCount) {
            original.getElementDescriptor(i).assertDescriptorEqualsTo(wrapped.getElementDescriptor(i))

            assertEquals(original.getElementName(i), wrapped.getElementName(i))
            assertEquals(original.getElementAnnotations(i), wrapped.getElementAnnotations(i))
            assertEquals(original.isElementOptional(i), wrapped.isElementOptional(i))
        }
    }

    @Test
    fun testWrappedList() {
        checkWrapped(ListSerializer(Int.serializer()).descriptor, "WrappedList")
    }

    @Test
    fun testWrappedMap() {
        checkWrapped(MapSerializer(String.serializer(), Int.serializer()).descriptor, "WrappedMap")
    }

    @Serializable
    class SimpleType(val int: Int, val float: Float)

    @Test
    fun testWrappedSimpleClass() {
        checkWrapped(SimpleType.serializer().descriptor, "WrappedSimpleType")
    }

    @Serializable
    class ComplexType(
            val string: String,
            val nullableClass: SimpleType?,
            val type: SimpleType,
            val int: Int,
            val nullableInt: Int?
    )

    @Test
    fun testWrappedComplexClass() {
        checkWrapped(ComplexType.serializer().descriptor, "WrappedComplexType")
    }
}