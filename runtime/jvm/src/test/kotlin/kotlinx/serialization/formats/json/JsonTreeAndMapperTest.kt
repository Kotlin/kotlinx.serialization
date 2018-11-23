/*
 * Copyright 2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.serialization.formats.json

import kotlinx.serialization.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*
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
        val tree = input.readTree() as? JsonObject
            ?: throw SerializationException("Expected JsonObject")
        if ("error" in tree) return DummyEither.Left(tree.getPrimitive("error").content)

        return DummyEither.Right(input.json.fromJson(tree, Payload.serializer()))
    }

    override fun serialize(encoder: Encoder, obj: DummyEither) {
        val output = encoder as? JsonOutput ?: throw SerializationException("This class can be saved only by Json")
        val tree = when (obj) {
            is DummyEither.Left -> JsonObject(mapOf("error" to JsonLiteral(obj.errorMsg)))
            is DummyEither.Right -> output.json.toJson(obj.data, Payload.serializer())
        }

        output.writeTree(tree)
    }
}

@Serializable
data class Event(
    val id: Int,
    @Serializable(with = EitherSerializer::class) val payload: DummyEither,
    val timestamp: Long
)

class JsonTreeAndMapperTest {
    private val decoderData = """{"id":0,"payload":{"from": 42, "to": 43, "msg": "Hello world"},"timestamp":1000}"""
    private val decoderError = """{"id":1,"payload":{"error": "Connection timed out"},"timestamp":1001}"""

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
