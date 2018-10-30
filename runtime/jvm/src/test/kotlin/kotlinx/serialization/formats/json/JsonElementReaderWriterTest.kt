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
import kotlinx.serialization.context.SimpleModule
import kotlinx.serialization.internal.SerialClassDescImpl
import kotlinx.serialization.json.*
import org.junit.Test
import kotlin.test.assertEquals

class JsonElementReaderWriterTest {
    private val inputDataString = """{"id":0,"payload":{"msg":"Hello world","from":42,"to":43},"timestamp":1000}"""
    private val inputErrorString = """{"id":1,"payload":{"error":"Connection timed out"},"timestamp":1001}"""
    private val inputDataJson = JsonTreeParser.parse(inputDataString)
    private val inputErrorJson = JsonTreeParser.parse(inputErrorString)
    private val inputRecursive = """{"type":"b","children":[{"type":"a","value":1},{"type":"a","value":2},{"type":"b","children":[]}]}"""
    private val outputRecursive = DummyRecursive.B(
            listOf(DummyRecursive.A(1), DummyRecursive.A(2), DummyRecursive.B(emptyList()))
    )

    @Test
    fun testParseDataString() {
        val ev = JSON.parse(Event.serializer(), inputDataString)
        with(ev) {
            assertEquals(0, id)
            assertEquals(DummyEither.Right(Payload(42, 43, "Hello world")), payload)
            assertEquals(1000, timestamp)
        }
    }

    @Test
    fun testParseErrorString() {
        val ev = JSON.parse(Event.serializer(), inputErrorString)
        with(ev) {
            assertEquals(1, id)
            assertEquals(DummyEither.Left("Connection timed out"), payload)
            assertEquals(1001, timestamp)
        }
    }

    @Test
    fun testWriteDataString() {
        val outputData = Event(0, DummyEither.Right(Payload(42, 43, "Hello world")), 1000)
        val ev = JSON.stringify(Event.serializer(), outputData)
        assertEquals(inputDataString, ev)
    }

    @Test
    fun testWriteDataStringUnquoted() {
        val outputData = Event(0, DummyEither.Right(Payload(42, 43, "Hello world")), 1000)
        val ev = JSON.unquoted.stringify(Event.serializer(), outputData)
        assertEquals("""{id:0,payload:{msg:"Hello world",from:42,to:43},timestamp:1000}""", ev)
    }

    @Test
    fun testWriteDataStringIndented() {
        val outputData = Event(0, DummyEither.Right(Payload(42, 43, "Hello world")), 1000)
        val ev = JSON.indented.stringify(Event.serializer(), outputData)
        assertEquals("""{
            |    "id": 0,
            |    "payload": {
            |        "msg": "Hello world",
            |        "from": 42,
            |        "to": 43
            |    },
            |    "timestamp": 1000
            |}""".trimMargin(), ev)
    }

    @Test
    fun testWriteErrorString() {
        val outputError = Event(1, DummyEither.Left("Connection timed out"), 1001)
        val ev = JSON.stringify(Event.serializer(), outputError)
        assertEquals(inputErrorString, ev)
    }

    @Test
    fun testParseDataJson() {
        val ev = JsonTreeMapper().readTree(inputDataJson, Event.serializer())
        with(ev) {
            assertEquals(0, id)
            assertEquals(DummyEither.Right(Payload(42, 43, "Hello world")), payload)
            assertEquals(1000, timestamp)
        }
    }

    @Test
    fun testParseErrorJson() {
        val ev = JsonTreeMapper().readTree(inputErrorJson, Event.serializer())
        with(ev) {
            assertEquals(1, id)
            assertEquals(DummyEither.Left("Connection timed out"), payload)
            assertEquals(1001, timestamp)
        }
    }

    @Test
    fun testWriteDataJson() {
        val outputData = Event(0, DummyEither.Right(Payload(42, 43, "Hello world")), 1000)
        val ev = JsonTreeMapper().writeTree(outputData, Event.serializer())
        // JsonObject#equals seems to be broken here.
        assertEquals(inputDataJson.toString(), ev.toString())
    }

