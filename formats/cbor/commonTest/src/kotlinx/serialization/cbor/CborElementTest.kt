/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.cbor

import kotlinx.serialization.*
import kotlin.test.*

class CborElementTest {

    private val cbor = Cbor {}

    /**
     * Helper method to decode a hex string to a CborElement
     */
    private fun decodeHexToCborElement(hexString: String): CborElement {
        val bytes = HexConverter.parseHexBinary(hexString.uppercase())
        return cbor.decodeFromByteArray(bytes)
    }

    @Test
    fun testCborNull() {
        val nullElement = CborNull
        val nullBytes = cbor.encodeToByteArray(nullElement)
        val decodedNull = cbor.decodeFromByteArray<CborElement>(nullBytes)
        assertEquals(nullElement, decodedNull)
    }

    @Test
    fun testCborNumber() {
        val numberElement = CborNumber.Signed(42)
        val numberBytes = cbor.encodeToByteArray(numberElement)
        val decodedNumber = cbor.decodeFromByteArray<CborElement>(numberBytes)
        assertEquals(numberElement, decodedNumber)
        assertEquals(42, (decodedNumber as CborNumber).int)
    }

    @Test
    fun testCborString() {
        val stringElement = CborString("Hello, CBOR!")
        val stringBytes = cbor.encodeToByteArray(stringElement)
        val decodedString = cbor.decodeFromByteArray<CborElement>(stringBytes)
        assertEquals(stringElement, decodedString)
        assertEquals("Hello, CBOR!", (decodedString as CborString).content)
    }

    @Test
    fun testCborBoolean() {
        val booleanElement = CborBoolean(true)
        val booleanBytes = cbor.encodeToByteArray(booleanElement)
        val decodedBoolean = cbor.decodeFromByteArray<CborElement>(booleanBytes)
        assertEquals(booleanElement, decodedBoolean)
        assertEquals(true, (decodedBoolean as CborBoolean).boolean)
    }

    @Test
    fun testCborByteString() {
        val byteArray = byteArrayOf(1, 2, 3, 4, 5)
        val byteStringElement = CborByteString(byteArray)
        val byteStringBytes = cbor.encodeToByteArray(byteStringElement)
        val decodedByteString = cbor.decodeFromByteArray<CborElement>(byteStringBytes)
        assertEquals(byteStringElement, decodedByteString)
        assertTrue((decodedByteString as CborByteString).bytes.contentEquals(byteArray))
    }

    @Test
    fun testCborList() {
        val listElement = CborList(
            listOf(
                CborNumber.Signed(1),
                CborString("two"),
                CborBoolean(true),
                CborNull
            )
        )
        val listBytes = cbor.encodeToByteArray(listElement)
        val decodedList = cbor.decodeFromByteArray<CborElement>(listBytes)

        // Verify the type and size
        assertTrue(decodedList is CborList)
        val decodedCborList = decodedList as CborList
        assertEquals(4, decodedCborList.size)

        // Verify individual elements
        assertTrue(decodedCborList[0] is CborNumber)
        assertEquals(1, (decodedCborList[0] as CborNumber).int)

        assertTrue(decodedCborList[1] is CborString)
        assertEquals("two", (decodedCborList[1] as CborString).content)

        assertTrue(decodedCborList[2] is CborBoolean)
        assertEquals(true, (decodedCborList[2] as CborBoolean).boolean)

        assertTrue(decodedCborList[3] is CborNull)
    }

