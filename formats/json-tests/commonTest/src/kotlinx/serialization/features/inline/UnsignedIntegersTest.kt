/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features.inline

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
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

    @ExperimentalUnsignedTypes
    @Serializable
    data class UnsignedArrays(
        val uByte: UByteArray,
        val uShort: UShortArray,
        val uInt: UIntArray,
        val uLong: ULongArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as UnsignedArrays

            if (!uByte.contentEquals(other.uByte)) return false
            if (!uShort.contentEquals(other.uShort)) return false
            if (!uInt.contentEquals(other.uInt)) return false
            if (!uLong.contentEquals(other.uLong)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = uByte.contentHashCode()
            result = 31 * result + uShort.contentHashCode()
            result = 31 * result + uInt.contentHashCode()
            result = 31 * result + uLong.contentHashCode()
            return result
        }
    }

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

    @Test
    fun testRoot() {
        assertJsonFormAndRestored(UByte.serializer(), 220U, "220")
        assertJsonFormAndRestored(UShort.serializer(), 65000U, "65000")
        assertJsonFormAndRestored(UInt.serializer(), 2147483657U, "2147483657")
        assertJsonFormAndRestored(ULong.serializer(), 9223372036854775817U, "9223372036854775817")
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun testRootArrays() = parametrizedTest {
        assertJsonFormAndRestoredCustom(
            UByteArraySerializer(),
            ubyteArrayOf(1U, 220U),
            "[1,220]"
        ) { l, r -> l.contentEquals(r) }

        assertJsonFormAndRestoredCustom(
            UShortArraySerializer(),
            ushortArrayOf(1U, 65000U),
            "[1,65000]"
        ) { l, r -> l.contentEquals(r) }

        assertJsonFormAndRestoredCustom(
            UIntArraySerializer(),
            uintArrayOf(1U, 2147483657U),
            "[1,2147483657]"
        ) { l, r -> l.contentEquals(r) }

        assertJsonFormAndRestoredCustom(
            ULongArraySerializer(),
            ulongArrayOf(1U, 9223372036854775817U),
            "[1,9223372036854775817]"
        ) { l, r -> l.contentEquals(r) }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun testArrays() {
        val data = UnsignedArrays(
            ubyteArrayOf(1U, 220U),
            ushortArrayOf(1U, 65000U),
            uintArrayOf(1U, 2147483657U),
            ulongArrayOf(1U, 9223372036854775817U)
        )
        val json = """{"uByte":[1,220],"uShort":[1,65000],"uInt":[1,2147483657],"uLong":[1,9223372036854775817]}"""

        assertJsonFormAndRestored(UnsignedArrays.serializer(), data, json)
    }

}
