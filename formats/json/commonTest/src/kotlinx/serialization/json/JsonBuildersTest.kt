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
}
