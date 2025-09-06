/*
 * Copyright 2017-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.cbor

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromHexString
import kotlinx.serialization.encodeToHexString
import kotlin.test.Test
import kotlin.test.assertEquals

@Serializable
data class NullableDataWithTags(
    @ValueTags(12uL)
    val a: ULong?,

    @KeyTags(34uL)
    val b: Int?,

    @KeyTags(56uL)
    @ValueTags(78uL)
    @ByteString val c: ByteArray?,

    @ValueTags(90uL, 12uL)
    val d: String?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as NullableDataWithTags

        if (b != other.b) return false
        if (a != other.a) return false
        if (!c.contentEquals(other.c)) return false
        if (d != other.d) return false

        return true
    }

    override fun hashCode(): Int {
        var result = b ?: 0
        result = 31 * result + (a?.hashCode() ?: 0)
        result = 31 * result + (c?.contentHashCode() ?: 0)
        result = 31 * result + (d?.hashCode() ?: 0)
        return result
    }
}

class CborUntaggedNullTest {
    @Test
    fun encodeAndDecodeUntaggedNullValues() {
        val cbor = Cbor {
            encodeValueTags = true
            verifyValueTags = true
            encodeKeyTags = true
            verifyKeyTags = true
            untaggedNullValueTags = true
        }

        val o = NullableDataWithTags(
            a = null,
            b = null,
            c = null,
            d = null
        )
        val hex = cbor.encodeToHexString(o)
        assertEquals("bf6161a0d8226162f6d8386163f66164f6ff", hex)

        val decoded = cbor.decodeFromHexString<NullableDataWithTags>(hex)
        assertEquals(o, decoded)
    }
}