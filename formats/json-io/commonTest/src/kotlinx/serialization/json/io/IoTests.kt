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

    @Serializable
    data class Text(val value: String)

    @Test
    fun testSurrogate() {
        val text = "\uD83D\uDE03"
        val originalChars = text.toCharArray()

        val buffer = Buffer()
        buffer.writeString(text)
        val reader = KxIoReader(buffer)

        val readArray = CharArray(2)
        assertEquals(1, reader.read(readArray, 0, 1) )
        assertEquals(1, reader.read(readArray, 1, 1) )

        assertContentEquals(originalChars, readArray)
    }

    @Test
    fun testMultibyteJsonString() {
        val expected = Text("Latin-1: é, BMP: 漢, supplementary: \uD83D\uDE03")
        val source = Buffer().apply { writeString(Json.encodeToString(expected)) }
        assertEquals(expected, Json.decodeFromSource<Text>(source))
    }

    @Test
    fun testMalformedUtf8() {
        assertEquals("\uFFFD", Json.decodeFromSource<String>(jsonStringWithBytes(0xc0, 0x80)))
        assertEquals("\uFFFD", Json.decodeFromSource<String>(jsonStringWithBytes(0xe0, 0x80, 0x80)))
        assertFailsWith<EOFException> {
            Json.decodeFromSource<String>(jsonStringWithBytes(0xf5))
        }
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

    private fun jsonStringWithBytes(vararg bytes: Int): Buffer = Buffer().apply {
        writeByte('"'.code.toByte())
        bytes.forEach { writeByte(it.toByte()) }
        writeByte('"'.code.toByte())
    }
}
