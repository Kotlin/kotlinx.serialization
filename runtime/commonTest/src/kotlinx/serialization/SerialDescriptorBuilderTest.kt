/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.internal.*
import kotlin.test.*

@ImplicitReflectionSerializer
class SerialDescriptorBuilderTest {

    @Serializable
    @SerialName("Wrapper")
    class Wrapper(val i: Int)

    private val wrapperDescriptor = SerialDescriptor("Wrapper", 1, StructureKind.CLASS) {
        element("i", IntSerializer.descriptor)
    }

    private val dataHolderDescriptor = SerialDescriptor("DataHolder", 5, StructureKind.CLASS) {
        element<String>("string")
        element("nullableWrapper", wrapperDescriptor.nullable)
        element("wrapper", wrapperDescriptor)
        element<Int>("int")
        element<Long?>("nullableOptionalLong", isOptional = true)
    }

    @Serializable
    @SerialName("DataHolder")
    class DataHolder(
        val string: String,
        val nullableWrapper: Wrapper?,
        val wrapper: Wrapper,
        val int: Int,
        val nullableOptionalLong: Long? = null
    )

    @Test
    fun testTrivialDescriptor() {
        Wrapper.serializer().descriptor.assertDescriptorEqualsTo(wrapperDescriptor)
    }


    @Test
    fun testNestedDescriptors() {
        DataHolder.serializer().descriptor.assertDescriptorEqualsTo(dataHolderDescriptor)
    }

    @Serializable
    @SerialName("Box")
    class Box<T>(val value: T, val list: List<T>)

    class CustomBoxSerializer<T>(val typeSerializer: KSerializer<T>) {
        val descriptor: SerialDescriptor = SerialDescriptor("Box", 2, StructureKind.CLASS) {
            element("value", typeSerializer.descriptor)
            element("list", ArrayListSerializer(typeSerializer).descriptor)
        }
    }

    @Test
    fun testGenericDescriptor() {
        val original = Box.serializer(Wrapper.serializer()).descriptor
        val userDefined = CustomBoxSerializer(object : KSerializer<Wrapper> {
            override val descriptor: SerialDescriptor
                get() = wrapperDescriptor

            override fun serialize(encoder: Encoder, value: Wrapper) = TODO()
            override fun deserialize(decoder: Decoder): Wrapper = TODO()
        }).descriptor
        original.assertDescriptorEqualsTo(userDefined)
    }

    @Test
    fun testMisconfiguration() {
        assertFailsWith<IllegalStateException> {
            SerialDescriptor("", 1, StructureKind.CLASS) {}
        }

        assertFailsWith<IllegalStateException> {
            SerialDescriptor("", 1, StructureKind.CLASS) {
                element<Int>("i")
                element<Int>("l")
            }
        }

        assertFailsWith<IllegalStateException> {
            SerialDescriptor("", 2, StructureKind.CLASS) {
                element<Int>("i")
                element<Int>("i")
            }
        }
    }

    fun SerialDescriptor.assertDescriptorEqualsTo(other: SerialDescriptor) {
        assertEquals(serialName, other.serialName)
        assertEquals(elementsCount, other.elementsCount)
        assertEquals(isNullable, other.isNullable)
        assertEquals(annotations, other.annotations)
        assertEquals(kind, other.kind)
        for (i in 0 until elementsCount) {
            getElementDescriptor(i).assertDescriptorEqualsTo(other.getElementDescriptor(i))
            val name = getElementName(i)
            val otherName = other.getElementName(i)
            assertEquals(name, otherName)
            assertEquals(getElementAnnotations(i), other.getElementAnnotations(i))
            assertEquals(name, otherName)
            assertEquals(isElementOptional(i), other.isElementOptional(i))
        }
    }
}
