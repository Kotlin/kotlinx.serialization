/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.json.internal.*
import kotlinx.serialization.test.*
import kotlin.jvm.*
import kotlin.test.*

@JvmInline
@Serializable
value class ComplexCarrier(val c: IntData)

@JvmInline
@Serializable
value class PrimitiveCarrier(val c: String)

class JsonMapKeysTest : JsonTestBase() {
    @Serializable
    private data class WithMap(val map: Map<Long, Long>)

    @Serializable
    private data class WithValueKeyMap(val map: Map<PrimitiveCarrier, Long>)

    @Serializable
    private data class WithEnum(val map: Map<SampleEnum, Long>)

    @Serializable
    private data class WithComplexKey(val map: Map<IntData, String>)

    @Serializable
    private data class WithComplexValueKey(val map: Map<ComplexCarrier, String>)

    @Test
    fun testMapKeysShouldBeStrings() = parametrizedTest(default) {
        assertStringFormAndRestored(
            """{"map":{"10":10,"20":20}}""",
            WithMap(mapOf(10L to 10L, 20L to 20L)),
            WithMap.serializer(),
            this
        )
    }

    @Test
    fun testStructuredMapKeysShouldBeProhibitedByDefault() = parametrizedTest { streaming ->
        noLegacyJs {
            verifyProhibition(WithComplexKey(mapOf(IntData(42) to "42")), streaming)
            verifyProhibition(WithComplexValueKey(mapOf(ComplexCarrier(IntData(42)) to "42")), streaming)
        }
    }

    private inline fun <reified T: Any> verifyProhibition(value: T, streaming: Boolean) {
        val e = assertFailsWith<JsonException> {
            Json.encodeToString(value, streaming)
        }
        assertTrue(e.message?.contains("can't be used in JSON as a key in the map") == true)
    }

    @Test
    fun testStructuredMapKeysAllowedWithFlag() = assertJsonFormAndRestored(
        WithComplexKey.serializer(),
        WithComplexKey(mapOf(IntData(42) to "42")),
        """{"map":[{"intV":42},"42"]}""",
        Json { allowStructuredMapKeys = true }
    )

    @Test
    fun testStructuredValueMapKeysAllowedWithFlag() =  noLegacyJs {
        assertJsonFormAndRestored(
            WithComplexValueKey.serializer(),
            WithComplexValueKey(mapOf(ComplexCarrier(IntData(42)) to "42")),
            """{"map":[{"intV":42},"42"]}""",
            Json { allowStructuredMapKeys = true }
        )
    }

    @Test
    fun testEnumsAreAllowedAsMapKeys() = assertJsonFormAndRestored(
        WithEnum.serializer(),
        WithEnum(mapOf(SampleEnum.OptionA to 1L, SampleEnum.OptionC to 3L)),
        """{"map":{"OptionA":1,"OptionC":3}}""",
        Json
    )

    @Test
    fun testPrimitivesAreAllowedAsValueMapKeys() =  noLegacyJs {
        assertJsonFormAndRestored(
            WithValueKeyMap.serializer(),
            WithValueKeyMap(mapOf(PrimitiveCarrier("fooKey") to 1)),
            """{"map":{"fooKey":1}}""",
            Json
        )
    }
}
