/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.test.*
import kotlin.test.*

class SerialDescriptorBuilderTest {

    @Serializable
    @SerialName("Wrapper")
    class Wrapper(val i: Int)

    private val wrapperDescriptor = buildClassSerialDescriptor("Wrapper") {
        element("i", Int.serializer().descriptor)
    }

    private val dataHolderDescriptor = buildClassSerialDescriptor("DataHolder") {
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
        val descriptor: SerialDescriptor = buildClassSerialDescriptor("Box") {
            element("value", typeSerializer.descriptor)
            element("list", listSerialDescriptor(typeSerializer.descriptor))
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
        assertFailsWith<IllegalArgumentException> {
            buildClassSerialDescriptor("a") {
                element<Int>("i")
                element<Int>("i")
            }
        }

        assertFailsWith<IllegalArgumentException> { buildClassSerialDescriptor("") }
        assertFailsWith<IllegalArgumentException> { buildClassSerialDescriptor("\t") }
        assertFailsWith<IllegalArgumentException> { buildClassSerialDescriptor("   ") }
        assertFailsWith<IllegalArgumentException> { PrimitiveSerialDescriptor("", PrimitiveKind.STRING) }
        assertFailsWith<IllegalArgumentException> { PrimitiveSerialDescriptor(" ", PrimitiveKind.STRING) }
        assertFailsWith<IllegalArgumentException> { PrimitiveSerialDescriptor("\t", PrimitiveKind.STRING) }
    }
}
