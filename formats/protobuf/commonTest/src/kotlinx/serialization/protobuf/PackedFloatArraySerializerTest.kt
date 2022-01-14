/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlin.random.*
import kotlin.test.*

class PackedFloatArraySerializerTest {

    @Serializable
    data class FloatArrayCarrier(
        @ProtoNumber(2) val createdAt: ULong,
        @ProtoPacked
        @ProtoNumber(3) val vector: FloatArray
    ) {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as FloatArrayCarrier

            if (createdAt != other.createdAt) return false
            if (!vector.contentEquals(other.vector)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = createdAt.hashCode()
            result = 31 * result + vector.contentHashCode()
            return result
        }
    }

    @Test
    fun testPackedFloatArrayProtobuf() {
        val obj = FloatArrayCarrier(1234567890L.toULong(), floatArrayOf(1f, 2f, 3f))
        val s = ProtoBuf.encodeToHexString(FloatArrayCarrier.serializer(), obj).uppercase()
        assertEquals("""10D285D8CC041A0C0000803F0000004000004040""", s)
        val obj2 = ProtoBuf.decodeFromHexString(FloatArrayCarrier.serializer(), s)
        assertEquals(obj, obj2)
    }

    @Serializable
    data class StringArrayCarrier(
        @ProtoNumber(1) val createdAt: ULong,
        @ProtoPacked
        @ProtoNumber(2) val vector: Array<String>
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as StringArrayCarrier

            if (createdAt != other.createdAt) return false
            if (!vector.contentEquals(other.vector)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = createdAt.hashCode()
            result = 31 * result + vector.contentHashCode()
            return result
        }
    }

    @Test
    fun testPackedStringArrayProtobuf() {
        val obj = StringArrayCarrier(1234567890L.toULong(), arrayOf("1.0", "2.0", "3"))
        val s = ProtoBuf.encodeToHexString(StringArrayCarrier.serializer(), obj).uppercase()
        assertEquals("""08D285D8CC04120A03312E3003322E300133""", s)
        val obj2 = ProtoBuf.decodeFromHexString(StringArrayCarrier.serializer(), s)
        assertEquals(obj, obj2)
    }


    @Serializable
    data class SimpleObject(@ProtoNumber(1) val a: Int, @ProtoNumber(2) val b: String)

    @Serializable
    data class ObjectListCarrier(
        @ProtoNumber(1) val createdAt: ULong,
        @ProtoPacked
        @ProtoNumber(2) val vector: List<SimpleObject>
    )

    @Test
    fun testPackedObjectListProtobuf() {
        val obj = ObjectListCarrier(1234567890L.toULong(), listOf(
            SimpleObject(12, "foo"),
            SimpleObject(876, "bar")
        ))
        val s = ProtoBuf.encodeToHexString(ObjectListCarrier.serializer(), obj).uppercase()
        assertEquals("""08D285D8CC04121107080C1203666F6F0808EC061203626172""", s)
        val obj2 = ProtoBuf.decodeFromHexString(ObjectListCarrier.serializer(), s)
        assertEquals(obj, obj2)
    }
}
