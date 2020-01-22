/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlin.test.*

@Serializable
data class Payload(val from: Long, val to: Long, val msg: String)

sealed class DummyEither {
    data class Left(val errorMsg: String) : DummyEither()
    data class Right(val data: Payload) : DummyEither()
}

object EitherSerializer : KSerializer<DummyEither> {
    override val descriptor: SerialDescriptor = SerialClassDescImpl("DummyEither")

    override fun deserialize(decoder: Decoder): DummyEither {
        val input = decoder as? JsonInput ?: throw SerializationException("This class can be loaded only by Json")
        val tree = input.decodeJson() as? JsonObject
            ?: throw SerializationException("Expected JsonObject")
        if ("error" in tree) return DummyEither.Left(tree.getPrimitive("error").content)

        return DummyEither.Right(input.json.fromJson(Payload.serializer(), tree))
    }

    override fun serialize(encoder: Encoder, value: DummyEither) {
        val output = encoder as? JsonOutput ?: throw SerializationException("This class can be saved only by Json")
        val tree = when (value) {
            is DummyEither.Left -> JsonObject(mapOf("error" to JsonLiteral(value.errorMsg)))
            is DummyEither.Right -> output.json.toJson(Payload.serializer(), value.data)
        }

        output.encodeJson(tree)
    }
}

@Serializable
data class Event(
    val id: Int,
    @Serializable(with = EitherSerializer::class) val payload: DummyEither,
    val timestamp: Long
)

class JsonTreeAndMapperTest {
    private val decoderData = """{"id":0,"payload":{"from":42,"to":43,"msg":"Hello world"},"timestamp":1000}"""
    private val decoderError = """{"id":1,"payload":{"error":"Connection timed out"},"timestamp":1001}"""

    @Test
    fun testParseData() {
        val ev = Json.parse(Event.serializer(), decoderData)
        with(ev) {
            assertEquals(0, id)
            assertEquals(DummyEither.Right(Payload(42, 43, "Hello world")), payload)
            assertEquals(1000, timestamp)
        }
    }

    @Test
    fun testParseError() {
        val ev = Json.parse(Event.serializer(), decoderError)
        with(ev) {
            assertEquals(1, id)
            assertEquals(DummyEither.Left("Connection timed out"), payload)
            assertEquals(1001, timestamp)
        }
    }

    @Test
    fun testWriteData() {
        val encoderData = Event(0, DummyEither.Right(Payload(42, 43, "Hello world")), 1000)
        val ev = Json.stringify(Event.serializer(), encoderData)
        assertEquals(decoderData, ev)
    }

    @Test
    fun testWriteError() {
        val encoderError = Event(1, DummyEither.Left("Connection timed out"), 1001)
        val ev = Json.stringify(Event.serializer(), encoderError)
        assertEquals(decoderError, ev)
    }

}
