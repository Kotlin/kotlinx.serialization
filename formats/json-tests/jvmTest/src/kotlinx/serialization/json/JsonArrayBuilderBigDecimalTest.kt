/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import java.math.*
import kotlin.test.*

@OptIn(ExperimentalSerializationApi::class)
class JsonArrayBuilderBigDecimalTest : JsonTestBase() {
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
        val json = buildJsonArray { add(value) }
        assertFalse(json[0].jsonPrimitive.isString)
        assertJsonFormAndRestored(JsonArray.serializer(), json, "[$value]")
    }

    @Test
    fun testNullableBigDecimal() {
        val value: BigDecimal? = BigDecimal("1.000000000000000000000000001")
        val absent: BigDecimal? = null
        val json = buildJsonArray {
            add(value)
            add(absent)
            add(null)
        }
        assertSame(JsonNull, json[1])
        assertSame(JsonNull, json[2])
        assertJsonFormAndRestored(JsonArray.serializer(), json, "[$value,null,null]")
    }

    @Test
    fun testReturnValueAndMixedOverloads() {
        val element: JsonElement = JsonPrimitive("element")
        val json = buildJsonArray {
            assertTrue(add(BigDecimal("1.2300")))
            assertTrue(add(null as BigDecimal?))
            assertTrue(add(1))
            assertTrue(add(true))
            assertTrue(add("string"))
            assertTrue(add(element))
        }
        assertJsonFormAndRestored(JsonArray.serializer(), json, "[1.2300,null,1,true,\"string\",\"element\"]")
    }

    @Test
    fun testNestedArrays() {
        val value = BigDecimal("1.000000000000000000000000001")
        val json = buildJsonObject {
            putJsonArray("items") {
                add(value)
                addJsonArray { add(value) }
                addJsonObject { put("number", value) }
            }
        }
        assertJsonFormAndRestored(
            JsonObject.serializer(), json, "{\"items\":[$value,[$value],{\"number\":$value}]}"
        )
    }

    @Test
    fun testNumberTypedBigDecimalUsesExistingOverload() {
        val number: Number = BigDecimal("1.000000000000000000000000001")
        val nullableNumber: Number? = number
        val absent: Number? = null
        val json = buildJsonArray {
            add(number)
            add(nullableNumber)
            add(JsonPrimitive(number))
            add(absent)
        }
        parametrizedTest { mode ->
            assertEquals("[1.0,1.0,1.0,null]", default.encodeToString(JsonArray.serializer(), json, mode), "mode:$mode")
        }
    }

    @Test
    fun testPrettyPrintAndExplicitNulls() {
        val json = buildJsonArray {
            add(BigDecimal("1.2300"))
            add(null as BigDecimal?)
        }
        val format = Json { prettyPrint = true; explicitNulls = false }
        assertJsonFormAndRestored(JsonArray.serializer(), json, "[\n    1.2300,\n    null\n]", format)
    }
}
