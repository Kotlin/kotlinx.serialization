package kotlinx.serialization.json.polymorphic

import kotlinx.serialization.*
import kotlinx.serialization.internal.SerialClassDescImpl
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.test.*

class JsonPolymorphicDefaultDeserializerTest : JsonTestBase() {

    private val defaultSerializer: KSerializer<InnerBase> = object : KSerializer<InnerBase> {
        override fun serialize(encoder: Encoder, obj: InnerBase) {
            encoder.beginStructure(descriptor).endStructure(descriptor)
        }

        override fun deserialize(decoder: Decoder): InnerBase = InnerImpl2(42)

        override val descriptor: SerialDescriptor = SerialClassDescImpl("NotExisting")
    }

    private val json = Json {
        unquoted = true
        serialModule = SerializersModule {
            polymorphic<InnerBase> {
                addSubclass(InnerImpl.serializer())
                setDefaultSerializer(defaultSerializer)
            }
        }
    }

    @Test
    fun testPolymorphicDefaultSerialization() {
        assertEquals("{type:NotExisting}", json.stringify(PolymorphicSerializer(InnerBase::class), InnerImpl2(42), true))
    }

    @Test
    fun testPolymorphicDefaultDeserialization() {
        val string = "{type:kotlinx.serialization.json.polymorphic.NotExistingInnerImpl}"
        assertEquals(InnerImpl2(42), json.parse(PolymorphicSerializer(InnerBase::class), string, true))
    }

}
