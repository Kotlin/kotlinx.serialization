/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlin.test.*

class JsonReifiedCollectionsTest : JsonTestBase() {
    @Serializable
    data class DataHolder(val data: String)

    @Test
    fun testReifiedList() = parametrizedTest { jsonTestingMode ->
        val data = listOf(DataHolder("data"), DataHolder("not data"))
        val json = default.encodeToString(data, jsonTestingMode)
        val data2 = default.decodeFromString<List<DataHolder>>(json, jsonTestingMode)
        assertEquals(data, data2)
    }

    @Test
    fun testReifiedMap() = parametrizedTest { jsonTestingMode ->
        val data = mapOf("data" to DataHolder("data"), "smth" to DataHolder("not data"))
        val json = lenient.encodeToString(data, jsonTestingMode)
        val data2 = lenient.decodeFromString<Map<String, DataHolder>>(json, jsonTestingMode)
        assertEquals(data, data2)
    }
}
