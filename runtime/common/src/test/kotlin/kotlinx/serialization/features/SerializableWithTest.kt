package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.internal.IntDescriptor
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

object MultiplyingIntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor
        get() = IntDescriptor

    override fun deserialize(input: Decoder): Int {
        return input.decodeInt() / 2
    }

    override fun serialize(output: Encoder, obj: Int) {
        output.encodeInt(obj * 2)
    }
}

@Serializable
class IntHolder(val data: Int)

@Serializer(IntHolder::class)
object IntHolderSerializer {}

@Serializable
data class Carrier(
    @Serializable(with = IntHolderSerializer::class) val a: IntHolder,
    @Serializable(with = MultiplyingIntSerializer::class) val i: Int
)

class SerializableWithTest {
    @Test
    fun testOnProperties() {
        val str = Json.stringify(Carrier.serializer(), Carrier(IntHolder(42), 2))
        assertEquals("""{"a":{"data":42},"i":4}""", str)
    }
}
