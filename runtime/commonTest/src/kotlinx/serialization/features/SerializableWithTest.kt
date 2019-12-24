package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

object MultiplyingIntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveDescriptor("MultiplyingInt", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Int {
        return decoder.decodeInt() / 2
    }

    override fun serialize(encoder: Encoder, obj: Int) {
        encoder.encodeInt(obj * 2)
    }
}

object DividingIntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveDescriptor("DividedInt", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Int {
        return decoder.decodeInt() * 2
    }

    override fun serialize(encoder: Encoder, obj: Int) {
        encoder.encodeInt(obj / 2)
    }
}

@Serializable(with = DividingIntHolderSerializer::class)
data class IntHolder(val data: Int)

@Serializer(IntHolder::class)
object MultiplyingIntHolderSerializer {
    override fun deserialize(decoder: Decoder): IntHolder {
        return IntHolder(decoder.decodeInt() / 2)
    }

    override fun serialize(encoder: Encoder, obj: IntHolder) {
        encoder.encodeInt(obj.data * 2)
    }
}

@Serializer(IntHolder::class)
object DividingIntHolderSerializer {
    override fun deserialize(decoder: Decoder): IntHolder {
        return IntHolder(decoder.decodeInt() * 2)
    }

    override fun serialize(encoder: Encoder, obj: IntHolder) {
        encoder.encodeInt(obj.data / 2)
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
        val str = Json.stringify(Carrier.serializer(), Carrier(IntHolder(42), 2))
        assertEquals("""{"a":84,"i":4}""", str)
    }
}
