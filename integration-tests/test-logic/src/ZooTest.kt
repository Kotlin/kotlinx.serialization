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

/*
 *  Copyright 2018 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */


import kotlinx.io.PrintWriter
import kotlinx.io.Reader
import kotlinx.io.StringReader
import kotlinx.io.StringWriter
import kotlinx.serialization.*
import kotlinx.serialization.StructureDecoder.Companion.READ_DONE
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ZooTest {
    @Test
    fun testZoo() {
        // save to string
        val sw = StringWriter()
        val out = KeyValueOutput(PrintWriter(sw))
        out.encode(Zoo.serializer(), zoo)
        // load from string
        val str = sw.toString()
        val inp = KeyValueInput(Parser(StringReader(str)))
        val other = inp.decode(Zoo.serializer())
        // assert we've got it back from string
        assertEquals(zoo, other)
        assertFalse(zoo === other)
    }

    val zoo = Zoo(
            Unit, true, 10, 20, 30, 40, 50f, 60.0, 'A', "Str0", Simple("70"), Attitude.POSITIVE,
            null, null, 11, 21, 31,  51f, 61.0, 'B', null, Simple("Str1"), Attitude.NEUTRAL,
            listOf(1, 2, 3),
            listOf(4, 5, null),
            setOf(6, 7, 8),
            mutableSetOf(null, 9, 10),
            listOf(listOf(Simple("1")), listOf(Simple("2"), Simple("3"))),
            listOf(listOf(Simple("1"), null, Simple(""))),
            mapOf("one" to 1, "two" to 2, "three" to 3),
            mapOf(0 to null, 1 to "first", 2 to "second")
    )

    // KeyValue Input/Output

    class KeyValueOutput(val out: PrintWriter) : ElementValueEncoder() {
        override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): StructureEncoder {
            out.print('{')
            return this
        }

        override fun endStructure(desc: SerialDescriptor) = out.print('}')

        override fun encodeElement(desc: SerialDescriptor, index: Int): Boolean {
            if (index > 0) out.print(", ")
            out.print(desc.getElementName(index));
            out.print(':')
            return true
        }

        override fun encodeNullValue() = out.print("null")
        override fun encodeNonSerializableValue(value: Any) = out.print(value)

        override fun encodeStringValue(value: String) {
            out.print('"')
            out.print(value)
            out.print('"')
        }

        override fun encodeCharValue(value: Char) = encodeStringValue(value.toString())
    }

    class KeyValueInput(val inp: Parser) : ElementValueDecoder() {
        override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): StructureDecoder {
            inp.expectAfterWhiteSpace('{')
            return this
        }

        override fun endStructure(desc: SerialDescriptor) = inp.expectAfterWhiteSpace('}')

        override fun decodeElement(desc: SerialDescriptor): Int {
            inp.skipWhitespace(',')
            val name = inp.nextUntil(':', '}')
            if (name.isEmpty())
                return READ_DONE
            val index = desc.getElementIndexOrThrow(name)
            inp.expect(':')
            return index
        }

        private fun readToken(): String {
            inp.skipWhitespace()
            return inp.nextUntil(' ', ',', '}')
        }

        override fun decodeNotNullMark(): Boolean {
            inp.skipWhitespace()
            if (inp.cur != 'n'.toInt()) return true
            return false
        }

        override fun decodeNullValue(): Nothing? {
            check(readToken() == "null") { "'null' expected" }
            return null
        }

        override fun decodeBooleanValue(): Boolean = readToken() == "true"
        override fun decodeByteValue(): Byte = readToken().toByte()
        override fun decodeShortValue(): Short = readToken().toShort()
        override fun decodeIntValue(): Int = readToken().toInt()
        override fun decodeLongValue(): Long = readToken().toLong()
        override fun decodeFloatValue(): Float = readToken().toFloat()
        override fun decodeDoubleValue(): Double = readToken().toDouble()

        override fun <T : Enum<T>> decodeEnumValue(enumCreator: EnumCreator<T>): T {
            return enumCreator.createFromName(readToken())
        }

        override fun decodeStringValue(): String {
            inp.expectAfterWhiteSpace('"')
            val value = inp.nextUntil('"')
            inp.expect('"')
            return value
        }

        override fun decodeCharValue(): Char = decodeStringValue().single()
    }

    // Parser

    // Very simple char-by-char parser
    class Parser(private val inp: Reader) {
        var cur: Int = inp.read()

        fun next() {
            cur = inp.read()
        }

        fun skipWhitespace(vararg c: Char) {
            while (cur >= 0 && (cur.toChar().isWhitespace() || cur.toChar() in c))
                next()
        }

        fun expect(c: Char) {
            check(cur == c.toInt()) { "Expected '$c'" }
            next()
        }

        fun expectAfterWhiteSpace(c: Char) {
            skipWhitespace()
            expect(c)
        }

        fun  nextUntil(vararg c: Char): String {
            val sb = StringBuilder()
            while (cur >= 0 && cur.toChar() !in c) {
                sb.append(cur.toChar())
                next()
            }
            return sb.toString()
        }
    }
}
