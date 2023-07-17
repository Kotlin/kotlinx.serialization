package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlinx.serialization.test.*
import kotlin.test.*

class LocalClassesTest {
    object ObjectCustomSerializer: KSerializer<Any?> {
        override val descriptor: SerialDescriptor = buildSerialDescriptor("tmp", PrimitiveKind.INT)
        override fun serialize(encoder: Encoder, value: Any?) {
            encoder.encodeNull()
        }

        override fun deserialize(decoder: Decoder): Any? {
            return decoder.decodeNull()
        }
    }

    class ClassCustomSerializer: KSerializer<Any?> {
        override val descriptor: SerialDescriptor = buildSerialDescriptor("tmp", PrimitiveKind.INT)
        override fun serialize(encoder: Encoder, value: Any?) {
            encoder.encodeNull()
        }

        override fun deserialize(decoder: Decoder): Any? {
            return decoder.decodeNull()
        }
    }

    @Test
    fun testGeneratedSerializer() {
        @Serializable
        data class Local(val i: Int)

        val origin = Local(42)

        val decoded: Local = Json.decodeFromString(Json.encodeToString(origin))
        assertEquals(origin, decoded)
    }

    @Test
    fun testInLambda() {
        42.let {
            @Serializable
            data class Local(val i: Int)

            val origin = Local(it)

            val decoded: Local = Json.decodeFromString(Json.encodeToString(origin))
            assertEquals(origin, decoded)
        }
    }

    @Suppress("SERIALIZER_TYPE_INCOMPATIBLE")
    @Test
    fun testObjectCustomSerializer() {
        @Serializable(with = ObjectCustomSerializer::class)
        data class Local(val i: Int)

        val origin: Local? = null

        val decoded: Local? = Json.decodeFromString(Json.encodeToString(origin))
        assertEquals(origin, decoded)
    }

    @Suppress("SERIALIZER_TYPE_INCOMPATIBLE")
    @Test
    fun testClassCustomSerializer() {
        @Serializable(with = ClassCustomSerializer::class)
        data class Local(val i: Int)

        val origin: Local? = null

        // FIXME change to `noLegacyJs` when lookup of `ClassCustomSerializer` will work on Native and JS/IR
        jvmOnly {
            val decoded: Local? = Json.decodeFromString(Json.encodeToString(origin))
            assertEquals(origin, decoded)
        }
    }

}
