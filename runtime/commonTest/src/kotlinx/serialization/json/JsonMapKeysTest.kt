/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.test.assertStringFormAndRestored
import kotlin.test.*

@Serializable
private data class WithMap(val map: Map<Long, Long>)

@Serializable
private data class WithEnum(val map: Map<SampleEnum, Long>)

@Serializable
private data class WithComplexKey(val map: Map<IntData, String>)

class JsonMapKeysTest : JsonTestBase() {
    @Test
    fun mapKeysShouldBeStrings() = parametrizedTest(strict) { fmt ->
        assertStringFormAndRestored(
            """{"map":{"10":10,"20":20}}""",
            WithMap(mapOf(10L to 10L, 20L to 20L)),
            WithMap.serializer(),
            fmt
        )
    }

    @Test
    fun structuredMapKeysShouldBeBannedByDefault() = parametrizedTest { streaming ->
        val e = assertFailsWith<JsonException> {
            Json(JsonConfiguration.Default).stringify(
                WithComplexKey.serializer(),
                WithComplexKey(mapOf(IntData(42) to "42")),
                streaming
            )
        }
        assertTrue(e.message?.contains("can't be used in json as map key") == true, "wrong exception text")
    }

    @Test
    fun structuredMapKeysAllowedWithFlag() = parametrizedTest(
        WithComplexKey.serializer(),
        WithComplexKey(mapOf(IntData(42) to "42")),
        """{"map":[{"intV":42},"42"]}""",
        Json(JsonConfiguration.Default.copy(allowStructuredMapKeys = true))
    )

    @Test
    fun enumsAreAllowedAsMapKeys() = parametrizedTest(
        WithEnum.serializer(),
        WithEnum(mapOf(SampleEnum.OptionA to 1L, SampleEnum.OptionC to 3L)),
        """{"map":{"OptionA":1,"OptionC":3}}""",
        Json.plain
    )
}
