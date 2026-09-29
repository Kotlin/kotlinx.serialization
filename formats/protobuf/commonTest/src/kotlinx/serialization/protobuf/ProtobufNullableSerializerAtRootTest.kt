/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlin.test.*

class ProtobufNullableSerializerAtRootTest {
    @Serializable
    data class Box(val value: String = "")

    @Test
    fun nonNullableDecodesByNullableSerializerTest() {
        val data = Box("test")
        val bytes = ProtoBuf.encodeToByteArray<Box>(data)
        val decoded = ProtoBuf.decodeFromByteArray<Box?>(bytes)
        assertEquals(data, decoded)
    }

    @Test
    fun nullableDecodesByNonNullableSerializerTest() {
        val data = Box("test")
        val bytes = ProtoBuf.encodeToByteArray<Box?>(data)
        val decoded = ProtoBuf.decodeFromByteArray<Box>(bytes)
        assertEquals(data, decoded)
    }
}
