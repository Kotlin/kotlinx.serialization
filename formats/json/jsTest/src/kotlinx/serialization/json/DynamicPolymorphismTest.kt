package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlin.test.Test
import kotlin.test.assertEquals

class DynamicPolymorphismTest {
    @Serializable
    sealed class Sealed(val intField: Int) {
        @Serializable
        @SerialName("object")
        object ObjectChild : Sealed(0)

        @Serializable
        @SerialName("data_class")
        data class DataClassChild(val name: String) : Sealed(1)

        @Serializable
        @SerialName("type_child")
        data class TypeChild(val type: String) : Sealed(2)
    }

    @Serializable
    data class CompositeClass(val mark: String, val nested: Sealed)

    @Serializable
    data class AnyWrapper(@Polymorphic val any: Any)

    @Serializable
    @SerialName("string_wrapper")
    data class StringWrapper(val text: String)

    private val arrayJson = Json {
        useArrayPolymorphism = true
    }

    private val objectJson = Json {
        useArrayPolymorphism = false
    }

    @Test
    fun testDiscriminatorName() {
        val newClassDiscriminator = "key"

        val json = Json {
            useArrayPolymorphism = false
            classDiscriminator = newClassDiscriminator
        }

        val value = Sealed.TypeChild("discriminator-test")
        encodeAndDecode(Sealed.serializer(), value, json) {
            assertEquals("type_child", this[newClassDiscriminator])
            assertEquals(value.type, this.type)
            assertEquals(value.intField, this.intField)
            assertEquals(3, fieldsCount(this))
        }
    }

    @Test
    fun testComposite() {
        val nestedValue = Sealed.DataClassChild("child")
        val value = CompositeClass("composite", nestedValue)
        encodeAndDecode(CompositeClass.serializer(), value, objectJson) {
            assertEquals(value.mark, this.mark)
            val nested = this.nested
            assertEquals("data_class", nested.type)
            assertEquals(nestedValue.name, nested.name)
            assertEquals(nestedValue.intField, nested.intField)
            assertEquals(3, fieldsCount(nested))
        }

        encodeAndDecode(CompositeClass.serializer(), value, arrayJson) {
            assertEquals(value.mark, this.mark)
            assertEquals("data_class", this.nested[0])
            val nested = this.nested[1]
            assertEquals(nestedValue.name, nested.name)
            assertEquals(nestedValue.intField, nested.intField)
            assertEquals(2, fieldsCount(nested))
        }
    }


    @Test
    fun testDataClass() {
        val value = Sealed.DataClassChild("data-class")

        encodeAndDecode(Sealed.serializer(), value, objectJson) {
            assertEquals("data_class", this.type)
            assertEquals(value.name, this.name)
            assertEquals(value.intField, this.intField)
            assertEquals(3, fieldsCount(this))
        }

        encodeAndDecode(Sealed.serializer(), value, arrayJson) {
            assertEquals("data_class", this[0])
            val dynamicValue = this[1]
            assertEquals(value.name, dynamicValue.name)
            assertEquals(value.intField, dynamicValue.intField)
            assertEquals(2, fieldsCount(dynamicValue))
        }
    }

    @Test
    fun testObject() {
        val value = Sealed.ObjectChild
        encodeAndDecode(Sealed.serializer(), value, objectJson) {
            assertEquals("object", this.type)
            assertEquals(1, fieldsCount(this))
        }

        encodeAndDecode(Sealed.serializer(), value, arrayJson) {
            assertEquals("object", this[0])
            assertEquals(0, fieldsCount(this[1]))
        }
    }

    @Test
    fun testAny() {
        val serializersModule = SerializersModule {
            polymorphic(Any::class) {
                subclass(StringWrapper.serializer())
            }
        }

        val json = Json(objectJson) { this.serializersModule = serializersModule }

        val anyValue = StringWrapper("some text")
        val value = AnyWrapper(anyValue)

        encodeAndDecode(AnyWrapper.serializer(), value, json) {
            assertEquals("string_wrapper", this.any.type)
            assertEquals(anyValue.text, this.any.text)
            assertEquals(1, fieldsCount(this))
            assertEquals(2, fieldsCount(this.any))
        }

        val json2 = Json(arrayJson) { this.serializersModule = serializersModule }

        encodeAndDecode(AnyWrapper.serializer(), value, json2) {
            assertEquals("string_wrapper", this.any[0])
            assertEquals(anyValue.text, this.any[1].text)
            assertEquals(1, fieldsCount(this))
            assertEquals(2, fieldsCount(this.any))
        }
    }

    private inline fun fieldsCount(dynamic: dynamic): Int {
        return js("Object").keys(dynamic).length as Int
    }

    private fun <T> encodeAndDecode(deserializer: KSerializer<T>, value: T, json: Json, assertBlock: dynamic.() -> Unit) {
        val dynamic = json.encodeToDynamic(deserializer, value)
        assertBlock(dynamic)
        val decodedValue = json.decodeFromDynamic(deserializer, dynamic)
        assertEquals(value, decodedValue)
    }
}
