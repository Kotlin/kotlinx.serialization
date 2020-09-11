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
import kotlinx.serialization.json.*
import kotlin.test.*

class SkipDefaultsTest {
    private val json = Json { encodeDefaults = false }

    @Serializable
    data class Data(val bar: String, val foo: Int = 42) {
        var list: List<Int> = emptyList()
        val listWithSomething: List<Int> = listOf(1, 2, 3)
    }

    @Test
    fun serializeCorrectlyDefaults() {
        val d = Data("bar")
        assertEquals("""{"bar":"bar","foo":42,"list":[],"listWithSomething":[1,2,3]}""", Json.encodeToString(Data.serializer(), d))
    }

    @Test
    fun serializeCorrectly() {
        val d = Data("bar", 100).apply { list = listOf(1, 2, 3) }
        assertEquals("""{"bar":"bar","foo":100,"list":[1,2,3]}""", json.encodeToString(Data.serializer(), d))
    }

    @Test
    fun serializeCorrectlyAndDropBody() {
        val d = Data("bar", 43)
        assertEquals("""{"bar":"bar","foo":43}""", json.encodeToString(Data.serializer(), d))
    }

    @Test
    fun serializeCorrectlyAndDropAll() {
        val d = Data("bar")
        assertEquals("""{"bar":"bar"}""", json.encodeToString(Data.serializer(), d))
    }

}
