/*
 *  Copyright 2017 JetBrains s.r.o.
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

package kotlinx.serialization

import kotlinx.serialization.internal.*
import kotlinx.serialization.json.JSON
import org.junit.Test
import kotlin.test.assertEquals

class GenericTest {

    @Serializable
    data class MyPair<K, V>(val k: K, val v: V)

    @Serializable
    data class PairWrapper(val p: Pair<Int, String>)

    @Serializable
    data class TripleWrapper(val t: Triple<Int, String, Boolean>)

    @Test
    fun writeGenericPair() {
        val pair = MyPair<Int, String>(42, "foo")
        val saver = MyPair.serializer(IntSerializer, StringSerializer)
        val s = JSON.unquoted.stringify(saver, pair)
        assertEquals("{k:42,v:foo}", s)
    }

    @Test
    fun writeDefaultPair() {
        val pair = 42 to "foo"
        val saver = PairSerializer(IntSerializer, StringSerializer)
        val s = JSON.unquoted.stringify(saver, pair)
        assertEquals("{first:42,second:foo}", s)
        val restored = JSON.unquoted.parse(saver, s)
        assertEquals(pair, restored)
    }

    @Test
    fun writePairInWrapper() {
        val pair = PairWrapper(42 to "foo")
        val saver = PairWrapper.serializer()
        val s = JSON.unquoted.stringify(saver, pair)
        assertEquals("{p:{first:42,second:foo}}", s)
    }

    @Test
    fun writeTripleInWrapper() {
        val triple = TripleWrapper(Triple(42 , "foo", false))
        val saver = TripleWrapper.serializer()
        val s = JSON.unquoted.stringify(saver, triple)
        assertEquals("{t:{first:42,second:foo,third:false}}", s)
    }

    @Test
    fun writePlainTriple() {
        val triple = Triple(42 , "foo", false)
        val saver = TripleSerializer(IntSerializer, StringSerializer, BooleanSerializer)
        val s = JSON.unquoted.stringify(saver, triple)
        assertEquals("{first:42,second:foo,third:false}", s)
        val restored = JSON.unquoted.parse(saver, s)
        assertEquals(triple, restored)
    }
}