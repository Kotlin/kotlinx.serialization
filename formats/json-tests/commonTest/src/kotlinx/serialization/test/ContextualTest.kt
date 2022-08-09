@file:UseContextualSerialization(ContextualTest.Cont::class)

package kotlinx.serialization.test

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseContextualSerialization
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual

class ContextualTest {
    data class Cont(val i: Int)

    @Serializable
    data class DateHolder(val cont: Cont?)

    object DateSerializer: KSerializer<Cont> {
        override fun deserialize(decoder: Decoder): Cont {
            return Cont(decoder.decodeInt())
        }

        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("ContSerializer", PrimitiveKind.INT)

        override fun serialize(encoder: Encoder, value: Cont) {
            encoder.encodeInt(value.i)
        }

    }

    val module = SerializersModule {
        contextual(DateSerializer)
    }

    @kotlin.test.Test
    fun test() {
        val json = Json { serializersModule = module }

        println(json.encodeToString(DateHolder(Cont(42))))
    }
}
