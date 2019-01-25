/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.polymorphic

import kotlinx.serialization.*
import kotlinx.serialization.context.installPolymorphicModule
import kotlinx.serialization.features.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.test.assertStringFormAndRestored
import kotlin.test.Test
import kotlin.test.assertFailsWith

class JsonArrayPolymorphismWithAnyTest {

    @Serializable
    data class MyPolyData(val data: Map<String, @Polymorphic Any>)

    @Serializable
    data class MyPolyDataWithPolyBase(
        val data: Map<String, @Polymorphic Any>,
        @Polymorphic val polyBase: PolyBase
    )

    @Test
    fun testWithoutModules() = assertStringFormAndRestored(
        expected = """{"data":{"stringKey":["kotlin.String","string1"],"mapKey":["kotlin.collections.HashMap",[["kotlin.String","nestedKey"],["kotlin.String","nestedValue"]]],"listKey":["kotlin.collections.ArrayList",[["kotlin.String","foo"]]]}}""",
        original = MyPolyData(linkedMapOf("stringKey" to "string1", "mapKey" to hashMapOf("nestedKey" to "nestedValue"), "listKey" to listOf("foo"))),
        serializer = MyPolyData.serializer(),
        format = Json(useArrayPolymorphism = true)
    )

    @Test
    fun testFailWithoutModulesWithCustomClass() {
        assertFailsWith<SubtypeNotRegisteredException> {
            Json(useArrayPolymorphism = true).stringify(MyPolyData.serializer(), MyPolyData(mapOf("a" to IntData(42))))
        }
    }

    @Test
    fun testWithModules() {
        val json = Json(useArrayPolymorphism = true).apply { installPolymorphicModule(Any::class) { +(IntData::class to IntData.serializer()) } }
        assertStringFormAndRestored(
            expected = """{"data":{"a":["kotlinx.serialization.IntData",{"intV":42}]}}""",
            original = MyPolyData(mapOf("a" to IntData(42))),
            serializer = MyPolyData.serializer(),
            format = json
        )
    }

    /*
     * This test should fail because PolyDerived registered in the scope of PolyBase, not kotlin.Any
     */
    @Test
    fun testFailWithModulesNotInAnyScope() {
        val json = Json(useArrayPolymorphism = true).apply { install(BaseAndDerivedModule) }
        assertFailsWith<SubtypeNotRegisteredException> {
            json.stringify(
                MyPolyData.serializer(),
                MyPolyData(mapOf("a" to PolyDerived("foo")))
            )
        }
    }

    @Test
    fun testRebindModules() {
        val json = Json(useArrayPolymorphism = true).apply { install(BaseAndDerivedModule.rebind(Any::class)) }
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
    fun testFailWithModulesNotInParticularScope() {
        val json = Json(useArrayPolymorphism = true).apply { install(BaseAndDerivedModule.rebind(Any::class)) }
        assertFailsWith<SubtypeNotRegisteredException> {
            json.stringify(
                MyPolyDataWithPolyBase.serializer(),
                MyPolyDataWithPolyBase(mapOf("a" to PolyDerived("foo")), PolyDerived("foo"))
            )
        }
    }

    @Test
    fun testBindModules() {
        val json = Json(useArrayPolymorphism = true).apply { install(BaseAndDerivedModule.bind(Any::class)) }
        assertStringFormAndRestored(
            expected = """{"data":{"a":["kotlinx.serialization.features.PolyDerived",{"id":1,"s":"foo"}]},"polyBase":["kotlinx.serialization.features.PolyDerived",{"id":1,"s":"foo"}]}""",
            original = MyPolyDataWithPolyBase(mapOf("a" to PolyDerived("foo")), PolyDerived("foo")),
            serializer = MyPolyDataWithPolyBase.serializer(),
            format = json
        )
    }
}
