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
import kotlinx.serialization.json.JSON
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UpdateTest {
    @Serializable
    data class Updatable1(val l: List<Int>)

    @Serializable
    data class Data(val a: Int)

    @Serializable
    data class Updatable2(val l: List<Data>)

    @Serializable
    data class NotUpdatable(val d: Data)

    @Serializable
    data class WrappedMap<T>(val mp: Map<String, T>)

    @Test
    fun canUpdatePrimitiveList() {
        val parsed = JSON(unquoted = true, nonstrict = true).parse<Updatable1>("""{l:[1,2],f:foo,l:[3,4]}""")
        assertEquals(Updatable1(listOf(1,2,3,4)), parsed)
    }

    @Test
    fun canUpdateObjectList() {
        val parsed = JSON(unquoted = true, nonstrict = true).parse<Updatable2>("""{f:bar,l:[{a:42}],l:[{a:43}]}""")
        assertEquals(Updatable2(listOf(Data(42), Data(43))), parsed)
    }

    @Test
    fun canUpdateMap() {
        val parsed = JSON.parse(WrappedMap.serializer(IntSerializer), """{"mp": { "x" : 23, "x" : 42, "y": 4 }}""")
        assertEquals(WrappedMap(mapOf("x" to 42, "y" to 4)), parsed)
    }

    @Test
    fun canUpdateValuesInMap() {
        val parsed = JSON.parse(WrappedMap.serializer(IntSerializer.list), """{"mp": { "x" : [23], "x" : [42], "y": [4] }}""")
        assertEquals(WrappedMap(mapOf("x" to listOf(23, 42), "y" to listOf(4))), parsed)
    }

    @Test
    fun cantUpdateNotUpdatable() {
        assertFailsWith<UpdateNotSupportedException> {
            JSON(unquoted = true, nonstrict = true).parse<NotUpdatable>("""{d:{a:42},d:{a:43}}""")
        }
    }
}