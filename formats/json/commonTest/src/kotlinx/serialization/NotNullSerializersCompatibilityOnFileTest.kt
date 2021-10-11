/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:UseSerializers(NotNullSerializersCompatibilityOnFileTest.NonNullableIntSerializer::class)
@file:UseContextualSerialization(NotNullSerializersCompatibilityOnFileTest.FileContextualType::class)

package kotlinx.serialization

import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlin.test.*

class NotNullSerializersCompatibilityOnFileTest {
    data class FileContextualType(val text: String)

    object FileContextualSerializer : KSerializer<FileContextualType> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FileContextualSerializer", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: FileContextualType) {
            return encoder.encodeString(value.text)
        }

        override fun deserialize(decoder: Decoder): FileContextualType {
            return FileContextualType(decoder.decodeString())
        }
    }

    @Serializable
    data class FileContextualHolder(val nullable: FileContextualType?, val nonNullable: FileContextualType)


    data class ContextualType(val text: String)

    object ContextualSerializer : KSerializer<ContextualType> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FileContextualSerializer", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: ContextualType) {
            return encoder.encodeString(value.text)
        }

        override fun deserialize(decoder: Decoder): ContextualType {
            return ContextualType(decoder.decodeString())
        }
    }

    @Serializable
    data class ContextualHolder(@Contextual val nullable: ContextualType?, @Contextual val nonNullable: ContextualType)


    @Serializable
    data class Holder(val nullable: Int?, val nonNullable: Int)

    @Serializer(forClass = Int::class)
    object NonNullableIntSerializer : KSerializer<Int> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("NotNullIntSerializer", PrimitiveKind.INT)

        override fun serialize(encoder: Encoder, value: Int) {
            return encoder.encodeInt(value + 2)
        }

        override fun deserialize(decoder: Decoder): Int {
            return (decoder.decodeInt() - 2)
        }
    }

    @Test
    fun testFileLevel() {
        assertEquals("""{"nullable":null,"nonNullable":52}""", Json.encodeToString(Holder(nullable = null, nonNullable = 50)))
        assertEquals("""{"nullable":2,"nonNullable":2}""", Json.encodeToString(Holder(nullable = 0, nonNullable = 0)))
        assertEquals("""{"nullable":12,"nonNullable":52}""", Json.encodeToString(Holder(nullable = 10, nonNullable = 50)))

        assertEquals(Holder(nullable = 0, nonNullable = 50), Json.decodeFromString("""{"nullable":2,"nonNullable":52}"""))
        assertEquals(Holder(nullable = null, nonNullable = 50), Json.decodeFromString("""{"nullable":null,"nonNullable":52}"""))
        assertEquals(Holder(nullable = 10, nonNullable = 50), Json.decodeFromString("""{"nullable":12,"nonNullable":52}"""))
    }

    @Test
    fun testFileContextual() {
        val module = SerializersModule {
            contextual(FileContextualSerializer)
        }

        val json = Json { serializersModule = module }

        assertEquals("""{"nullable":null,"nonNullable":"foo"}""", json.encodeToString(FileContextualHolder(null, FileContextualType("foo"))))
        assertEquals("""{"nullable":"foo","nonNullable":"bar"}""", json.encodeToString(FileContextualHolder(FileContextualType("foo"), FileContextualType("bar"))))

        assertEquals(FileContextualHolder(null, FileContextualType("foo")), json.decodeFromString("""{"nullable":null,"nonNullable":"foo"}"""))
        assertEquals(FileContextualHolder(FileContextualType("foo"), FileContextualType("bar")), json.decodeFromString("""{"nullable":"foo","nonNullable":"bar"}"""))
    }

    @Test
    fun testContextual() {
        val module = SerializersModule {
            contextual(ContextualSerializer)
        }

        val json = Json { serializersModule = module }

        assertEquals("""{"nullable":null,"nonNullable":"foo"}""", json.encodeToString(ContextualHolder(null, ContextualType("foo"))))
        assertEquals("""{"nullable":"foo","nonNullable":"bar"}""", json.encodeToString(ContextualHolder(ContextualType("foo"), ContextualType("bar"))))

        assertEquals(ContextualHolder(null, ContextualType("foo")), json.decodeFromString("""{"nullable":null,"nonNullable":"foo"}"""))
        assertEquals(ContextualHolder(ContextualType("foo"), ContextualType("bar")), json.decodeFromString("""{"nullable":"foo","nonNullable":"bar"}"""))
    }
}
