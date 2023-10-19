/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlinx.serialization.modules.plus
import kotlinx.serialization.test.assertStringFormAndRestored
import kotlin.test.*

class PolymorphismWithAnyTest: JsonTestBase() {

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
            "Serializer for subclass '$className' is not found in the polymorphic scope of '$scopeName'"
        assertTrue(exception.message!!.startsWith(expectedText),
            "Found $exception, but expected to start with: $expectedText")
    }

    @Test
    fun testFailWithoutModulesWithCustomClass() = parametrizedTest { mode ->
        checkNotRegisteredMessage(
            "kotlinx.serialization.IntData", "kotlin.Any",
            assertFailsWith<SerializationException>("not registered") {
                Json.encodeToString(
                    MyPolyData.serializer(),
                    MyPolyData(mapOf("a" to IntData(42))),
                    mode
                )
            }
        )
    }

    @Test
    fun testWithModules() {
        val json = Json {
            serializersModule = SerializersModule { polymorphic(Any::class) { subclass(IntData.serializer()) } }
        }
        assertJsonFormAndRestored(
            expected = """{"data":{"a":{"type":"kotlinx.serialization.IntData","intV":42}}}""",
            data = MyPolyData(mapOf("a" to IntData(42))),
            serializer = MyPolyData.serializer(),
            json = json
        )
    }

    /**
     * This test should fail because PolyDerived registered in the scope of PolyBase, not kotlin.Any
     */
    @Test
    fun testFailWithModulesNotInAnyScope() = parametrizedTest { mode ->
        val json = Json { serializersModule = BaseAndDerivedModule }
        checkNotRegisteredMessage(
            "kotlinx.serialization.PolyDerived", "kotlin.Any",
            assertFailsWith<SerializationException> {
                json.encodeToString(
                    MyPolyData.serializer(),
                    MyPolyData(mapOf("a" to PolyDerived("foo"))),
                    mode
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
        assertJsonFormAndRestored(
            expected = """{"data":{"a":{"type":"kotlinx.serialization.PolyDerived","id":1,"s":"foo"}}}""",
            data = MyPolyData(mapOf("a" to PolyDerived("foo"))),
            serializer = MyPolyData.serializer(),
            json = json
        )
    }

    /**
     * This test should fail because PolyDerived registered in the scope of kotlin.Any, not PolyBase
     */
    @Test
    fun testFailWithModulesNotInParticularScope() = parametrizedTest { mode ->
        val json = Json { serializersModule = baseAndDerivedModuleAtAny }
        checkNotRegisteredMessage(
            "kotlinx.serialization.PolyDerived", "kotlinx.serialization.PolyBase",
            assertFailsWith {
                json.encodeToString(
                    MyPolyDataWithPolyBase.serializer(),
                    MyPolyDataWithPolyBase(
                        mapOf("a" to PolyDerived("foo")),
                        PolyDerived("foo")
                    ),
                    mode
                )
            }
        )
    }

    @Test
    fun testBindModules() {
        val json = Json { serializersModule = (baseAndDerivedModuleAtAny + BaseAndDerivedModule) }
        assertJsonFormAndRestored(
            expected = """{"data":{"a":{"type":"kotlinx.serialization.PolyDerived","id":1,"s":"foo"}},
                |"polyBase":{"type":"kotlinx.serialization.PolyDerived","id":1,"s":"foo"}}""".trimMargin().lines().joinToString(
                ""
            ),
            data = MyPolyDataWithPolyBase(
                mapOf("a" to PolyDerived("foo")),
                PolyDerived("foo")
            ),
            serializer = MyPolyDataWithPolyBase.serializer(),
            json = json
        )
    }

    @Test
    fun testTypeKeyLastInInput() = parametrizedTest { mode ->
        val json = Json { serializersModule = (baseAndDerivedModuleAtAny + BaseAndDerivedModule) }
        val input = """{"data":{"a":{"id":1,"s":"foo","type":"kotlinx.serialization.PolyDerived"}},
                |"polyBase":{"id":1,"s":"foo","type":"kotlinx.serialization.PolyDerived"}}""".trimMargin().lines().joinToString(
            "")
        val data = MyPolyDataWithPolyBase(
            mapOf("a" to PolyDerived("foo")),
            PolyDerived("foo")
        )
        assertEquals(data, json.decodeFromString(MyPolyDataWithPolyBase.serializer(), input, mode))
    }
}
