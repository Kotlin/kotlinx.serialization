/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.internal.*
import kotlinx.serialization.test.*
import kotlin.test.*

@ImplicitReflectionSerializer
class SerialDescriptorBuilderTest {

    @Serializable
    @SerialName("Wrapper")
    class Wrapper(val i: Int)

    private val wrapperDescriptor = SerialDescriptor("Wrapper", StructureKind.CLASS) {
        element("i", IntSerializer.descriptor)
    }

    private val dataHolderDescriptor = SerialDescriptor("DataHolder", StructureKind.CLASS) {
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
        val descriptor: SerialDescriptor = SerialDescriptor("Box", StructureKind.CLASS) {
            element("value", typeSerializer.descriptor)
            element("list", ArrayListSerializer(typeSerializer).descriptor)
        }
    }

    @Test
    fun testGenericDescriptor() {
        val original = Box.serializer(Wrapper.serializer()).descriptor
        val userDefined = CustomBoxSerializer(object : KSerializer<Wrapper> {
            override val descriptor: SerialDescriptor = wrapperDescriptor
            override fun serialize(encoder: Encoder, value: Wrapper) = TODO()
            override fun deserialize(decoder: Decoder): Wrapper = TODO()
        }).descriptor
        original.assertDescriptorEqualsTo(userDefined)
    }

    @Test
    fun testMisconfiguration() {
        assertFailsWith<IllegalStateException> {
            SerialDescriptor("", StructureKind.CLASS) {
                element<Int>("i")
                element<Int>("i")
            }
        }
    }
}
