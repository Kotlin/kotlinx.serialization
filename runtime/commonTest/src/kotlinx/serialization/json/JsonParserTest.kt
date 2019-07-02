/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.test.*
import kotlin.test.*

class JsonParserTest {

    @Test
    fun testQuotedBrace() {
        val tree = parse("""{x: "{"}""")
        assertTrue("x" in tree)
        assertEquals("{", tree.getAs<JsonLiteral>("x").content)
    }

    private fun parse(input: String) = Json.plain.parseJson(input).jsonObject

    @Test
    fun testEmptyKey() {
        val tree = parse("""{"":"","":""}""")
        assertTrue("" in tree)
        assertEquals("", tree.getAs<JsonLiteral>("").content)
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

    val strict = Json(JsonConfiguration.Stable.copy(strictMode = true))

    @Test
    fun testParseEscapedSymbols() {
        assertEquals(
            StringData("https://t.co/M1uhwigsMT"),
            strict.parse(StringData.serializer(), """{"data":"https:\/\/t.co\/M1uhwigsMT"}""")
        )
        assertEquals(StringData("\"test\""), strict.parse(StringData.serializer(), """{"data": "\"test\""}"""))
        assertEquals(StringData("\u00c9"), strict.parse(StringData.serializer(), """{"data": "\u00c9"}"""))
        assertEquals(StringData("""\\"""), strict.parse(StringData.serializer(), """{"data": "\\\\"}"""))
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
        val e = assertFailsWith<JsonParsingException> {  Json.plain.parseJson(content) }
        val msg = e.message!!
        assertTrue(msg.contains("Expected end of the object"))
    }
}
