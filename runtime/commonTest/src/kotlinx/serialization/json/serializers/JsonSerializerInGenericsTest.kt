/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.serializers

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.test.assertStringFormAndRestored
import kotlin.test.*

class JsonSerializerInGenericsTest : JsonTestBase() {

    @Serializable
    data class NonTrivialClass(
        val list: List<JsonElement?>,
        val nullableNull: JsonNull?,
        val nestedMap: Map<String, Map<String, JsonElement?>>
    )

    private val expected = "{\"list\":[42,[{\"key\":\"value\"}],null],\"nullableNull\":null,\"nestedMap\":{\"key1\":{\"nested\":{\"first\":\"second\"},\"nullable\":null}}}"

    @Test
    fun testGenericsWithNulls() = parametrizedTest(default) {
        assertStringFormAndRestored(expected, create(), NonTrivialClass.serializer())
    }

    private fun create(): NonTrivialClass {
        return NonTrivialClass(
            arrayListOf(JsonPrimitive(42), jsonArray { +json { "key" to "value" } }, null),
            null,
            mapOf("key1" to mapOf("nested" to json {
                "first" to "second"
            }, "nullable" to null))
        )
    }
}
