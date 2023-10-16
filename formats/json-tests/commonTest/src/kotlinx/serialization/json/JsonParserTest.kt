/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.internal.*
import kotlinx.serialization.test.*
import kotlin.test.*

class JsonParserTest : JsonTestBase() {

    @Test
    fun testQuotedBrace() {
        val tree = parse("""{"x": "{"}""")
        assertTrue("x" in tree)
        assertEquals("{", (tree.getValue("x") as JsonPrimitive).content)
    }

    private fun parse(input: String) = default.parseToJsonElement(input).jsonObject

    @Test
    fun testEmptyKey() {
        val tree = parse("""{"":"","":""}""")
        assertTrue("" in tree)
        assertEquals("", (tree.getValue("") as JsonPrimitive).content)
    }

    @Test
    fun testEmptyValue() {
        assertFailsWithSerial("JsonDecodingException") {
            parse("""{"X": "foo", "Y"}""")
        }
    }

    @Test
    fun testIncorrectUnicodeEscape() {
        assertFailsWithSerial("JsonDecodingException") {
            parse("""{"X": "\uDD1H"}""")
        }
    }

    @Test
    fun testParseEscapedSymbols() {
        assertEquals(
            StringData("https://t.co/M1uhwigsMT"),
            default.decodeFromString(StringData.serializer(), """{"data":"https:\/\/t.co\/M1uhwigsMT"}""")
        )
        assertEquals(StringData("\"test\""), default.decodeFromString(StringData.serializer(), """{"data": "\"test\""}"""))
        assertEquals(StringData("\u00c9"), default.decodeFromString(StringData.serializer(), """{"data": "\u00c9"}"""))
        assertEquals(StringData("""\\"""), default.decodeFromString(StringData.serializer(), """{"data": "\\\\"}"""))
    }

    @Test
    fun testWorkWithNonAsciiSymbols() {
        assertStringFormAndRestored(
            """{"data":"Русские Буквы 🤔"}""",
            StringData("Русские Буквы \uD83E\uDD14"),
            StringData.serializer()
        )
    }

    @Test
    fun testUnicodeEscapes() {
        val data = buildString {
            append(1.toChar())
            append(".")
            append(0x20.toChar())
            append(".")
            append("\n")
        }

        assertJsonFormAndRestored(String.serializer(), data, "\"\\u0001. .\\n\"")
    }

    @Test
    fun testTrailingComma() {
        testTrailingComma("{\"id\":0,}")
        testTrailingComma("{\"id\":0  ,}")
        testTrailingComma("{\"id\":0  , ,}")
    }

    private fun testTrailingComma(content: String) {
        assertFailsWithSerialMessage("JsonDecodingException", "Trailing comma before the end of JSON object") {  Json.parseToJsonElement(content) }
    }

    @Test
    fun testUnclosedStringLiteral() {
        assertFailsWithSerial("JsonDecodingException") {
            parse("\"")
        }

        assertFailsWithSerial("JsonDecodingException") {
            parse("""{"id":"""")
        }
    }

    @Test
    fun testNullValue() {
        val obj = Json.parseToJsonElement("""{"k":null}""").jsonObject
        val value = obj["k"]!!
        assertTrue { value is JsonNull }
        assertFalse { value.jsonPrimitive.isString }
    }

    @Test
    fun testNullStringValue() {
        val obj = Json.parseToJsonElement("""{"k":"null"}""").jsonObject
        val value = obj["k"]!!
        assertFalse { value is JsonNull }
        assertTrue { value.jsonPrimitive.isString }
        assertEquals("null", obj["k"]!!.jsonPrimitive.content)
    }
}
