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

import kotlinx.serialization.internal.IntSerializer
import kotlinx.serialization.internal.PairSerializer
import kotlinx.serialization.internal.StringSerializer
import kotlinx.serialization.json.JSON
import org.junit.Test
import kotlin.test.assertEquals

class GenericTest {

    @Serializable
    data class MyPair<K, V>(val k: K, val v: V)

    @Serializable
    data class PairWrapper(val p: Pair<Int, String>)

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
    }

    @Test
    fun writePairInWrapper() {
        val pair = PairWrapper(42 to "foo")
        val saver = PairWrapper.serializer()
        val s = JSON.unquoted.stringify(saver, pair)
        assertEquals("{p:{first:42,second:foo}}", s)
    }
}