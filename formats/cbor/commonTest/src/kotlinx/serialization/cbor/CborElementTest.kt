package kotlinx.serialization.cbor

import kotlinx.serialization.*
import kotlinx.serialization.cbor.internal.*
import kotlin.test.*

class CborElementTest {

    private val cbor = Cbor {}

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testEncodeToCborElementRootPrimitiveInt() {
        val element = cbor.encodeToCborElement(42)
        assertEquals(CborInt(42), element)
        assertEquals(42, cbor.decodeFromCborElement<Int>(element))
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testEncodeToCborElementRootPrimitiveByteArrayAlwaysUseByteString() {
        val configured = Cbor { alwaysUseByteString = true }
        val element = configured.encodeToCborElement(byteArrayOf(1, 2, 3))
        assertTrue(element is CborByteString)
        assertTrue(element.value.contentEquals(byteArrayOf(1, 2, 3)))
        assertTrue(configured.decodeFromCborElement<ByteArray>(element).contentEquals(byteArrayOf(1, 2, 3)))
    }

    @Serializable
    private data class Wrapped(val x: Int)

    @Serializable
    private data class Wrapper(val datum: Wrapped?)

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testEncodeDecodeNullableClassViaCborElement() {
        val wrapper = Wrapper(null)
        val element = cbor.encodeToCborElement(wrapper)
        assertEquals(wrapper, cbor.decodeFromCborElement<Wrapper>(element))
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testEncodeDecodeRootListViaCborElement() {
        val value = listOf(1, 2, 3)
        val element = cbor.encodeToCborElement(value)
        assertTrue(element is CborArray)
        assertEquals(value, cbor.decodeFromCborElement<List<Int>>(element))
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
        val numberElement = CborInt(42u)
        val numberBytes = cbor.encodeToByteArray(numberElement)
        val decodedNumber = cbor.decodeFromByteArray<CborElement>(numberBytes)
        assertEquals(numberElement, decodedNumber)
        assertEquals(42u, (decodedNumber as CborInteger).value)
    }

    @Test
    fun testCborNumberZero() {
        val numberElement = CborInt(0uL)
        assertEquals(numberElement, CborInt(0))
        assertEquals(numberElement.isPositive, true)
        assertEquals(numberElement.value, 0uL)
        val numberBytes = cbor.encodeToByteArray(numberElement)
        val decodedNumber = cbor.decodeFromByteArray<CborElement>(numberBytes)
        assertEquals(numberElement, decodedNumber)
        assertEquals(0uL, (decodedNumber as CborInteger).value)
    }

    @Test
    fun testCborNumberMax() {
        val numberElement = CborInt(ULong.MAX_VALUE)
        assertEquals(numberElement.isPositive, true)
        assertEquals(numberElement.value, ULong.MAX_VALUE)
        val numberBytes = cbor.encodeToByteArray(numberElement)
        val decodedNumber = cbor.decodeFromByteArray<CborElement>(numberBytes)
        assertEquals(numberElement, decodedNumber)
        assertEquals(ULong.MAX_VALUE, (decodedNumber as CborInteger).value)
    }

    @Test
    fun testCborNumberMaxHalv() {
        val numberElement = CborInt(Long.MAX_VALUE)
        assertEquals(numberElement.isPositive, true)
        assertEquals(numberElement.value, Long.MAX_VALUE.toULong())
        val numberBytes = cbor.encodeToByteArray(numberElement)
        val decodedNumber = cbor.decodeFromByteArray<CborElement>(numberBytes)
        assertEquals(numberElement, decodedNumber)
        assertEquals(Long.MAX_VALUE.toULong(), (decodedNumber as CborInteger).value)
    }


    @Test
    fun testCborNumberMin() {
        val numberElement = CborInteger(ULong.MAX_VALUE, isPositive = false)
        assertEquals(numberElement.isPositive, false)
        assertEquals(numberElement.value, ULong.MAX_VALUE)
        val numberBytes = cbor.encodeToByteArray(numberElement)
        val decodedNumber = cbor.decodeFromByteArray<CborElement>(numberBytes)
        assertEquals(numberElement, decodedNumber)
        assertEquals(ULong.MAX_VALUE, (decodedNumber as CborInteger).value)

        assertNull(numberElement.longOrNull)
        assertFailsWith<ArithmeticException> { numberElement.long }
        assertFailsWith<SerializationException> { cbor.decodeFromCborElement<Long>(numberElement) }
    }


    @Test
    fun testCborNumberMinHalv() {
        val numberElement = CborInteger(Long.MAX_VALUE.toULong(), isPositive = false)
        assertEquals(numberElement.isPositive, false)
        assertEquals(numberElement.value, Long.MAX_VALUE.toULong())
        val numberBytes = cbor.encodeToByteArray(numberElement)
        val decodedNumber = cbor.decodeFromByteArray<CborElement>(numberBytes)
        assertEquals(numberElement, decodedNumber)
        assertEquals(Long.MAX_VALUE.toULong(), (decodedNumber as CborInteger).value)

        val long = cbor.decodeFromCborElement<Long>(numberElement)

        assertEquals(Long.MIN_VALUE+1, long)
        assertEquals(Long.MIN_VALUE + 1, numberElement.long)
    }



    @Test
    fun testCborNumberLong() {
        assertEquals(Long.MAX_VALUE, CborInt(Long.MAX_VALUE).long)
        assertEquals(Long.MIN_VALUE, CborInt(Long.MIN_VALUE).long)
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
        assertEquals(true, (decodedBoolean as CborBoolean).value)
    }

    @Test
    fun testCborByteString() {
        val byteArray = byteArrayOf(1, 2, 3, 4, 5)
        val byteStringElement = CborByteString(byteArray)
        val byteStringBytes = cbor.encodeToByteArray(byteStringElement)
        val decodedByteString = cbor.decodeFromByteArray<CborElement>(byteStringBytes)
        assertEquals(byteStringElement, decodedByteString)
        assertTrue((decodedByteString as CborByteString).value.contentEquals(byteArray))
    }

    @Test
    fun testCborArray() {
        val listElement = CborArray(
            listOf(
                CborInt(1u),
                CborString("two"),
                CborBoolean(true),
                CborNull()
            )
        )
        val listBytes = cbor.encodeToByteArray(listElement)
        val decodedList = cbor.decodeFromByteArray<CborElement>(listBytes)

        // Verify the type and size
        assertTrue(decodedList is CborArray)
        assertEquals(4, decodedList.size)

        // Verify individual elements
        assertTrue(decodedList[0] is CborInteger)
        assertEquals(1u, (decodedList[0] as CborInteger).value)

        assertTrue(decodedList[1] is CborString)
        assertEquals("two", (decodedList[1] as CborString).value)

        assertTrue(decodedList[2] is CborBoolean)
        assertEquals(true, (decodedList[2] as CborBoolean).value)

        assertTrue(decodedList[3] is CborNull)
    }

    @Test
    fun testCborMap() {
        val mapElement = CborMap(
            mapOf(
                CborString("key1") to CborInt(42u),
                CborString("key2") to CborString("value"),
                CborInt(3u) to CborBoolean(true),
                CborNull() to CborNull()
            )
        )
        val mapBytes = cbor.encodeToByteArray(mapElement)

        val output = ByteArrayOutput()
        IndefiniteLengthCborWriter(cbor, output).encodeCborElement(mapElement)
        assertEquals(mapBytes.toHexString(),output.toByteArray().toHexString() )

        val decodedMap = cbor.decodeFromByteArray<CborElement>(mapBytes)

        // Verify the type and size
        assertTrue(decodedMap is CborMap)
        assertEquals(4, decodedMap.size)

        // Verify individual entries
        assertTrue(decodedMap.containsKey(CborString("key1")))
        val value1 = decodedMap[CborString("key1")]
        assertTrue(value1 is CborInteger)
        assertEquals(42u, (value1 as CborInteger).value)

        assertTrue(decodedMap.containsKey(CborString("key2")))
        val value2 = decodedMap[CborString("key2")]
        assertTrue(value2 is CborString)
        assertEquals("value", (value2 as CborString).value)

        assertTrue(decodedMap.containsKey(CborInt(3u)))
        val value3 = decodedMap[CborInt(3u)]
        assertTrue(value3 is CborBoolean)
        assertEquals(true, (value3 as CborBoolean).value)

        assertTrue(decodedMap.containsKey(CborNull()))
        val value4 = decodedMap[CborNull()]
        assertTrue(value4 is CborNull)
    }

    @Test
    fun testComplexNestedStructure() {
        // Create a complex nested structure with maps and lists
        val complexElement = CborMap(
            mapOf(
                CborString("primitives") to CborArray(
                    listOf(
                        CborInt(123u),
                        CborString("text"),
                        CborBoolean(false),
                        CborByteString(byteArrayOf(10, 20, 30)),
                        CborNull()
                    )
                ),
                CborString("nested") to CborMap(
                    mapOf(
                        CborString("inner") to CborArray(
                            listOf(
                                CborInt(1u),
                                CborInt(2u)
                            )
                        ),
                        CborString("empty") to CborArray(emptyList())
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
        assertTrue(primitivesValue is CborArray)

        assertEquals(5, primitivesValue.size)

        assertTrue(primitivesValue[0] is CborInteger)
        assertEquals(123u, (primitivesValue[0] as CborInteger).value)

        assertTrue(primitivesValue[1] is CborString)
        assertEquals("text", (primitivesValue[1] as CborString).value)

        assertTrue(primitivesValue[2] is CborBoolean)
        assertEquals(false, (primitivesValue[2] as CborBoolean).value)

        assertTrue(primitivesValue[3] is CborByteString)
        assertTrue((primitivesValue[3] as CborByteString).value.contentEquals(byteArrayOf(10, 20, 30)))

        assertTrue(primitivesValue[4] is CborNull)

        // Verify the nested map
        assertTrue(decodedComplex.containsKey(CborString("nested")))
        val nestedValue = decodedComplex[CborString("nested")]
        assertTrue(nestedValue is CborMap)

        assertEquals(2, nestedValue.size)

        // Verify the inner list
        assertTrue(nestedValue.containsKey(CborString("inner")))
        val innerValue = nestedValue[CborString("inner")]
        assertTrue(innerValue is CborArray)

        assertEquals(2, innerValue.size)

        assertTrue(innerValue[0] is CborInteger)
        assertEquals(1u, (innerValue[0] as CborInteger).value)

        assertTrue(innerValue[1] is CborInteger)
        assertEquals(2u, (innerValue[1] as CborInteger).value)

        // Verify the empty list
        assertTrue(nestedValue.containsKey(CborString("empty")))
        val emptyValue = nestedValue[CborString("empty")]
        assertTrue(emptyValue is CborArray)
        val empty = emptyValue

        assertEquals(0, empty.size)
    }

    @Test
    fun testDecodePositiveInt() {
        // Test data from CborParserTest.testParseIntegers
        val element = cbor.decodeFromHexString<CborElement>("0C") as CborInteger
        assertEquals(12u, element.value)
    }

    @Test
    fun testDecodeStrings() {
        // Test data from CborParserTest.testParseStrings
        val element = cbor.decodeFromHexString<CborElement>("6568656C6C6F")
        assertTrue(element is CborString)
        assertEquals("hello", element.value)

        val longStringElement =
            cbor.decodeFromHexString<CborElement>("7828737472696E672074686174206973206C6F6E676572207468616E2032332063686172616374657273")
        assertTrue(longStringElement is CborString)
        assertEquals("string that is longer than 23 characters", longStringElement.value)
    }

    @Test
    fun testDecodeFloatingPoint() {
        // Test data from CborParserTest.testParseDoubles
        val doubleElement = cbor.decodeFromHexString<CborElement>("fb7e37e43c8800759c")
        assertTrue(doubleElement is CborFloat)
        assertEquals(1e+300, doubleElement.value)

        val floatElement = cbor.decodeFromHexString<CborElement>("fa47c35000")
        assertTrue(floatElement is CborFloat)
        assertEquals(100000.0f, floatElement.value.toFloat())
    }

    @Test
    fun testDecodeByteString() {
        // Test data from CborParserTest.testRfc7049IndefiniteByteStringExample
        val element = cbor.decodeFromHexString<CborElement>("5F44aabbccdd43eeff99FF")
        assertTrue(element is CborByteString)
        val byteString = element as CborByteString
        val expectedBytes = HexConverter.parseHexBinary("aabbccddeeff99")
        assertTrue(byteString.value.contentEquals(expectedBytes))
    }

    @Test
    fun testDecodeArray() {
        // Test data from CborParserTest.testSkipCollections
        val element = cbor.decodeFromHexString<CborElement>("830118ff1a00010000")
        assertTrue(element is CborArray)
        val list = element as CborArray
        assertEquals(3, list.size)
        assertEquals(1u, (list[0] as CborInteger).value)
        assertEquals(255u, (list[1] as CborInteger).value)
        assertEquals(65536u, (list[2] as CborInteger).value)
    }

    @Test
    fun testDecodeMap() {
        // Test data from CborParserTest.testSkipCollections
        val element = cbor.decodeFromHexString<CborElement>("a26178676b6f746c696e7861796d73657269616c697a6174696f6e")
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
            cbor.decodeFromHexString<CborElement>("a461615f42cafe43010203ff61627f6648656c6c6f2065776f726c64ff61639f676b6f746c696e786d73657269616c697a6174696f6eff6164bf613101613202613303ff")
        assertTrue(element is CborMap)
        val map = element as CborMap
        assertEquals(4, map.size)

        // Check the byte string
        val byteString = map[CborString("a")] as CborByteString
        val expectedBytes = HexConverter.parseHexBinary("cafe010203")
        assertTrue(byteString.value.contentEquals(expectedBytes))

        // Check the text string
        assertEquals(CborString("Hello world"), map[CborString("b")])

        // Check the array
        val array = map[CborString("c")] as CborArray
        assertEquals(2, array.size)
        assertEquals(CborString("kotlinx"), array[0])
        assertEquals(CborString("serialization"), array[1])

        // Check the nested map
        val nestedMap = map[CborString("d")] as CborMap
        assertEquals(3, nestedMap.size)
        assertEquals(CborInt(1u), nestedMap[CborString("1")])
        assertEquals(CborInt(2u), nestedMap[CborString("2")])
        assertEquals(CborInt(3u), nestedMap[CborString("3")])
    }

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
        assertEquals(1, decodedElement.tags.size)
        assertEquals(42u, decodedElement.tags.first())
    }

    @Test
    fun testGenericAndCborSpecificMixed() {
        Triple(
            Cbor {
                encodeValueTags = true
                encodeKeyTags = true
                verifyKeyTags = true
                verifyObjectTags = true
                verifyValueTags = true
            },
            MixedBag(
                str = "A string, is a string, is a string",
                bStr = CborByteString(byteArrayOf()),
                cborElement = CborBoolean(false),
                cborPositiveInt = CborInt(1u),
                cborInt = CborInt(-1),
                tagged = 26
            ),
            "bf6373747278224120737472696e672c206973206120737472696e672c206973206120737472696e676462537472406b63626f72456c656d656e74f46f63626f72506f736974697665496e74016763626f72496e7420d82a66746167676564d90921181aff"
        )
            .let { (cbor, obj, hex) ->
                val struct = cbor.encodeToCborElement(obj)
                assertEquals(hex, cbor.encodeToHexString(obj))
                assertEquals(hex, cbor.encodeToHexString(struct))
                assertEquals(struct, cbor.decodeFromHexString<CborElement>(hex))
                assertEquals(obj, cbor.decodeFromCborElement(struct))
                assertEquals(obj, cbor.decodeFromHexString(hex))
            }

        Triple(
            Cbor {
                encodeValueTags = true
                encodeKeyTags = true
                verifyKeyTags = true
                verifyObjectTags = true
                verifyValueTags = true
            },
            MixedBag(
                str = "A string, is a string, is a string",
                bStr = null,
                cborElement = CborBoolean(false),
                cborPositiveInt = CborInt(1u),
                cborInt = CborInt(-1),
                tagged = 26
            ),
            "bf6373747278224120737472696e672c206973206120737472696e672c206973206120737472696e676462537472f66b63626f72456c656d656e74f46f63626f72506f736974697665496e74016763626f72496e7420d82a66746167676564d90921181aff"
        )
            .let { (cbor, obj, hex) ->
                val struct = cbor.encodeToCborElement(obj)
                assertEquals(hex, cbor.encodeToHexString(obj))
                assertEquals(hex, cbor.encodeToHexString(struct))
                assertEquals(struct, cbor.decodeFromHexString<CborElement>(hex))
                assertEquals(obj, cbor.decodeFromCborElement(struct))
                assertEquals(obj, cbor.decodeFromHexString(hex))
            }


        Triple(
            Cbor {
                encodeValueTags = true
                encodeKeyTags = true
                verifyKeyTags = true
                verifyObjectTags = true
                verifyValueTags = true
            },
            MixedBag(
                str = "A string, is a string, is a string",
                bStr = null,
                cborElement = CborMap(mapOf(CborByteString(byteArrayOf(1, 3, 3, 7)) to CborNull())),
                cborPositiveInt = CborInt(1u),
                cborInt = CborInt(-1),
                tagged = 26
            ),
            "bf6373747278224120737472696e672c206973206120737472696e672c206973206120737472696e676462537472f66b63626f72456c656d656e74bf4401030307f6ff6f63626f72506f736974697665496e74016763626f72496e7420d82a66746167676564d90921181aff"
        )
            .let { (cbor, obj, hex) ->
                val struct = cbor.encodeToCborElement(obj)
                assertEquals(hex, cbor.encodeToHexString(obj))
                assertEquals(hex, cbor.encodeToHexString(struct))
                assertEquals(struct, cbor.decodeFromHexString<CborElement>(hex))
                assertEquals(obj, cbor.decodeFromCborElement(struct))
                assertEquals(obj, cbor.decodeFromHexString(hex))
            }



        Triple(
            Cbor {
                encodeValueTags = true
                encodeKeyTags = true
                verifyKeyTags = true
                verifyObjectTags = true
                verifyValueTags = true
            },
            MixedBag(
                str = "A string, is a string, is a string",
                bStr = null,
                cborElement = CborNull(),
                cborPositiveInt = CborInt(1u),
                cborInt = CborInt(-1),
                tagged = 26
            ),
            "bf6373747278224120737472696e672c206973206120737472696e672c206973206120737472696e676462537472f66b63626f72456c656d656e74f66f63626f72506f736974697665496e74016763626f72496e7420d82a66746167676564d90921181aff"
        )
            .let { (cbor, obj, hex) ->
                val struct = cbor.encodeToCborElement(obj)
                assertEquals(hex, cbor.encodeToHexString(obj))
                assertEquals(hex, cbor.encodeToHexString(struct))
                assertEquals(struct, cbor.decodeFromHexString<CborElement>(hex))
                //we have an ambiguity here (null vs. CborNull), so we cannot compare for equality with the object
                //assertEquals(obj, cbor.decodeFromCbor(struct))
                //assertEquals(obj, cbor.decodeFromHexString(hex))
            }

        Triple(
            Cbor {
                encodeValueTags = true
                encodeKeyTags = true
                verifyKeyTags = true
                verifyObjectTags = true
                verifyValueTags = true
            },
            MixedBag(
                str = "A string, is a string, is a string",
                bStr = CborByteString(byteArrayOf(), 1u, 3u),
                cborElement = CborBoolean(false),
                cborPositiveInt = CborInt(1u),
                cborInt = CborInt(-1),
                tagged = 26
            ),
            "bf6373747278224120737472696e672c206973206120737472696e672c206973206120737472696e676462537472c1c3406b63626f72456c656d656e74f46f63626f72506f736974697665496e74016763626f72496e7420d82a66746167676564d90921181aff"
        )
            .let { (cbor, obj, hex) ->
                val struct = cbor.encodeToCborElement(obj)
                assertEquals(hex, cbor.encodeToHexString(obj))
                assertEquals(hex, cbor.encodeToHexString(struct))
                assertEquals(struct, cbor.decodeFromHexString<CborElement>(hex))
                assertEquals(obj, cbor.decodeFromCborElement(struct))
                assertEquals(obj, cbor.decodeFromHexString(hex))
            }

        Triple(
            Cbor {
                encodeValueTags = false
                encodeKeyTags = true
                verifyKeyTags = true
                verifyObjectTags = true
                verifyValueTags = false
            },
            MixedBag(
                str = "A string, is a string, is a string",
                bStr = CborByteString(byteArrayOf(), 1u, 3u),
                cborElement = CborBoolean(false),
                cborPositiveInt = CborInt(1u),
                cborInt = CborInt(-1),
                tagged = 26
            ),
            "bf6373747278224120737472696e672c206973206120737472696e672c206973206120737472696e676462537472c1c3406b63626f72456c656d656e74f46f63626f72506f736974697665496e74016763626f72496e7420d82a66746167676564181aff"
        )
            .let { (cbor, obj, hex) ->
                val struct = cbor.encodeToCborElement(obj)
                assertEquals(hex, cbor.encodeToHexString(obj))
                assertEquals(hex, cbor.encodeToHexString(struct))
                assertEquals(struct, cbor.decodeFromHexString<CborElement>(hex))
                assertEquals(obj, cbor.decodeFromCborElement(struct))
                assertEquals(obj, cbor.decodeFromHexString(hex))
            }

        Triple(
            Cbor {
                encodeValueTags = true
                encodeKeyTags = true
                verifyKeyTags = true
                verifyObjectTags = true
                verifyValueTags = true
            },
            MixedTag(
                cborElement = CborBoolean(false),
            ),
            "bfd82a6b63626f72456c656d656e74d90921f4ff"
        )
            .let { (cbor, obj, hex) ->
                val struct = cbor.encodeToCborElement(obj)
                assertEquals(hex, cbor.encodeToHexString(obj))
                assertEquals(hex, cbor.encodeToHexString(struct))
                assertEquals(struct, cbor.decodeFromHexString<CborElement>(hex))
                // this is ambiguous. the cborBoolean has a tag attached in the struct, coming from the valueTag (as intended),
                // so now the resulting object won't have a tag but the cborElement property will have a tag attached
                // hence, the following two will have:
                //      Expected :MixedTag(cborElement=CborPrimitive(kind=Boolean, tags=, value=false))
                //      Actual   :MixedTag(cborElement=CborPrimitive(kind=Boolean, tags=2337, value=false))
                assertNotEquals(obj, cbor.decodeFromCborElement(struct))
                assertNotEquals(obj, cbor.decodeFromHexString(hex))
            }
        Triple(
            Cbor {
                encodeValueTags = true
                encodeKeyTags = true
                verifyKeyTags = true
                verifyObjectTags = true
                verifyValueTags = true
            },
            MixedTag(
                cborElement = CborBoolean(false, 90u),
            ),
            //valueTags first, then CborElement tags
            "bfd82a6b63626f72456c656d656e74d90921d85af4ff"
        )
            .let { (cbor, obj, hex) ->
                val struct = cbor.encodeToCborElement(obj)
                assertEquals(hex, cbor.encodeToHexString(obj))
                assertEquals(hex, cbor.encodeToHexString(struct))
                assertEquals(struct, cbor.decodeFromHexString<CborElement>(hex))
                // this is ambiguous. the cborBoolean has a tag attached in the struct, coming from the valueTag (as intended),
                // so now the resulting object won't have a tag but the cborElement property will have a tag attached
                // hence, the following two will have:
                //      Expected :MixedTag(cborElement=CborPrimitive(kind=Boolean, tags=90, value=false))
                //      Actual   :MixedTag(cborElement=CborPrimitive(kind=Boolean, tags=2337, 90, value=false))
                //of course, the value tag verification will also fail hard
                assertFailsWith(
                    CborDecodingException::class,
                    "CBOR tags [2337, 90] do not match expected tags [2337]"
                ) {
                    assertNotEquals(obj, cbor.decodeFromCborElement(struct))
                }
                assertFailsWith(
                    CborDecodingException::class,
                    "CBOR tags [2337, 90] do not match expected tags [2337]"
                ) {
                    assertNotEquals(obj, cbor.decodeFromHexString(hex))
                }
            }

        Triple(
            Cbor {
                encodeValueTags = true
                encodeKeyTags = true
                verifyKeyTags = true
                verifyObjectTags = true
                verifyValueTags = false
            },
            MixedTag(
                cborElement = CborBoolean(false, 90u),
            ),
            //valueTags first, then CborElement tags
            "bfd82a6b63626f72456c656d656e74d90921d85af4ff"
        )
            .let { (cbor, obj, hex) ->
                val struct = cbor.encodeToCborElement(obj)
                assertEquals(hex, cbor.encodeToHexString(obj))
                assertEquals(hex, cbor.encodeToHexString(struct))
                assertEquals(struct, cbor.decodeFromHexString<CborElement>(hex))
                // this is ambiguous. the cborBoolean has a tag attached in the struct, coming from the valueTag (as intended),
                // so now the resulting object won't have a tag but the cborElement property will have a tag attached
                // hence, the following two will have:
                //      Expected :MixedTag(cborElement=CborPrimitive(kind=Boolean, tags=90, value=false))
                //      Actual   :MixedTag(cborElement=CborPrimitive(kind=Boolean, tags=2337, 90, value=false))
                assertNotEquals(obj, cbor.decodeFromCborElement(struct))
                assertNotEquals(obj, cbor.decodeFromHexString(hex))
            }


        Triple(
            Cbor {
                encodeValueTags = false
                encodeKeyTags = true
                verifyKeyTags = true
                verifyObjectTags = true
                verifyValueTags = false
            },
            MixedTag(
                cborElement = CborBoolean(false, 90u),
            ),
            "bfd82a6b63626f72456c656d656e74d85af4ff"
        )
            .let { (cbor, obj, hex) ->
                val struct = cbor.encodeToCborElement(obj)
                assertEquals(hex, cbor.encodeToHexString(obj))
                assertEquals(hex, cbor.encodeToHexString(struct))
                assertEquals(struct, cbor.decodeFromHexString<CborElement>(hex))
                //no value tags means everything's fine again
                assertEquals(obj, cbor.decodeFromCborElement(struct))
                assertEquals(obj, cbor.decodeFromHexString(hex))
            }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun testCborUndefinedRoundTrip() {
        val element = CborUndefined(1uL)
        val bytes = cbor.encodeToByteArray(element)
        assertEquals("c1f7", bytes.toHexString())
        assertEquals(element, cbor.decodeFromByteArray<CborElement>(bytes))
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun testTagsPreservedWhenDecodingTypedElements() {
        val taggedMap = CborMap(mapOf(CborString("a") to CborInt(1)), 1uL)
        assertEquals(taggedMap, cbor.decodeFromByteArray<CborMap>(cbor.encodeToByteArray(taggedMap)))

        val taggedList = CborArray(listOf(CborInt(1)), 2uL)
        assertEquals(taggedList, cbor.decodeFromByteArray<CborArray>(cbor.encodeToByteArray(taggedList)))

        val taggedFloat = CborFloat(1.5, 3uL)
        assertEquals(taggedFloat, cbor.decodeFromByteArray<CborFloat>(cbor.encodeToByteArray(taggedFloat)))

        val taggedNull = CborNull(4uL)
        assertEquals(taggedNull, cbor.decodeFromByteArray<CborNull>(cbor.encodeToByteArray(taggedNull)))
    }

}

@Serializable
data class MixedBag(
    val str: String,
    val bStr: CborByteString?,
    val cborElement: CborElement?,
    val cborPositiveInt: CborPrimitive,
    val cborInt: CborInteger,
    @KeyTags(42u)
    @ValueTags(2337u)
    val tagged: Int
)


@Serializable
data class MixedTag(
    @KeyTags(42u)
    @ValueTags(2337u)
    val cborElement: CborElement?,
)