    @Test
    fun testCborMap() {
        val mapElement = CborMap(
            mapOf(
                CborString("key1") to CborNumber.Signed(42),
                CborString("key2") to CborString("value"),
                CborNumber.Signed(3) to CborBoolean(true),
                CborNull to CborNull
            )
        )
        val mapBytes = cbor.encodeToByteArray(mapElement)
        val decodedMap = cbor.decodeFromByteArray<CborElement>(mapBytes)

        // Verify the type and size
        assertTrue(decodedMap is CborMap)
        val decodedCborMap = decodedMap as CborMap
        assertEquals(4, decodedCborMap.size)

        // Verify individual entries
        assertTrue(decodedCborMap.containsKey(CborString("key1")))
        val value1 = decodedCborMap[CborString("key1")]
        assertTrue(value1 is CborNumber)
        assertEquals(42, (value1 as CborNumber).int)

        assertTrue(decodedCborMap.containsKey(CborString("key2")))
        val value2 = decodedCborMap[CborString("key2")]
        assertTrue(value2 is CborString)
        assertEquals("value", (value2 as CborString).content)

        assertTrue(decodedCborMap.containsKey(CborNumber.Signed(3)))
        val value3 = decodedCborMap[CborNumber.Signed(3)]
        assertTrue(value3 is CborBoolean)
        assertEquals(true, (value3 as CborBoolean).boolean)

        assertTrue(decodedCborMap.containsKey(CborNull))
        val value4 = decodedCborMap[CborNull]
        assertTrue(value4 is CborNull)
    }

    @Test
    fun testComplexNestedStructure() {
        // Create a complex nested structure with maps and lists
        val complexElement = CborMap(
            mapOf(
                CborString("primitives") to CborList(
                    listOf(
                        CborNumber.Signed(123),
                        CborString("text"),
                        CborBoolean(false),
                        CborByteString(byteArrayOf(10, 20, 30)),
                        CborNull
                    )
                ),
                CborString("nested") to CborMap(
                    mapOf(
                        CborString("inner") to CborList(
                            listOf(
                                CborNumber.Signed(1),
                                CborNumber.Signed(2)
                            )
                        ),
                        CborString("empty") to CborList(emptyList())
                    )
                )
            )
        )

        val complexBytes = cbor.encodeToByteArray(complexElement)
        val decodedComplex = cbor.decodeFromByteArray<CborElement>(complexBytes)

        // Verify the type
        assertTrue(decodedComplex is CborMap)
        val map = decodedComplex as CborMap

        // Verify the primitives list
        assertTrue(map.containsKey(CborString("primitives")))
        val primitivesValue = map[CborString("primitives")]
        assertTrue(primitivesValue is CborList)
        val primitives = primitivesValue as CborList

        assertEquals(5, primitives.size)

        assertTrue(primitives[0] is CborNumber)
        assertEquals(123, (primitives[0] as CborNumber).int)

        assertTrue(primitives[1] is CborString)
        assertEquals("text", (primitives[1] as CborString).content)

        assertTrue(primitives[2] is CborBoolean)
        assertEquals(false, (primitives[2] as CborBoolean).boolean)

        assertTrue(primitives[3] is CborByteString)
        assertTrue((primitives[3] as CborByteString).bytes.contentEquals(byteArrayOf(10, 20, 30)))

        assertTrue(primitives[4] is CborNull)

        // Verify the nested map
        assertTrue(map.containsKey(CborString("nested")))
        val nestedValue = map[CborString("nested")]
        assertTrue(nestedValue is CborMap)
        val nested = nestedValue as CborMap

        assertEquals(2, nested.size)

        // Verify the inner list
        assertTrue(nested.containsKey(CborString("inner")))
        val innerValue = nested[CborString("inner")]
        assertTrue(innerValue is CborList)
        val inner = innerValue as CborList

        assertEquals(2, inner.size)

        assertTrue(inner[0] is CborNumber)
        assertEquals(1, (inner[0] as CborNumber).int)

        assertTrue(inner[1] is CborNumber)
        assertEquals(2, (inner[1] as CborNumber).int)

        // Verify the empty list
        assertTrue(nested.containsKey(CborString("empty")))
        val emptyValue = nested[CborString("empty")]
        assertTrue(emptyValue is CborList)
        val empty = emptyValue as CborList

        assertEquals(0, empty.size)
    }

    @Test
    fun testDecodeIntegers() {
        // Test data from CborParserTest.testParseIntegers
        val element = decodeHexToCborElement("0C")  as CborNumber
        assertEquals(12, element.int)

    }

    @Test
    fun testDecodeStrings() {
        // Test data from CborParserTest.testParseStrings
        val element = decodeHexToCborElement("6568656C6C6F")
        assertTrue(element is CborString)
        assertEquals("hello", element.content)

        val longStringElement = decodeHexToCborElement("7828737472696E672074686174206973206C6F6E676572207468616E2032332063686172616374657273")
        assertTrue(longStringElement is CborString)
        assertEquals("string that is longer than 23 characters", longStringElement.content)
    }

