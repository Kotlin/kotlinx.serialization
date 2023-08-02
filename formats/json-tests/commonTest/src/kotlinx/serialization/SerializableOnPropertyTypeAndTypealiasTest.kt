package kotlinx.serialization

import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlin.test.*

@Serializable
data class WithDefault(val s: String)

@Serializable(SerializerA::class)
data class WithoutDefault(val s: String)

object SerializerA : KSerializer<WithoutDefault> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("Bruh", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: WithoutDefault) {
        encoder.encodeString(value.s)
    }

    override fun deserialize(decoder: Decoder): WithoutDefault {
        return WithoutDefault(decoder.decodeString())
    }
}

object SerializerB : KSerializer<WithoutDefault> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("Bruh", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: WithoutDefault) {
        encoder.encodeString(value.s + "#")
    }

    override fun deserialize(decoder: Decoder): WithoutDefault {
        return WithoutDefault(decoder.decodeString().removeSuffix("#"))
    }
}

object SerializerC : KSerializer<WithDefault> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("Bruh", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: WithDefault) {
        encoder.encodeString(value.s + "#")
    }

    override fun deserialize(decoder: Decoder): WithDefault {
        return WithDefault(decoder.decodeString().removeSuffix("#"))
    }
}

typealias WithoutDefaultAlias = @Serializable(SerializerB::class) WithoutDefault
typealias WithDefaultAlias = @Serializable(SerializerC::class) WithDefault

@Serializable
data class TesterWithoutDefault(
    val b1: WithoutDefault,
    @Serializable(SerializerB::class) val b2: WithoutDefault,
    val b3: @Serializable(SerializerB::class) WithoutDefault,
    val b4: WithoutDefaultAlias
)

@Serializable
data class TesterWithDefault(
    val b1: WithDefault,
    @Serializable(SerializerC::class) val b2: WithDefault,
    val b3: @Serializable(SerializerC::class) WithDefault,
    val b4: WithDefaultAlias
)

class SerializableOnPropertyTypeAndTypealiasTest : JsonTestBase() {

    @Test
    fun testWithDefault() {
        val t = TesterWithDefault(WithDefault("a"), WithDefault("b"), WithDefault("c"), WithDefault("d"))
        assertJsonFormAndRestored(
            TesterWithDefault.serializer(),
            t,
            """{"b1":{"s":"a"},"b2":"b#","b3":"c#","b4":"d#"}"""
        )
    }

    @Test
    fun testWithoutDefault() { // Ignored by #1895
        val t = TesterWithoutDefault(WithoutDefault("a"), WithoutDefault("b"), WithoutDefault("c"), WithoutDefault("d"))
        assertJsonFormAndRestored(
            TesterWithoutDefault.serializer(),
            t,
            """{"b1":"a","b2":"b#","b3":"c#","b4":"d#"}"""
        )
    }
}
