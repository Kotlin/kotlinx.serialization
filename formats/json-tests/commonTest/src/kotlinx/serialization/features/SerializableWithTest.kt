/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

object MultiplyingIntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("MultiplyingInt", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Int {
        return decoder.decodeInt() / 2
    }

    override fun serialize(encoder: Encoder, value: Int) {
        encoder.encodeInt(value * 2)
    }
}

object DividingIntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("DividedInt", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Int {
        return decoder.decodeInt() * 2
    }

    override fun serialize(encoder: Encoder, value: Int) {
        encoder.encodeInt(value / 2)
    }
}

@Serializable(with = DividingIntHolderSerializer::class)
data class IntHolder(val data: Int)

@Serializer(IntHolder::class)
object MultiplyingIntHolderSerializer {
    override fun deserialize(decoder: Decoder): IntHolder {
        return IntHolder(decoder.decodeInt() / 2)
    }

    override fun serialize(encoder: Encoder, value: IntHolder) {
        encoder.encodeInt(value.data * 2)
    }
}

@Serializer(IntHolder::class)
object DividingIntHolderSerializer {
    override fun deserialize(decoder: Decoder): IntHolder {
        return IntHolder(decoder.decodeInt() * 2)
    }

    override fun serialize(encoder: Encoder, value: IntHolder) {
        encoder.encodeInt(value.data / 2)
    }
}

@Serializable
data class Carrier(
    @Serializable(with = MultiplyingIntHolderSerializer::class) val a: IntHolder,
    @Serializable(with = MultiplyingIntSerializer::class) val i: Int
)

class SerializableWithTest {
    @Test
    fun testOnProperties() {
        val str = Json.encodeToString(Carrier.serializer(), Carrier(IntHolder(42), 2))
        assertEquals("""{"a":84,"i":4}""", str)
    }
}
