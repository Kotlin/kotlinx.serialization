/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.json.internal.*
import kotlinx.serialization.modules.*
import kotlin.test.assertEquals

abstract class JsonTestBase {
    protected val strict = Json()
    protected val unquoted = Json(unquoted = true)
    protected val nonStrict = Json(strictMode = false)

    @ImplicitReflectionSerializer
    internal inline fun <reified T : Any> Json.stringify(value: T, useStreaming: Boolean): String {
        val serializer = context.getContextualOrDefault(T::class)
        return stringify(serializer, value, useStreaming)
    }

    internal fun <T> Json.stringify(serializer: SerializationStrategy<T>, value: T, useStreaming: Boolean): String {
        return if (useStreaming) {
            stringify(serializer, value)
        } else {
            val tree = writeJson(value, serializer)
            // kotlinx.serialization/issues/277
            stringify(JsonElementSerializer, tree)
        }
    }

    @ImplicitReflectionSerializer
    inline fun <reified T : Any> Json.stringify(list: List<T>, useStreaming: Boolean): String {
        return if (useStreaming) {
            // Overload to test public list extension
            stringify(list)
        } else {
            stringify(context.getContextualOrDefault(T::class).list, list)
        }
    }

    @ImplicitReflectionSerializer
    inline fun <reified K : Any, reified V : Any> Json.stringify(map: Map<K, V>, useStreaming: Boolean): String {
        return if (useStreaming) {
            // Overload to test public map extension
            stringify(map)
        } else {
            stringify((context.getContextualOrDefault(K::class) to context.getContextualOrDefault(V::class)).map, map)
        }
    }

    @ImplicitReflectionSerializer
    internal inline fun <reified T : Any> Json.parse(source: String, useStreaming: Boolean): T {
        val deserializer = context.getContextualOrDefault(T::class)
        return parse(deserializer, source, useStreaming)
    }

    internal fun <T> Json.parse(deserializer: DeserializationStrategy<T>, source: String, useStreaming: Boolean): T {
        return if (useStreaming) {
            parse(deserializer, source)
        } else {
            val parser = JsonReader(source)
            val input = StreamingJsonInput(this, WriteMode.OBJ, parser)
            val tree = input.decodeJson()
            readJson(tree, deserializer)
        }
    }

    @ImplicitReflectionSerializer
    internal inline fun <reified T : Any> Json.parseList(content: String, useStreaming: Boolean): List<T> {
        return if (useStreaming) {
            // Overload to test public list extension
            parseList(content)
        } else {
            parse(context.getContextualOrDefault(T::class).list, content, useStreaming)
        }
    }

    @ImplicitReflectionSerializer
    internal inline fun <reified K : Any, reified V : Any> Json.parseMap(
        content: String,
        useStreaming: Boolean
    ): Map<K, V> {
        return if (useStreaming) {
            // Overload to test public map extension
            parseMap(content)
        } else {
            parse((context.getContextualOrDefault(K::class) to context.getContextualOrDefault(V::class)).map, content, useStreaming)
        }
    }

    protected fun parametrizedTest(test: (Boolean) -> Unit) {
        val streamingResult = kotlin.runCatching { test(true) }
        val treeResult = kotlin.runCatching { test(false) }
        processResults(streamingResult, treeResult)
    }

    private inner class DualFormat(
        val json: Json,
        val useStreaming: Boolean,
        override val context: SerialModule = EmptyModule
    ) : StringFormat {
        override fun <T> stringify(serializer: SerializationStrategy<T>, obj: T): String {
            return json.stringify(serializer, obj, useStreaming)
        }

        override fun <T> parse(deserializer: DeserializationStrategy<T>, string: String): T {
            return json.parse(deserializer, string, useStreaming)
        }
    }

    protected fun parametrizedTest(json: Json, test: StringFormat.(StringFormat) -> Unit) {
        val streamingResult = kotlin.runCatching { json.test(DualFormat(json, true)) }
        val treeResult = kotlin.runCatching { json.test(DualFormat(json, false)) }
        processResults(streamingResult, treeResult)
    }

    private fun processResults(streamingResult: Result<*>, treeResult: Result<*>) {
        val results = listOf(streamingResult, treeResult)
        results.forEachIndexed { index, result ->
            if (result.isFailure)
                throw Exception("Failed ${if (index == 0) "streaming" else "tree"} test", result.exceptionOrNull()!!)
        }
        assertEquals(streamingResult.getOrNull()!!, treeResult.getOrNull()!!)
    }

    internal inline fun <reified T: Any> parametrizedTest(data: T, expected: String, json: Json = unquoted) {
        parametrizedTest { useStreaming ->
            val serialized = json.stringify(data, useStreaming)
            assertEquals(expected, serialized)
            val deserialized: T = json.parse(serialized, useStreaming)
            assertEquals(data, deserialized)
        }
    }
}
