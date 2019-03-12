/*
 * Copyright 2017-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.context.*
import kotlinx.serialization.json.internal.*
import kotlin.test.*

abstract class JsonTestBase {
    protected val strict = Json()
    protected val unquoted = Json(unquoted = true)
    protected val nonStrict = Json(strictMode = false)

    internal inline fun <reified T : Any> Json.stringify(value: T, useStreaming: Boolean): String {
        val serializer = context.getOrDefault(T::class)
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
            stringify(context.getOrDefault(T::class).list, list)
        }
    }

    @ImplicitReflectionSerializer
    inline fun <reified K : Any, reified V : Any> Json.stringify(map: Map<K, V>, useStreaming: Boolean): String {
        return if (useStreaming) {
            // Overload to test public map extension
            stringify(map)
        } else {
            stringify((context.getOrDefault(K::class) to context.getOrDefault(V::class)).map, map)
        }
    }

    internal inline fun <reified T : Any> Json.parse(source: String, useStreaming: Boolean): T {
        val deserializer = context.getOrDefault(T::class)
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
            parse(context.getOrDefault(T::class).list, content, useStreaming)
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
            parse((context.getOrDefault(K::class) to context.getOrDefault(V::class)).map, content, useStreaming)
        }
    }

    protected fun parametrizedTest(test: (Boolean) -> Unit) {
        val streamingResult = kotlin.runCatching { test(true) }
        val treeResult = kotlin.runCatching { test(false) }
        assertEquals(streamingResult, treeResult)
        streamingResult.getOrThrow()
    }
}
