/*
 * Copyright 2017-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.cbor

import kotlin.test.*

class CborElementEqualityTest {

    //TODO ULONG MIN VALUE TESTS

    @Test
    fun testCborPositiveIntEquality() {
        val int1 = CborInt(42u)
        val int2 = CborInt(42u)
        val int3 = CborInt(43u)
        val int4 = CborInt(42u, 1u)

        // Same values should be equal
        assertEquals(int1, int2)
        assertEquals(int1.hashCode(), int2.hashCode())

        // Different values should not be equal
        assertNotEquals(int1, int3)

        // Different tags should not be equal
        assertNotEquals(int1, int4)

        // Null comparison
        assertNotEquals(int1, null as CborElement?)

        // Different type comparison
        assertNotEquals(int1 as CborElement, CborString("42"))
        assertNotEquals(int1, CborString("42") as CborElement)
    }

    @Test
    fun testCborNegativeIntEquality() {
        val int1 = CborInt(-42)
        val int2 = CborInt(-42)
        val int3 = CborInt(-43)
        val int4 = CborInt(-42, 1u)

        assertEquals(int1, int2)
        assertEquals(int1.hashCode(), int2.hashCode())
        assertNotEquals(int1, int3)
        assertNotEquals(int1, int4)
        assertNotEquals(int1, null as CborElement?)
        assertNotEquals(int1, CborInt(42u) as CborElement)
        assertNotEquals(int1 as CborElement, CborInt(42u))
    }

    @Test
    fun testCborDoubleEquality() {
        val double1 = CborFloat(3.14)
        val double2 = CborFloat(3.14)
        val double3 = CborFloat(2.71)
        val double4 = CborFloat(3.14, 1u)

        assertEquals(double1, double2)
        assertEquals(double1.hashCode(), double2.hashCode())
        assertNotEquals(double1, double3)
        assertNotEquals(double1, double4)
        assertNotEquals(double1, null as CborElement?)
        assertNotEquals(double1 as CborElement, CborString("3.14"))
        assertNotEquals(double1, CborString("3.14") as CborElement)
    }

    @Test
    fun testCborStringEquality() {
        val string1 = CborString("hello")
        val string2 = CborString("hello")
        val string3 = CborString("world")
        val string4 = CborString("hello", 1u)

        assertEquals(string1, string2)
        assertEquals(string1.hashCode(), string2.hashCode())
        assertNotEquals(string1, string3)
        assertNotEquals(string1, string4)
        assertNotEquals(string1, null as CborElement?)
        assertNotEquals(string1 as CborElement, CborInt(123u))
        assertNotEquals(string1, CborInt(123u) as CborElement)
    }

    @Test
    fun testCborBooleanEquality() {
        val bool1 = CborBoolean(true)
        val bool2 = CborBoolean(true)
        val bool3 = CborBoolean(false)
        val bool4 = CborBoolean(true, 1u)

        assertEquals(bool1, bool2)
        assertEquals(bool1.hashCode(), bool2.hashCode())
        assertNotEquals(bool1, bool3)
        assertNotEquals(bool1, bool4)
        assertNotEquals(bool1, null as CborElement?)
        assertNotEquals(bool1 as CborElement, CborString("true"))
        assertNotEquals(bool1, CborString("true") as CborElement)
    }

    @Test
    fun testCborByteStringEquality() {
        val bytes1 = byteArrayOf(1, 2, 3)
        val bytes2 = byteArrayOf(1, 2, 3)
        val bytes3 = byteArrayOf(4, 5, 6)

        val byteString1 = CborByteString(bytes1)
        val byteString2 = CborByteString(bytes2)
        val byteString3 = CborByteString(bytes3)
        val byteString4 = CborByteString(bytes1, 1u)

        assertEquals(byteString1, byteString2)
        assertEquals(byteString1.hashCode(), byteString2.hashCode())
        assertNotEquals(byteString1, byteString3)
        assertNotEquals(byteString1, byteString4)
        assertNotEquals(byteString1, null as CborElement?)
        assertNotEquals(byteString1 as CborElement, CborString("123"))
        assertNotEquals(byteString1, CborString("123") as CborElement)
    }

    @Test
    fun testCborNullEquality() {
        val null1 = CborNull()
        val null2 = CborNull()
        val null3 = CborNull(1u)

        assertEquals(null1, null2)
        assertEquals(null1.hashCode(), null2.hashCode())
        assertNotEquals(null1, null3)
        assertNotEquals(null1, null as CborElement?)
        assertNotEquals(null1 as CborElement, CborString("null"))
        assertNotEquals(null1, CborString("null") as CborElement)
    }

    @Test
    fun testCborArrayEquality() {
        val list1 = CborArray(listOf(CborInt(1u), CborString("test")))
        val list2 = CborArray(listOf(CborInt(1u), CborString("test")))
        val list3 = CborArray(listOf(CborInt(2u), CborString("test")))
        val list4 = CborArray(listOf(CborInt(1u), CborString("test")), 1u)
        val list5 = CborArray(listOf(CborInt(1u)))

        assertEquals(list1, list2)
        assertEquals(list1.hashCode(), list2.hashCode())
        assertNotEquals(list1, list3)
        assertNotEquals(list1, list4)
        assertNotEquals(list1, list5)
        assertNotEquals(list1, null as CborElement?)
        assertNotEquals(list1 as CborElement, CborString("list"))
        assertNotEquals(list1, CborString("list") as CborElement)
    }

    @Test
    fun testCborMapEquality() {
        val map1 = CborMap(
            mapOf(
                CborString("key1") to CborInt(1u),
                CborString("key2") to CborString("value")
            )
        )
        val map2 = CborMap(
            mapOf(
                CborString("key1") to CborInt(1u),
                CborString("key2") to CborString("value")
            )
        )
        val map3 = CborMap(
            mapOf(
                CborString("key1") to CborInt(2u),
                CborString("key2") to CborString("value")
            )
        )
        val map4 = CborMap(
            mapOf(
                CborString("key1") to CborInt(1u),
                CborString("key2") to CborString("value")
            ), 1u
        )
        val map5 = CborMap(
            mapOf(
                CborString("key1") to CborInt(1u)
            )
        )

        assertEquals(map1, map2)
        assertEquals(map1.hashCode(), map2.hashCode())
        assertNotEquals(map1, map3)
        assertNotEquals(map1, map4)
        assertNotEquals(map1, map5)
        assertNotEquals(map1, null as CborElement?)
        assertNotEquals(map1 as CborElement, CborString("map"))
        assertNotEquals(map1, CborString("map") as CborElement)
    }

    @Test
    fun testTagsEquality() {
        val tags1 = ulongArrayOf(1u, 2u, 3u)
        val tags2 = ulongArrayOf(1u, 2u, 3u)
        val tags3 = ulongArrayOf(1u, 2u, 4u)

        val string1 = CborString("test", tags = tags1)
        val string2 = CborString("test", tags = tags2)
        val string3 = CborString("test", tags = tags3)

        assertEquals(string1, string2)
        assertEquals(string1.hashCode(), string2.hashCode())
        assertNotEquals(string1, string3)
    }

    @Test
    fun testEmptyCollectionsEquality() {
        val emptyList1 = CborArray(emptyList())
        val emptyList2 = CborArray(emptyList())
        val emptyMap1 = CborMap(emptyMap())
        val emptyMap2 = CborMap(emptyMap())

        assertEquals(emptyList1, emptyList2)
        assertEquals(emptyList1.hashCode(), emptyList2.hashCode())
        assertEquals(emptyMap1, emptyMap2)
        assertEquals(emptyMap1.hashCode(), emptyMap2.hashCode())
        assertNotEquals(emptyList1 as CborElement, emptyMap1)
        assertNotEquals(emptyList1, emptyMap1 as CborElement)
    }

    @Test
    fun testNestedStructureEquality() {
        val nested1 = CborMap(
            mapOf(
                CborString("list") to CborArray(
                    listOf(
                        CborInt(1u),
                        CborMap(mapOf(CborString("inner") to CborNull()))
                    )
                )
            )
        )
        val nested2 = CborMap(
            mapOf(
                CborString("list") to CborArray(
                    listOf(
                        CborInt(1u),
                        CborMap(mapOf(CborString("inner") to CborNull()))
                    )
                )
            )
        )
        val nested3 = CborMap(
            mapOf(
                CborString("list") to CborArray(
                    listOf(
                        CborInt(2u),
                        CborMap(mapOf(CborString("inner") to CborNull()))
                    )
                )
            )
        )

        assertEquals(nested1, nested2)
        assertEquals(nested1.hashCode(), nested2.hashCode())
        assertNotEquals(nested1, nested3)
    }

    @Test
    fun testReflexiveEquality() {
        val elements = listOf(
            CborInt(42u),
            CborInt(-42),
            CborFloat(3.14),
            CborString("test"),
            CborBoolean(true),
            CborByteString(byteArrayOf(1, 2, 3)),
            CborNull(),
            CborArray(listOf(CborInt(1u))),
            CborMap(mapOf(CborString("key") to CborInt(1u)))
        )

        elements.forEach { element ->
            assertEquals(element, element, "Element should be equal to itself")
            assertEquals(element.hashCode(), element.hashCode(), "Hash code should be consistent")
        }
    }

    @Test
    fun testSymmetricEquality() {
        val pairs = listOf(
            CborInt(42u) to CborInt(42u),
            CborInt(-42) to CborInt(-42),
            CborFloat(3.14) to CborFloat(3.14),
            CborString("test") to CborString("test"),
            CborBoolean(true) to CborBoolean(true),
            CborByteString(byteArrayOf(1, 2, 3)) to CborByteString(byteArrayOf(1, 2, 3)),
            CborNull() to CborNull(),
            CborArray(listOf(CborInt(1u))) to CborArray(listOf(CborInt(1u))),
            CborMap(mapOf(CborString("key") to CborInt(1u))) to CborMap(
                mapOf(
                    CborString("key") to CborInt(
                        1u
                    )
                )
            )
        )

        pairs.forEach { (a, b) ->
            assertEquals(a, b, "a should equal b")
            assertEquals(b, a, "b should equal a (symmetry)")
            assertEquals(a.hashCode(), b.hashCode(), "Hash codes should be equal")
        }
    }

    @Test
    fun testTransitiveEquality() {
        val a = CborString("test")
        val b = CborString("test")
        val c = CborString("test")

        assertEquals(a, b)
        assertEquals(b, c)
        assertEquals(a, c, "Transitivity: if a==b and b==c, then a==c")
    }
}