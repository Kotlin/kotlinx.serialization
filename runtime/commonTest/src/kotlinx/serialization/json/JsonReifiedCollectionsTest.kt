/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlin.test.*

@UseExperimental(ImplicitReflectionSerializer::class)
class JsonReifiedCollectionsTest : JsonTestBase() {
    @Serializable
    data class DataHolder(val data: String)

    @Test
    fun testReifiedList() = parametrizedTest { useStreaming ->
        val data = listOf(DataHolder("data"), DataHolder("not data"))
        val json = strict.stringify(data, useStreaming)
        val data2 = strict.parseList<DataHolder>(json, useStreaming)
        assertEquals(data, data2)
    }
    @Test
    fun testReifiedMap() = parametrizedTest { useStreaming ->
        val data = mapOf("data" to DataHolder("data"), "smth" to DataHolder("not data"))
        val json = nonStrict.stringify(data, useStreaming)
        val data2 = nonStrict.parseMap<String, DataHolder>(json, useStreaming)
        assertEquals(data, data2)
    }

    @Test
    fun testPrimitiveSerializer() {
        val intClass = Int::class
        val serial = intClass.serializer()
        assertSame(IntSerializer, serial)
    }
}
