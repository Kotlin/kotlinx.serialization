/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.okio.internal

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlinx.serialization.json.okio.*
import okio.*
import kotlin.test.*

class OkioTests {

    @Serializable
    data class Simple(val i: Int)

    @Test
    fun testSurrogate() {
        val text = "\uD83D\uDE03"
        val originalChars = text.toCharArray()

        val buffer = Buffer()
        buffer.writeUtf8(text)
        val reader = OkioSerialReader(buffer)

        val readArray = CharArray(2)
        assertEquals(1, reader.read(readArray, 0, 1) )
        assertEquals(1, reader.read(readArray, 1, 1) )

        assertContentEquals(originalChars, readArray)
    }


    @Test
    fun testEncodingAndDecoding() {
        val json = "{\"i\":42}"
        val value = Simple(42)
        val buffer = Buffer()
        Json.encodeToBufferedSink(value, buffer)
        val encoded = buffer.readUtf8()
        assertEquals(json, encoded)

        buffer.writeUtf8(encoded)
        val decoded = Json.decodeFromBufferedSource<Simple>(buffer)
        assertEquals(value, decoded)

        assertTrue(buffer.exhausted())
    }

    @Test
    fun testDecodeSequence() {
        val json = "{\"i\":1}{\"i\":2}"
        val value1 = Simple(1)
        val value2 = Simple(2)
        val buffer = Buffer()
        buffer.writeUtf8(json)
        val decoded = Json.decodeBufferedSourceToSequence<Simple>(buffer).toList()

        assertTrue(buffer.exhausted())
        assertEquals(2, decoded.size)
        assertEquals(listOf(value1, value2), decoded)

        buffer.writeUtf8(json)
        val decodedExplicit = Json.decodeBufferedSourceToSequence(buffer, Simple.serializer()).toList()
        assertTrue(buffer.exhausted())
        assertEquals(2, decodedExplicit.size)
        assertEquals(listOf(value1, value2), decodedExplicit)
    }
}