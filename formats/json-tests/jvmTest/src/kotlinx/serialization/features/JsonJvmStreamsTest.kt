/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlinx.serialization.test.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JsonJvmStreamsTest {
    val BATCH_SIZE = 16 * 1024 // kotlinx.serialization.json.internal.BATCH_SIZE
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

    interface Poly

    @Serializable
    @SerialName("Impl")
    data class Impl(val str: String) : Poly

    @Test
    fun testPolymorphismWhenCrossingBatchSizeNonLeadingKey() {
        val json = Json { 
            serializersModule = SerializersModule { 
                polymorphic(Poly::class) {
                    subclass(Impl::class, Impl.serializer())
                }
            }
        }

        val longString = "a".repeat(BATCH_SIZE - 5)
        val string = """{"str":"$longString", "type":"Impl"}"""
        val golden = Impl(longString)

        val deserialized = json.decodeViaStream(serializer<Poly>(), string)
        assertEquals(golden, deserialized as Impl)
    }

    @Test
    fun testPolymorphismWhenCrossingBatchSize() {
        val json = Json {
            serializersModule = SerializersModule {
                polymorphic(Poly::class) {
                    subclass(Impl::class, Impl.serializer())
                }
            }
        }

        val aLotOfWhiteSpaces = " ".repeat(BATCH_SIZE - 5)
        val string = """{$aLotOfWhiteSpaces"type":"Impl", "str":"value"}"""
        val golden = Impl("value")

        val deserialized = json.decodeViaStream(serializer<Poly>(), string)
        assertEquals(golden, deserialized as Impl)
    }
}
