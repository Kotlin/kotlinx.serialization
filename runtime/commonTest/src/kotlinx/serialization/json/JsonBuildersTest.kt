/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlin.test.*

class JsonBuildersTest {

    @Test
    fun testBuildJson() {
        val json = buildJson {
            addJson("object") {
                add("k", JsonPrimitive("v"))
            }

            addArray("array") {
                addJson { add("nestedLiteral", true) }
            }

            val number: Number? = null
            add("null", number)
            add("primitive", JsonPrimitive(42))
            add("boolean", true)
            add("literal", "foo")
        }
        assertEquals("""{"object":{"k":"v"},"array":[{"nestedLiteral":true}],"null":null,"primitive":42,"boolean":true,"literal":"foo"}""", json.toString())
    }

    @Test
    fun testBuildJsonArray() {
        val json = buildJsonArray {
            add(true)
            addArray {
                for (i in 1..10) add(i)
            }
            addJson {
                add("stringKey", "stringValue")
            }
        }
        assertEquals("""[true,[1,2,3,4,5,6,7,8,9,10],{"stringKey":"stringValue"}]""", json.toString())
    }
}
