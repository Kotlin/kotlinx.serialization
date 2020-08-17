/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlin.random.*
import kotlin.test.*

class ByteArraySerializerTest {

    @Serializable
    class ByteArrayCarrier(@ProtoNumber(2) val data: ByteArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as ByteArrayCarrier

            if (!data.contentEquals(other.data)) return false

            return true
        }

        override fun hashCode(): Int {
            return data.contentHashCode()
        }

        override fun toString(): String {
            return "ByteArrayCarrier(data=${data.contentToString()})"
        }
    }

    @Test
    fun testByteArrayProtobuf() {
        val obj = ByteArrayCarrier(byteArrayOf(42, 100))
        val s = ProtoBuf.encodeToHexString(ByteArrayCarrier.serializer(), obj)
        assertEquals("""12022a64""", s)
        val obj2 = ProtoBuf.decodeFromHexString(ByteArrayCarrier.serializer(), s)
        assertEquals(obj, obj2)
    }

    @Test
    fun testWrappedByteArrayProtobuf() {
        val arraySize = 301
        val arr = Random.nextBytes(ByteArray(arraySize))
        val obj = ByteArrayCarrier(arr)
        val bytes = ProtoBuf.encodeToByteArray(ByteArrayCarrier.serializer(), obj)
        assertEquals(obj, ProtoBuf.decodeFromByteArray(ByteArrayCarrier.serializer(), bytes))
    }
}
