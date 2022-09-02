package kotlinx.serialization.modules

import kotlinx.serialization.*
import kotlinx.serialization.encoding.*
import kotlin.test.*

open class ContextualInterfaceTest {
    sealed interface Dummy {
        val data: String
    }

    @Serializable
    data class DummyImpl(override val data: String) : Dummy

    object DummySerializer : KSerializer<Dummy> {
        override val descriptor = DummyImpl.serializer().descriptor

        override fun serialize(encoder: Encoder, value: Dummy) =
            DummyImpl.serializer().serialize(encoder, DummyImpl(value.data))

        override fun deserialize(decoder: Decoder): Dummy = DummyImpl.serializer().deserialize(decoder)
    }

    private val module = SerializersModule { contextual(Dummy::class, DummySerializer) }

    @Test
    fun testContextualWinsOverCompiled() {
        val serializer = module.serializer<Dummy>()
        assertEquals(DummySerializer, serializer, "Contextual serializer should win over polymorphicSerializer.")
    }
}