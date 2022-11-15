/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:UseSerializers(NullableIntSerializer::class, NonNullableIntSerializer::class)

package kotlinx.serialization

import kotlinx.serialization.SerializationForNullableTypeOnFileTest.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlin.test.*

class SerializationForNullableTypeOnFileTest {

    @Serializable
    data class Holder(val nullable: Int?, val nonNullable: Int)

    @Serializer(forClass = Int::class)
    object NullableIntSerializer : KSerializer<Int?> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("NullableIntSerializer", PrimitiveKind.INT).nullable

        override fun serialize(encoder: Encoder, value: Int?) {
            if (value == null) encoder.encodeNull()
            else encoder.encodeInt(value + 1)
        }
        override fun deserialize(decoder: Decoder): Int? {
            return if (decoder.decodeNotNullMark()) {
                val value = decoder.decodeInt()
                value - 1
            } else {
                decoder.decodeNull()
            }
        }
    }

    @Serializer(forClass = Int::class)
    object NonNullableIntSerializer : KSerializer<Int> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("NotNullIntSerializer", PrimitiveKind.INT)

        override fun serialize(encoder: Encoder, value: Int) {
            return encoder.encodeInt(value + 2)
        }

        override fun deserialize(decoder: Decoder): Int {
            return (decoder.decodeInt() - 2)
        }
    }

    @Test
    fun testFileLevel() {
        assertEquals("""{"nullable":null,"nonNullable":52}""", Json.encodeToString(Holder(nullable = null, nonNullable = 50)))
        assertEquals("""{"nullable":1,"nonNullable":2}""", Json.encodeToString(Holder(nullable = 0, nonNullable = 0)))
        assertEquals("""{"nullable":11,"nonNullable":52}""", Json.encodeToString(Holder(nullable = 10, nonNullable = 50)))

        assertEquals(Holder(nullable = 0, nonNullable = 50), Json.decodeFromString("""{"nullable":1,"nonNullable":52}"""))
        assertEquals(Holder(nullable = null, nonNullable = 50), Json.decodeFromString("""{"nullable":null,"nonNullable":52}"""))
        assertEquals(Holder(nullable = 10, nonNullable = 50), Json.decodeFromString("""{"nullable":11,"nonNullable":52}"""))
    }
}
