/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlin.js.*
import kotlin.test.*

class EncodeToDynamicSpecialCasesTest {

    @Test
    fun testTopLevelInt() = assertDynamicForm(42)

    @Test
    fun testTopLevelString() = assertDynamicForm("42")

    @Test
    fun testTopLevelList() = assertDynamicForm(listOf(1, 2, 3))

    @Test
    fun testStringMap() = assertDynamicForm(mapOf("1" to 2, "3" to 4))

    @Test
    fun testByteMap() = assertDynamicForm(mapOf(1.toByte() to 2, 3.toByte() to 4))

    @Test
    fun testCharMap() = assertDynamicForm(mapOf('1' to 2, '3' to 4))

    @Test
    fun testShortMap() = assertDynamicForm(mapOf(1.toShort() to 2, 3.toShort() to 4))

    @Test
    fun testIntMap() = assertDynamicForm(mapOf(1 to 2, 3 to 4))

    @Test
    fun testLongMap()  = assertDynamicForm(mapOf(1L to 2, 3L to 4))

    @Test
    fun testDoubleMap()  = assertDynamicForm(mapOf(1.0 to 2, 3.0 to 4))

    @Test
    fun testFloatMap()  = assertDynamicForm(mapOf(1.0f to 2, 3.0f to 4))

    @Test
    fun testJsonPrimitive() {
        assertDynamicForm(JsonPrimitive(42))
        assertDynamicForm<JsonElement>(JsonPrimitive(42))
    }

    @Test
    fun testJsonPrimitiveDouble() {
        assertDynamicForm<JsonElement>(JsonPrimitive(42.0))
        assertDynamicForm<JsonPrimitive>(JsonPrimitive(42.0))
    }

    @Test
    fun testParsedJsonNumbersRemainDynamicNumbers() {
        listOf("1.2300", "1.000000000000000000000000001", "18446744073709551617", "1E+400").forEach { source ->
            val parsed = Json.parseToJsonElement(source)
            val actual = Json.encodeToDynamic(JsonElement.serializer(), parsed)
            assertEquals("number", jsTypeOf(actual))
            assertEquals(source.toDouble(), actual as Double)
        }
    }

    @Test
    fun testParsedJsonNumbersRemainDynamicNumbersWhenNested() {
        val parsed = Json.parseToJsonElement("""{"number":1.2300,"items":[1.000000000000000000000000001]}""")
        val actual = Json.encodeToDynamic(JsonElement.serializer(), parsed)
        assertEquals("number", jsTypeOf(actual.number))
        assertEquals(1.23, actual.number as Double)
        assertEquals("number", jsTypeOf(actual.items[0]))
        assertEquals(1.0, actual.items[0] as Double)
    }

    @Test
    fun testParsedJsonNumbersRemainDynamicNumbersAfterTreeEncoding() {
        listOf("42", "42.0", "-0.0", "1.2300", "1.000000000000000000000000001", "18446744073709551617", "1E+400").forEach { source ->
            var tree = Json.parseToJsonElement(source)
            repeat(3) {
                tree = Json.encodeToJsonElement(JsonElement.serializer(), tree)
                assertEquals(source, Json.encodeToString(JsonElement.serializer(), tree))
                val actual = Json.encodeToDynamic(JsonElement.serializer(), tree)
                assertEquals("number", jsTypeOf(actual), "source:$source, iteration:$it")
                assertEquals(source.toDouble(), actual as Double)
            }
        }
    }

    @Test
    fun testParsedJsonNumbersRemainDynamicNumbersWhenNestedAfterTreeEncoding() {
        val source = """{"count":42,"number":1.2300,"items":[1.000000000000000000000000001,false,null,"42"]}"""
        val parsed = Json.parseToJsonElement(source)
        val tree = Json.encodeToJsonElement(JsonElement.serializer(), parsed)
        assertEquals(source, Json.encodeToString(JsonElement.serializer(), tree))
        val actual = Json.encodeToDynamic(JsonElement.serializer(), tree)
        assertEquals("number", jsTypeOf(actual.count))
        assertEquals(42.0, actual.count as Double)
        assertEquals("number", jsTypeOf(actual.number))
        assertEquals(1.23, actual.number as Double)
        assertEquals("number", jsTypeOf(actual.items[0]))
        assertEquals(1.0, actual.items[0] as Double)
        assertEquals(false, actual.items[1] as Boolean)
        assertNull(actual.items[2])
        assertEquals("42", actual.items[3] as String)
    }

