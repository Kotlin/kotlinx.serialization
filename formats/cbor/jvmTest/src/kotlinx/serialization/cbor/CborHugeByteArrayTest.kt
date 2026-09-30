/*
 * Copyright 2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.cbor

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlin.test.*

class CborHugeByteArrayTest {

    @Test
    fun testEncodeByteStringLargerThanOneGibibyte() {
        val size = (1 shl 30) + 1
        // The source array, the output buffer and the resulting array have to fit into the heap
        if (Runtime.getRuntime().maxMemory() < 4L * size) return

        val value = ByteArray(size)
        value[0] = 1
        value[size - 1] = 2

        val encoded = Cbor { alwaysUseByteString = true }.encodeToByteArray(ByteArraySerializer(), value)

        val headerSize = 5 // major type 2 with a 4-byte length
        assertEquals(headerSize + size, encoded.size)
        assertEquals(0x5A.toByte(), encoded[0])
        assertEquals(1.toByte(), encoded[headerSize])
        assertEquals(2.toByte(), encoded[headerSize + size - 1])
    }

    @Test
    fun testEncodingLargerThanMaximumArraySizeThrows() {
        val size = (1 shl 30) + 1
        if (Runtime.getRuntime().maxMemory() < 3L * size) return

        val value = ByteArray(size)
        val exception = assertFailsWith<SerializationException> {
            Cbor { alwaysUseByteString = true }.encodeToByteArray(ListSerializer(ByteArraySerializer()), listOf(value, value))
        }
        assertEquals("Cannot grow array to 2147483661 elements, the maximum supported array size is 2147483639", exception.message)
    }
}
