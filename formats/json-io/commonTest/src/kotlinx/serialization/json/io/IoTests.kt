/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.io

import kotlinx.io.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlinx.serialization.json.io.internal.*
import kotlin.test.*

class IoTests {

    @Serializable
    data class Simple(val i: Int)

    @Test
    fun testSurrogate() {
        val text = "\uD83D\uDE03"
        val originalChars = text.toCharArray()

        val buffer = Buffer()
        buffer.writeString(text)
        val reader = IoSerialReader(buffer)

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
        Json.encodeToSink(value, buffer)
        val encoded = buffer.readString()
        assertEquals(json, encoded)

        buffer.writeString(encoded)
        val decoded = Json.decodeFromSource<Simple>(buffer)
        assertEquals(value, decoded)

        assertTrue(buffer.exhausted())
    }

    @Test
    fun testDecodeSequence() {
        val json = "{\"i\":1}{\"i\":2}"
        val value1 = Simple(1)
        val value2 = Simple(2)
        val buffer = Buffer()
        buffer.writeString(json)
        val decoded = Json.decodeSourceToSequence<Simple>(buffer).toList()

        assertTrue(buffer.exhausted())
        assertEquals(2, decoded.size)
        assertEquals(listOf(value1, value2), decoded)

        buffer.writeString(json)
        val decodedExplicit = Json.decodeSourceToSequence(buffer, Simple.serializer()).toList()
        assertTrue(buffer.exhausted())
        assertEquals(2, decodedExplicit.size)
        assertEquals(listOf(value1, value2), decodedExplicit)
    }
}