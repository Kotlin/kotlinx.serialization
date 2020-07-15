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
    data class Box(@ProtoNumber(1) val i: Int)

    @Serializable
    data class StringHolder(val foo: String)

    @Test
    fun testPrimitivesCompatibility() {
        testCompatibility(true, Boolean.serializer(), "01") { writeBoolNoTag(it) }
        testCompatibility('c', Char.serializer(), "63") { writeInt32NoTag(it.toInt()) }
        testCompatibility(1, Byte.serializer(), "01") { writeInt32NoTag(it.toInt()) }
        testCompatibility(1, Short.serializer(), "01") { writeInt32NoTag(it.toInt()) }
        testCompatibility(1, Int.serializer(), "01") { writeInt32NoTag(it) }
        testCompatibility(1, Long.serializer(), "01") { writeInt64NoTag(it) }
        testCompatibility(1f, Float.serializer(), "0000803F") { writeFloatNoTag(it) }
        testCompatibility(1.0, Double.serializer(), "000000000000F03F") { writeDoubleNoTag(it) }
        testCompatibility("string", String.serializer(), "06737472696E67") { writeStringNoTag(it) }
    }

    @Test
    fun testArraysCompatibility() {
        testCompatibility(byteArrayOf(1, 2, 3), ByteArraySerializer(), "03010203") { writeByteArrayNoTag(it) }
        testCompatibility(byteArrayOf(), ByteArraySerializer(), "00") { writeByteArrayNoTag(it) }
        testCompatibility(intArrayOf(1, 2, 3), IntArraySerializer(), "03010203") {
            writeUInt32NoTag(it.size)
            for (i in it) writeInt32NoTag(i)
        }

        testCompatibility(arrayOf(Box(1), Box(2)), serializer(), "02020801020802") {
            writeUInt32NoTag(it.size)
            for (box in it) {
                writeInt32NoTag(2) // Size in bytes
                writeInt32(1, box.i)
            }
        }

        testCompatibility(arrayOf<Box>(), serializer(), "00") {
            writeUInt32NoTag(it.size)
        }
    }

    @Test
    fun testListsCompatibility() {
        testCompatibility(listOf(1, 2, 3), serializer(), "03010203") {
            writeUInt32NoTag(it.size)
            for (i in it) writeInt32NoTag(i)
        }
        testCompatibility(listOf(Box(1), Box(2)), serializer(), "02020801020802") {
            writeUInt32NoTag(it.size)
            for (box in it) {
                writeInt32NoTag(2) // Size-prefix
                writeInt32(1, box.i)
            }
        }

        testCompatibility(listOf(StringHolder("a"), StringHolder("bb")), serializer(), "02030A0161040A026262") {
            writeUInt32NoTag(it.size)
            for (holder in it) {
                writeInt32NoTag(holder.foo.protoSize)
                writeString(1, holder.foo)
            }
        }

        testCompatibility(listOf<Int>(), serializer(), "00") {
            writeUInt32NoTag(it.size)
        }
    }


    @Test
    fun testNestedListsCompatibility() {
        testCompatibility(
            listOf(listOf(StringHolder("a")), listOf(StringHolder("bb"), StringHolder("cc"))),
            serializer(),
            "0201030A016102040A026262040A026363"
        ) {
            writeUInt32NoTag(it.size) // Top level list size
            for (list in it) {
                writeUInt32NoTag(list.size) //Nested list size
                for (h in list) {
                    writeInt32NoTag(h.foo.protoSize)
                    writeString(1, h.foo)
                }
            }
        }
    }

    @Test
    fun testMapsCompatibility() {
        testCompatibility(mapOf(1 to 2, 3 to 4), serializer(), "0204080110020408031004") {
            writeUInt32NoTag(it.size)
            for (pair in it) {
                writeInt32NoTag(4) // Size
                writeInt32(1, pair.key)
                writeInt32(2, pair.value)
            }
        }

        testCompatibility(mapOf<Int, Int>(), serializer(), "00") {
            writeInt32NoTag(0)
        }
    }

    @Test
    fun testMapOfListsCompatibility() {
        testCompatibility(
            mapOf(listOf(1, 2, 3) to listOf("aa"), listOf(7) to listOf("a", "b", "c")),
            serializer(),
            "020A080108020803120261610B0807120161120162120163"
        ) {
            writeUInt32NoTag(it.size) // outer map size
            for ((index, entry) in it.entries.withIndex()) {
                val key = entry.key
                val value = entry.value
                // Hand-rolled size of entry
                val sz = if (index == 0) 10 else 11
                writeInt32NoTag(sz) // Size of entry
                for (i in key) writeInt32(1, i)
                for (s in value) {
                    writeString(2, s)
                }
            }
        }
    }

    @Test
    fun testNestedMaps() {
        // No compatibility on purpose, too much to write
        val key1 = mapOf(listOf(Box(2), Box(3)) to listOf(StringHolder("aa"), StringHolder("bb")))
        val value1 = mapOf(listOf("a", "b", "c") to 42)
        val key2 = mapOf(listOf(Box(42)) to listOf(StringHolder("ff")))
        val value2 = mapOf(listOf("d", "e") to 42)
        val map = mapOf(key1 to value1, key2 to value2)
        assertSerializedToBinaryAndRestored(map, serializer(), ProtoBuf)
    }

    @Serializable
    enum class Enum {
        E1, E2
    }

    @Test
    fun testTopLevelEnum() {
        testCompatibility(Enum.E1, serializer<Enum>(), "00") {
            writeUInt32NoTag(0)
        }

        testCompatibility(Enum.E2, serializer<Enum>(), "01") {
            writeUInt32NoTag(1)
        }
    }

    @Serializable
    object SomeObject

    @Test
    fun testTopLevelObject() {
        testCompatibility(SomeObject, serializer(), "") {}
    }

    private fun <T> testCompatibility(
        data: T,
        serializer: KSerializer<T>,
        expectedHexString: String,
        block: CodedOutputStream.(T) -> Unit
    ) {
        val bytes = ProtoBuf.encodeToByteArray(serializer, data)
        val string = HexConverter.printHexBinary(bytes)
        val baos = ByteArrayOutputStream()
        val cos = CodedOutputStream.newInstance(baos)
        cos.block(data)
        cos.flush()
        assertTrue(
            baos.toByteArray().contentEquals(bytes),
            "Original: ${baos.toByteArray().contentToString()},\n\t  kx: ${bytes.contentToString()}"
        )
        assertEquals(expectedHexString, string)
        val deserialized = ProtoBuf.decodeFromHexString(serializer, string)
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

    private val String.protoSize: Int get() = 2 + length
}
