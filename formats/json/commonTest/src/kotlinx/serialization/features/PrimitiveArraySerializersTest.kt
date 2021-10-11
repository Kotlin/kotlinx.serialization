/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.test.*
import kotlin.test.*

class PrimitiveArraySerializersTest : JsonTestBase() {

    @Serializable
    data class A(
        val arr: ByteArray,
        val arr2: IntArray = intArrayOf(1, 2),
        val arr3: BooleanArray = booleanArrayOf(true, false),
        var arr4: CharArray = charArrayOf('a', 'b', 'c'),
        val arr5: DoubleArray = doubleArrayOf(Double.NaN, 0.1, -0.25),
        val arr6: ShortArray = shortArrayOf(1, 2, 3),
        val arr7: LongArray = longArrayOf(1, 2, 3),
        val arr8: FloatArray = floatArrayOf(1.25f, 2.25f, 3.25f)
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is A) return false

            if (!arr.contentEquals(other.arr)) return false
            if (!arr2.contentEquals(other.arr2)) return false
            if (!arr3.contentEquals(other.arr3)) return false
            if (!arr4.contentEquals(other.arr4)) return false
            if (!arr5.contentEquals(other.arr5)) return false
            if (!arr6.contentEquals(other.arr6)) return false
            if (!arr7.contentEquals(other.arr7)) return false
            if (!arr8.contentEquals(other.arr8)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = arr.contentHashCode()
            result = 31 * result + arr2.contentHashCode()
            result = 31 * result + arr3.contentHashCode()
            result = 31 * result + arr4.contentHashCode()
            result = 31 * result + arr5.contentHashCode()
            result = 31 * result + arr6.contentHashCode()
            result = 31 * result + arr7.contentHashCode()
            result = 31 * result + arr8.contentHashCode()
            return result
        }

        override fun toString(): String {
            return "A(arr=${arr.contentToString()}, arr2=${arr2.contentToString()}, arr3=${arr3.contentToString()}, arr4=${arr4.contentToString()}, arr5=${arr5.contentToString()}, arr6=${arr6.contentToString()}, arr7=${arr7.contentToString()}, arr8=${arr8.contentToString()})"
        }
    }

    @Test
    fun testCanBeSerialized() = assertStringFormAndRestored(
        """{"arr":[1,2,3],"arr2":[1,2],"arr3":[true,false],"arr4":["a","b","c"],"arr5":[NaN,0.1,-0.25],"arr6":[1,2,3],"arr7":[1,2,3],"arr8":[1.25,2.25,3.25]}""",
        A(byteArrayOf(1, 2, 3)),
        A.serializer(),
        lenient
    )
}
