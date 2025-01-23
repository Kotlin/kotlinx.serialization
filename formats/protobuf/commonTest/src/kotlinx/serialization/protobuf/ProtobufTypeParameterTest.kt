/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlin.test.*

class ProtobufTypeParameterTest {
    @Serializable
    data class Box<T>(val value: T)

    @Serializable
    data class ExplicitNullableUpperBoundBox<T : Any?>(val value: T)

    @Serializable
    data class ExplicitNullableUpperNullablePropertyBoundBox<T : Any?>(val value: T?)

    inline fun <reified T> testBox(value: T, expectedHexString: String) {
        testConversion(Box(value), expectedHexString)
        testConversion(ExplicitNullableUpperBoundBox(value), expectedHexString)
        testConversion(ExplicitNullableUpperNullablePropertyBoundBox(value), expectedHexString)
    }

    @Serializable
    private data class DefaultArgPair<T>(val first: T, val second: T = first)

    companion object {
        val testList0 = emptyList<Int>()
        val testList1 = listOf(0)
        val testMap0 = emptyMap<Int, Int>()
        val testMap1 = mapOf(0 to 0)
    }


    @Test
    fun testNothingBoxesWithNull() {
        // Cannot use 'Nothing?' as reified type parameter
        //testBox(null, "")
        testConversion(Box(null), "")
        testConversion(ExplicitNullableUpperBoundBox(null), "")
        @Suppress("RemoveExplicitTypeArguments")
        testConversion(ExplicitNullableUpperNullablePropertyBoundBox<Nothing>(null), "")
        testConversion(ExplicitNullableUpperNullablePropertyBoundBox<Nothing?>(null), "")
    }

    @Test
    fun testIntBoxes() {
        testBox(0, "0800")
        testBox(1, "0801")
    }

    @Test
    fun testNullableIntBoxes() {
        testBox<Int?>(null, "")
        testBox<Int?>(0, "0800")
    }

    @Test
    fun testCollectionBoxes() {
        testBox(testList0, "")
        testBox(testList1, "0800")
        testBox(testMap0, "")
        testBox(testMap1, "0A0408001000")
    }

    @Test
    fun testNullableCollectionBoxes() {
        fun assertFailsForNullForCollectionTypes(block: () -> Unit) {
            try {
                block()
                fail()
            } catch (e: SerializationException) {
                assertEquals(
                    "'null' is not supported as the value of collection types in ProtoBuf", e.message
                )
            }
        }
        assertFailsForNullForCollectionTypes {
            testBox<List<Int>?>(null, "")
        }
        assertFailsForNullForCollectionTypes {
            testBox<Map<Int, Int>?>(null, "")
        }
        testBox<List<Int>?>(testList0, "")
        testBox<Map<Int, Int>?>(testMap0, "")
    }

    @Test
    fun testWithDefaultArguments() {
        testConversion(DefaultArgPair(null), "")
        testConversion(DefaultArgPair(1), "0801")
        testConversion(DefaultArgPair(null, null), "")
        testConversion(DefaultArgPair(null, 1), "1001")
        assertFailsWith<SerializationException> {
            testConversion(DefaultArgPair(1, null), "0801")
        }
        testConversion(DefaultArgPair(1, 1), "0801")
        testConversion(DefaultArgPair(1, 2), "08011002")
    }
}