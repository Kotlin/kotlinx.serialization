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
import kotlinx.serialization.context.installPolymorphicModule
import kotlinx.serialization.json.Json
import kotlinx.serialization.test.assertStringFormAndRestored
import kotlin.test.*


class PolymorphicMap {
    @Serializable
    data class MyPolyData(val data: Map<String, Any>)

    @Test
    fun withoutModules() = assertStringFormAndRestored(
        expected = """{"data":{"key1":["kotlin.String","string1"],"key2":["kotlin.collections.HashMap",[["kotlin.String","nestedKey"],["kotlin.String","nestedValue"]]]}}""",
        original = MyPolyData(hashMapOf("key1" to "string1", "key2" to hashMapOf("nestedKey" to "nestedValue"))),
        serializer = MyPolyData.serializer()
    )

    @Test
    fun failWithoutModulesWithCustomClass() {
        assertFailsWith<SubtypeNotRegisteredException> {
            Json.stringify(
                MyPolyData.serializer(),
                MyPolyData(mapOf("a" to IntData(42)))
            )
        }
    }

    @Test
    fun withModules() {
        val json = Json().apply { installPolymorphicModule(Any::class) { +(IntData::class to IntData.serializer()) } }
        assertStringFormAndRestored(
            expected = """{"data":{"a":["kotlinx.serialization.IntData",{"intV":42}]}}""",
            original = MyPolyData(mapOf("a" to IntData(42))),
            serializer = MyPolyData.serializer(),
            format = json
        )
    }
}
