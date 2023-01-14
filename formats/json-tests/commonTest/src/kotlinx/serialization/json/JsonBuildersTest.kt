/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlin.test.*

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
        assertEquals("""{"object":{"k":"v"},"array":[{"nestedLiteral":true}],"null":null,"primitive":42,"boolean":true,"literal":"foo","null2":null}""", json.toString())
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
        assertEquals(
            """[1,2,3,4,5,null,1,2,3,4,5,null]""",
            buildJsonArray {
                assertTrue { addAll(1) }
                assertTrue { addAll(2, 3, 4, 5, null) }
                assertTrue { addAll(listOf(1, 2, 3, 4, 5, null)) }
            }.toString()
        )

        assertEquals(
            """["a","b","c",null,"a","b","c",null]""",
            buildJsonArray {
                assertTrue { addAll("a") }
                assertTrue { addAll("b", "c", null) }
                assertTrue { addAll(listOf("a", "b", "c", null)) }
            }.toString()
        )

        assertEquals(
            """[true,true,true,null,false,false,false,null]""",
            buildJsonArray {
                assertTrue { addAll(true) }
                assertTrue { addAll(true, true, null) }
                assertTrue { addAll(listOf(false, false, false, null)) }
            }.toString()
        )

        assertEquals(
            """[1,"a",false,null,2,"b",true,null]""",
            buildJsonArray {
                assertTrue {
                    addAll(
                        JsonPrimitive(1),
                        JsonPrimitive("a"),
                        JsonPrimitive(false),
                        JsonNull,
                    )
                }
                assertTrue {
                    addAll(
                        listOf(
                            JsonPrimitive(2),
                            JsonPrimitive("b"),
                            JsonPrimitive(true),
                            JsonNull
                        )
                    )
                }
            }.toString()
        )

        assertEquals(
            """[{},{},{},null,{},{},{},null]""",
            buildJsonArray {
                assertTrue {
                    addAll(
                        JsonObject(emptyMap()),
                        JsonObject(emptyMap()),
                        JsonObject(emptyMap()),
                        JsonNull,
                    )
                }
                assertTrue {
                    addAll(
                        listOf(
                            JsonObject(emptyMap()),
                            JsonObject(emptyMap()),
                            JsonObject(emptyMap()),
                            JsonNull
                        )
                    )
                }
            }.toString()
        )

        assertEquals(
            """[[],[],[],null,[],[],[],null]""",
            buildJsonArray {
                assertTrue {
                    addAll(
                        JsonArray(emptyList()),
                        JsonArray(emptyList()),
                        JsonArray(emptyList()),
                        JsonNull,
                    )
                }
                assertTrue {
                    addAll(
                        listOf(
                            JsonArray(emptyList()),
                            JsonArray(emptyList()),
                            JsonArray(emptyList()),
                            JsonNull
                        )
                    )
                }
            }.toString()
        )

        assertEquals(
            """[null,null,null,null]""",
            buildJsonArray {
                assertTrue {
                    addAll(JsonNull, JsonNull)
                }
                assertTrue {
                    addAll(listOf(JsonNull, JsonNull))
                }
            }.toString()
        )
    }
}
