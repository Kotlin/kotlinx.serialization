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
import kotlin.test.Test
import kotlin.test.assertFailsWith


class PolymorphismWithAnyTest {
    @Serializable
    data class MyPolyData(val data: Map<String, @Polymorphic Any>)

    @Serializable
    data class MyPolyDataWithPolyBase(
        val data: Map<String, @Polymorphic Any>,
        @Polymorphic val polyBase: PolyBase
    )

    @Test
    fun withoutModules() = assertStringFormAndRestored(
        expected = """{"data":{"stringKey":["kotlin.String","string1"],"mapKey":["kotlin.collections.HashMap",[["kotlin.String","nestedKey"],["kotlin.String","nestedValue"]]],"listKey":["kotlin.collections.ArrayList",[["kotlin.String","foo"]]]}}""",
        original = MyPolyData(linkedMapOf("stringKey" to "string1", "mapKey" to hashMapOf("nestedKey" to "nestedValue"), "listKey" to listOf("foo"))),
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

    /**
     * This test should fail because PolyDerived registered in the scope of PolyBase, not kotlin.Any
     */
    @Test
    fun failWithModulesNotInAnyScope() {
        val json = Json().apply { install(BaseAndDerivedModule) }
        assertFailsWith<SubtypeNotRegisteredException> {
            json.stringify(
                MyPolyData.serializer(),
                MyPolyData(mapOf("a" to PolyDerived("foo")))
            )
        }
    }

    @Test
    fun rebindModules() {
        val json = Json().apply { install(BaseAndDerivedModule.rebind(Any::class)) }
        assertStringFormAndRestored(
            expected = """{"data":{"a":["kotlinx.serialization.features.PolyDerived",{"id":1,"s":"foo"}]}}""",
            original = MyPolyData(mapOf("a" to PolyDerived("foo"))),
            serializer = MyPolyData.serializer(),
            format = json
        )
    }

    /**
     * This test should fail because PolyDerived registered in the scope of kotlin.Any, not PolyBase
     */
    @Test
    fun failWithModulesNotInParticularScope() {
        val json = Json().apply { install(BaseAndDerivedModule.rebind(Any::class)) }
        assertFailsWith<SubtypeNotRegisteredException> {
            json.stringify(
                MyPolyDataWithPolyBase.serializer(),
                MyPolyDataWithPolyBase(mapOf("a" to PolyDerived("foo")), PolyDerived("foo"))
            )
        }
    }

    @Test
    fun bindModules() {
        val json = Json().apply { install(BaseAndDerivedModule.bind(Any::class)) }
        assertStringFormAndRestored(
            expected = """{"data":{"a":["kotlinx.serialization.features.PolyDerived",{"id":1,"s":"foo"}]},"polyBase":["kotlinx.serialization.features.PolyDerived",{"id":1,"s":"foo"}]}""",
            original = MyPolyDataWithPolyBase(mapOf("a" to PolyDerived("foo")), PolyDerived("foo")),
            serializer = MyPolyDataWithPolyBase.serializer(),
            format = json
        )
    }
}
