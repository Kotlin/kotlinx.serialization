/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.JsonTestBase
import kotlinx.serialization.test.*
import kotlin.test.*

class EnumSerializationTest : JsonTestBase() {

    @Serializable
    enum class RegularEnum {
        VALUE
    }

    @Serializable
    data class Regular(val a: RegularEnum)

    @Serializable
    data class RegularNullable(val a: RegularEnum?)

    @Serializable
    @SerialName("custom_enum")
    private enum class CustomEnum {
        @SerialName("foo_a")
        FooA,

        @SerialName("foo_b")
        @Id(10)
        FooB
    }

    @Serializable
    private data class WithCustomEnum(val c: CustomEnum)

    @Serializable(CustomEnumSerializer::class)
    private enum class WithCustom {
        @SerialName("1")
        ONE,
        @SerialName("2")
        TWO
    }

    @Serializer(WithCustom::class)
    private class CustomEnumSerializer : KSerializer<WithCustom> {
        override val descriptor: SerialDescriptor = buildSerialDescriptor("WithCustom", SerialKind.ENUM) {
            element("1", buildSerialDescriptor("WithCustom.1", StructureKind.OBJECT))
            element("2", buildSerialDescriptor("WithCustom.2", StructureKind.OBJECT))
        }

        override fun serialize(encoder: Encoder, value: WithCustom) {
            encoder.encodeInt(value.ordinal + 1)
        }

        override fun deserialize(decoder: Decoder): WithCustom {
            return WithCustom.values()[decoder.decodeInt() - 1]
        }
    }

    @Serializable
    private data class CustomInside(val inside: WithCustom)

    @Test
    fun testEnumSerialization() =
        assertJsonFormAndRestored(
            WithCustomEnum.serializer(),
            WithCustomEnum(CustomEnum.FooB),
            """{"c":"foo_b"}""",
            default
        )

    @Test
    fun testEnumWithCustomSerializers() =
        assertJsonFormAndRestored(
            CustomInside.serializer(),
            CustomInside(WithCustom.TWO), """{"inside":2}"""
        )


    @Test
    fun testHasMeaningfulToString() {
        val regular = Regular.serializer().descriptor.toString()
        assertEquals(
            "kotlinx.serialization.EnumSerializationTest.Regular(a: kotlinx.serialization.EnumSerializationTest.RegularEnum)",
            regular
        )
        val regularNullable = RegularNullable.serializer().descriptor.toString()
        assertEquals(
            "kotlinx.serialization.EnumSerializationTest.RegularNullable(a: kotlinx.serialization.EnumSerializationTest.RegularEnum?)",
            regularNullable
        )
        // slightly differs from previous one
        val regularNullableJoined = RegularNullable.serializer().descriptor.elementDescriptors.joinToString()
        assertEquals("kotlinx.serialization.EnumSerializationTest.RegularEnum(VALUE)?", regularNullableJoined)

        val regularEnum = RegularEnum.serializer().descriptor.toString()
        assertEquals("kotlinx.serialization.EnumSerializationTest.RegularEnum(VALUE)", regularEnum)
    }


    @Test
    fun testHasMeaningfulHashCode() {
        val a = Regular.serializer().descriptor.hashCode()
        val b = RegularNullable.serializer().descriptor.hashCode()
        val c = RegularEnum.serializer().descriptor.hashCode()
        assertTrue(setOf(a, b, c).size == 3, ".hashCode must give different result for different descriptors")
    }

    enum class MyEnum {
        A, B, C;
    }

    @Serializable
    @SerialName("kotlinx.serialization.EnumSerializationTest.MyEnum")
    enum class MyEnum2 {
        A, B, C;
    }

    @Serializable
    class Wrapper(val a: MyEnum)

    @Test
    fun testStructurallyEqualDescriptors() {
        val libraryGenerated = Wrapper.serializer().descriptor.getElementDescriptor(0)
        val codeGenerated = MyEnum2.serializer().descriptor
        assertNotEquals(libraryGenerated::class, codeGenerated::class)
        libraryGenerated.assertDescriptorEqualsTo(codeGenerated)
    }
}
