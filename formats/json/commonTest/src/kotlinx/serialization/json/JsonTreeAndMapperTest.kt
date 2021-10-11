/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlin.test.*

class JsonTreeAndMapperTest {
    private val decoderData = """{"id":0,"payload":{"from":42,"to":43,"msg":"Hello world"},"timestamp":1000}"""
    private val decoderError = """{"id":1,"payload":{"error":"Connection timed out"},"timestamp":1001}"""

    @Serializable
    data class Payload(val from: Long, val to: Long, val msg: String)

    sealed class Either {
        data class Left(val errorMsg: String) : Either()
        data class Right(val data: Payload) : Either()
    }

    object EitherSerializer : KSerializer<Either> {
        override val descriptor: SerialDescriptor = buildSerialDescriptor("Either", PolymorphicKind.SEALED) {
            val leftDescriptor = buildClassSerialDescriptor("Either.Left") {
                element<String>("errorMsg")
            }
            val rightDescriptor = buildClassSerialDescriptor("Either.Right") {
                element("data", Payload.serializer().descriptor)
            }
            element("left", leftDescriptor)
            element("right", rightDescriptor)
        }

        override fun deserialize(decoder: Decoder): Either {
            val input = decoder as? JsonDecoder ?: throw SerializationException("This class can be loaded only by Json")
            val tree = input.decodeJsonElement() as? JsonObject
                ?: throw SerializationException("Expected JsonObject")
            if ("error" in tree) return Either.Left(tree.getValue("error").jsonPrimitive.content)

            return Either.Right(input.json.decodeFromJsonElement(Payload.serializer(), tree))
        }

        override fun serialize(encoder: Encoder, value: Either) {
            val output = encoder as? JsonEncoder ?: throw SerializationException("This class can be saved only by Json")
            val tree = when (value) {
                is Either.Left -> JsonObject(mapOf("error" to JsonPrimitive(value.errorMsg)))
                is Either.Right -> output.json.encodeToJsonElement(Payload.serializer(), value.data)
            }

            output.encodeJsonElement(tree)
        }
    }

    @Serializable
    data class Event(
        val id: Int,
        @Serializable(with = EitherSerializer::class) val payload: Either,
        val timestamp: Long
    )

    @Test
    fun testParseData() {
        val ev = Json.decodeFromString(Event.serializer(), decoderData)
        with(ev) {
            assertEquals(0, id)
            assertEquals(Either.Right(Payload(42, 43, "Hello world")), payload)
            assertEquals(1000, timestamp)
        }
    }

    @Test
    fun testParseError() {
        val ev = Json.decodeFromString(Event.serializer(), decoderError)
        with(ev) {
            assertEquals(1, id)
            assertEquals(Either.Left("Connection timed out"), payload)
            assertEquals(1001, timestamp)
        }
    }

    @Test
    fun testWriteData() {
        val encoderData = Event(0, Either.Right(Payload(42, 43, "Hello world")), 1000)
        val ev = Json.encodeToString(Event.serializer(), encoderData)
        assertEquals(decoderData, ev)
    }

    @Test
    fun testWriteError() {
        val encoderError = Event(1, Either.Left("Connection timed out"), 1001)
        val ev = Json.encodeToString(Event.serializer(), encoderError)
        assertEquals(decoderError, ev)
    }
}
