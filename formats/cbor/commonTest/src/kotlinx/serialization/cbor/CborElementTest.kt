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
        val nullElement = CborNull()
        val nullBytes = cbor.encodeToByteArray(nullElement)
        val decodedNull = cbor.decodeFromByteArray<CborElement>(nullBytes)
        assertEquals(nullElement, decodedNull)
    }

    @Test
    fun testCborNumber() {
        val numberElement = CborPositiveInt(42u)
        val numberBytes = cbor.encodeToByteArray(numberElement)
        val decodedNumber = cbor.decodeFromByteArray<CborElement>(numberBytes)
        assertEquals(numberElement, decodedNumber)
        assertEquals(42u, (decodedNumber as CborPositiveInt).value)
    }

    @Test
    fun testCborString() {
        val stringElement = CborString("Hello, CBOR!")
        val stringBytes = cbor.encodeToByteArray(stringElement)
        val decodedString = cbor.decodeFromByteArray<CborElement>(stringBytes)
        assertEquals(stringElement, decodedString)
        assertEquals("Hello, CBOR!", (decodedString as CborString).value)
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
                CborPositiveInt(1u),
                CborString("two"),
                CborBoolean(true),
                CborNull()
            )
        )
        val listBytes = cbor.encodeToByteArray(listElement)
        val decodedList = cbor.decodeFromByteArray<CborElement>(listBytes)

        // Verify the type and size
        assertTrue(decodedList is CborList)
        assertEquals(4, decodedList.size)

        // Verify individual elements
        assertTrue(decodedList[0] is CborPositiveInt)
        assertEquals(1u, (decodedList[0] as CborPositiveInt).value)

        assertTrue(decodedList[1] is CborString)
        assertEquals("two", (decodedList[1] as CborString).value)

        assertTrue(decodedList[2] is CborBoolean)
        assertEquals(true, (decodedList[2] as CborBoolean).boolean)

        assertTrue(decodedList[3] is CborNull)
    }

    @Test
    fun testCborMap() {
        val mapElement = CborMap(
            mapOf(
                CborString("key1") to CborPositiveInt(42u),
                CborString("key2") to CborString("value"),
                CborPositiveInt(3u) to CborBoolean(true),
                CborNull() to CborNull()
            )
        )
        val mapBytes = cbor.encodeToByteArray(mapElement)
        val decodedMap = cbor.decodeFromByteArray<CborElement>(mapBytes)

        // Verify the type and size
        assertTrue(decodedMap is CborMap)
        assertEquals(4, decodedMap.size)

        // Verify individual entries
        assertTrue(decodedMap.containsKey(CborString("key1")))
        val value1 = decodedMap[CborString("key1")]
        assertTrue(value1 is CborPositiveInt)
        assertEquals(42u, (value1 as CborPositiveInt).value)

        assertTrue(decodedMap.containsKey(CborString("key2")))
        val value2 = decodedMap[CborString("key2")]
        assertTrue(value2 is CborString)
        assertEquals("value", (value2 as CborString).value)

        assertTrue(decodedMap.containsKey(CborPositiveInt(3u)))
        val value3 = decodedMap[CborPositiveInt(3u)]
        assertTrue(value3 is CborBoolean)
        assertEquals(true, (value3 as CborBoolean).boolean)

        assertTrue(decodedMap.containsKey(CborNull()))
        val value4 = decodedMap[CborNull()]
        assertTrue(value4 is CborNull)
    }

    @Test
    fun testComplexNestedStructure() {
        // Create a complex nested structure with maps and lists
        val complexElement = CborMap(
            mapOf(
                CborString("primitives") to CborList(
                    listOf(
                        CborPositiveInt(123u),
                        CborString("text"),
                        CborBoolean(false),
                        CborByteString(byteArrayOf(10, 20, 30)),
                        CborNull()
                    )
                ),
                CborString("nested") to CborMap(
                    mapOf(
                        CborString("inner") to CborList(
                            listOf(
                                CborPositiveInt(1u),
                                CborPositiveInt(2u)
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

        // Verify the primitives list
        assertTrue(decodedComplex.containsKey(CborString("primitives")))
        val primitivesValue = decodedComplex[CborString("primitives")]
        assertTrue(primitivesValue is CborList)

        assertEquals(5, primitivesValue.size)

        assertTrue(primitivesValue[0] is CborPositiveInt)
        assertEquals(123u, (primitivesValue[0] as CborPositiveInt).value)

        assertTrue(primitivesValue[1] is CborString)
        assertEquals("text", (primitivesValue[1] as CborString).value)

        assertTrue(primitivesValue[2] is CborBoolean)
        assertEquals(false, (primitivesValue[2] as CborBoolean).boolean)

        assertTrue(primitivesValue[3] is CborByteString)
        assertTrue((primitivesValue[3] as CborByteString).bytes.contentEquals(byteArrayOf(10, 20, 30)))

        assertTrue(primitivesValue[4] is CborNull)

        // Verify the nested map
        assertTrue(decodedComplex.containsKey(CborString("nested")))
        val nestedValue = decodedComplex[CborString("nested")]
        assertTrue(nestedValue is CborMap)

        assertEquals(2, nestedValue.size)

        // Verify the inner list
        assertTrue(nestedValue.containsKey(CborString("inner")))
        val innerValue = nestedValue[CborString("inner")]
        assertTrue(innerValue is CborList)

        assertEquals(2, innerValue.size)

        assertTrue(innerValue[0] is CborPositiveInt)
        assertEquals(1u, (innerValue[0] as CborPositiveInt).value)

        assertTrue(innerValue[1] is CborPositiveInt)
        assertEquals(2u, (innerValue[1] as CborPositiveInt).value)

        // Verify the empty list
        assertTrue(nestedValue.containsKey(CborString("empty")))
        val emptyValue = nestedValue[CborString("empty")]
        assertTrue(emptyValue is CborList)
        val empty = emptyValue

        assertEquals(0, empty.size)
    }

    @Test
    fun testDecodeIntegers() {
        // Test data from CborParserTest.testParseIntegers
        val element = decodeHexToCborElement("0C") as CborPositiveInt
        assertEquals(12u, element.value)

    }

    @Test
    fun testDecodeStrings() {
        // Test data from CborParserTest.testParseStrings
        val element = decodeHexToCborElement("6568656C6C6F")
        assertTrue(element is CborString)
        assertEquals("hello", element.value)

        val longStringElement =
            decodeHexToCborElement("7828737472696E672074686174206973206C6F6E676572207468616E2032332063686172616374657273")
        assertTrue(longStringElement is CborString)
        assertEquals("string that is longer than 23 characters", longStringElement.value)
    }

    @Test
    fun testDecodeFloatingPoint() {
        // Test data from CborParserTest.testParseDoubles
        val doubleElement = decodeHexToCborElement("fb7e37e43c8800759c")
        assertTrue(doubleElement is CborDouble)
        assertEquals(1e+300, doubleElement.value)

        val floatElement = decodeHexToCborElement("fa47c35000")
        assertTrue(floatElement is CborDouble)
        assertEquals(100000.0f, floatElement.value.toFloat())
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
        assertEquals(1u, (list[0] as CborPositiveInt).value)
        assertEquals(255u, (list[1] as CborPositiveInt).value)
        assertEquals(65536u, (list[2] as CborPositiveInt).value)
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
        val element =
            decodeHexToCborElement("a461615f42cafe43010203ff61627f6648656c6c6f2065776f726c64ff61639f676b6f746c696e786d73657269616c697a6174696f6eff6164bf613101613202613303ff")
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
        assertEquals(CborPositiveInt(1u), nestedMap[CborString("1")])
        assertEquals(CborPositiveInt(2u), nestedMap[CborString("2")])
        assertEquals(CborPositiveInt(3u), nestedMap[CborString("3")])
    }

    // Test removed due to incompatibility with the new tag implementation

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testTagsRoundTrip() {
        // Create a CborElement with tags
        val originalElement = CborString("Hello, tagged world!", tags = ulongArrayOf(42u))

        // Encode and decode
        val bytes = cbor.encodeToByteArray(originalElement)
        println(bytes.toHexString())
        val decodedElement = cbor.decodeFromByteArray<CborElement>(bytes)

        // Verify the value and tags
        assertTrue(decodedElement is CborString)
        assertEquals("Hello, tagged world!", decodedElement.value)
        assertNotNull(decodedElement.tags)
        assertEquals(1, decodedElement.tags.size)
        assertEquals(42u, decodedElement.tags.first())
    }
}
