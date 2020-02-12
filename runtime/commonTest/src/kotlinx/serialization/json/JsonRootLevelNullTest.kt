/*
 * Copyright 2017-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.Serializable
import kotlinx.serialization.*
import kotlin.test.*

class JsonRootLevelNullTest : JsonTestBase() {

    @Serializable
    private data class Simple(val a: Int = 42)

    @Test
    fun testNullableStringify() {
        // Top-level nulls in tagged encoder is not yet supported, no parametrized test
        val obj: Simple? = null
        val json = default.stringify(Simple.serializer().nullable, obj)
        assertEquals("null", json)
    }

    @Test
    fun testNullableParse() = parametrizedTest { useStreaming ->
        val result = default.parse(Simple.serializer().nullable, "null", useStreaming)
        assertNull(result)
    }
}
