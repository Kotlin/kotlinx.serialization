/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.SerializationException
import kotlinx.serialization.StringData
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.*
import kotlinx.serialization.json.internal.BATCH_SIZE
import kotlinx.serialization.test.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JsonJvmStreamsTest {
    private val strLen = BATCH_SIZE * 2 + 42

    @Test
    fun testParsesStringsLongerThanBuffer() {
        val str = "a".repeat(strLen)
        val input = """{"data":"$str"}"""
        assertEquals(input, Json.encodeViaStream(StringData.serializer(), StringData(str)))
        assertEquals(str, Json.decodeViaStream(StringData.serializer(), input).data)
        assertEquals(str, Json.decodeViaStream(String.serializer(), "\"$str\""))
    }

    @Test
    fun testSkipsWhitespacesLongerThanBuffer() {
        val str = "a".repeat(strLen)
        val ws = " ".repeat(strLen)
        val input = """{"data":$ws"$str"}"""
        assertEquals("""{"data":"$str"}""", Json.encodeViaStream(StringData.serializer(), StringData(str)))
        assertEquals(str, Json.decodeViaStream(StringData.serializer(), input).data)
    }

    @Test
    fun testHandlesEscapesLongerThanBuffer() {
        val str = "\\t".repeat(strLen)
        val expected = "\t".repeat(strLen)
        val input = """{"data":"$str"}"""
        assertEquals(input, Json.encodeViaStream(StringData.serializer(), StringData(expected)))
        assertEquals(expected, Json.decodeViaStream(StringData.serializer(), input).data)
    }

    @Test
    fun testHandlesLongLenientStrings() {
        val str = "a".repeat(strLen)
        val input = """{"data":$str}"""
        val json = Json { isLenient = true }
        assertEquals(str, json.decodeViaStream(StringData.serializer(), input).data)
        assertEquals(str, json.decodeViaStream(String.serializer(), str))
    }

    @Test
    fun testThrowsCorrectExceptionOnEof() {
        assertFailsWith<SerializationException> {
            Json.decodeViaStream(StringData.serializer(), """{"data":""")
        }
        assertFailsWith<SerializationException> {
            Json.decodeViaStream(StringData.serializer(), "")
        }
        assertFailsWith<SerializationException> {
            Json.decodeViaStream(String.serializer(), "\"")
        }
    }

    @Test
    fun testRandomEscapeSequences()  {
        repeat(1000) {
            val s = generateRandomUnicodeString(strLen)
            try {
                val serializer = String.serializer()
                val b = ByteArrayOutputStream()
                Json.encodeToStream(serializer, s, b)
                val restored = Json.decodeFromStream(serializer, ByteArrayInputStream(b.toByteArray()))
                assertEquals(s, restored)
            } catch (e: Throwable) {
                // Not assertion error to preserve cause
                throw IllegalStateException("Unexpectedly failed test, cause string: $s", e)
            }
        }
    }

}
