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

package kotlinx.serialization

import kotlinx.serialization.internal.*
import kotlinx.serialization.json.Json
import kotlin.test.*

class GenericTest {

    @Serializable
    class Array2DBox(val arr: Array<Array<Double>>) {
        override fun toString(): String {
            return arr.contentDeepToString()
        }
    }

    @Test
    fun writeDefaultPair() {
        val pair = 42 to "foo"
        val saver = PairSerializer(IntSerializer, StringSerializer)
        val s = Json.unquoted.stringify(saver, pair)
        assertEquals("{first:42,second:foo}", s)
        val restored = Json.unquoted.parse(saver, s)
        assertEquals(pair, restored)
    }

    @Test
    fun writePlainTriple() {
        val triple = Triple(42 , "foo", false)
        val saver = TripleSerializer(IntSerializer, StringSerializer, BooleanSerializer)
        val s = Json.unquoted.stringify(saver, triple)
        assertEquals("{first:42,second:foo,third:false}", s)
        val restored = Json.unquoted.parse(saver, s)
        assertEquals(triple, restored)
    }

    @Test
    fun recursiveArrays() {
        val arr = Array2DBox(arrayOf(arrayOf(2.1, 1.2), arrayOf(42.3, -3.4)))
        val str = Json.stringify(arr)
        assertEquals("""{"arr":[[2.1,1.2],[42.3,-3.4]]}""", str)
        val restored = Json.parse<Array2DBox>(str)
        assertTrue(arr.arr.contentDeepEquals(restored.arr))
    }
}
