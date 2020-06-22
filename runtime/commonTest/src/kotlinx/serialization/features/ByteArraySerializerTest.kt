/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.Json
import kotlin.test.*

class ByteArraySerializerTest {

    @Serializable
    class ByteArrayCarrier(@Id(2) val data: ByteArray) {
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
    fun testByteArrayJson() {
        val bytes = byteArrayOf(42, 43, 44, 45)
        val s = Json.encodeToString(ByteArraySerializer(), bytes)
        assertEquals(s, """[42,43,44,45]""")
        val bytes2 = Json.decodeFromString(ByteArraySerializer(), s)
        assertTrue(bytes.contentEquals(bytes2))
    }

    @Test
    fun testWrappedByteArrayJson() {
        val obj = ByteArrayCarrier(byteArrayOf(42, 100))
        val s = Json.encodeToString(ByteArrayCarrier.serializer(), obj)
        assertEquals("""{"data":[42,100]}""", s)
        val obj2 = Json.decodeFromString(ByteArrayCarrier.serializer(), s)
        assertEquals(obj, obj2)
    }
}
