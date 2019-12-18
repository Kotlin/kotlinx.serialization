/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.internal.EnumDescriptor
import kotlinx.serialization.json.JsonTestBase
import kotlin.test.Test
import kotlin.test.assertEquals

class SerializableEnumTest : JsonTestBase() {
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
        override val descriptor: SerialDescriptor = EnumDescriptor("WithCustom", arrayOf("1", "2"))

        override fun serialize(encoder: Encoder, obj: WithCustom) {
            encoder.encodeInt(obj.ordinal + 1)
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
            strict)

    @Test
    fun testEnumWithCustomSerializers() =
        assertJsonFormAndRestored(
            CustomInside.serializer(),
            CustomInside(WithCustom.TWO), """{inside:2}""")
}
