/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.test.*

class PolymorphismTest : JsonTestBase() {

    @Serializable
    data class Wrapper(
        @Id(1) @Polymorphic val polyBase1: PolyBase,
        @Id(2) @Polymorphic val polyBase2: PolyBase
    )

    private val module: SerialModule = BaseAndDerivedModule + SerializersModule {
        polymorphic(
            PolyDerived::class,
            PolyDerived.serializer()
        )
    }

    private val json = Json { unquotedPrint = true; useArrayPolymorphism = true; serialModule = module }

    @Test
    fun testInheritanceJson() = parametrizedTest { useStreaming ->
        val obj = Wrapper(
            PolyBase(2),
            PolyDerived("b")
        )
        val bytes = json.stringify(Wrapper.serializer(), obj, useStreaming)
        assertEquals(
            "{polyBase1:[kotlinx.serialization.PolyBase,{id:2}]," +
                    "polyBase2:[kotlinx.serialization.PolyDerived,{id:1,s:b}]}", bytes
        )
    }

    @Test
    fun testSerializeWithExplicitPolymorphicSerializer() = parametrizedTest { useStreaming ->
        val obj = PolyDerived("b")
        val s = json.stringify(PolymorphicSerializer(PolyDerived::class), obj, useStreaming)
        assertEquals("[kotlinx.serialization.PolyDerived,{id:1,s:b}]", s)
    }

    object PolyDefaultSerializer : JsonTransformingSerializer<PolyDefault>(PolyDefault.serializer(), "foo") {
        override fun readTransform(element: JsonElement): JsonElement {
            return json {
                "json" to element
                "id" to 42
            }
        }
    }

    @Test
    fun testDefaultSerializer() = parametrizedTest { useStreaming ->
        val withDefault = module + SerializersModule {
            defaultPolymorphic(PolyBase::class) { name ->
                if (name == "foo") {
                    PolyDefaultSerializer
                } else {
                    null
                }
            }
        }

        val adjustedJson = Json(json.configuration.copy(useArrayPolymorphism = false), withDefault)
        val string = """
            {"polyBase1":{"type":"kotlinx.serialization.PolyBase","id":239},
            "polyBase2":{"type":"foo","key":42}}""".trimIndent()
        val result = adjustedJson.parse(Wrapper.serializer(), string, useStreaming)
        assertEquals(Wrapper(PolyBase(239), PolyDefault(JsonObject(mapOf("key" to JsonPrimitive(42))))), result)

        val replaced = string.replace("foo", "bar")
        assertFailsWithMessage<SerializationException>("not registered") { adjustedJson.parse(Wrapper.serializer(), replaced, useStreaming) }
    }
}
