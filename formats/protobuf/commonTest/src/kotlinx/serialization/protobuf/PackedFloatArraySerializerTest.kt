/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlin.random.*
import kotlin.test.*

class PackedFloatArraySerializerTest {

    @Serializable
    class FloatArrayCarrier(
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

        override fun toString(): String {
            return "FloatArrayCarrier(createdAt=$createdAt, vector=${vector.contentToString()})"
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
}