    @Test
    fun testJsonPrimitiveDoubleRemainsDynamicNumberAfterTreeEncoding() {
        val tree = Json.encodeToJsonElement(JsonPrimitive.serializer(), JsonPrimitive(42.5))
        val actual = Json.encodeToDynamic(JsonPrimitive.serializer(), tree.jsonPrimitive)
        assertEquals("number", jsTypeOf(actual))
        assertEquals(42.5, actual as Double)
    }

    @Test
    fun testNegativeZeroDynamicBehaviorAfterTreeEncoding() {
        for (source in listOf("-0", "-0.0")) {
            val parsed = Json.parseToJsonElement(source)
            val tree = Json.encodeToJsonElement(JsonElement.serializer(), parsed)
            assertEquals(source, Json.encodeToString(JsonElement.serializer(), tree))
            val actual = Json.encodeToDynamic(JsonElement.serializer(), tree)
            assertEquals("number", jsTypeOf(actual))
            val expectedReciprocal = if (source == "-0.0") Double.NEGATIVE_INFINITY else Double.POSITIVE_INFINITY
            assertEquals(expectedReciprocal, 1.0 / (actual as Double))
        }
    }

    @Test
    fun testNumericDynamicMapKeyRestrictionsAfterTreeEncoding() {
        val serializer = MapSerializer(JsonPrimitive.serializer(), String.serializer())
        for (source in listOf("1.2300", "1E+400", "9007199254740992")) {
            val key = Json.parseToJsonElement(source).jsonPrimitive
            val treeKey = Json.encodeToJsonElement(JsonPrimitive.serializer(), key).jsonPrimitive
            assertFailsWith<IllegalArgumentException>(source) {
                Json.encodeToDynamic(serializer, mapOf(treeKey to "value"))
            }
        }
    }

    @Test
    fun testUnquotedLiteralRetainsDynamicStringAfterTreeEncoding() {
        val literal = JsonUnquotedLiteral("1.2300")
        val tree = Json.encodeToJsonElement(JsonPrimitive.serializer(), literal)
        val actual = Json.encodeToDynamic(JsonPrimitive.serializer(), tree.jsonPrimitive)
        assertEquals("string", jsTypeOf(actual))
        assertEquals("1.2300", actual as String)
    }

    @Test
    fun testJsonStringPrimitive() {
        assertDynamicForm<JsonElement>(JsonPrimitive("42"))
        assertDynamicForm<JsonPrimitive>(JsonPrimitive("42"))
    }

    @Test
    fun testJsonArray() {
        assertDynamicForm<JsonElement>(JsonArray((1..3).map(::JsonPrimitive)))
        assertDynamicForm<JsonArray>(JsonArray((1..3).map(::JsonPrimitive)))
    }

    @Test
    fun testJsonObject() {
        assertDynamicForm<JsonElement>(
            JsonObject(mapOf("1" to JsonPrimitive(2), "3" to JsonPrimitive(4)))
        )
        assertDynamicForm<JsonObject>(
            JsonObject(mapOf("1" to JsonPrimitive(2), "3" to JsonPrimitive(4)))
        )
    }


    @Serializable
    data class Wrapper(val e: JsonElement, val p: JsonPrimitive, val o: JsonObject, val a: JsonArray)

    @Test
    fun testJsonElementWrapper() {
        assertDynamicForm(Wrapper(JsonPrimitive(42), JsonPrimitive("239"), buildJsonObject { put("k", "v") }, JsonArray((1..3).map(::JsonPrimitive))))
    }
}
