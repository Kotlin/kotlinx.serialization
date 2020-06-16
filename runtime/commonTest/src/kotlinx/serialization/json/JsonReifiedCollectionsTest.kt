/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.test.*
import kotlin.test.*

class JsonReifiedCollectionsTest : JsonTestBase() {
    @Serializable
    data class DataHolder(val data: String)

    @Test
    fun testReifiedList() = parametrizedTest { useStreaming ->
        val data = listOf(DataHolder("data"), DataHolder("not data"))
        val json = default.stringify(data, useStreaming)
        val data2 = default.parse<List<DataHolder>>(json, useStreaming)
        assertEquals(data, data2)
    }

    @Test
    fun testReifiedMap() = parametrizedTest { useStreaming ->
        val data = mapOf("data" to DataHolder("data"), "smth" to DataHolder("not data"))
        val json = lenient.stringify(data, useStreaming)
        val data2 = lenient.parse<Map<String, DataHolder>>(json, useStreaming)
        assertEquals(data, data2)
    }
}
