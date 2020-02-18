/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlin.test.*

class ScatteredArraysTest {
    @Serializable
    data class ListData(val data: List<String>, val separator: String)

    @Serializable
    data class ByteData(val data: ByteArray, val separator: String) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ByteData) return false

            if (!data.contentEquals(other.data)) return false
            if (separator != other.separator) return false

            return true
        }

        override fun hashCode(): Int {
            var result = data.contentHashCode()
            result = 31 * result + separator.hashCode()
            return result
        }
    }

    private fun prepareListTestData(): String {
        // Concatenate two serialized representations
        // Resulting bytes would be [1, 2, foo, 3, bar]
        // Protobuf per spec must read it as ListData([1,2,3], bar)
        val d1 = ListData(listOf("1", "2"), "foo")
        val d2 = ListData(listOf("3"), "bar")
        return ProtoBuf.dumps(ListData.serializer(), d1) + ProtoBuf.dumps(ListData.serializer(), d2)
    }

    private fun prepareByteTestData(): String {
        // Concatenate two serialized representations
        // Resulting bytes would be [1, 2, foo, 3, bar]
        // Protobuf per spec must read it as ByteData([1,2,3], bar)
        val d1 = ByteData(byteArrayOf(1, 2), "foo")
        val d2 = ByteData(byteArrayOf(3), "bar")
        return ProtoBuf.dumps(ByteData.serializer(), d1) + ProtoBuf.dumps(ByteData.serializer(), d2)
    }

    private fun <T> doTest(serializer: KSerializer<T>, testData: String, goldenValue: T) {
        val parsed = ProtoBuf.loads(serializer, testData)
        assertEquals(goldenValue, parsed)
    }

    @Test
    fun testListData() =
        doTest(ListData.serializer(), prepareListTestData(), ListData(listOf("1", "2", "3"), "bar"))

    @Test
    fun testByteData() =
        doTest(ByteData.serializer(), prepareByteTestData(), ByteData(byteArrayOf(1, 2, 3), "bar"))
}
