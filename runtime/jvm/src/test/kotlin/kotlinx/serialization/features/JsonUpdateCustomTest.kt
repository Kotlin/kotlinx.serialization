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

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.internal.IntSerializer
import kotlinx.serialization.json.Json
import org.junit.*
import kotlin.test.assertEquals

// can't be in common yet because of issue with class literal annotations
// and .serializer() resolving
@Ignore
class JsonUpdateCustomTest {
    @Serializable
    data class Data(val a: Int)

    @Serializer(forClass = Data::class)
    object CustomDataUpdater {
        override fun patch(decoder: Decoder, old: Data): Data {
            val newData = decoder.decodeSerializableValue(this)
            return Data(old.a + newData.a)
        }
    }

    @Serializable
    data class Updatable(@Serializable(with=CustomDataUpdater::class) val d: Data)

    @Test
    fun canUpdateCustom() {
        val parsed: Updatable =
                Json { unquoted = true; strictMode = false }.parse("""{d:{a:42},d:{a:43}}""")
        assertEquals(Data(42 + 43), parsed.d)
    }

    @Serializable
    data class WrappedMap<T>(val mp: Map<String, T>)

    @Test
    fun canUpdateMap() {
        val json = Json()
        val parsed = json.parse(WrappedMap.serializer(IntSerializer), """{"mp": { "x" : 23, "x" : 42, "y": 4 }}""")
        assertEquals(WrappedMap(mapOf("x" to 42, "y" to 4)), parsed)
    }

    @Test
    fun canUpdateValuesInMap() {
        val json = Json()
        val parsed = json.parse(WrappedMap.serializer(IntSerializer.list), """{"mp": { "x" : [23], "x" : [42], "y": [4] }}""")
        assertEquals(WrappedMap(mapOf("x" to listOf(23, 42), "y" to listOf(4))), parsed)
    }
}
