/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.json.internal.*
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.test.*
import kotlin.test.assertEquals

enum class JsonTestingMode {
    STREAMING,
    TREE,
    JAVA_STREAMS;

    companion object {
        fun value(i: Int) = values()[i]
    }
}

abstract class JsonTestBase {
    protected val default = Json { encodeDefaults = true }
    protected val lenient = Json { isLenient = true; ignoreUnknownKeys = true; allowSpecialFloatingPointValues = true }

    internal inline fun <reified T : Any> Json.encodeToString(value: T, jsonTestingMode: JsonTestingMode): String {
        val serializer = serializersModule.serializer<T>()
        return encodeToString(serializer, value, jsonTestingMode)
    }

    internal fun <T> Json.encodeToString(
        serializer: SerializationStrategy<T>,
        value: T,
        jsonTestingMode: JsonTestingMode
    ): String =
        when (jsonTestingMode) {
            JsonTestingMode.STREAMING -> {
                encodeToString(serializer, value)
            }
            JsonTestingMode.JAVA_STREAMS -> {
                encodeViaStream(serializer, value)
            }
            JsonTestingMode.TREE -> {
                val tree = writeJson(value, serializer)
                encodeToString(tree)
            }
        }

    internal inline fun <reified T : Any> Json.decodeFromString(source: String, jsonTestingMode: JsonTestingMode): T {
        val deserializer = serializersModule.serializer<T>()
        return decodeFromString(deserializer, source, jsonTestingMode)
    }

    internal fun <T> Json.decodeFromString(
        deserializer: DeserializationStrategy<T>,
        source: String,
        jsonTestingMode: JsonTestingMode
    ): T =
        when (jsonTestingMode) {
            JsonTestingMode.STREAMING -> {
                decodeFromString(deserializer, source)
            }
            JsonTestingMode.JAVA_STREAMS -> {
                decodeViaStream(deserializer, source)
            }
            JsonTestingMode.TREE -> {
                val lexer = StringJsonLexer(source)
                val input = StreamingJsonDecoder(this, WriteMode.OBJ, lexer)
                val tree = input.decodeJsonElement()
                lexer.expectEof()
                readJson(tree, deserializer)
            }
        }

    protected open fun parametrizedTest(test: (JsonTestingMode) -> Unit) {
        processResults(buildList {
            add(runCatching { test(JsonTestingMode.STREAMING) })
            add(runCatching { test(JsonTestingMode.TREE) })
            if (isJvm()) {
                add(runCatching { test(JsonTestingMode.JAVA_STREAMS) })
            }
        })
    }

    private inner class SwitchableJson(
        val json: Json,
        val jsonTestingMode: JsonTestingMode,
        override val serializersModule: SerializersModule = EmptySerializersModule
    ) : StringFormat {
        override fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String {
            return json.encodeToString(serializer, value, jsonTestingMode)
        }

        override fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String): T {
            return json.decodeFromString(deserializer, string, jsonTestingMode)
        }
    }

    protected fun parametrizedTest(json: Json, test: StringFormat.() -> Unit) {
        val streamingResult = runCatching { SwitchableJson(json, JsonTestingMode.STREAMING).test() }
        val treeResult = runCatching { SwitchableJson(json, JsonTestingMode.TREE).test() }
        processResults(listOf(streamingResult, treeResult))
    }

    protected fun processResults(results: List<Result<*>>) {
        results.forEachIndexed { i, result ->
            result.onFailure {
                println("Failed test for ${JsonTestingMode.value(i)}")
                throw it
            }
        }
        for (i in results.indices) {
            for (j in results.indices) {
                if (i == j) continue
                assertEquals(
                    results[i].getOrNull()!!,
                    results[j].getOrNull()!!,
                    "Results differ for ${JsonTestingMode.value(i)} and ${JsonTestingMode.value(j)}"
                )
            }
        }
    }

    /**
     * Same as [assertStringFormAndRestored], but tests both json converters (streaming and tree)
     * via [parametrizedTest]
     */
    internal fun <T> assertJsonFormAndRestored(
        serializer: KSerializer<T>,
        data: T,
        expected: String,
        json: Json = default
    ) {
        parametrizedTest { jsonTestingMode ->
            val serialized = json.encodeToString(serializer, data, jsonTestingMode)
            assertEquals(expected, serialized, "Failed with streaming = $jsonTestingMode")
            val deserialized: T = json.decodeFromString(serializer, serialized, jsonTestingMode)
            assertEquals(data, deserialized, "Failed with streaming = $jsonTestingMode")
        }
    }
}
