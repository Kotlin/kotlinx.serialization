/*
 * Copyright 2017-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.context.*
import kotlinx.serialization.json.internal.*
import kotlinx.serialization.json.serializers.*
import kotlin.test.*

abstract class JsonTestBase {
    protected val strict = Json()
    protected val unquoted = Json(unquoted = true)
    protected val indented = Json(indented = true)
    protected val nonstrict = Json(strictMode = false)

    internal inline fun <reified T : Any> Json.stringify(value: T, useStreaming: Boolean): String {
        val serializer = context.getOrDefault(T::class)
        return stringify(serializer, value, useStreaming)
    }

    internal fun <T> Json.stringify(serializer: SerializationStrategy<T>, value: T, useStreaming: Boolean): String {
        return if (useStreaming) {
            stringify(serializer, value)
        } else {
            val tree = writeJson(value, serializer)
            // TODO ask Leonid about discoverability
            stringify(JsonElementSerializer, tree)
        }
    }

    internal inline fun <reified T : Any> Json.parse(source: String, useStreaming: Boolean): T {
        val deserializer = context.getOrDefault(T::class)
        return parse(deserializer, source, useStreaming)
    }

    internal fun <T : Any> Json.parse(deserializer: DeserializationStrategy<T>, source: String, useStreaming: Boolean): T {
        return if (useStreaming) {
            parse(deserializer, source)
        } else {
            val parser = JsonReader(source)
            val input = StreamingJsonInput(this, WriteMode.OBJ, parser)
            val tree = input.readTree()
            readJson(tree, deserializer)
        }
    }

    protected fun parametrizedTest(test: (Boolean) -> Unit) {
        val streamingResult = kotlin.runCatching { test(true) }
        val treeResult = kotlin.runCatching { test(false) }
        assertEquals(streamingResult, treeResult)
        streamingResult.getOrThrow()
    }
}
