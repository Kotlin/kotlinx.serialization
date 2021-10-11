/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.*
import kotlinx.serialization.modules.plus
import kotlinx.serialization.test.assertStringFormAndRestored
import kotlinx.serialization.test.isJs
import kotlin.test.*

class PolymorphismWithAnyTest {

    @Serializable
    data class MyPolyData(val data: Map<String, @Polymorphic Any>)

    @Serializable
    data class MyPolyDataWithPolyBase(
        val data: Map<String, @Polymorphic Any>,
        @Polymorphic val polyBase: PolyBase
    )

    // KClass.toString() on JS prints simple name, not FQ one
    @Suppress("NAME_SHADOWING")
    private fun checkNotRegisteredMessage(className: String, scopeName: String, exception: SerializationException) {
        val className = className.substringAfterLast('.')
        val scopeName = scopeName.substringAfterLast('.')
        val expectedText =
            "Class '$className' is not registered for polymorphic serialization in the scope of '$scopeName'"
        assertTrue(exception.message!!.startsWith(expectedText),
            "Found $exception, but expected to start with: $expectedText")
    }

    @Test
    fun testFailWithoutModulesWithCustomClass() {
        checkNotRegisteredMessage(
            "kotlinx.serialization.IntData", "kotlin.Any",
            assertFailsWith<SerializationException>("not registered") {
                Json.encodeToString(
                    MyPolyData.serializer(),
                    MyPolyData(mapOf("a" to IntData(42)))
                )
            }
        )
    }

    @Test
    fun testWithModules() {
        val json = Json {
            serializersModule = SerializersModule { polymorphic(Any::class) { subclass(IntData.serializer()) } }
        }
        assertStringFormAndRestored(
            expected = """{"data":{"a":{"type":"kotlinx.serialization.IntData","intV":42}}}""",
            original = MyPolyData(mapOf("a" to IntData(42))),
            serializer = MyPolyData.serializer(),
            format = json
        )
    }

    /**
     * This test should fail because PolyDerived registered in the scope of PolyBase, not kotlin.Any
     */
    @Test
    fun testFailWithModulesNotInAnyScope() {
        val json = Json { serializersModule = BaseAndDerivedModule }
        checkNotRegisteredMessage(
            "kotlinx.serialization.PolyDerived", "kotlin.Any",
            assertFailsWith<SerializationException> {
                json.encodeToString(
                    MyPolyData.serializer(),
                    MyPolyData(mapOf("a" to PolyDerived("foo")))
                )
            }
        )
    }

    private val baseAndDerivedModuleAtAny = SerializersModule {
        polymorphic(Any::class) {
            subclass(PolyDerived.serializer())
        }
    }


    @Test
    fun testRebindModules() {
        val json = Json { serializersModule = baseAndDerivedModuleAtAny }
        assertStringFormAndRestored(
            expected = """{"data":{"a":{"type":"kotlinx.serialization.PolyDerived","id":1,"s":"foo"}}}""",
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
        val json = Json { serializersModule = baseAndDerivedModuleAtAny }
        checkNotRegisteredMessage(
            "kotlinx.serialization.PolyDerived", "kotlinx.serialization.PolyBase",
            assertFailsWith {
                json.encodeToString(
                    MyPolyDataWithPolyBase.serializer(),
                    MyPolyDataWithPolyBase(
                        mapOf("a" to PolyDerived("foo")),
                        PolyDerived("foo")
                    )
                )
            }
        )
    }

    @Test
    fun testBindModules() {
        val json = Json { serializersModule = (baseAndDerivedModuleAtAny + BaseAndDerivedModule) }
        assertStringFormAndRestored(
            expected = """{"data":{"a":{"type":"kotlinx.serialization.PolyDerived","id":1,"s":"foo"}},
                |"polyBase":{"type":"kotlinx.serialization.PolyDerived","id":1,"s":"foo"}}""".trimMargin().lines().joinToString(
                ""
            ),
            original = MyPolyDataWithPolyBase(
                mapOf("a" to PolyDerived("foo")),
                PolyDerived("foo")
            ),
            serializer = MyPolyDataWithPolyBase.serializer(),
            format = json
        )
    }
}
