/*
 * Copyright 2019 JetBrains s.r.o.
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

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.modules.*
import kotlinx.serialization.json.internal.*
import kotlin.test.*

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
        assertEquals(streamingResult, treeResult)
        streamingResult.getOrThrow()
    }
}
