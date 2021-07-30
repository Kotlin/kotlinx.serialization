/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.json.internal.*
import kotlinx.serialization.modules.*
import kotlinx.serialization.test.*
import kotlin.test.*

abstract class JsonTestBase {
    protected val default = Json { encodeDefaults = true }
    protected val lenient = Json { isLenient = true; ignoreUnknownKeys = true; allowSpecialFloatingPointValues = true }

    internal inline fun <reified T : Any> Json.encodeToString(value: T, useStreaming: Boolean): String {
        val serializer = serializersModule.serializer<T>()
        return encodeToString(serializer, value, useStreaming)
    }

    internal fun <T> Json.encodeToString(serializer: SerializationStrategy<T>, value: T, useStreaming: Boolean): String {
        return if (useStreaming) {
            encodeToString(serializer, value)
        } else {
            val tree = writeJson(value, serializer)
            encodeToString(tree)
        }
    }

    internal inline fun <reified T : Any> Json.decodeFromString(source: String, useStreaming: Boolean): T {
        val deserializer = serializersModule.serializer<T>()
        return decodeFromString(deserializer, source, useStreaming)
    }

    internal fun <T> Json.decodeFromString(deserializer: DeserializationStrategy<T>, source: String, useStreaming: Boolean): T {
        return if (useStreaming) {
            decodeFromString(deserializer, source)
        } else {
            val lexer = JsonLexer(source)
            val input = StreamingJsonDecoder(this, WriteMode.OBJ, lexer, deserializer.descriptor)
            val tree = input.decodeJsonElement()
            lexer.expectEof()
            readJson(tree, deserializer)
        }
    }

    protected open fun parametrizedTest(test: (Boolean) -> Unit) {
        val streamingResult = runCatching { test(true) }
        val treeResult = runCatching { test(false) }
        processResults(streamingResult, treeResult)
    }

    private inner class SwitchableJson(
        val json: Json,
        val useStreaming: Boolean,
        override val serializersModule: SerializersModule = EmptySerializersModule
    ) : StringFormat {
        override fun <T> encodeToString(serializer: SerializationStrategy<T>, value: T): String {
            return json.encodeToString(serializer, value, useStreaming)
        }

        override fun <T> decodeFromString(deserializer: DeserializationStrategy<T>, string: String): T {
            return json.decodeFromString(deserializer, string, useStreaming)
        }
    }

    protected fun parametrizedTest(json: Json, test: StringFormat.() -> Unit) {
        val streamingResult = runCatching { SwitchableJson(json, true).test() }
        val treeResult = runCatching { SwitchableJson(json, false).test() }
        processResults(streamingResult, treeResult)
    }

    protected fun processResults(streamingResult: Result<*>, treeResult: Result<*>) {
        val results = listOf(streamingResult, treeResult)
        results.forEachIndexed { _, result ->
            result.onFailure { throw it }
        }
        assertEquals(streamingResult.getOrNull()!!, treeResult.getOrNull()!!)
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
        parametrizedTest { useStreaming ->
            val serialized = json.encodeToString(serializer, data, useStreaming)
            assertEquals(expected, serialized, "Failed with streaming = $useStreaming")
            val deserialized: T = json.decodeFromString(serializer, serialized, useStreaming)
            assertEquals(data, deserialized, "Failed with streaming = $useStreaming")
        }
    }
}
