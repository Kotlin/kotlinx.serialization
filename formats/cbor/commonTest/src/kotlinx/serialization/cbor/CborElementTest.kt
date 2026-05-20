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
        assertEquals(CborInteger(42), element)
        assertEquals(42, cbor.decodeFromCborElement<Int>(element))
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testEncodeToCborElementRootPrimitiveByteArrayAlwaysUseByteString() {
        val configured = Cbor { alwaysUseByteString = true }
        val element = configured.encodeToCborElement(byteArrayOf(1, 2, 3))
        assertTrue(element is CborByteString)
        assertTrue(element.toByteArray().contentEquals(byteArrayOf(1, 2, 3)))
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
        val numberElement = CborInteger(42u)
        val numberBytes = cbor.encodeToByteArray(numberElement)
        val decodedNumber = cbor.decodeFromByteArray<CborElement>(numberBytes)
        assertEquals(numberElement, decodedNumber)
        assertEquals(42uL, (decodedNumber as CborInteger).absoluteValue)
    }

    @Test
    fun testCborNumberZero() {
        val numberElement = CborInteger(0uL)
        assertEquals(numberElement, CborInteger(0))
        assertEquals(numberElement.isPositive, true)
        assertEquals(numberElement.absoluteValue, 0uL)
        val numberBytes = cbor.encodeToByteArray(numberElement)
        val decodedNumber = cbor.decodeFromByteArray<CborElement>(numberBytes)
        assertEquals(numberElement, decodedNumber)
        assertEquals(0uL, (decodedNumber as CborInteger).absoluteValue)
    }

    @Test
    fun testCborNumberMax() {
        val numberElement = CborInteger(ULong.MAX_VALUE)
        assertEquals(numberElement.isPositive, true)
        assertEquals(numberElement.absoluteValue, ULong.MAX_VALUE)
        val numberBytes = cbor.encodeToByteArray(numberElement)
        val decodedNumber = cbor.decodeFromByteArray<CborElement>(numberBytes)
        assertEquals(numberElement, decodedNumber)
        assertEquals(ULong.MAX_VALUE, (decodedNumber as CborInteger).absoluteValue)
    }

    @Test
    fun testCborNumberMaxHalv() {
        val numberElement = CborInteger(Long.MAX_VALUE)
        assertEquals(numberElement.isPositive, true)
        assertEquals(numberElement.absoluteValue, Long.MAX_VALUE.toULong())
        val numberBytes = cbor.encodeToByteArray(numberElement)
        val decodedNumber = cbor.decodeFromByteArray<CborElement>(numberBytes)
        assertEquals(numberElement, decodedNumber)
        assertEquals(Long.MAX_VALUE.toULong(), (decodedNumber as CborInteger).absoluteValue)
    }


    @Test
    fun testCborNumberMin() {
        val numberElement = CborInteger(ULong.MAX_VALUE, isPositive = false)
        assertEquals(numberElement.isPositive, false)
        assertEquals(numberElement.absoluteValue, ULong.MAX_VALUE)
        val numberBytes = cbor.encodeToByteArray(numberElement)
        val decodedNumber = cbor.decodeFromByteArray<CborElement>(numberBytes)
        assertEquals(numberElement, decodedNumber)
        assertEquals(ULong.MAX_VALUE, (decodedNumber as CborInteger).absoluteValue)

        assertNull(numberElement.longOrNull)
        assertFailsWith<ArithmeticException> { numberElement.long }
        assertFailsWith<SerializationException> { cbor.decodeFromCborElement<Long>(numberElement) }
    }


    @Test
    fun testCborNumberMinHalv() {
        val numberElement = CborInteger(Long.MAX_VALUE.toULong(), isPositive = false)
        assertEquals(numberElement.isPositive, false)
        assertEquals(numberElement.absoluteValue, Long.MAX_VALUE.toULong())
        val numberBytes = cbor.encodeToByteArray(numberElement)
        val decodedNumber = cbor.decodeFromByteArray<CborElement>(numberBytes)
        assertEquals(numberElement, decodedNumber)
        assertEquals(Long.MAX_VALUE.toULong(), (decodedNumber as CborInteger).absoluteValue)

        val long = cbor.decodeFromCborElement<Long>(numberElement)

        assertEquals(Long.MIN_VALUE+1, long)
        assertEquals(Long.MIN_VALUE + 1, numberElement.long)
    }



    @Test
    fun testCborNumberLong() {
        assertEquals(Long.MAX_VALUE, CborInteger(Long.MAX_VALUE).long)
        assertEquals(Long.MIN_VALUE, CborInteger(Long.MIN_VALUE).long)
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
        assertTrue((decodedByteString as CborByteString).toByteArray().contentEquals(byteArray))
    }

    @Test
    fun testCborArray() {
        val listElement = CborArray(
            listOf(
                CborInteger(1u),
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
        assertEquals(1uL, (decodedList[0] as CborInteger).absoluteValue)

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
                CborString("key1") to CborInteger(42u),
                CborString("key2") to CborString("value"),
                CborInteger(3u) to CborBoolean(true),
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
        assertEquals(42uL, (value1 as CborInteger).absoluteValue)

        assertTrue(decodedMap.containsKey(CborString("key2")))
        val value2 = decodedMap[CborString("key2")]
        assertTrue(value2 is CborString)
        assertEquals("value", (value2 as CborString).value)

        assertTrue(decodedMap.containsKey(CborInteger(3u)))
        val value3 = decodedMap[CborInteger(3u)]
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
                        CborInteger(123u),
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
                                CborInteger(1u),
                                CborInteger(2u)
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
        assertEquals(123uL, (primitivesValue[0] as CborInteger).absoluteValue)

        assertTrue(primitivesValue[1] is CborString)
        assertEquals("text", (primitivesValue[1] as CborString).value)

        assertTrue(primitivesValue[2] is CborBoolean)
        assertEquals(false, (primitivesValue[2] as CborBoolean).value)

        assertTrue(primitivesValue[3] is CborByteString)
        assertTrue((primitivesValue[3] as CborByteString).toByteArray().contentEquals(byteArrayOf(10, 20, 30)))

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
        assertEquals(1uL, (innerValue[0] as CborInteger).absoluteValue)

        assertTrue(innerValue[1] is CborInteger)
        assertEquals(2uL, (innerValue[1] as CborInteger).absoluteValue)

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
        assertEquals(12uL, element.absoluteValue)
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
        assertTrue(byteString.toByteArray().contentEquals(expectedBytes))
    }

    @Test
    fun testDecodeArray() {
        // Test data from CborParserTest.testSkipCollections
        val element = cbor.decodeFromHexString<CborElement>("830118ff1a00010000")
        assertTrue(element is CborArray)
        val list = element as CborArray
        assertEquals(3, list.size)
        assertEquals(1uL, (list[0] as CborInteger).absoluteValue)
        assertEquals(255uL, (list[1] as CborInteger).absoluteValue)
        assertEquals(65536uL, (list[2] as CborInteger).absoluteValue)
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
        assertTrue(byteString.toByteArray().contentEquals(expectedBytes))

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
        assertEquals(CborInteger(1u), nestedMap[CborString("1")])
        assertEquals(CborInteger(2u), nestedMap[CborString("2")])
        assertEquals(CborInteger(3u), nestedMap[CborString("3")])
    }

    @OptIn(ExperimentalStdlibApi::class)
    @Test
    fun testTagsRoundTrip() {
        val cbor = Cbor { encodeValueTags = true }
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
                cborPositiveInt = CborInteger(1u),
                cborInt = CborInteger(-1),
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
                cborPositiveInt = CborInteger(1u),
                cborInt = CborInteger(-1),
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
                cborPositiveInt = CborInteger(1u),
                cborInt = CborInteger(-1),
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
                cborPositiveInt = CborInteger(1u),
                cborInt = CborInteger(-1),
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
                cborPositiveInt = CborInteger(1u),
                cborInt = CborInteger(-1),
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
                cborPositiveInt = CborInteger(1u),
                cborInt = CborInteger(-1),
                tagged = 26
            ),
            "bf6373747278224120737472696e672c206973206120737472696e672c206973206120737472696e676462537472406b63626f72456c656d656e74f46f63626f72506f736974697665496e74016763626f72496e7420d82a66746167676564181aff"
        )
            .let { (cbor, obj, hex) ->
                val expectedDecoded = obj.copy(bStr = CborByteString(byteArrayOf()))
                val struct = cbor.encodeToCborElement(obj)
                assertEquals(hex, cbor.encodeToHexString(obj))
                assertEquals(hex, cbor.encodeToHexString(struct))
                assertEquals(struct, cbor.decodeFromHexString<CborElement>(hex))
                assertNotEquals(obj, cbor.decodeFromCborElement(struct))
                assertNotEquals(obj, cbor.decodeFromHexString(hex))
                assertEquals(expectedDecoded, cbor.decodeFromCborElement(struct))
                assertEquals(expectedDecoded, cbor.decodeFromHexString(hex))
            }

    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testCborElementWithValueTagsFails() {
        val cbor = Cbor { encodeValueTags = true }
        val message = assertFailsWith<SerializationException> {
            cbor.encodeToByteArray(MixedValueTaggedElement.serializer(), MixedValueTaggedElement(CborBoolean(false)))
        }.message
        assertEquals(
            "CBOR tag annotations cannot be applied to CborElement properties; add tags to the CborElement instance directly.",
            message
        )

        val structuredMessage = assertFailsWith<SerializationException> {
            cbor.encodeToCborElement(MixedValueTaggedElement.serializer(), MixedValueTaggedElement(CborBoolean(false)))
        }.message
        assertEquals(
            "CBOR tag annotations cannot be applied to CborElement properties; add tags to the CborElement instance directly.",
            structuredMessage
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testCborElementWithKeyTagsFails() {
        val cbor = Cbor { encodeKeyTags = true }
        val message = assertFailsWith<SerializationException> {
            cbor.encodeToByteArray(MixedKeyTaggedElement.serializer(), MixedKeyTaggedElement(CborBoolean(false)))
        }.message
        assertEquals(
            "KeyTags cannot be represented by a CborElement value; model the containing CborMap key directly if tagged keys are required.",
            message
        )

        val structuredMessage = assertFailsWith<SerializationException> {
            cbor.encodeToCborElement(MixedKeyTaggedElement.serializer(), MixedKeyTaggedElement(CborBoolean(false)))
        }.message
        assertEquals(
            "KeyTags cannot be represented by a CborElement value; model the containing CborMap key directly if tagged keys are required.",
            structuredMessage
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testConcreteCborElementWithValueTagsFails() {
        val cbor = Cbor { encodeValueTags = true }
        val message = assertFailsWith<SerializationException> {
            cbor.encodeToByteArray(MixedValueTaggedInteger.serializer(), MixedValueTaggedInteger(CborInteger(1)))
        }.message
        assertEquals(
            "CBOR tag annotations cannot be applied to CborElement properties; add tags to the CborElement instance directly.",
            message
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testTaggedCborElementPropertyDecodingFails() {
        val hex = cbor.encodeToHexString(CborMap(mapOf(CborString("cborElement") to CborBoolean(false))))
        val message = assertFailsWith<SerializationException> {
            cbor.decodeFromHexString(MixedValueTaggedElement.serializer(), hex)
        }.message
        assertEquals(
            "CBOR tag annotations cannot be applied to CborElement properties; add tags to the CborElement instance directly.",
            message
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testKeyTaggedCborElementPropertyDecodingFails() {
        val hex = cbor.encodeToHexString(CborMap(mapOf(CborString("cborElement") to CborBoolean(false))))
        val message = assertFailsWith<SerializationException> {
            cbor.decodeFromHexString(MixedKeyTaggedElement.serializer(), hex)
        }.message
        assertEquals(
            "KeyTags cannot be represented by a CborElement value; model the containing CborMap key directly if tagged keys are required.",
            message
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testConcreteCborElementPropertyDecodingFails() {
        val hex = cbor.encodeToHexString(CborMap(mapOf(CborString("cborElement") to CborInteger(1))))
        val message = assertFailsWith<SerializationException> {
            cbor.decodeFromHexString(MixedValueTaggedInteger.serializer(), hex)
        }.message
        assertEquals(
            "CBOR tag annotations cannot be applied to CborElement properties; add tags to the CborElement instance directly.",
            message
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testCborElementTagsRemainAllowed() {
        val cbor = Cbor { encodeValueTags = true }
        val element = CborBoolean(false, 2337u)
        val obj = MixedUntaggedElement(element)
        val hex = cbor.encodeToHexString(MixedUntaggedElement.serializer(), obj)
        assertEquals(obj, cbor.decodeFromHexString(MixedUntaggedElement.serializer(), hex))
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testTaggedCborMapKeyRemainsAllowed() {
        val cbor = Cbor { encodeKeyTags = true }
        val element = CborMap(mapOf(CborString("key", 42u) to CborBoolean(true)))
        val hex = cbor.encodeToHexString(CborElement.serializer(), element)
        assertEquals(element, cbor.decodeFromHexString(CborElement.serializer(), hex))
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun testCborUndefinedRoundTrip() {
        val cbor = Cbor { encodeValueTags = true }
        val element = CborUndefined(1uL)
        val bytes = cbor.encodeToByteArray(element)
        assertEquals("c1f7", bytes.toHexString())
        assertEquals(element, cbor.decodeFromByteArray<CborElement>(bytes))
    }

    @OptIn(ExperimentalSerializationApi::class, ExperimentalUnsignedTypes::class)
    @Test
    fun testEncodeCborElementWritesTaggedElements() {
        val cbor = Cbor {
            encodeKeyTags = true
            encodeValueTags = true
        }
        val element = CborMap(
            mapOf(
                CborString("undefined") to CborUndefined(1uL),
                CborString("positive") to CborInteger(ULong.MAX_VALUE, isPositive = true, tags = ulongArrayOf(2uL)),
                CborString("negative") to CborInteger(ULong.MAX_VALUE, isPositive = false, tags = ulongArrayOf(3uL)),
                CborString("bytes") to CborByteString(byteArrayOf(0xca.toByte(), 0xfe.toByte()), 4uL),
                CborString("array") to CborArray(listOf(CborNull(5uL)), 6uL),
            ),
            7uL
        )

        val bytes = cbor.encodeToByteArray(CborElement.serializer(), element)
        assertEquals(element, cbor.decodeFromByteArray(CborElement.serializer(), bytes))
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testRootCborElementTagsRespectEncodeValueTags() {
        val element = CborBoolean(false, 1u)

        assertEquals("f4", cbor.encodeToHexString(CborElement.serializer(), element))
        assertEquals("c1f4", Cbor { encodeValueTags = true }.encodeToHexString(CborElement.serializer(), element))
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testArrayCborElementTagsRespectEncodeValueTags() {
        val element = CborArray(listOf(CborBoolean(false, 1u)))

        assertEquals("9ff4ff", cbor.encodeToHexString(CborElement.serializer(), element))
        assertEquals(
            "9fc1f4ff",
            Cbor { encodeValueTags = true }.encodeToHexString(CborElement.serializer(), element)
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testMapCborElementTagsRespectKeyAndValueSwitches() {
        val element = CborMap(mapOf(CborString("k", 1u) to CborBoolean(false, 2u)))

        assertEquals("bf616bf4ff", cbor.encodeToHexString(CborElement.serializer(), element))
        assertEquals(
            "bfc1616bf4ff",
            Cbor { encodeKeyTags = true }.encodeToHexString(CborElement.serializer(), element)
        )
        assertEquals(
            "bf616bc2f4ff",
            Cbor { encodeValueTags = true }.encodeToHexString(CborElement.serializer(), element)
        )
        assertEquals(
            "bfc1616bc2f4ff",
            Cbor {
                encodeKeyTags = true
                encodeValueTags = true
            }.encodeToHexString(CborElement.serializer(), element)
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testNestedCborElementTagsRespectLocalPosition() {
        val element = CborMap(
            mapOf(
                CborString("outer") to CborMap(
                    mapOf(CborString("inner", 1u) to CborBoolean(false, 2u)),
                    3u
                )
            )
        )

        assertEquals(
            "bf656f75746572c3bf65696e6e6572c2f4ffff",
            Cbor { encodeValueTags = true }.encodeToHexString(CborElement.serializer(), element)
        )
        assertEquals(
            "bf656f75746572bfc165696e6e6572f4ffff",
            Cbor { encodeKeyTags = true }.encodeToHexString(CborElement.serializer(), element)
        )
        assertEquals(
            "bf656f75746572c3bfc165696e6e6572c2f4ffff",
            Cbor {
                encodeKeyTags = true
                encodeValueTags = true
            }.encodeToHexString(CborElement.serializer(), element)
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testCborMapTagsRespectPositionWhenUsedAsMapKey() {
        val element = CborMap(mapOf(CborMap(emptyMap(), 1u) to CborBoolean(true, 2u)))

        assertEquals("bfbffff5ff", cbor.encodeToHexString(CborElement.serializer(), element))
        assertEquals(
            "bfc1bffff5ff",
            Cbor { encodeKeyTags = true }.encodeToHexString(CborElement.serializer(), element)
        )
        assertEquals(
            "bfbfffc2f5ff",
            Cbor { encodeValueTags = true }.encodeToHexString(CborElement.serializer(), element)
        )
        assertEquals(
            "bfc1bfffc2f5ff",
            Cbor {
                encodeKeyTags = true
                encodeValueTags = true
            }.encodeToHexString(CborElement.serializer(), element)
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testEncodeToCborElementRespectsRawElementTagSwitches() {
        val element = CborMap(mapOf(CborString("k", 1u) to CborBoolean(false, 2u)), 3u)

        assertEquals(
            CborMap(mapOf(CborString("k") to CborBoolean(false))),
            cbor.encodeToCborElement(CborElement.serializer(), element)
        )
        assertEquals(
            CborMap(mapOf(CborString("k", 1u) to CborBoolean(false))),
            Cbor { encodeKeyTags = true }.encodeToCborElement(CborElement.serializer(), element)
        )
        assertEquals(
            CborMap(mapOf(CborString("k") to CborBoolean(false, 2u)), 3u),
            Cbor { encodeValueTags = true }.encodeToCborElement(CborElement.serializer(), element)
        )
        assertEquals(
            element,
            Cbor {
                encodeKeyTags = true
                encodeValueTags = true
            }.encodeToCborElement(CborElement.serializer(), element)
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testAllRootCborElementKindsRespectEncodeValueTags() {
        val elements = listOf(
            CborNull(1u) to CborNull(),
            CborUndefined(1u) to CborUndefined(),
            CborInteger(1, 1u) to CborInteger(1),
            CborInteger(1uL, isPositive = false, tags = ulongArrayOf(1u)) to CborInteger(1uL, isPositive = false),
            CborFloat(1.5, 1u) to CborFloat(1.5),
            CborString("s", 1u) to CborString("s"),
            CborBoolean(false, 1u) to CborBoolean(false),
            CborByteString(byteArrayOf(1, 2), 1u) to CborByteString(byteArrayOf(1, 2)),
            CborArray(listOf(CborString("v")), 1u) to CborArray(listOf(CborString("v"))),
            CborMap(mapOf(CborString("k") to CborString("v")), 1u) to
                CborMap(mapOf(CborString("k") to CborString("v"))),
        )

        for ((tagged, untagged) in elements) {
            assertEquals(
                untagged,
                cbor.decodeFromByteArray(CborElement.serializer(), cbor.encodeToByteArray(CborElement.serializer(), tagged))
            )
            assertEquals(
                tagged,
                Cbor { encodeValueTags = true }
                    .decodeFromByteArray(CborElement.serializer(), Cbor { encodeValueTags = true }
                        .encodeToByteArray(CborElement.serializer(), tagged))
            )
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testAllRawCborElementMapKeyKindsRespectEncodeKeyTags() {
        val taggedKeys = listOf(
            CborString("text", 1u) to CborString("text"),
            CborInteger(2, 1u) to CborInteger(2),
            CborByteString(byteArrayOf(3), 1u) to CborByteString(byteArrayOf(3)),
            CborNull(1u) to CborNull(),
            CborUndefined(1u) to CborUndefined(),
            CborArray(listOf(CborInteger(4)), 1u) to CborArray(listOf(CborInteger(4))),
            CborMap(emptyMap(), 1u) to CborMap(emptyMap()),
        )
        val tagged = CborMap(taggedKeys.associate { (key, _) -> key to CborBoolean(true) })
        val untagged = CborMap(taggedKeys.associate { (_, key) -> key to CborBoolean(true) })

        assertEquals(
            untagged,
            cbor.decodeFromByteArray(CborElement.serializer(), cbor.encodeToByteArray(CborElement.serializer(), tagged))
        )
        assertEquals(
            tagged,
            Cbor { encodeKeyTags = true }
                .decodeFromByteArray(CborElement.serializer(), Cbor { encodeKeyTags = true }
                    .encodeToByteArray(CborElement.serializer(), tagged))
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testGenericSerializableRawCborElementRespectsNestedTagSwitches() {
        val element = CborMap(mapOf(CborString("k", 1u) to CborBoolean(false, 2u)), 3u)
        val box = GenericBox<CborElement>(element)
        val serializer = GenericBox.serializer(CborElement.serializer())

        assertEquals(
            GenericBox<CborElement>(CborMap(mapOf(CborString("k") to CborBoolean(false)))),
            cbor.decodeFromByteArray(serializer, cbor.encodeToByteArray(serializer, box))
        )
        assertEquals(
            GenericBox<CborElement>(CborMap(mapOf(CborString("k", 1u) to CborBoolean(false)))),
            Cbor { encodeKeyTags = true }.decodeFromByteArray(
                serializer,
                Cbor { encodeKeyTags = true }.encodeToByteArray(serializer, box)
            )
        )
        assertEquals(
            GenericBox<CborElement>(CborMap(mapOf(CborString("k") to CborBoolean(false, 2u)), 3u)),
            Cbor { encodeValueTags = true }.decodeFromByteArray(
                serializer,
                Cbor { encodeValueTags = true }.encodeToByteArray(serializer, box)
            )
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testGenericSerializableRawCborElementListAndMapRespectTagSwitches() {
        val listBox = GenericListBox<CborElement>(listOf(CborString("v", 1u)))
        val listSerializer = GenericListBox.serializer(CborElement.serializer())
        val mapBox = GenericMapBox<CborElement, CborElement>(mapOf(CborInteger(1, 2u) to CborString("v", 3u)))
        val mapSerializer = GenericMapBox.serializer(CborElement.serializer(), CborElement.serializer())

        assertEquals(
            GenericListBox<CborElement>(listOf(CborString("v"))),
            cbor.decodeFromByteArray(listSerializer, cbor.encodeToByteArray(listSerializer, listBox))
        )
        assertEquals(
            GenericListBox<CborElement>(listOf(CborString("v", 1u))),
            Cbor { encodeValueTags = true }.decodeFromByteArray(
                listSerializer,
                Cbor { encodeValueTags = true }.encodeToByteArray(listSerializer, listBox)
            )
        )
        assertEquals(
            GenericMapBox<CborElement, CborElement>(mapOf(CborInteger(1, 2u) to CborString("v", 3u))),
            Cbor {
                encodeKeyTags = true
                encodeValueTags = true
            }.decodeFromByteArray(
                mapSerializer,
                Cbor {
                    encodeKeyTags = true
                    encodeValueTags = true
                }.encodeToByteArray(mapSerializer, mapBox)
            )
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testGenericSerializableConcreteCborElementSubtypesRespectTagSwitches() {
        val mapBox = GenericBox(CborMap(mapOf(CborString("k", 1u) to CborString("v", 2u)), 3u))
        val mapSerializer = GenericBox.serializer(CborMap.serializer())
        val arrayBox = GenericBox(CborArray(listOf(CborString("v", 1u)), 2u))
        val arraySerializer = GenericBox.serializer(CborArray.serializer())
        val stringBox = GenericBox(CborString("v", 1u))
        val stringSerializer = GenericBox.serializer(CborString.serializer())

        assertEquals(
            GenericBox(CborMap(mapOf(CborString("k") to CborString("v")))),
            cbor.decodeFromByteArray(mapSerializer, cbor.encodeToByteArray(mapSerializer, mapBox))
        )
        assertEquals(
            mapBox,
            Cbor {
                encodeKeyTags = true
                encodeValueTags = true
            }.decodeFromByteArray(
                mapSerializer,
                Cbor {
                    encodeKeyTags = true
                    encodeValueTags = true
                }.encodeToByteArray(mapSerializer, mapBox)
            )
        )
        assertEquals(
            GenericBox(CborArray(listOf(CborString("v")))),
            cbor.decodeFromByteArray(arraySerializer, cbor.encodeToByteArray(arraySerializer, arrayBox))
        )
        assertEquals(
            arrayBox,
            Cbor { encodeValueTags = true }.decodeFromByteArray(
                arraySerializer,
                Cbor { encodeValueTags = true }.encodeToByteArray(arraySerializer, arrayBox)
            )
        )
        assertEquals(
            GenericBox(CborString("v")),
            cbor.decodeFromByteArray(stringSerializer, cbor.encodeToByteArray(stringSerializer, stringBox))
        )
        assertEquals(
            stringBox,
            Cbor { encodeValueTags = true }.decodeFromByteArray(
                stringSerializer,
                Cbor { encodeValueTags = true }.encodeToByteArray(stringSerializer, stringBox)
            )
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testCborLabelOnCborElementPropertyFails() {
        val box = LabelledRawElementBox(CborString("x"))
        val cbor = Cbor {
            preferCborLabelsOverNames = true
            encodeKeyTags = true
            encodeValueTags = true
        }

        val encodeMessage = assertFailsWith<SerializationException> {
            cbor.encodeToByteArray(LabelledRawElementBox.serializer(), box)
        }.message
        assertEquals(
            "CborLabel cannot be represented by a CborElement value; model the containing CborMap key directly if numeric labels are required.",
            encodeMessage
        )

        val structuredMessage = assertFailsWith<SerializationException> {
            cbor.encodeToCborElement(LabelledRawElementBox.serializer(), box)
        }.message
        assertEquals(
            "CborLabel cannot be represented by a CborElement value; model the containing CborMap key directly if numeric labels are required.",
            structuredMessage
        )

        val decodeMessage = assertFailsWith<SerializationException> {
            cbor.decodeFromCborElement(LabelledRawElementBox.serializer(), CborMap(mapOf(CborInteger(1) to CborString("x"))))
        }.message
        assertEquals(
            "CborLabel cannot be represented by a CborElement value; model the containing CborMap key directly if numeric labels are required.",
            decodeMessage
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testCborLabelOnConcreteCborElementPropertyFails() {
        val message = assertFailsWith<SerializationException> {
            cbor.encodeToByteArray(LabelledRawIntegerBox.serializer(), LabelledRawIntegerBox(CborInteger(1)))
        }.message
        assertEquals(
            "CborLabel cannot be represented by a CborElement value; model the containing CborMap key directly if numeric labels are required.",
            message
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testRawCborMapNumericKeyRemainsAllowed() {
        val element = CborMap(mapOf(CborInteger(1) to CborString("x")))
        val encoded = cbor.encodeToByteArray(CborElement.serializer(), element)
        assertEquals(element, cbor.decodeFromByteArray(CborElement.serializer(), encoded))
        assertEquals(
            element,
            cbor.decodeFromCborElement(CborElement.serializer(), CborMap(mapOf(CborInteger(1) to CborString("x"))))
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testCborLabelDoesNotMakeTaggedCborElementPropertyAllowed() {
        val valueMessage = assertFailsWith<SerializationException> {
            cbor.encodeToByteArray(LabelledValueTaggedElement.serializer(), LabelledValueTaggedElement(CborBoolean(false)))
        }.message
        assertEquals(
            "CBOR tag annotations cannot be applied to CborElement properties; add tags to the CborElement instance directly.",
            valueMessage
        )

        val keyMessage = assertFailsWith<SerializationException> {
            cbor.encodeToByteArray(LabelledKeyTaggedElement.serializer(), LabelledKeyTaggedElement(CborBoolean(false)))
        }.message
        assertEquals(
            "KeyTags cannot be represented by a CborElement value; model the containing CborMap key directly if tagged keys are required.",
            keyMessage
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testDecodingRawCborElementPreservesTagsIndependentOfEncodeSwitches() {
        assertEquals(CborBoolean(false, 1u), cbor.decodeFromHexString(CborElement.serializer(), "c1f4"))
        assertEquals(
            CborMap(mapOf(CborString("k", 1u) to CborBoolean(false, 2u))),
            cbor.decodeFromHexString(CborElement.serializer(), "bfc1616bc2f4ff")
        )
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun testConcreteCborElementSerializersRespectRawTagSwitches() {
        val string = CborString("v", 1u)
        assertEquals(CborString("v"), cbor.decodeFromByteArray(CborString.serializer(), cbor.encodeToByteArray(string)))
        assertEquals(
            string,
            Cbor { encodeValueTags = true }
                .decodeFromByteArray(CborString.serializer(), Cbor { encodeValueTags = true }.encodeToByteArray(string))
        )

        val map = CborMap(mapOf(CborString("k", 1u) to CborString("v", 2u)), 3u)
        assertEquals(
            CborMap(mapOf(CborString("k", 1u) to CborString("v", 2u)), 3u),
            Cbor {
                encodeKeyTags = true
                encodeValueTags = true
            }.decodeFromByteArray(
                CborMap.serializer(),
                Cbor {
                    encodeKeyTags = true
                    encodeValueTags = true
                }.encodeToByteArray(CborMap.serializer(), map)
            )
        )
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun testTagsPreservedWhenDecodingTypedElements() {
        val cbor = Cbor { encodeValueTags = true }
        val taggedMap = CborMap(mapOf(CborString("a") to CborInteger(1)), 1uL)
        assertEquals(taggedMap, cbor.decodeFromByteArray<CborMap>(cbor.encodeToByteArray(taggedMap)))

        val taggedList = CborArray(listOf(CborInteger(1)), 2uL)
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
data class MixedValueTaggedElement(
    @ValueTags(2337u)
    val cborElement: CborElement?,
)

@Serializable
data class MixedKeyTaggedElement(
    @KeyTags(42u)
    val cborElement: CborElement?,
)

@Serializable
data class MixedValueTaggedInteger(
    @ValueTags(2337u)
    val cborElement: CborInteger,
)

@Serializable
data class MixedUntaggedElement(
    val cborElement: CborElement?,
)

@Serializable
data class GenericBox<T>(
    val value: T,
)

@Serializable
data class GenericListBox<T>(
    val values: List<T>,
)

@Serializable
data class GenericMapBox<K, V>(
    val values: Map<K, V>,
)

@Serializable
data class LabelledRawElementBox(
    @CborLabel(1)
    val value: CborElement,
)

@Serializable
data class LabelledRawIntegerBox(
    @CborLabel(1)
    val value: CborInteger,
)

@Serializable
data class LabelledValueTaggedElement(
    @CborLabel(1)
    @ValueTags(2337u)
    val value: CborElement,
)

@Serializable
data class LabelledKeyTaggedElement(
    @CborLabel(1)
    @KeyTags(42u)
    val value: CborElement,
)
