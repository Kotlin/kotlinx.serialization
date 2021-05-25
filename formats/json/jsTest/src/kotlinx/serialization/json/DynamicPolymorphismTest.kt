/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

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

        @Serializable
        @SerialName("nullable_child")
        data class NullableChild(val nullable: String?): Sealed(3)

        @Serializable
        @SerialName("list_child")
        data class ListChild(val list: List<String>): Sealed(4)

        @Serializable
        @SerialName("default_child")
        data class DefaultChild(val default: String? = "default"): Sealed(5)
    }

    @Serializable
    @JsonClassDiscriminator("sealed_custom")
    sealed class SealedCustom {
        @Serializable
        @SerialName("data_class")
        data class DataClassChild(val name: String) : SealedCustom()
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
    fun testCustomClassDiscriminator() {
        val value = SealedCustom.DataClassChild("custom-discriminator-test")
        encodeAndDecode(SealedCustom.serializer(), value, objectJson) {
            assertEquals("data_class", this["sealed_custom"])
            assertEquals(undefined, this.type)
            assertEquals(2, fieldsCount(this))
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
    fun testNullable() {
        val nonNullChild = Sealed.NullableChild("nonnull")
        encodeAndDecode(Sealed.serializer(), nonNullChild, arrayJson) {
            assertEquals("nullable_child", this[0])
            val dynamicValue = this[1]
            assertEquals(nonNullChild.nullable, dynamicValue.nullable)
            assertEquals(nonNullChild.intField, dynamicValue.intField)
            assertEquals(2, fieldsCount(dynamicValue))
        }
        encodeAndDecode(Sealed.serializer(), nonNullChild, objectJson) {
            assertEquals("nullable_child", this.type)
            assertEquals(nonNullChild.nullable, this.nullable)
            assertEquals(nonNullChild.intField, this.intField)
            assertEquals(3, fieldsCount(this))
        }

        val nullChild = Sealed.NullableChild(null)
        encodeAndDecode(Sealed.serializer(), nullChild, arrayJson) {
            assertEquals("nullable_child", this[0])
            val dynamicValue = this[1]
            assertEquals(nullChild.nullable, dynamicValue.nullable)
            assertEquals(nullChild.intField, dynamicValue.intField)
            assertEquals(2, fieldsCount(dynamicValue))
        }
        encodeAndDecode(Sealed.serializer(), nullChild, objectJson) {
            assertEquals("nullable_child", this.type)
            assertEquals(nullChild.nullable, this.nullable)
            assertEquals(nullChild.intField, this.intField)
            assertEquals(3, fieldsCount(this))
        }
    }

    @Test
    fun testList() {
        val listChild = Sealed.ListChild(listOf("one", "two"))
        encodeAndDecode(Sealed.serializer(), listChild, arrayJson) {
            assertEquals("list_child", this[0])
            val dynamicValue = this[1]
            assertEquals(listChild.list, (dynamicValue.list as Array<String>).toList())
            assertEquals(listChild.intField, dynamicValue.intField)
            assertEquals(2, fieldsCount(dynamicValue))
        }
        encodeAndDecode(Sealed.serializer(), listChild, objectJson) {
            assertEquals("list_child", this.type)
            assertEquals(listChild.list, (this.list as Array<String>).toList())
            assertEquals(listChild.intField, this.intField)
            assertEquals(3, fieldsCount(this))
        }
    }

    @Test
    fun testEmptyList() {
        val emptyListChild = Sealed.ListChild(emptyList())
        encodeAndDecode(Sealed.serializer(), emptyListChild, arrayJson) {
            assertEquals("list_child", this[0])
            val dynamicValue = this[1]
            assertEquals(emptyListChild.list, (dynamicValue.list as Array<String>).toList())
            assertEquals(emptyListChild.intField, dynamicValue.intField)
            assertEquals(2, fieldsCount(dynamicValue))
        }
        encodeAndDecode(Sealed.serializer(), emptyListChild, objectJson) {
            assertEquals("list_child", this.type)
            assertEquals(emptyListChild.list, (this.list as Array<String>).toList())
            assertEquals(emptyListChild.intField, this.intField)
            assertEquals(3, fieldsCount(this))
        }
    }

    @Test
    fun testDefaultValue() {
        val objectJsonWithDefaults = Json(objectJson) {
            encodeDefaults = true
        }

        val arrayJsonWithDefaults = Json(arrayJson) {
            encodeDefaults = true
        }

        val defaultChild = Sealed.DefaultChild()
        encodeAndDecode(Sealed.serializer(), defaultChild, arrayJson) {
            assertEquals("default_child", this[0])
            val dynamicValue = this[1]
            assertEquals(null, dynamicValue.default, "arrayJson should not encode defaults")
            assertEquals(defaultChild.intField, dynamicValue.intField)
            assertEquals(1, fieldsCount(dynamicValue))
        }
        encodeAndDecode(Sealed.serializer(), defaultChild, arrayJsonWithDefaults) {
            assertEquals("default_child", this[0])
            val dynamicValue = this[1]
            assertEquals(defaultChild.default, dynamicValue.default, "arrayJsonWithDefaults should encode defaults")
            assertEquals(defaultChild.intField, dynamicValue.intField)
            assertEquals(2, fieldsCount(dynamicValue))
        }

        encodeAndDecode(Sealed.serializer(), defaultChild, objectJson) {
            assertEquals("default_child", this.type)
            assertEquals(null, this.default, "objectJson should not encode defaults")
            assertEquals(defaultChild.intField, this.intField)
            assertEquals(2, fieldsCount(this))
        }
        encodeAndDecode(Sealed.serializer(), defaultChild, objectJsonWithDefaults) {
            assertEquals("default_child", this.type)
            assertEquals(defaultChild.default, this.default, "objectJsonWithDefaults should encode defaults")
            assertEquals(defaultChild.intField, this.intField)
            assertEquals(3, fieldsCount(this))
        }

    }

    @Test
    fun testNonDefaultValue() {
        val nonDefaultChild = Sealed.DefaultChild("non default value")
        encodeAndDecode(Sealed.serializer(), nonDefaultChild, arrayJson) {
            assertEquals("default_child", this[0])
            val dynamicValue = this[1]
            assertEquals(nonDefaultChild.default, dynamicValue.default)
            assertEquals(nonDefaultChild.intField, dynamicValue.intField)
            assertEquals(2, fieldsCount(dynamicValue))
        }

        encodeAndDecode(Sealed.serializer(), nonDefaultChild, objectJson) {
            assertEquals("default_child", this.type)
            assertEquals(nonDefaultChild.default, this.default)
            assertEquals(nonDefaultChild.intField, this.intField)
            assertEquals(3, fieldsCount(this))
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

    @Suppress("NOTHING_TO_INLINE")
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