    @Test
    fun testDecodeFloatingPoint() {
        // Test data from CborParserTest.testParseDoubles
        val doubleElement = decodeHexToCborElement("fb7e37e43c8800759c")
        assertTrue(doubleElement is CborNumber)
        assertEquals(1e+300, doubleElement.double)

        val floatElement = decodeHexToCborElement("fa47c35000")
        assertTrue(floatElement is CborNumber)
        assertEquals(100000.0f, floatElement.float)
    }

    @Test
    fun testDecodeByteString() {
        // Test data from CborParserTest.testRfc7049IndefiniteByteStringExample
        val element = decodeHexToCborElement("5F44aabbccdd43eeff99FF")
        assertTrue(element is CborByteString)
        val byteString = element as CborByteString
        val expectedBytes = HexConverter.parseHexBinary("aabbccddeeff99")
        assertTrue(byteString.bytes.contentEquals(expectedBytes))
    }

    @Test
    fun testDecodeArray() {
        // Test data from CborParserTest.testSkipCollections
        val element = decodeHexToCborElement("830118ff1a00010000")
        assertTrue(element is CborList)
        val list = element as CborList
        assertEquals(3, list.size)
        assertEquals(1, (list[0] as CborNumber).int)
        assertEquals(255, (list[1] as CborNumber).int)
        assertEquals(65536, (list[2] as CborNumber).int)
    }

    @Test
    fun testDecodeMap() {
        // Test data from CborParserTest.testSkipCollections
        val element = decodeHexToCborElement("a26178676b6f746c696e7861796d73657269616c697a6174696f6e")
        assertTrue(element is CborMap)
        val map = element as CborMap
        assertEquals(2, map.size)
        assertEquals(CborString("kotlinx"), map[CborString("x")])
        assertEquals(CborString("serialization"), map[CborString("y")])
    }

    @Test
    fun testDecodeComplexStructure() {
        // Test data from CborParserTest.testSkipIndefiniteLength
        val element = decodeHexToCborElement("a461615f42cafe43010203ff61627f6648656c6c6f2065776f726c64ff61639f676b6f746c696e786d73657269616c697a6174696f6eff6164bf613101613202613303ff")
        assertTrue(element is CborMap)
        val map = element as CborMap
        assertEquals(4, map.size)

        // Check the byte string
        val byteString = map[CborString("a")] as CborByteString
        val expectedBytes = HexConverter.parseHexBinary("cafe010203")
        assertTrue(byteString.bytes.contentEquals(expectedBytes))

        // Check the text string
        assertEquals(CborString("Hello world"), map[CborString("b")])

        // Check the array
        val array = map[CborString("c")] as CborList
        assertEquals(2, array.size)
        assertEquals(CborString("kotlinx"), array[0])
        assertEquals(CborString("serialization"), array[1])

        // Check the nested map
        val nestedMap = map[CborString("d")] as CborMap
        assertEquals(3, nestedMap.size)
        assertEquals(CborNumber.Signed(1), nestedMap[CborString("1")])
        assertEquals(CborNumber.Signed(2), nestedMap[CborString("2")])
        assertEquals(CborNumber.Signed(3), nestedMap[CborString("3")])
    }

    @Test
    fun testDecodeWithTags() {
        // Test data from CborParserTest.testSkipTags
        val element = decodeHexToCborElement("A46161CC1BFFFFFFFFFFFFFFFFD822616220D8386163D84E42CAFE6164D85ACC6B48656C6C6F20776F726C64")
        assertTrue(element is CborMap)
        val map = element as CborMap
        assertEquals(4, map.size)

        // The tags are not preserved in the CborElement structure, but the values should be correct
        assertEquals(CborNumber.Signed(Long.MAX_VALUE), map[CborString("a")])
        assertEquals(CborNumber.Signed(-1), map[CborString("b")])

        val byteString = map[CborString("c")] as CborByteString
        val expectedBytes = HexConverter.parseHexBinary("cafe")
        assertTrue(byteString.bytes.contentEquals(expectedBytes))

        assertEquals(CborString("Hello world"), map[CborString("d")])
    }
}
