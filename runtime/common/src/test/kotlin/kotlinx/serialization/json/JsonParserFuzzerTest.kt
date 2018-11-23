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

import kotlin.test.*

class JsonParserFuzzerTest {

    @Test
    fun testQuotedBrace() {
        val tree = parse("""{x: "{"}""")
        assertTrue("x" in tree)
        assertEquals("{", tree.getAs<JsonLiteral>("x").content)
    }

    private fun parse(input: String) = JsonTreeParser(input).readFully() as JsonObject

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
}