    @Test
    fun testWriteErrorJson() {
        val outputError = Event(1, DummyEither.Left("Connection timed out"), 1001)
        val ev = JsonTreeMapper().writeTree(outputError, Event.serializer())
        // JsonObject#equals seems to be broken here.
        assertEquals(inputErrorJson.toString(), ev.toString())
    }

    @Test
    fun testParseRecursive() {
        val ev = JSON.parse(RecursiveSerializer, inputRecursive)
        assertEquals(outputRecursive, ev)
    }

    @Test
    fun testWriteRecursive() {
        val ev = JSON.stringify(RecursiveSerializer, outputRecursive)
        assertEquals(inputRecursive, ev)
    }

    @Serializable
    private data class Payload(val from: Long, val to: Long, val msg: String)

    private sealed class DummyEither {
        data class Left(val errorMsg: String): DummyEither()
        data class Right(val data: Payload): DummyEither()
    }

    private object EitherSerializer: KSerializer<DummyEither> {
        override val descriptor: SerialDescriptor = SerialClassDescImpl("DummyEither")

        override fun deserialize(input: Decoder): DummyEither {
            val jsonReader = input as? JSON.JsonElementReader
                    ?: throw SerializationException("This class can be loaded only by JSON")
            val tree = jsonReader.readAsTree() as? JsonObject
                    ?: throw SerializationException("Expected JSON object")
            if ("error" in tree) return DummyEither.Left(tree.getPrimitive("error").content)
            return DummyEither.Right(JsonTreeMapper().readTree(tree, Payload.serializer()))
        }

        override fun serialize(output: Encoder, obj: DummyEither) {
            val jsonWriter = output as? JSON.JsonElementWriter
                    ?: throw SerializationException("This class can be saved only by JSON")
            val tree = when (obj) {
                is DummyEither.Left -> JsonObject(mapOf("error" to JsonLiteral(obj.errorMsg)))
                is DummyEither.Right -> JsonTreeMapper().writeTree(obj.data, Payload.serializer())
            }
            jsonWriter.writeJsonElement(tree)
        }
    }

    @Serializable
    private data class Event(
            val id: Int,
            @Serializable(with=EitherSerializer::class) val payload: DummyEither,
            val timestamp: Long
    )

    @Serializable(with = RecursiveSerializer::class)
    private sealed class DummyRecursive {
        @Serializable
        data class A(val value: Int): DummyRecursive()

        @Serializable
        data class B(val children: List<DummyRecursive>): DummyRecursive()
    }

    private object RecursiveSerializer: KSerializer<DummyRecursive> {
        private const val typeAttribute = "type"
        private const val typeNameA = "a"
        private const val typeNameB = "b"

        override val descriptor: SerialDescriptor
            get() = SerialClassDescImpl("DummyRecursive")

        override fun serialize(output: Encoder, obj: DummyRecursive) {
            if (output !is JSON.JsonElementWriter) throw SerializationException("This class can be saved only by JSON")
            val mapper = JsonTreeMapper()
            val (tree, typeName) = when (obj) {
                is DummyRecursive.A -> mapper.writeTree(obj, DummyRecursive.A.serializer()) to typeNameA
                is DummyRecursive.B -> mapper.writeTree(obj, DummyRecursive.B.serializer()) to typeNameB
            }
            val contents: MutableMap<String, JsonElement> = mutableMapOf(typeAttribute to JsonPrimitive(typeName))
            contents.putAll(tree.jsonObject.content)
            val typedTree = tree.jsonObject.copy(content = contents)
            output.writeJsonElement(typedTree)
        }

        override fun deserialize(input: Decoder): DummyRecursive {
            val jsonReader = input as? JSON.JsonElementReader
                    ?: throw SerializationException("This class can be loaded only by JSON")
            val tree = jsonReader.readAsTree() as? JsonObject
                    ?: throw SerializationException("Expected JSON object")
            val typeName = tree[typeAttribute].primitive.content
            val mapper = JsonTreeMapper()
            return when (typeName) {
                typeNameA -> mapper.readTree(tree, DummyRecursive.A.serializer())
                typeNameB -> mapper.readTree(tree, DummyRecursive.B.serializer())
                else -> throw SerializationException("Unknown type: $typeName")
            }
        }
    }
}