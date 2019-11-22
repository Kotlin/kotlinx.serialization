/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.kinds

import kotlinx.serialization.*
import kotlinx.serialization.internal.EnumDescriptor
import kotlinx.serialization.json.JsonTestBase
import kotlin.test.Test
import kotlin.test.assertEquals

@Serializable
@SerialName("custom_enum")
private enum class CustomEnum {
    @SerialName("foo_a")
    FooA,
    @SerialName("foo_b")
    @SerialId(10)
    FooB
}

@Serializable
private enum class One { A, B }

private enum class Two { A, B }

@Serializable
private data class TwoEnums(val one: One, val two: Two)

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
    override val descriptor: SerialDescriptor
        get() = EnumDescriptor("WithCustom", arrayOf("1", "2"))

    override fun serialize(encoder: Encoder, obj: WithCustom) {
        encoder.encodeInt(obj.ordinal + 1)
    }

    override fun deserialize(decoder: Decoder): WithCustom {
        return WithCustom.values()[decoder.decodeInt() - 1]
    }
}

@Serializable
private data class CustomInside(val inside: WithCustom)

class SerializableEnumTest : JsonTestBase() {
    @Test
    fun serializedCorrectly() =
        assertJsonFormAndRestored(
            WithCustomEnum.serializer(),
            WithCustomEnum(CustomEnum.FooB),
            """{"c":"foo_b"}""",
            strict,
            printResult = true
        )

    @Test
    fun enumsCanHaveCustomSerializers() =
        assertJsonFormAndRestored(CustomInside.serializer(), CustomInside(WithCustom.TWO), """{inside:2}""")

    @Test
    fun hasCorrectDescriptor() {
        val desc = WithCustomEnum.serializer().descriptor.getElementDescriptor(0)
        assertEquals("custom_enum", desc.name)
        assertEquals(listOf("foo_a", "foo_b"), desc.elementNames())
        assertEquals("""custom_enum(foo_a, foo_b)""", desc.toString())
        assertEquals(desc, desc.getElementDescriptor(0))
        assertEquals(10, getSerialId(desc, 1))
    }

    @Test
    fun annotationDoesNotChangeEnum() = assertJsonFormAndRestored(
        TwoEnums.serializer(),
        TwoEnums(One.B, Two.B),
        """{"one":"B","two":"B"}""",
        strict
    )

    @Test
    fun annotationDoesNotChangeEnumDescriptor() {
        val descOne = TwoEnums.serializer().descriptor.getElementDescriptor(0)
        val descTwo = TwoEnums.serializer().descriptor.getElementDescriptor(1)
        assertEquals("kotlinx.serialization.kinds.Two", descTwo.name)
        assertEquals("kotlinx.serialization.kinds.One", descOne.name)
        assertEquals(descTwo.elementNames(), descOne.elementNames())
    }
}
