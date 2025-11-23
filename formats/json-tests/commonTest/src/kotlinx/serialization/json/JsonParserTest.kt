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

    // ==================================================================================
    // Unicode Escape Sequence Tests
    // These tests verify correct parsing of \uXXXX sequences per RFC 8259 Section 7.
    // The JSON spec mandates exactly 4 hexadecimal digits after \u.
    // ==================================================================================

    @Test
    fun testUnicodeEscapeWithFollowingHex() {
        // Verifies fix for greedy parsing bug where hex characters following a valid
        // 4-digit unicode escape were incorrectly consumed as part of the escape sequence.
        val input = "\"\\u00f3a\""
        val decoded = Json.decodeFromString<String>(input)
        assertEquals("óa", decoded, "Should parse 'ó' (U+00F3) followed by literal 'a'")
    }

    @Test
    fun testUnicodeEscapeFollowedByVariousHexChars() {
        // Ensures all hex characters [0-9a-fA-F] following a unicode escape are treated as literals.
        assertEquals("ó0", Json.decodeFromString<String>("\"\\u00f30\""))
        assertEquals("ó9", Json.decodeFromString<String>("\"\\u00f39\""))
        assertEquals("óa", Json.decodeFromString<String>("\"\\u00f3a\""))
        assertEquals("óf", Json.decodeFromString<String>("\"\\u00f3f\""))
        assertEquals("óA", Json.decodeFromString<String>("\"\\u00f3A\""))
        assertEquals("óF", Json.decodeFromString<String>("\"\\u00f3F\""))
    }

    @Test
    fun testConsecutiveUnicodeEscapes() {
        // Validates correct parsing of multiple consecutive unicode escape sequences.
        assertEquals("óéí", Json.decodeFromString<String>("\"\\u00f3\\u00e9\\u00ed\""))
        assertEquals("ABC", Json.decodeFromString<String>("\"\\u0041\\u0042\\u0043\""))
    }

    @Test
    fun testUnicodeEscapeAtStringBoundaries() {
        // Tests unicode escapes at the beginning, middle, and end of strings.
        assertEquals("ó", Json.decodeFromString<String>("\"\\u00f3\""))
        assertEquals("ótest", Json.decodeFromString<String>("\"\\u00f3test\""))
        assertEquals("testó", Json.decodeFromString<String>("\"test\\u00f3\""))
        assertEquals("teóst", Json.decodeFromString<String>("\"te\\u00f3st\""))
    }

    @Test
    fun testSurrogatePairs() {
        // Verifies correct handling of UTF-16 surrogate pairs for characters outside the BMP.
        // U+1D11E (Musical Symbol G Clef) requires surrogate pair \uD834\uDD1E.
        assertEquals("\uD834\uDD1E", Json.decodeFromString<String>("\"\\uD834\\uDD1E\""))
        
        // U+1F914 (Thinking Face emoji) requires surrogate pair \uD83E\uDD14.
        assertEquals("\uD83E\uDD14", Json.decodeFromString<String>("\"\\uD83E\\uDD14\""))
    }

    @Test
    fun testSurrogatePairFollowedByHex() {
        // Ensures hex characters after a complete surrogate pair are not consumed.
        val decoded = Json.decodeFromString<String>("\"\\uD83E\\uDD14abc\"")
        assertEquals("\uD83E\uDD14abc", decoded)
    }

    @Test
    fun testUnicodeEscapeMixedWithRegularText() {
        // Tests complex strings mixing unicode escapes with regular text and hex characters.
        assertEquals(
            "Caf\u00e9 costs \u20ac5",
            Json.decodeFromString<String>("\"Caf\\u00e9 costs \\u20ac5\"")
        )
    }

    @Test
    fun testUnicodeEscapeWithInsufficientDigits() {
        // Validates rejection of malformed escapes with fewer than 4 hex digits.
        assertFailsWithSerial("JsonDecodingException") {
            Json.decodeFromString<String>("\"\\u00f\"")
        }
        assertFailsWithSerial("JsonDecodingException") {
            Json.decodeFromString<String>("\"\\u00\"")
        }
        assertFailsWithSerial("JsonDecodingException") {
            Json.decodeFromString<String>("\"\\u0\"")
        }
        assertFailsWithSerial("JsonDecodingException") {
            Json.decodeFromString<String>("\"\\u\"")
        }
    }

    @Test
    fun testUnicodeEscapeWithInvalidHexDigits() {
        // Validates rejection of escapes containing non-hexadecimal characters.
        assertFailsWithSerial("JsonDecodingException") {
            Json.decodeFromString<String>("\"\\u00gf\"")
        }
        assertFailsWithSerial("JsonDecodingException") {
            Json.decodeFromString<String>("\"\\uXYZW\"")
        }
    }

    @Test
    fun testTruncatedUnicodeEscapeAtEndOfInput() {
        // Ensures proper error handling when input ends mid-escape sequence.
        assertFailsWithSerial("JsonDecodingException") {
            Json.decodeFromString<String>("\"\\u00f")
        }
        assertFailsWithSerial("JsonDecodingException") {
            Json.decodeFromString<String>("\"test\\u00")
        }
    }

    @Test
    fun testNullCharacterEscape() {
        // Verifies correct parsing of the null character (U+0000).
        assertEquals("\u0000", Json.decodeFromString<String>("\"\\u0000\""))
        assertEquals("a\u0000b", Json.decodeFromString<String>("\"a\\u0000b\""))
    }

    @Test
    fun testMaxBmpCodepoint() {
        // Tests the maximum Basic Multilingual Plane codepoint (U+FFFF).
        assertEquals("\uFFFF", Json.decodeFromString<String>("\"\\uFFFF\""))
        assertEquals("\uffff", Json.decodeFromString<String>("\"\\uffff\""))
    }

    @Test
    fun testCaseInsensitiveHexDigits() {
        // Confirms hex digits are parsed case-insensitively per JSON specification.
        assertEquals("\u00AB", Json.decodeFromString<String>("\"\\u00AB\""))
        assertEquals("\u00AB", Json.decodeFromString<String>("\"\\u00ab\""))
        assertEquals("\u00AB", Json.decodeFromString<String>("\"\\u00Ab\""))
        assertEquals("\u00AB", Json.decodeFromString<String>("\"\\u00aB\""))
    }
}