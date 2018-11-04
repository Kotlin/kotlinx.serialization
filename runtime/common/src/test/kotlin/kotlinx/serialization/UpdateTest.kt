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

import kotlinx.serialization.json.Json
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
    data class NullableInnerIntList(val data: List<Int?>)

    @Serializable
    data class NullableUpdatable(val data: List<Data>?)

    @Test
    fun canUpdatePrimitiveList() {
        val parsed =
                Json(unquoted = true, strictMode = false, updateMode = UpdateMode.UPDATE)
                .parse<Updatable1>("""{l:[1,2],f:foo,l:[3,4]}""")
        assertEquals(Updatable1(listOf(1,2,3,4)), parsed)
    }

    @Test
    fun canUpdateObjectList() {
        val parsed =
                Json(unquoted = true, strictMode = false, updateMode = UpdateMode.UPDATE)
                .parse<Updatable2>("""{f:bar,l:[{a:42}],l:[{a:43}]}""")
        assertEquals(Updatable2(listOf(Data(42), Data(43))), parsed)
    }

    @Test
    fun cantUpdateNotUpdatable() {
        assertFailsWith<UpdateNotSupportedException> {
            Json(unquoted = true, strictMode = false, updateMode = UpdateMode.UPDATE).parse<NotUpdatable>("""{d:{a:42},d:{a:43}}""")
        }
    }

    @Test
    fun canUpdateNullableValuesInside() {
        val json = Json(updateMode = UpdateMode.UPDATE)
        val a1 = json.parse<NullableInnerIntList>("""{data:[null],data:[1]}""")
        assertEquals(NullableInnerIntList(listOf(null, 1)), a1)
        val a2 = json.parse<NullableInnerIntList>("""{data:[42],data:[null]}""")
        assertEquals(NullableInnerIntList(listOf(42, null)), a2)
        val a3 = json.parse<NullableInnerIntList>("""{data:[31],data:[1]}""")
        assertEquals(NullableInnerIntList(listOf(31, 1)), a3)
    }

    @Test
    fun canUpdateNullableValues() {
        val json = Json(updateMode = UpdateMode.UPDATE)
        val a1 = json.parse<NullableUpdatable>("""{data:null,data:[{a:42}]}""")
        assertEquals(NullableUpdatable(listOf(Data(42))), a1)
        val a2 = json.parse<NullableUpdatable>("""{data:[{a:42}],data:null}""")
        assertEquals(NullableUpdatable(listOf(Data(42))), a2)
        val a3 = json.parse<NullableUpdatable>("""{data:[{a:42}],data:[{a:43}]}""")
        assertEquals(NullableUpdatable(listOf(Data(42), Data(43))), a3)
    }
}