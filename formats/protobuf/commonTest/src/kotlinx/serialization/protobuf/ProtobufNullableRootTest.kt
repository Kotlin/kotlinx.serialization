/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlin.test.*

class ProtobufNullableRootTest {
    @Serializable
    data class Box(val value: String)

    @Test
    fun testNullableRootElementFailsWithDescriptiveError() {
        val bytes = ProtoBuf.encodeToByteArray(Box("test"))
        assertFailsWithMessage<SerializationException>("Root-level nullable types are not supported by ProtoBuf") {
            ProtoBuf.decodeFromByteArray<Box?>(bytes)
        }
    }

    @Test
    fun testNullablePrimitiveRootElementFailsWithDescriptiveError() {
        val bytes = ProtoBuf.encodeToByteArray(42)
        assertFailsWithMessage<SerializationException>("Root-level nullable types are not supported by ProtoBuf") {
            ProtoBuf.decodeFromByteArray<Int?>(bytes)
        }
    }

    @Test
    fun testNonNullableRootElementStillWorks() {
        val bytes = ProtoBuf.encodeToByteArray(Box("test"))
        assertEquals(Box("test"), ProtoBuf.decodeFromByteArray<Box>(bytes))
    }
}
