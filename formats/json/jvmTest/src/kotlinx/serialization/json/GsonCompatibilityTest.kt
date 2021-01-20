package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import org.junit.*

class GsonCompatibilityTest {

    @Serializable(with = ValueSerializer::class)
    data class Value<T>(val isSet: Boolean, val value: T?)

class ValueSerializer<T>(private val dataSerializer: KSerializer<T>) : KSerializer<Value<T>> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Value", PrimitiveKind.STRING).nullable

    override fun serialize(encoder: Encoder, value: Value<T>) {
        encoder.encodeNullableSerializableValue(dataSerializer, value.value)
    }

    override fun deserialize(decoder: Decoder) = TODO("Not implemented!")
}

    class ValueClassSerializer<T : Any>(private val dataSerializer: KSerializer<T>) :
        JsonTransformingSerializer<T>(dataSerializer) {
        override fun transformSerialize(element: JsonElement): JsonElement =
            element.jsonObject
    }

    @Serializable
    data class TestObject(
        val test1: Value<String> = Value(true, "Hello World"),
        val test2: Value<String> = Value(false, null),
        val test3: Value<String> = Value(true, null),
    )

    @Test
    fun f() {
        println(
            Json { encodeDefaults = true }.encodeToString(
                ValueClassSerializer(TestObject.serializer()),
                TestObject()
            )
        )
    }
}
