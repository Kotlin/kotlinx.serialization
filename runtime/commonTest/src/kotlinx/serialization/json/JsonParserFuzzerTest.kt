/*
 * Copyright 2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.serialization.json

import kotlinx.serialization.StringData
import kotlinx.serialization.test.assertStringFormAndRestored
import kotlin.test.*

class JsonParserFuzzerTest {

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
        assertFailsWith<JsonParsingException> {
            parse("""{"X": "foo", "Y"}""")
        }
    }

    @Test
    fun testIncorrectUnicodeEscape() {
        assertFailsWith<JsonParsingException> {
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
}
