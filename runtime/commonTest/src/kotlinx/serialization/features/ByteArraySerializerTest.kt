/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.internal.ByteArraySerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.random.Random
import kotlin.test.*

@Serializable
class ByteArrayCarrier(@SerialId(2) @Serializable(with = ByteArraySerializer::class) val data: ByteArray) {
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

class ByteArraySerializerTest {
    @Test
    fun plain() {
        val bytes = byteArrayOf(42, 43, 44, 45)
        val s = Json.stringify(ByteArraySerializer, bytes)
        assertEquals(s, """[42,43,44,45]""")
        val bytes2 = Json.parse(ByteArraySerializer, s)
        assertTrue(bytes.contentEquals(bytes2))
    }

    @Test
    fun inObject() {
        val obj = ByteArrayCarrier(byteArrayOf(42, 100))
        val s = Json.stringify(ByteArrayCarrier.serializer(), obj)
        assertEquals("""{"data":[42,100]}""", s)
        val obj2 = Json.parse(ByteArrayCarrier.serializer(), s)
        assertEquals(obj, obj2)
    }

    @Test
    fun inProto() {
        val obj = ByteArrayCarrier(byteArrayOf(42, 100))
        val s = ProtoBuf.dumps(ByteArrayCarrier.serializer(), obj)
        assertEquals("""12022a64""", s)
        val obj2 = ProtoBuf.loads(ByteArrayCarrier.serializer(), s)
        assertEquals(obj, obj2)
    }

    @Test
    fun serializeAndDeserializeLongArrays() {
        val arraySize = 301
        val arr = Random.nextBytes(ByteArray(arraySize))
        val obj = ByteArrayCarrier(arr)
        val bytes = ProtoBuf.dump(ByteArrayCarrier.serializer(), obj)
        assertEquals(obj, ProtoBuf.load(ByteArrayCarrier.serializer(), bytes))
    }

}
