/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.internal.SerialClassDescImpl
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonInputOutputRecursiveTest : JsonTestBase() {
    private val inputDataString = """{"id":0,"payload":{"from":42,"to":43,"msg":"Hello world"},"timestamp":1000}"""
    private val inputErrorString = """{"id":1,"payload":{"error":"Connection timed out"},"timestamp":1001}"""
    private val inputDataJson = strict.parseJson(inputDataString)
    private val inputErrorJson = strict.parseJson(inputErrorString)
    private val inputRecursive = """{"type":"b","children":[{"type":"a","value":1},{"type":"a","value":2},{"type":"b","children":[]}]}"""
    private val outputRecursive = DummyRecursive.B(
            listOf(DummyRecursive.A(1), DummyRecursive.A(2), DummyRecursive.B(emptyList()))
    )

    @Test
    fun testParseDataString() = parametrizedTest { streaming ->
        val ev = strict.parse(Event.serializer(), inputDataString, streaming)
        with(ev) {
            assertEquals(0, id)
            assertEquals(DummyEither.Right(Payload(42, 43, "Hello world")), payload)
            assertEquals(1000, timestamp)
        }
    }

    @Test
    fun testParseErrorString() = parametrizedTest { useStreaming ->
        val ev = strict.parse(Event.serializer(), inputErrorString, useStreaming)
        with(ev) {
            assertEquals(1, id)
            assertEquals(DummyEither.Left("Connection timed out"), payload)
            assertEquals(1001, timestamp)
        }
    }

    @Test
    fun testWriteDataString() = parametrizedTest { useStreaming ->
        val outputData = Event(0, DummyEither.Right(Payload(42, 43, "Hello world")), 1000)
        val ev = strict.stringify(Event.serializer(), outputData, useStreaming)
        assertEquals(inputDataString, ev)
    }

    @Test
    fun testWriteDataStringUnquoted() = parametrizedTest { useStreaming ->
        val outputData = Event(0, DummyEither.Right(Payload(42, 43, "Hello world")), 1000)
        val ev = unquoted.stringify(Event.serializer(), outputData, useStreaming)
        assertEquals("""{id:0,payload:{from:42,to:43,msg:"Hello world"},timestamp:1000}""", ev)
    }

    @Test
    fun testWriteDataStringIndented() = parametrizedTest { useStreaming ->
        val outputData = Event(0, DummyEither.Right(Payload(42, 43, "Hello world")), 1000)
        val ev = Json.indented.stringify(Event.serializer(), outputData, useStreaming)
        assertEquals("""{
            |    "id": 0,
            |    "payload": {
            |        "from": 42,
            |        "to": 43,
            |        "msg": "Hello world"
            |    },
            |    "timestamp": 1000
            |}""".trimMargin(), ev)
    }

    @Test
    fun testWriteErrorString() = parametrizedTest { useStreaming ->
        val outputError = Event(1, DummyEither.Left("Connection timed out"), 1001)
        val ev = strict.stringify(Event.serializer(), outputError, useStreaming)
        assertEquals(inputErrorString, ev)
    }

    @Test
    fun testParseDataJson() {
        val ev = strict.fromJson(Event.serializer(), inputDataJson)
        with(ev) {
            assertEquals(0, id)
            assertEquals(DummyEither.Right(Payload(42, 43, "Hello world")), payload)
            assertEquals(1000, timestamp)
        }
    }

    @Test
    fun testParseErrorJson() {
        val ev = strict.fromJson(Event.serializer(), inputErrorJson)
        with(ev) {
            assertEquals(1, id)
            assertEquals(DummyEither.Left("Connection timed out"), payload)
            assertEquals(1001, timestamp)
        }
    }

    @Test
    fun testWriteDataJson() {
        val outputData = Event(0, DummyEither.Right(Payload(42, 43, "Hello world")), 1000)
        val ev = strict.toJson(Event.serializer(), outputData)
        assertEquals(inputDataJson, ev)
    }

    @Test
    fun testWriteErrorJson() {
        val outputError = Event(1, DummyEither.Left("Connection timed out"), 1001)
        val ev = strict.toJson(Event.serializer(), outputError)
        assertEquals(inputErrorJson, ev)
    }

    @Test
    fun testParseRecursive() = parametrizedTest { useStreaming ->
        val ev = strict.parse(RecursiveSerializer, inputRecursive, useStreaming)
        assertEquals(outputRecursive, ev)
    }

    @Test
    fun testWriteRecursive() = parametrizedTest { useStreaming ->
        val ev = strict.stringify(RecursiveSerializer, outputRecursive, useStreaming)
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

        override fun deserialize(decoder: Decoder): DummyEither {
            val jsonReader = decoder as? JsonInput
                    ?: throw SerializationException("This class can be loaded only by JSON")
            val tree = jsonReader.decodeJson() as? JsonObject
                    ?: throw SerializationException("Expected JSON object")
            if ("error" in tree) return DummyEither.Left(tree.getPrimitive("error").content)
            return DummyEither.Right(decoder.json.fromJson(Payload.serializer(), tree))
        }

        override fun serialize(encoder: Encoder, value: DummyEither) {
            val jsonWriter = encoder as? JsonOutput
                    ?: throw SerializationException("This class can be saved only by JSON")
            val tree = when (value) {
                is DummyEither.Left -> JsonObject(mapOf("error" to JsonLiteral(value.errorMsg)))
                is DummyEither.Right -> encoder.json.toJson(Payload.serializer(), value.data)
            }
            jsonWriter.encodeJson(tree)
        }
    }

    @Serializable
    private data class Event(
        val id: Int,
        @Serializable(with = EitherSerializer::class) val payload: DummyEither,
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

        override fun serialize(encoder: Encoder, value: DummyRecursive) {
            if (encoder !is JsonOutput) throw SerializationException("This class can be saved only by JSON")
            val (tree, typeName) = when (value) {
                is DummyRecursive.A -> encoder.json.toJson(DummyRecursive.A.serializer(), value) to typeNameA
                is DummyRecursive.B -> encoder.json.toJson(DummyRecursive.B.serializer(), value) to typeNameB
            }
            val contents: MutableMap<String, JsonElement> = mutableMapOf(typeAttribute to JsonPrimitive(typeName))
            contents.putAll(tree.jsonObject.content)
            val element = JsonObject(contents)
            encoder.encodeJson(element)
        }

        override fun deserialize(decoder: Decoder): DummyRecursive {
            val jsonReader = decoder as? JsonInput
                    ?: throw SerializationException("This class can be loaded only by JSON")
            val tree = jsonReader.decodeJson() as? JsonObject
                    ?: throw SerializationException("Expected JSON object")
            val typeName = tree.getValue(typeAttribute).primitive.content
            val objTree = JsonObject(tree.content - typeAttribute)
            return when (typeName) {
                typeNameA -> decoder.json.fromJson(DummyRecursive.A.serializer(), objTree)
                typeNameB -> decoder.json.fromJson(DummyRecursive.B.serializer(), objTree)
                else -> throw SerializationException("Unknown type: $typeName")
            }
        }
    }
}
