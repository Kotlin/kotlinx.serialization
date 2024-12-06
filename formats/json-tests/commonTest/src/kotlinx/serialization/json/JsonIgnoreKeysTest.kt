/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.Serializable
import kotlinx.serialization.test.checkSerializationException
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class JsonIgnoreKeysTest : JsonTestBase() {
    val ignoresKeys = Json(default) { ignoreUnknownKeys = true }

    @Serializable
    class Outer(val a: Int, val inner: Inner)

    @Serializable
    @JsonIgnoreUnknownKeys
    class Inner(val x: String)

    @Test
    fun testIgnoresKeyWhenGlobalSettingNotSet() = parametrizedTest { mode ->
        val jsonString = """{"a":1,"inner":{"x":"value","unknownKey":"unknownValue"}}"""
        val result = default.decodeFromString<Outer>(jsonString, mode)
        assertEquals(1, result.a)
        assertEquals("value", result.inner.x)
    }

    @Test
    fun testThrowsWithoutAnnotationWhenGlobalSettingNotSet() = parametrizedTest { mode ->
        val jsonString = """{"a":1,"inner":{"x":"value","unknownKey":"unknownValue"}, "b":2}"""
        checkSerializationException({
            default.decodeFromString<Outer>(jsonString, mode)
        }) { msg ->
            assertContains(
                msg,
                if (mode == JsonTestingMode.TREE) "Encountered an unknown key 'b' at element: \$\n"
                else "Encountered an unknown key 'b' at offset 59 at path: \$\n"
            )
        }
    }

    @Test
    fun testIgnoresBothKeysWithGlobalSetting() = parametrizedTest { mode ->
        val jsonString = """{"a":1,"inner":{"x":"value","unknownKey":"unknownValue"}, "b":2}"""
        val result = ignoresKeys.decodeFromString<Outer>(jsonString, mode)
        assertEquals(1, result.a)
        assertEquals("value", result.inner.x)
    }
}
