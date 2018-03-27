/*
 * Copyright 2017 JetBrains s.r.o.
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

package kotlinx.serialization

import kotlinx.serialization.internal.HexConverter
import kotlinx.serialization.internal.createString
import kotlin.test.Test
import kotlin.test.assertEquals

class StringTest {
    val str = "i ♥ u"
    val hex = "6920E299A52075"

    @Test
    fun toUtf8() {
        val bytes = str.toUtf8Bytes()
        assertEquals(hex, HexConverter.printHexBinary(bytes, false))
    }

    @Test
    fun fromUtf8() {
        val s = stringFromUtf8Bytes(HexConverter.parseHexBinary(hex))
        assertEquals(str, s)
    }

    @Test
    fun testCreateString() {
        val charArr = charArrayOf('a', 'b', 'c', 'd')
        val content = charArr.createString(2)
        assertEquals("ab", content)
    }
}
