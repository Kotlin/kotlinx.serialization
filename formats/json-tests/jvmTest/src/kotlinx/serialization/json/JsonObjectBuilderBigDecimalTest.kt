/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import java.math.*
import kotlin.test.*

@OptIn(ExperimentalSerializationApi::class)
class JsonObjectBuilderBigDecimalTest : JsonTestBase() {
    @Test
    fun testPrecision() {
        checkNumber("1.000000000000000000000000001")
        checkNumber("-3.141592653589793238462643383279")
        checkNumber("18446744073709551617")
    }

    @Test
    fun testExponentAndScale() {
        listOf("1E+400", "1E-400", "-1E+400", "0", "0.0000", "1.2300", "1E+3").forEach {
            checkNumber(it)
        }
    }

    private fun checkNumber(source: String) {
        val value = BigDecimal(source)
        val json = buildJsonObject { put("number", value) }
        assertFalse(json.getValue("number").jsonPrimitive.isString)
        assertJsonFormAndRestored(JsonObject.serializer(), json, "{\"number\":$value}")
    }

    @Test
    fun testNullableBigDecimal() {
        val value: BigDecimal? = BigDecimal("1.000000000000000000000000001")
        val absent: BigDecimal? = null
        val json = buildJsonObject {
            put("number", value)
            put("absent", absent)
            put("literalNull", null)
        }
        assertJsonFormAndRestored(
            JsonObject.serializer(), json,
            "{\"number\":$value,\"absent\":null,\"literalNull\":null}"
        )
    }

    @Test
    fun testPreviousValue() {
        val json = buildJsonObject {
            assertNull(put("number", BigDecimal("1.2300")))
            assertEquals(JsonUnquotedLiteral("1.2300"), put("number", BigDecimal.TEN))
            assertEquals(JsonUnquotedLiteral("10"), put("number", null as BigDecimal?))
            assertSame(JsonNull, put("number", BigDecimal.ONE))
        }
        assertEquals(JsonUnquotedLiteral("1"), json["number"])
    }

    @Test
    fun testNestedObject() {
        val value = BigDecimal("1.000000000000000000000000001")
        val json = buildJsonObject {
            putJsonArray("items") {
                addJsonObject { put("number", value) }
            }
        }
        assertJsonFormAndRestored(JsonObject.serializer(), json, "{\"items\":[{\"number\":$value}]}")
    }

    @Test
    fun testOtherOverloads() {
        val number: Number = BigDecimal("1.000000000000000000000000001")
        val json = buildJsonObject {
            put("int", 1)
            put("long", Long.MAX_VALUE)
            put("double", 1.5)
            put("boolean", true)
            put("string", "1.2300")
            put("number", number)
            put("primitive", JsonPrimitive(number))
        }
        parametrizedTest { mode ->
            assertEquals(
                "{\"int\":1,\"long\":9223372036854775807,\"double\":1.5,\"boolean\":true," +
                    "\"string\":\"1.2300\",\"number\":1.0,\"primitive\":1.0}",
                default.encodeToString(JsonObject.serializer(), json, mode), "mode:$mode"
            )
        }
    }

    @Test
    fun testSpecialFloatingPointValidationUnchanged() {
        val json = buildJsonObject { put("number", Double.NaN) }
        assertFailsWith<SerializationException> { default.encodeToString(JsonObject.serializer(), json) }
        assertEquals("{\"number\":NaN}", lenient.encodeToString(JsonObject.serializer(), json))
    }

    @Test
    fun testPrettyPrintAndExplicitNulls() {
        val value = BigDecimal("1.2300")
        val json = buildJsonObject {
            put("number", value)
            put("absent", null as BigDecimal?)
        }
        val format = Json { prettyPrint = true; explicitNulls = false }
        assertJsonFormAndRestored(
            JsonObject.serializer(), json,
            "{\n    \"number\": 1.2300,\n    \"absent\": null\n}", format
        )
    }
}
