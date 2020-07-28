/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlinx.serialization.test.*
import kotlin.test.*

public class SerializerForNullableTypeTest : JsonTestBase() {

    // Nullable boxes
    @Serializable(with = StringHolderSerializer::class)
    data class StringHolder(val s: String)

    @Serializer(forClass = StringHolder::class)
    object StringHolderSerializer : KSerializer<StringHolder?> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("SHS", PrimitiveKind.STRING).nullable

        override fun serialize(encoder: Encoder, value: StringHolder?) {
            if (value == null) encoder.encodeString("nullable")
            else encoder.encodeString("non-nullable")
        }

        override fun deserialize(decoder: Decoder): StringHolder? {
            if (decoder.decodeNotNullMark()) {
                return StringHolder("non-null: " + decoder.decodeString())
            }
            decoder.decodeNull()
            return StringHolder("nullable")
        }
    }

    @Serializable
    data class Box(val s: StringHolder?)

    @Test
    fun testNullableBoxWithNotNull() {
        val b = Box(StringHolder("box"))
        val string = Json.encodeToString(b)
        assertEquals("""{"s":"non-nullable"}""", string)
        val deserialized = Json.decodeFromString<Box>(string)
        assertEquals(Box(StringHolder("non-null: non-nullable")), deserialized)
    }

    @Test
    fun testNullableBoxWithNull() {
        val b = Box(null)
        val string = Json.encodeToString(b)
        assertEquals("""{"s":"nullable"}""", string)
        val deserialized = Json.decodeFromString<Box>(string)
        assertEquals(Box(StringHolder("non-null: nullable")), deserialized)
    }

    @Test
    fun testNullableBoxDeserializeNull() {
        val deserialized = Json.decodeFromString<Box>("""{"s":null}""")
        assertEquals(Box(StringHolder("nullable")), deserialized)
    }

    // Nullable primitives
    object NullableLongSerializer : KSerializer<Long?> {

        @Serializable
        data class OptionalLong(val initialized: Boolean, val value: Long? = 0)

        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("NLS") {
            element<Boolean>("initialized")
            element<Long?>("value")
        }.nullable

        override fun serialize(encoder: Encoder, value: Long?) {
            val opt = OptionalLong(value != null, value)
            encoder.encodeSerializableValue(OptionalLong.serializer(), opt)
        }

        override fun deserialize(decoder: Decoder): Long? {
            val value = decoder.decodeSerializableValue(OptionalLong.serializer())
            return if (value.initialized) value.value else null
        }
    }

    @Serializable
    data class NullablePrimitive(
        @Serializable(with = NullableLongSerializer::class) val value: Long? = null
    )

    @Test
    fun testNullableLongWithNotNull() {
        val data = NullablePrimitive(42)
        val json = Json.encodeToString(data)
        assertEquals("""{"value":{"initialized":true,"value":42}}""", Json.encodeToString(data))
        assertEquals(data, Json.decodeFromString(json))
    }

    @Test
    fun testNullableLongWithNull() {
        val data = NullablePrimitive(null)
        val json = Json.encodeToString(data)
        assertEquals("""{"value":{"initialized":false,"value":null}}""", Json.encodeToString(data))
        assertEquals(data, Json.decodeFromString(json))
    }

    // Now generics
    @Serializable
    data class GenericNullableBox<T: Any>(val value: T?)

    @Serializable
    data class GenericBox<T>(val value: T?)

    @Test
    fun testGenericBoxNullable() {
        if (isJsLegacy()) return
        val data = GenericBox<StringHolder?>(null)
        val json = Json.encodeToString(data)
        assertEquals("""{"value":"nullable"}""", Json.encodeToString(data))
        assertEquals(GenericBox(StringHolder("non-null: nullable")), Json.decodeFromString(json))
    }

    @Test
    fun testGenericNullableBoxFromNull() {
        if (isJsLegacy()) return
        assertEquals(GenericBox(StringHolder("nullable")), Json.decodeFromString("""{"value":null}"""))
    }

    @Test
    fun testGenericNullableBoxNullable() {
        if (isJsLegacy()) return
        val data = GenericNullableBox<StringHolder>(null)
        val json = Json.encodeToString(data)
        assertEquals("""{"value":"nullable"}""", Json.encodeToString(data))
        assertEquals(GenericNullableBox(StringHolder("non-null: nullable")), Json.decodeFromString(json))
    }

    @Test
    fun testGenericBoxNullableFromNull() {
        if (isJsLegacy()) return
        assertEquals(GenericNullableBox(StringHolder("nullable")), Json.decodeFromString("""{"value":null}"""))
    }

}
