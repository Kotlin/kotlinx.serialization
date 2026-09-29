/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecodingException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonTestBase
import kotlinx.serialization.json.JsonTestingMode
import kotlinx.serialization.test.assertFailsWithMessage
import kotlinx.serialization.test.checkDecodingException
import kotlin.test.Test

class NestingJsonTest: JsonTestBase() {
    val defaultMsg = "Json input is too nested and may be impossible to parse without stack overflow."
    val defaultHint = "To adjust it, use 'maxNestingDepth' in 'Json {}' builder."

    @Serializable
    data class Rec(val child: Rec? = null, val listChild: List<Rec?> = emptyList())

    @Test
    fun testDeepObject() = parametrizedTest { mode ->
            val deepObject = buildString { append("""{"child":"""); repeat(50_000) { append("""{"child":""") }; append("null"); repeat(50_001) { append("}") } }
            checkDecodingException(mode, {
                default.decodeFromString(Rec.serializer(), deepObject, mode)
            }) {
                message(defaultMsg)
                hint(defaultHint)
            }
        }

    @Test
    fun testDeepObjectMixedWithArray() = parametrizedTest { mode ->
        val deepArray = buildString { append("""{"listChild": ["""); repeat(50_000) { append("""{"listChild": [""") }; append("null"); repeat(50_001) { append("]}") } }
        checkDecodingException(mode, {
            default.decodeFromString(Rec.serializer(), deepArray, mode)
        }) {
            message(defaultMsg)
            hint(defaultHint)
        }
    }

    @Test
    fun testDeepArray() {
        // It is impossible to represent [[[...]]] as @Serializable class, so we need only to test parseToJsonElement()
        val deepArray = buildString { append("""{"listChild": ["""); repeat(50_000) { append("""[""") }; append("null"); repeat(50_001) { append("]") } }
        checkDecodingException(JsonTestingMode.TREE, { default.parseToJsonElement(deepArray) }) {
            message(defaultMsg)
            hint(defaultHint)
        }
    }

    val defaultLim = default.configuration.maxNestingDepth

    @Test
    fun defaultLimitObject() = parametrizedTest { mode ->
        val defaultLimObject = buildString { repeat(defaultLim) { append("""{"child":""") }; append("null"); repeat(defaultLim) { append("}") } }
        val _ = default.decodeFromString(Rec.serializer(), defaultLimObject, mode) // should not throw
        val defaultPlus1 = buildString {repeat(defaultLim + 1) { append("""{"child":""") }; append("null"); repeat(defaultLim + 1) { append("}") } }
        assertFailsWithMessage<JsonDecodingException>(defaultMsg) { default.decodeFromString(Rec.serializer(), defaultPlus1, mode) }
        val increased = Json(default) { maxNestingDepth = defaultLim + 1 }
        val _ = increased.decodeFromString(Rec.serializer(), defaultPlus1, mode)
    }

    @Serializable
    data class RecWithElement(val child: RecWithElement? = null, val element: JsonElement? = null)

    @Test
    fun testWithElement() = parametrizedTest { mode ->
        if (mode == JsonTestingMode.TREE) return@parametrizedTest // we need to test Streaming->Tree parser switch
        val halfLim = defaultLim / 2
        val deepObj = buildString { repeat(halfLim) { append("""{"child":""") }; repeat(halfLim + 1) { append("""{"element":""") }; append("null"); repeat(defaultLim + 1) { append("}") } }
        checkDecodingException(mode, {
            default.decodeFromString(RecWithElement.serializer(), deepObj, mode)
        }) {
            message(defaultMsg)
            hint(defaultHint)
        }
    }

    @Test
    fun veryLowLimit() = parametrizedTest { mode ->
        val lowLimit = Json(default) { maxNestingDepth = 1 }
        // 0 is top-level primitive, 1 is object without nested objects
        val twoLevelsOfNesting = """{"child":{"child":null}}"""
        assertFailsWithMessage<JsonDecodingException>(defaultMsg) { lowLimit.decodeFromString<Rec>(twoLevelsOfNesting, mode) }
        val _ = lowLimit.decodeFromString<Rec>("""{"child":null}""", mode)
    }
}