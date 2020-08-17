/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlin.test.*

class JsonEncoderDecoderRecursiveTest : JsonTestBase() {
    private val inputDataString = """{"id":0,"payload":{"from":42,"to":43,"msg":"Hello world"},"timestamp":1000}"""
    private val inputErrorString = """{"id":1,"payload":{"error":"Connection timed out"},"timestamp":1001}"""
    private val inputDataJson = default.parseToJsonElement(inputDataString)
    private val inputErrorJson = default.parseToJsonElement(inputErrorString)
    private val inputRecursive =
        """{"type":"b","children":[{"type":"a","value":1},{"type":"a","value":2},{"type":"b","children":[]}]}"""
    private val outputRecursive = SealedRecursive.B(
        listOf(SealedRecursive.A(1), SealedRecursive.A(2), SealedRecursive.B(emptyList()))
    )

    @Test
    fun testParseDataString() = parametrizedTest { streaming ->
        val ev = default.decodeFromString(Event.serializer(), inputDataString, streaming)
        with(ev) {
            assertEquals(0, id)
            assertEquals(Either.Right(Payload(42, 43, "Hello world")), payload)
            assertEquals(1000, timestamp)
        }
    }

    @Test
    fun testParseErrorString() = parametrizedTest { useStreaming ->
        val ev = default.decodeFromString(Event.serializer(), inputErrorString, useStreaming)
        with(ev) {
            assertEquals(1, id)
            assertEquals(Either.Left("Connection timed out"), payload)
            assertEquals(1001, timestamp)
        }
    }

    @Test
    fun testWriteDataString() = parametrizedTest { useStreaming ->
        val outputData = Event(0, Either.Right(Payload(42, 43, "Hello world")), 1000)
        val ev = default.encodeToString(Event.serializer(), outputData, useStreaming)
        assertEquals(inputDataString, ev)
    }

    @Test
    fun testWriteDataStringIndented() = parametrizedTest { useStreaming ->
        val outputData = Event(0, Either.Right(Payload(42, 43, "Hello world")), 1000)
        val ev = Json { prettyPrint = true }.encodeToString(Event.serializer(), outputData, useStreaming)
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
        val outputError = Event(1, Either.Left("Connection timed out"), 1001)
        val ev = default.encodeToString(Event.serializer(), outputError, useStreaming)
        assertEquals(inputErrorString, ev)
    }

    @Test
    fun testParseDataJson() {
        val ev = default.decodeFromJsonElement(Event.serializer(), inputDataJson)
        with(ev) {
            assertEquals(0, id)
            assertEquals(Either.Right(Payload(42, 43, "Hello world")), payload)
            assertEquals(1000, timestamp)
        }
    }

    @Test
    fun testParseErrorJson() {
        val ev = default.decodeFromJsonElement(Event.serializer(), inputErrorJson)
        with(ev) {
            assertEquals(1, id)
            assertEquals(Either.Left("Connection timed out"), payload)
            assertEquals(1001, timestamp)
        }
    }

    @Test
    fun testWriteDataJson() {
        val outputData = Event(0, Either.Right(Payload(42, 43, "Hello world")), 1000)
        val ev = default.encodeToJsonElement(Event.serializer(), outputData)
        assertEquals(inputDataJson, ev)
    }

    @Test
    fun testWriteErrorJson() {
        val outputError = Event(1, Either.Left("Connection timed out"), 1001)
        val ev = default.encodeToJsonElement(Event.serializer(), outputError)
        assertEquals(inputErrorJson, ev)
    }

    @Test
    fun testParseRecursive() = parametrizedTest { useStreaming ->
        val ev = default.decodeFromString(RecursiveSerializer, inputRecursive, useStreaming)
        assertEquals(outputRecursive, ev)
    }

    @Test
    fun testWriteRecursive() = parametrizedTest { useStreaming ->
        val ev = default.encodeToString(RecursiveSerializer, outputRecursive, useStreaming)
        assertEquals(inputRecursive, ev)
    }

    @Serializable
    private data class Payload(val from: Long, val to: Long, val msg: String)

    private sealed class Either {
        data class Left(val errorMsg: String): Either()
        data class Right(val data: Payload): Either()
    }

    private object EitherSerializer: KSerializer<Either> {
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
            val jsonReader = decoder as? JsonDecoder
                    ?: throw SerializationException("This class can be loaded only by JSON")
            val tree = jsonReader.decodeJsonElement() as? JsonObject
                    ?: throw SerializationException("Expected JSON object")
            if ("error" in tree) return Either.Left(tree.getValue("error").jsonPrimitive.content)
            return Either.Right(decoder.json.decodeFromJsonElement(Payload.serializer(), tree))
        }

        override fun serialize(encoder: Encoder, value: Either) {
            val jsonWriter = encoder as? JsonEncoder
                    ?: throw SerializationException("This class can be saved only by JSON")
            val tree = when (value) {
                is Either.Left -> JsonObject(mapOf("error" to JsonPrimitive(value.errorMsg)))
                is Either.Right -> encoder.json.encodeToJsonElement(Payload.serializer(), value.data)
            }
            jsonWriter.encodeJsonElement(tree)
        }
    }

    @Serializable
    private data class Event(
        val id: Int,
        @Serializable(with = EitherSerializer::class) val payload: Either,
        val timestamp: Long
    )

    @Serializable(with = RecursiveSerializer::class)
    private sealed class SealedRecursive {
        @Serializable
        data class A(val value: Int) : SealedRecursive()

        @Serializable
        data class B(val children: List<SealedRecursive>) : SealedRecursive()
    }

    private object RecursiveSerializer : KSerializer<SealedRecursive> {
        private const val typeAttribute = "type"
        private const val typeNameA = "a"
        private const val typeNameB = "b"

        // TODO in builder is not suitable for recursive descriptors
        override val descriptor: SerialDescriptor = buildSerialDescriptor("SealedRecursive", PolymorphicKind.SEALED) {
            element("a", SealedRecursive.A.serializer().descriptor)
            element("b", SealedRecursive.B.serializer().descriptor)
        }

        override fun serialize(encoder: Encoder, value: SealedRecursive) {
            if (encoder !is JsonEncoder) throw SerializationException("This class can be saved only by JSON")
            val (tree, typeName) = when (value) {
                is SealedRecursive.A -> encoder.json.encodeToJsonElement(SealedRecursive.A.serializer(), value) to typeNameA
                is SealedRecursive.B -> encoder.json.encodeToJsonElement(SealedRecursive.B.serializer(), value) to typeNameB
            }
            val contents: MutableMap<String, JsonElement> = mutableMapOf(typeAttribute to JsonPrimitive(typeName))
            contents.putAll(tree.jsonObject)
            val element = JsonObject(contents)
            encoder.encodeJsonElement(element)
        }

        override fun deserialize(decoder: Decoder): SealedRecursive {
            val jsonReader = decoder as? JsonDecoder
                ?: throw SerializationException("This class can be loaded only by JSON")
            val tree = jsonReader.decodeJsonElement() as? JsonObject
                ?: throw SerializationException("Expected JSON object")
            val typeName = tree.getValue(typeAttribute).jsonPrimitive.content
            val objTree = JsonObject(tree - typeAttribute)
            return when (typeName) {
                typeNameA -> decoder.json.decodeFromJsonElement(SealedRecursive.A.serializer(), objTree)
                typeNameB -> decoder.json.decodeFromJsonElement(SealedRecursive.B.serializer(), objTree)
                else -> throw SerializationException("Unknown type: $typeName")
            }
        }
    }
}
