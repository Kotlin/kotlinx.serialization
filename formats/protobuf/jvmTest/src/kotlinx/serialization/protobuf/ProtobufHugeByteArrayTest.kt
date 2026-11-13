/*
 * Copyright 2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.builtins.*
import kotlin.test.*

class ProtobufHugeByteArrayTest {

    @Test
    fun testEncodeByteArrayLargerThanOneGibibyte() {
        val size = (1 shl 30) + 1
        // The source array, the output buffer and the resulting array have to fit into the heap
        if (Runtime.getRuntime().maxMemory() < 4L * size) return

        val value = ByteArray(size)
        value[0] = 1
        value[size - 1] = 2

        val encoded = ProtoBuf.encodeToByteArray(ByteArraySerializer(), value)

        val headerSize = 5 // varint-encoded length
        assertEquals(headerSize + size, encoded.size)
        assertEquals(1.toByte(), encoded[headerSize])
        assertEquals(2.toByte(), encoded[headerSize + size - 1])
    }
}
