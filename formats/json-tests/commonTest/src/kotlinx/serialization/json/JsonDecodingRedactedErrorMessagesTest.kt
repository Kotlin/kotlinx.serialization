/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.Box
import kotlinx.serialization.StringData
import kotlinx.serialization.serializer
import kotlinx.serialization.test.checkDecodingException
import kotlin.test.Test

class JsonDecodingRedactedErrorMessagesTest : JsonTestBase() {
    val redacted = Json(default) { exceptionsWithDebugInfo = false }

    @Test
    fun testPrimitiveInsteadOfObjectOrList() = parametrizedTest { mode ->
        val input = """{"boxed": 42}"""
        checkDecodingException(mode, {
            redacted.decodeFromString(Box.serializer(StringData.serializer()), input, mode)
        }, {
            message(
                "Expected start of the object '{', but had '4' instead",
                "Expected JsonObject, but had JsonLiteral as the serialized body of kotlinx.serialization.StringData",
            )
            offset(10)
            path("$.boxed")
            noInput()
        })
    }

    @Test
    fun testMapPath() = parametrizedTest { mode ->
        val input = """{"boxed": {"x": "y"}}"""
        checkDecodingException(mode, {
            redacted.decodeFromString(serializer<Box<Map<String, Int>>>(), input, mode)
        }, {
            message(
                "Unexpected symbol 'y' in numeric literal",
                "Failed to parse literal '\"y\"' as an int value",
            )
            offset(17)
            if (mode != JsonTestingMode.TREE) path("$.boxed[<debug info disabled>]") else path("$.x") // #3170
            noInput()
        })
    }
}
