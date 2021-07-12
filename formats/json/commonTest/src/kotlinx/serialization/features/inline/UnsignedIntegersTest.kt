@file:Suppress("INLINE_CLASSES_NOT_SUPPORTED", "SERIALIZER_NOT_FOUND")
@file:OptIn(ExperimentalUnsignedTypes::class)
/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features.inline

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.test.*

class UnsignedIntegersTest : JsonTestBase() {
    @Serializable
    data class AllUnsigned(
        val uInt: UInt,
        val uLong: ULong,
        val uByte: UByte,
        val uShort: UShort,
        val signedInt: Int,
        val signedLong: Long,
        val double: Double
    )

    @Serializable
    data class UnsignedWithoutLong(val uInt: UInt, val uByte: UByte, val uShort: UShort)

    @Test
    fun testUnsignedIntegersJson() {
        val data = AllUnsigned(
            Int.MAX_VALUE.toUInt() + 10.toUInt(),
            Long.MAX_VALUE.toULong() + 10.toULong(),
            239.toUByte(),
            65000.toUShort(),
            -42,
            Long.MIN_VALUE,
            1.1
        )
        assertJsonFormAndRestored(
            AllUnsigned.serializer(),
            data,
            """{"uInt":2147483657,"uLong":9223372036854775817,"uByte":239,"uShort":65000,"signedInt":-42,"signedLong":-9223372036854775808,"double":1.1}""",
        )
    }

    @Test
    fun testUnsignedIntegersWithoutLongJson() {
        val data = UnsignedWithoutLong(
            Int.MAX_VALUE.toUInt() + 10.toUInt(),
            239.toUByte(),
            65000.toUShort(),
        )
        assertJsonFormAndRestored(
            UnsignedWithoutLong.serializer(),
            data,
            """{"uInt":2147483657,"uByte":239,"uShort":65000}""",
        )
    }
}
