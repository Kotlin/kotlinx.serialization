/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.json.internal.*
import kotlinx.serialization.test.*
import kotlin.test.*

class JsonParserTest : JsonTestBase() {

    @Test
    fun testQuotedBrace() {
        val tree = parse("""{"x": "{"}""")
        assertTrue("x" in tree)
        assertEquals("{", (tree.getValue("x") as JsonLiteral).content)
    }

    private fun parse(input: String) = default.parseToJsonElement(input).jsonObject

    @Test
    fun testEmptyKey() {
        val tree = parse("""{"":"","":""}""")
        assertTrue("" in tree)
        assertEquals("", (tree.getValue("") as JsonLiteral).content)
    }

    @Test
    fun testEmptyValue() {
        assertFailsWith<JsonDecodingException> {
            parse("""{"X": "foo", "Y"}""")
        }
    }

    @Test
    fun testIncorrectUnicodeEscape() {
        assertFailsWith<JsonDecodingException> {
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
            StringData.serializer(),
            printResult = false
        )
    }

    @Test
    fun testTrailingComma() {
        testTrailingComma("{\"id\":0,}")
        testTrailingComma("{\"id\":0  ,}")
        testTrailingComma("{\"id\":0  , ,}")
    }


    private fun testTrailingComma(content: String) {
        val e = assertFailsWith<JsonDecodingException> {  Json.parseToJsonElement(content) }
        val msg = e.message!!
        assertTrue(msg.contains("Expected end of the object"))
    }
}
