@file:Suppress("INLINE_CLASSES_NOT_SUPPORTED", "SERIALIZER_NOT_FOUND")
@file:OptIn(ExperimentalUnsignedTypes::class)
/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features.inline

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.test.*
import kotlin.test.*

class UnsignedIntegersTest : JsonTestBase() {
    @Serializable
    data class AllUnsigned(val uInt: UInt, val uLong: ULong, val uByte: UByte, val uShort: UShort)

    @Serializable
    data class UnsignedWithoutLong(val uInt: UInt, val uByte: UByte, val uShort: UShort)

    @Test
    fun testUnsignedIntegersJson() {
        val data = AllUnsigned(
            Int.MAX_VALUE.toUInt() + 10.toUInt(),
            Long.MAX_VALUE.toULong() + 10.toULong(),
            239.toUByte(),
            65000.toUShort(),
        )
        // todo: fix unsigned longs in JsonLiteralSerializer
        assertStringFormAndRestored(
            """{"uInt":2147483657,"uLong":9223372036854775817,"uByte":239,"uShort":65000}""",
            data,
            AllUnsigned.serializer(),
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
