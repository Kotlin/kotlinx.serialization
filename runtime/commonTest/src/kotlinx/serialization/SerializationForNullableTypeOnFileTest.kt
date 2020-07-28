/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:UseSerializers(MyIntSerializer::class)

package kotlinx.serialization

import kotlinx.serialization.SerializationForNullableTypeOnFileTest.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlin.test.*

class SerializationForNullableTypeOnFileTest {

    @Serializable
    data class Holder(val i: Int?)

    @Serializer(forClass = Int::class)
    object MyIntSerializer : KSerializer<Int?> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("MIS", PrimitiveKind.INT).nullable

        override fun serialize(encoder: Encoder, value: Int?) {
            if (value == null) encoder.encodeInt(42)
            else encoder.encodeInt(239)
        }

        override fun deserialize(decoder: Decoder): Int {
            TODO()
        }
    }

    @Test
    fun testFileLevel() {
        assertEquals("""{"i":42}""", Json.encodeToString(Holder(null)))
        assertEquals("""{"i":239}""", Json.encodeToString(Holder(314)))
    }
}
