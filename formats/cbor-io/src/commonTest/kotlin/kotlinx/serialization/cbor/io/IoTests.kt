/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.cbor.io

import kotlinx.io.*
import kotlinx.serialization.*
import kotlinx.serialization.cbor.*
import kotlin.test.*

class IoTests {

    @Serializable
    data class Simple(val i: Int)

    @Test
    fun testEncodingDefinite() {
        val value = Simple(42)
        val buffer = Buffer()
        Cbor { useDefiniteLengthEncoding = true }.encodeToSink(value, buffer)
        assertEquals(expected = "a16169182a", actual = buffer.readByteArray().toHexString())
    }

    @Test
    fun testEncodingIndefinite() {
        val buffer = Buffer()
        Cbor { useDefiniteLengthEncoding = false }.encodeToSink(Simple(42), buffer)
        assertEquals(expected = "bf6169182aff", actual = buffer.readByteArray().toHexString())
    }

    @Test
    fun testDecoding() {
        val buffer = Buffer()
        buffer.write("a16169182a".hexToByteArray())
        val decoded = Cbor.decodeFromSource<Simple>(buffer)
        assertEquals(expected = Simple(42), actual = decoded)

        assertTrue(buffer.exhausted())
    }

    @Test
    fun testDecodingFailsWithUnprocessedBytes() {
        val buffer = Buffer()
        buffer.write("bf6169182aff00".hexToByteArray())
        assertFailsWith<SerializationException> { Cbor.decodeFromSource<Simple>(buffer) }
    }
}
