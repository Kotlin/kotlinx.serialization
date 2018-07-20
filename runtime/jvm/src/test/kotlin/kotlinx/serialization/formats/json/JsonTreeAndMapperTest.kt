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
import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals

@Serializable
data class Payload(val from: Long, val to: Long, val msg: String)

sealed class DummyEither {
    data class Left(val errorMsg: String): DummyEither()
    data class Right(val data: Payload): DummyEither()
}

@Serializer(forClass = DummyEither::class)
object EitherSerializer: KSerializer<DummyEither> {
    override fun deserialize(input: Decoder): DummyEither {
        val jsonReader = input as? JSON.JsonInput
                ?: throw SerializationException("This class can be loaded only by JSON")
        val tree = jsonReader.readAsTree() as? JsonObject
                ?: throw SerializationException("Expected JSON object")
        if ("error" in tree) return DummyEither.Left(tree.getPrimitive("error").content)
        return DummyEither.Right(JsonTreeMapper().readTree(tree, Payload.serializer()))
    }

    override fun serialize(output: Encoder, obj: DummyEither) {
        val jsonWriter = output as? JSON.JsonOutput
                ?: throw SerializationException("This class can be saved only by JSON")
        val tree = when (obj) {
            is DummyEither.Left -> JsonObject(mapOf("error" to JsonLiteral(obj.errorMsg)))
            is DummyEither.Right -> JsonTreeMapper().writeTree(obj.data, Payload.serializer())
        }
        jsonWriter.writeTree(tree)
    }
}

@Serializable
data class Event(
    val id: Int,
    @Serializable(with=EitherSerializer::class) val payload: DummyEither,
    val timestamp: Long
)

class JsonTreeAndMapperTest {
    val inputData = """{"id":0,"payload":{"msg": "Hello world", "from": 42, "to": 43},"timestamp":1000}"""
    val inputError = """{"id":1,"payload":{"error": "Connection timed out"},"timestamp":1001}"""

    @Test
    fun testParseData() {
        val ev = JSON.parse(Event.serializer(), inputData)
        with(ev) {
            assertEquals(0, id)
            assertEquals(DummyEither.Right(Payload(42, 43, "Hello world")), payload)
            assertEquals(1000, timestamp)
        }
    }

    @Test
    fun testParseError() {
        val ev = JSON.parse(Event.serializer(), inputError)
        with(ev) {
            assertEquals(1, id)
            assertEquals(DummyEither.Left("Connection timed out"), payload)
            assertEquals(1001, timestamp)
        }
    }

    @Test
    fun testWriteData() {
        val outputData = Event(0, DummyEither.Right(Payload(42, 43, "Hello world")), 1000)
        val ev = JSON.stringify(Event.serializer(), outputData)
        assertEquals(inputData, ev)
    }

    @Test
    fun testWriteError() {
        val outputError = Event(1, DummyEither.Left("Connection timed out"), 1001)
        val ev = JSON.stringify(Event.serializer(), outputError)
        assertEquals(inputError, ev)
    }

}
