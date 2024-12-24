/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json


import kotlinx.serialization.*
import kotlinx.serialization.test.*
import kotlin.test.*

class MissingCommaTest : JsonTestBase() {
    @Serializable
    class Holder(
        val i: Int,
        val c: Child,
    )

    @Serializable
    class Child(
        val i: String
    )

    private val withTrailingComma = Json { allowTrailingComma = true }

    @Test
    fun missingCommaBetweenFieldsAfterPrimitive() {
        val message =
            "Unexpected JSON token at offset 8: Expected comma after the key-value pair at path: \$.i"
        val json = """{"i":42 "c":{"i":"string"}}"""

        assertFailsWithSerialMessage("JsonDecodingException", message) {
            default.decodeFromString<Holder>(json)
        }
    }

    @Test
    fun missingCommaBetweenFieldsAfterObject() {
        val message =
            "Unexpected JSON token at offset 19: Expected comma after the key-value pair at path: \$.c"
        val json = """{"c":{"i":"string"}"i":42}"""

        assertFailsWithSerialMessage("JsonDecodingException", message) {
            default.decodeFromString<Holder>(json)
        }
    }

    @Test
    fun allowTrailingCommaDoesNotApplyToCommaBetweenFields() {
        val message =
            "Unexpected JSON token at offset 8: Expected comma after the key-value pair at path: \$.i"
        val json = """{"i":42 "c":{"i":"string"}}"""

        assertFailsWithSerialMessage("JsonDecodingException", message) {
            withTrailingComma.decodeFromString<Holder>(json)
        }
    }

    @Test
    fun lenientSerializeDoesNotAllowToSkipCommaBetweenFields() {
        val message =
            "Unexpected JSON token at offset 8: Expected comma after the key-value pair at path: \$.i"
        val json = """{"i":42 "c":{"i":"string"}}"""

        assertFailsWithSerialMessage("JsonDecodingException", message) {
            lenient.decodeFromString<Holder>(json)
        }
    }

    @Test
    fun missingCommaInDynamicMap() {
        val m = "Unexpected JSON token at offset 9: Expected end of the object or comma at path: \$"
        val json = """{"i":42 "c":{"i":"string"}}"""
        assertFailsWithSerialMessage("JsonDecodingException", m) {
            default.parseToJsonElement(json)
        }
    }

    @Test
    fun missingCommaInArray() {
        val m = "Unexpected JSON token at offset 3: Expected end of the array or comma at path: \$[0]"
        val json = """[1 2 3 4]"""

        assertFailsWithSerialMessage("JsonDecodingException", m) {
            default.decodeFromString<List<Int>>(json)
        }
    }

    @Test
    fun missingCommaInStringMap() {
        val m = "Unexpected JSON token at offset 9: Expected comma after the key-value pair at path: \$['a']"
        val json = """{"a":"1" "b":"2"}"""

        assertFailsWithSerialMessage("JsonDecodingException", m) {
            default.decodeFromString<Map<String, String>>(json)
        }
    }
}