/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlin.test.Test
import kotlin.test.assertEquals

class JsonBuildersTest {

    @Test
    fun testBuildJson() {
        val json = buildJsonObject {
            putJsonObject("object") {
                put("k", JsonPrimitive("v"))
            }

            putJsonArray("array") {
                addJsonObject { put("nestedLiteral", true) }
            }

            val number: Number? = null
            put("null", number)
            put("primitive", JsonPrimitive(42))
            put("boolean", true)
            put("literal", "foo")
            put("null2", null)
        }
        assertEquals(
            """{"object":{"k":"v"},"array":[{"nestedLiteral":true}],"null":null,"primitive":42,"boolean":true,"literal":"foo","null2":null}""",
            json.toString()
        )
    }

    @Test
    fun testBuildJsonArray() {
        val json = buildJsonArray {
            add(true)
            addJsonArray {
                for (i in 1..10) add(i)
            }
            add(null)
            addJsonObject {
                put("stringKey", "stringValue")
            }
        }
        assertEquals("""[true,[1,2,3,4,5,6,7,8,9,10],null,{"stringKey":"stringValue"}]""", json.toString())
    }

    @Test
    fun testBuildJsonArrayAddAll() {
        val jsonNumbers = buildJsonArray {
            addAll(1)
            addAll(2, 3, 4, 5, null)
            addAll(listOf(1, 2, 3, 4, 5, null))
        }
        assertEquals("""[1,2,3,4,5,null,1,2,3,4,5,null]""", jsonNumbers.toString())

        val jsonStrings = buildJsonArray {
            addAll("a")
            addAll("b", "c", null)
            addAll(listOf("a", "b", "c", null))
        }
        assertEquals("""["a","b","c",null,"a","b","c",null]""", jsonStrings.toString())

        val jsonBooleans = buildJsonArray {
            addAll(true)
            addAll(true, true, null)
            addAll(listOf(false, false, false, null))
        }
        assertEquals("""[true,true,true,null,false,false,false,null]""", jsonBooleans.toString())

        val jsonPrimitiveElements = buildJsonArray {
            addAll(JsonPrimitive(1), JsonPrimitive("a"), JsonPrimitive(false), JsonNull)
            addAll(listOf(JsonPrimitive(2), JsonPrimitive("b"), JsonPrimitive(true), JsonNull))
        }
        assertEquals("""[1,"a",false,null,2,"b",true,null]""", jsonPrimitiveElements.toString())

        val jsonObjectElements = buildJsonArray {
            addAll(JsonObject(emptyMap()), JsonObject(emptyMap()), JsonObject(emptyMap()), JsonNull)
            addAll(listOf(JsonObject(emptyMap()), JsonObject(emptyMap()), JsonObject(emptyMap()), JsonNull))
        }
        assertEquals("""[{},{},{},null,{},{},{},null]""", jsonObjectElements.toString())

        val jsonArrayElements = buildJsonArray {
            addAll(JsonArray(emptyList()), JsonArray(emptyList()), JsonArray(emptyList()), JsonNull)
            addAll(listOf(JsonArray(emptyList()), JsonArray(emptyList()), JsonArray(emptyList()), JsonNull))
        }
        assertEquals("""[[],[],[],null,[],[],[],null]""", jsonArrayElements.toString())
    }
}
