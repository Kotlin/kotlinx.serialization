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

package kotlinx.serialization.features

import kotlinx.serialization.Serializable
import kotlinx.serialization.internal.IntSerializer
import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

// todo: move to common tests when extension points released
class GenericTest {
    @Serializable
    data class MyPair<K, V>(val k: K, val v: V)

    @Serializable
    data class PairWrapper(val p: Pair<Int, String>)

    @Serializable
    data class TripleWrapper(val t: Triple<Int, String, Boolean>)

    @Test
    fun writeGenericPair() {
        val pair = MyPair(42, "foo")
        val serializer = MyPair.serializer(IntSerializer, StringSerializer)
        val s = Json.unquoted.stringify(serializer, pair)
        assertEquals("{k:42,v:foo}", s)
    }

    @Test
    fun writePairInWrapper() {
        val pair = PairWrapper(42 to "foo")
        val serializer = PairWrapper.serializer()
        val s = Json.unquoted.stringify(serializer, pair)
        assertEquals("{p:{first:42,second:foo}}", s)
    }

    @Test
    fun writeTripleInWrapper() {
        val triple = TripleWrapper(Triple(42 , "foo", false))
        val serializer = TripleWrapper.serializer()
        val s = Json.unquoted.stringify(serializer, triple)
        assertEquals("{t:{first:42,second:foo,third:false}}", s)
    }
}