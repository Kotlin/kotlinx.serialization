/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import com.google.protobuf.*
import kotlinx.serialization.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.*
import org.junit.Test
import java.io.*
import kotlin.test.*

class ProtobufTopLevelPrimitivesCompatibilityTest {

    @Serializable
    data class Box(@ProtoId(1) val i: Int)

    @Test
    fun testPrimitivesCompatibility() {
        testConversion(true, Boolean.serializer(), "01") { writeBoolNoTag(it) }
        testConversion('c', Char.serializer(), "63") { writeInt32NoTag(it.toInt()) }
        testConversion(1, Byte.serializer(), "01") { writeInt32NoTag(it.toInt()) }
        testConversion(1, Short.serializer(), "01") { writeInt32NoTag(it.toInt()) }
        testConversion(1, Int.serializer(), "01") { writeInt32NoTag(it) }
        testConversion(1, Long.serializer(), "01") { writeInt64NoTag(it) }
        testConversion(1f, Float.serializer(), "0000803F") { writeFloatNoTag(it) }
        testConversion(1.0, Double.serializer(), "000000000000F03F") { writeDoubleNoTag(it) }
        testConversion("string", String.serializer(), "06737472696E67") { writeStringNoTag(it) }
    }

    @Test
    fun testArraysCompatibility() {
        testConversion(byteArrayOf(1, 2, 3), ByteArraySerializer(), "03010203") { writeByteArrayNoTag(it) }
        testConversion(byteArrayOf(), ByteArraySerializer(), "00") { writeByteArrayNoTag(it) }
        testConversion(intArrayOf(1, 2, 3), IntArraySerializer(), "03010203") {
            writeUInt32NoTag(it.size)
            for (i in it) writeInt32NoTag(i)
        }

        testConversion(arrayOf(Box(2)), serializer(), "010802") {
            writeUInt32NoTag(it.size)
            for (box in it) writeInt32(1, box.i)
        }

        testConversion(arrayOf<Box>(), serializer(), "00") {
            writeUInt32NoTag(it.size)
        }
    }

    @Test
    fun testListsCompatibility() {
        testConversion(listOf(1, 2, 3), serializer(), "03010203") {
            writeUInt32NoTag(it.size)
            for (i in it) writeInt32NoTag(i)
        }
        testConversion(listOf(Box(2)), serializer(), "010802") {
            writeUInt32NoTag(it.size)
            for (box in it) writeInt32(1, box.i)
        }

        testConversion(listOf<Int>(), serializer(), "00") {
            writeUInt32NoTag(it.size)
        }
    }

    @Test
    fun testMapsCompatibility() {
        testConversion(mapOf(1 to 2, 3 to 4), serializer(), "0204080110020408031004") {
            writeUInt32NoTag(it.size)
            for (pair in it) {
                writeInt32NoTag(4)
                writeInt32(1, pair.key)
                writeInt32(2, pair.value)
            }
        }

        testConversion(mapOf<Int, Int>(), serializer(), "00") {
            writeInt32NoTag(0)
        }
    }

    @Serializable
    enum class Enum {
        E1, E2
    }

    @Test
    fun testTopLevelEnum() {
        testConversion(Enum.E1, serializer<Enum>(), "00") {
            writeUInt32NoTag(0)
        }

        testConversion(Enum.E2, serializer<Enum>(), "01") {
            writeUInt32NoTag(1)
        }
    }

    private fun <T> testConversion(
        data: T,
        serializer: KSerializer<T>,
        expectedHexString: String,
        block: CodedOutputStream.(T) -> Unit
    ) {
        val bytes = ProtoBuf.dump(serializer, data)
        val string = HexConverter.printHexBinary(bytes)
        val baos = ByteArrayOutputStream()
        val cos = CodedOutputStream.newInstance(baos)
        cos.block(data)
        cos.flush()
        assertTrue(
            baos.toByteArray().contentEquals(bytes),
            "Original: ${baos.toByteArray().contentToString()}, kx: ${bytes.contentToString()}"
        )
        assertEquals(expectedHexString, string)
        val deserialized = ProtoBuf.loads(serializer, string)
        when (data) {
            is Array<*> -> {
                assertTrue(data.contentEquals(deserialized as Array<out Any?>))
            }
            is IntArray -> {
                assertTrue(data.contentEquals(deserialized as IntArray))
            }
            is ByteArray -> {
                assertTrue(data.contentEquals(deserialized as ByteArray))
            }
            else -> {
                assertEquals(data, deserialized)
            }
        }
    }
}
