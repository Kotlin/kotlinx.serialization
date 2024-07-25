/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(ExperimentalUnsignedTypes::class)

package kotlinx.serialization.cbor

import kotlinx.serialization.*
import kotlinx.serialization.SimpleSealed.*
import kotlinx.serialization.cbor.internal.*
import kotlin.test.*

class CborParserTest {

    private fun withParser(input: String, block: CborParser.() -> Unit) {
        val bytes = HexConverter.parseHexBinary(input.uppercase())
        CborParser(ByteArrayInput(bytes), false).block()
    }

    @Test
    fun testParseIntegers() {
        withParser("0C1903E8") {
            assertEquals(12L, nextNumber())
            assertEquals(1000L, nextNumber())
        }
        withParser("203903e7") {
            assertEquals(-1L, nextNumber())
            assertEquals(-1000L, nextNumber())
        }
    }

    @Test
    fun testParseStrings() {
        withParser("6568656C6C6F") {
            assertEquals("hello", nextString())
        }
        withParser("7828737472696E672074686174206973206C6F6E676572207468616E2032332063686172616374657273") {
            assertEquals("string that is longer than 23 characters", nextString())
        }
    }

    @Test
    fun testParseDoubles() {
        withParser("fb7e37e43c8800759c") {
            assertEquals(1e+300, nextDouble())
        }
        withParser("fa47c35000") {
            assertEquals(100000.0f, nextFloat())
        }
    }


    /**
     * Test using example shown on page 11 of [RFC 7049 2.2.2](https://tools.ietf.org/html/rfc7049#section-2.2.2):
     *
     * ```
     * 0b010_11111 0b010_00100 0xaabbccdd 0b010_00011 0xeeff99 0b111_11111
     *
     * 5F              -- Start indefinite-length byte string
     *    44           -- Byte string of length 4
     *       aabbccdd  -- Bytes content
     *    43           -- Byte string of length 3
     *       eeff99    -- Bytes content
     *    FF           -- "break"
     *
     * After decoding, this results in a single byte string with seven
     * bytes: 0xaabbccddeeff99.
     * ```
     */
    @Test
    fun testRfc7049IndefiniteByteStringExample() {
        withParser(input = "5F44aabbccdd43eeff99FF") {
            assertEquals(
                expected = "aabbccddeeff99",
                actual = HexConverter.printHexBinary(nextByteString(), lowerCase = true)
            )
        }
    }


    /**
     * Tests skipping unknown keys associated with values of the following CBOR types:
     * - Major type 0: an unsigned integer
     * - Major type 1: a negative integer
     * - Major type 2: a byte string
     * - Major type 3: a text string
     */
    @Test
    fun testSkipPrimitives() {
        /* A4                           # map(4)
         *    61                        # text(1)
         *       61                     # "a"
         *    1B FFFFFFFFFFFFFFFF       # unsigned(18446744073709551615)
         *    61                        # text(1)
         *       62                     # "b"
         *    20                        # negative(0)
         *    61                        # text(1)
         *       63                     # "c"
         *    42                        # bytes(2)
         *       CAFE                   # "\xCA\xFE"
         *    61                        # text(1)
         *       64                     # "d"
         *    6B                        # text(11)
         *       48656C6C6F20776F726C64 # "Hello world"
         */
        withParser("a461611bffffffffffffffff616220616342cafe61646b48656c6c6f20776f726c64") {
            expectMap(size = 4)
            expect("a")
            skipElement() // unsigned(18446744073709551615)
            expect("b")
            skipElement() // negative(0)
            expect("c")
            skipElement() // "\xCA\xFE"
            expect("d")
            skipElement() // "Hello world"
            expectEof()
        }
    }

    /**
     * Tests skipping unknown keys associated with values (that are empty) of the following CBOR types:
     * - Major type 2: a byte string
     * - Major type 3: a text string
     */
    @Test
    fun testSkipEmptyPrimitives() {
        /* A2       # map(2)
         *    61    # text(1)
         *       61 # "a"
         *    40    # bytes(0)
         *          # ""
         *    61    # text(1)
         *       62 # "b"
         *    60    # text(0)
         *          # ""
         */
        withParser("a2616140616260") {
            expectMap(size = 2)
            expect("a")
            skipElement() // bytes(0)
            expect("b")
            skipElement() // text(0)
            expectEof()
        }
    }

    /**
     * Tests skipping unknown keys associated with values of the following CBOR types:
     * - Major type 4: an array of data items
     * - Major type 5: a map of pairs of data items
     */
    @Test
    fun testSkipCollections() {
        /* A2                                  # map(2)
         *    61                               # text(1)
         *       61                            # "a"
         *    83                               # array(3)
         *       01                            # unsigned(1)
         *       18 FF                         # unsigned(255)
         *       1A 00010000                   # unsigned(65536)
         *    61                               # text(1)
         *       62                            # "b"
         *    A2                               # map(2)
         *       61                            # text(1)
         *          78                         # "x"
         *       67                            # text(7)
         *          6B6F746C696E78             # "kotlinx"
         *       61                            # text(1)
         *          79                         # "y"
         *       6D                            # text(13)
         *          73657269616C697A6174696F6E # "serialization"
         */
        withParser("a26161830118ff1a000100006162a26178676b6f746c696e7861796d73657269616c697a6174696f6e") {
            expectMap(size = 2)
            expect("a")
            skipElement() // [1, 255, 65536]
            expect("b")
            skipElement() // {"x": "kotlinx", "y": "serialization"}
            expectEof()
        }
    }

    /**
     * Tests skipping unknown keys associated with values (empty collections) of the following CBOR types:
     * - Major type 4: an array of data items
     * - Major type 5: a map of pairs of data items
     */
    @Test
    fun testSkipEmptyCollections() {
        /* A2       # map(2)
         *    61    # text(1)
         *       61 # "a"
         *    80    # array(0)
         *    61    # text(1)
         *       62 # "b"
         *    A0    # map(0)
         */
        withParser("a26161806162a0") {
            expectMap(size = 2)
            expect("a")
            skipElement() // [1, 255, 65536]
            expect("b")
            skipElement() // {"x": "kotlinx", "y": "serialization"}
            expectEof()
        }
    }

    /**
     * Tests skipping unknown keys associated with **indefinite length** values of the following CBOR types:
     * - Major type 2: a byte string
     * - Major type 3: a text string
     * - Major type 4: an array of data items
     * - Major type 5: a map of pairs of data items
     */
    @Test
    fun testSkipIndefiniteLength() {
        /* A4                                  # map(4)
         *    61                               # text(1)
         *       61                            # "a"
         *    5F                               # bytes(*)
         *       42                            # bytes(2)
         *          CAFE                       # "\xCA\xFE"
         *       43                            # bytes(3)
         *          010203                     # "\x01\x02\x03"
         *       FF                            # primitive(*)
         *    61                               # text(1)
         *       62                            # "b"
         *    7F                               # text(*)
         *       66                            # text(6)
         *          48656C6C6F20               # "Hello "
         *       65                            # text(5)
         *          776F726C64                 # "world"
         *       FF                            # primitive(*)
         *    61                               # text(1)
         *       63                            # "c"
         *    9F                               # array(*)
         *       67                            # text(7)
         *          6B6F746C696E78             # "kotlinx"
         *       6D                            # text(13)
         *          73657269616C697A6174696F6E # "serialization"
         *       FF                            # primitive(*)
         *    61                               # text(1)
         *       64                            # "d"
         *    BF                               # map(*)
         *       61                            # text(1)
         *          31                         # "1"
         *       01                            # unsigned(1)
         *       61                            # text(1)
         *          32                         # "2"
         *       02                            # unsigned(2)
         *       61                            # text(1)
         *          33                         # "3"
         *       03                            # unsigned(3)
         *       FF                            # primitive(*)
         */
        withParser("a461615f42cafe43010203ff61627f6648656c6c6f2065776f726c64ff61639f676b6f746c696e786d73657269616c697a6174696f6eff6164bf613101613202613303ff") {
            expectMap(size = 4)
            expect("a")
            skipElement() // "\xCA\xFE\x01\x02\x03"
            expect("b")
            skipElement() // "Hello world"
            expect("c")
            skipElement() // ["kotlinx", "serialization"]
            expect("d")
            skipElement() // {"1": 1, "2": 2, "3": 3}
            expectEof()
        }
    }

    /**
     * Tests that skipping unknown keys also skips over associated tags.
     *
     * Includes tags on the key, tags on the value, and tags on both key and value.
     */
    @Test
    fun testSkipTags() {
        /*
         * A4                                 # map(4)
         * 61                              # text(1)
         *    61                           # "a"
         * CC                              # tag(12)
         *    1B FFFFFFFFFFFFFFFF          # unsigned(18446744073709551615)
         * D8 22                           # tag(34)
         *    61                           # text(1)
         *       62                        # "b"
         * 20                              # negative(0)
         * D8 38                           # tag(56)
         *    61                           # text(1)
         *       63                        # "c"
         * D8 4E                           # tag(78)
         *    42                           # bytes(2)
         *       CAFE                      # "\xCA\xFE"
         * 61                              # text(1)
         *    64                           # "d"
         * D8 5A                           # tag(90)
         *    CC                           # tag(12)
         *       6B                        # text(11)
         *          48656C6C6F20776F726C64 # "Hello world"
         */
        withParser("A46161CC1BFFFFFFFFFFFFFFFFD822616220D8386163D84E42CAFE6164D85ACC6B48656C6C6F20776F726C64") {
            expectMap(size = 4)
            expect("a")
            skipElement() // unsigned(18446744073709551615)
            expect("b")
            skipElement() // negative(0)
            expect("c")
            skipElement() // "\xCA\xFE"
            expect("d")
            skipElement() // "Hello world"
            expectEof()
        }
    }

    /**
     * Tests that skipping unknown keys also skips over associated tags.
     *
     * Includes tags on the key, tags on the value, and tags on both key and value.
     */
    @Test
    fun testVerifyTags() {
        /*
         * A4                                 # map(4)
         * 61                              # text(1)
         *    61                           # "a"
         * CC                              # tag(12)
         *    1B FFFFFFFFFFFFFFFF          # unsigned(18446744073709551615)
         * D8 22                           # tag(34)
         *    61                           # text(1)
         *       62                        # "b"
         * 20                              # negative(0)
         * D8 38                           # tag(56)
         *    61                           # text(1)
         *       63                        # "c"
         * D8 4E                           # tag(78)
         *    42                           # bytes(2)
         *       CAFE                      # "\xCA\xFE"
         * 61                              # text(1)
         *    64                           # "d"
         * D8 5A                           # tag(90)
         *    CC                           # tag(12)
         *       6B                        # text(11)
         *          48656C6C6F20776F726C64 # "Hello world"
         */
        withParser("A46161CC1BFFFFFFFFFFFFFFFFD822616220D8386163D84E42CAFE6164D85ACC6B48656C6C6F20776F726C64") {
            expectMap(size = 4)
            expect("a")
            skipElement(12uL) // unsigned(18446744073709551615)
            expect("b", 34uL)
            skipElement(null) // negative(0); explicitly setting parameter to null for clearer semantics
            expect("c", 56uL)
            skipElement(78uL) // "\xCA\xFE"
            expect("d")
            skipElement(ulongArrayOf(90uL, 12uL)) // "Hello world"
            expectEof()
        }
    }

    @Test
    fun testIgnoresTagsOnStrings() {
        /*
         * 84                                # array(4)
         * 68                             # text(8)
         *    756E746167676564            # "untagged"
         * C0                             # tag(0)
         *    68                          # text(8)
         *       7461676765642D30         # "tagged-0"
         * D8 F5                          # tag(245)
         *    6A                          # text(10)
         *       7461676765642D323435     # "tagged-244"
         * D9 3039                        # tag(12345)
         *    6C                          # text(12)
         *       7461676765642D3132333435 # "tagged-12345"
         *
         */
        withParser("8468756E746167676564C0687461676765642D30D8F56A7461676765642D323435D930396C7461676765642D3132333435") {
            assertEquals(4, startArray())
            assertEquals("untagged", nextString())
            assertEquals("tagged-0", nextString())
            assertEquals("tagged-245", nextString())
            assertEquals("tagged-12345", nextString())
        }
    }

    @Test
    fun testVerifyTagsOnStrings() {
        /*
         * 84                             # array(4)
         * 68                             # text(8)
         *    756E746167676564            # "untagged"
         * C0                             # tag(0)
         *    68                          # text(8)
         *       7461676765642D30         # "tagged-0"
         * D8 F5                          # tag(245)
         *    6A                          # text(10)
         *       7461676765642D323435     # "tagged-244"
         * D9 3039                        # tag(12345)
         *    6C                          # text(12)
         *       7461676765642D3132333435 # "tagged-12345"
         *
         */
        withParser("8468756E746167676564C0687461676765642D30D8F56A7461676765642D323435D930396C7461676765642D3132333435") {
            assertEquals(4, startArray(null))
            assertEquals("untagged", nextString(null))
            assertEquals("tagged-0", nextString(0u))
            assertEquals("tagged-245", nextString(245uL))
            assertEquals("tagged-12345", nextString(12345uL))
        }
    }

    @Test
    fun testIgnoresTagsOnNumbers() {
        /*
         * 86                     # array(6)
         * 18 7B                  # unsigned(123)
         * C0                     # tag(0)
         *    1A 0001E240         # unsigned(123456)
         * D8 F5                  # tag(245)
         *    1A 000F423F         # unsigned(999999)
         * D9 3039                # tag(12345)
         *    38 31               # negative(49)
         * D8 22                  # tag(34)
         *    FB 3FE161F9F01B866E # primitive(4603068020252444270)
         * D9 0237                # tag(567)
         *    FB 401999999999999A # primitive(4618891777831180698)
         */
        withParser("86187BC01A0001E240D8F51A000F423FD930393831D822FB3FE161F9F01B866ED90237FB401999999999999A") {
            assertEquals(6, startArray())
            assertEquals(123, nextNumber())
            assertEquals(123456, nextNumber())
            assertEquals(999999, nextNumber())
            assertEquals(-50, nextNumber())
            assertEquals(0.54321, nextDouble(), 0.00001)
            assertEquals(6.4, nextDouble(), 0.00001)
        }
    }

    @Test
    fun testVerifiesTagsOnNumbers() {
        /*
         * 86                     # array(6)
         * 18 7B                  # unsigned(123)
         * C0                     # tag(0)
         *    1A 0001E240         # unsigned(123456)
         * D8 F5                  # tag(245)
         *    1A 000F423F         # unsigned(999999)
         * D9 3039                # tag(12345)
         *    38 31               # negative(49)
         * D8 22                  # tag(34)
         *    FB 3FE161F9F01B866E # primitive(4603068020252444270)
         * D9 0237                # tag(567)
         *    FB 401999999999999A # primitive(4618891777831180698)
         */
        withParser("86187BC01A0001E240D8F51A000F423FD930393831D822FB3FE161F9F01B866ED90237FB401999999999999A") {
            assertEquals(6, startArray(null))
            assertEquals(123, nextNumber(null))
            assertEquals(123456, nextNumber(0uL))
            assertEquals(999999, nextNumber(245uL))
            assertEquals(-50, nextNumber(12345uL))
            assertEquals(0.54321, nextDouble(34uL), 0.00001)
            assertEquals(6.4, nextDouble(567uL), 0.00001)
        }
    }

    @Test
    fun testIgnoresTagsOnArraysAndMaps() {
        /*
         * A2                                  # map(2)
         * 63                                  # text(3)
         *    6D6170                           # "map"
         * D8 7B                               # tag(123)
         *    A1                               # map(1)
         *       68                            # text(8)
         *          74686973206D6170           # "this map"
         *       6D                            # text(13)
         *          69732074616767656420313233 # "is tagged 123"
         * 65                                  # text(5)
         *    6172726179                       # "array"
         * DA 0012D687                         # tag(1234567)
         *    83                               # array(3)
         *       6A                            # text(10)
         *          74686973206172726179       # "this array"
         *       69                            # text(9)
         *          697320746167676564         # "is tagged"
         *       67                            # text(7)
         *          31323334353637             # "1234567"
         */
        withParser("A2636D6170D87BA16874686973206D61706D69732074616767656420313233656172726179DA0012D687836A74686973206172726179696973207461676765646731323334353637") {
            assertEquals(2, startMap())
            assertEquals("map", nextString())
            assertEquals(1, startMap())
            assertEquals("this map", nextString())
            assertEquals("is tagged 123", nextString())
            assertEquals("array", nextString())
            assertEquals(3, startArray())
            assertEquals("this array", nextString())
            assertEquals("is tagged", nextString())
            assertEquals("1234567", nextString())
        }
    }

    @Test
    fun testVerifiesTagsOnArraysAndMaps() {
        /*
         * A2                                  # map(2)
         * 63                                  # text(3)
         *    6D6170                           # "map"
         * D8 7B                               # tag(123)
         *    A1                               # map(1)
         *       68                            # text(8)
         *          74686973206D6170           # "this map"
         *       6D                            # text(13)
         *          69732074616767656420313233 # "is tagged 123"
         * 65                                  # text(5)
         *    6172726179                       # "array"
         * DA 0012D687                         # tag(1234567)
         *    83                               # array(3)
         *       6A                            # text(10)
         *          74686973206172726179       # "this array"
         *       69                            # text(9)
         *          697320746167676564         # "is tagged"
         *       67                            # text(7)
         *          31323334353637             # "1234567"
         */
        withParser("A2636D6170D87BA16874686973206D61706D69732074616767656420313233656172726179DA0012D687836A74686973206172726179696973207461676765646731323334353637") {
            assertEquals(2, startMap(null))
            assertEquals("map", nextString(null))
            assertEquals(1, startMap(123uL))
            assertEquals("this map", nextString(null))
            assertEquals("is tagged 123", nextString(null))
            assertEquals("array", nextString(null))
            assertEquals(3, startArray(1234567uL))
            assertEquals("this array", nextString(null))
            assertEquals("is tagged", nextString(null))
            assertEquals("1234567", nextString(null))
        }
    }
}


private fun CborParser.nextNumber(tag: ULong): Long = nextNumber(ulongArrayOf(tag))

private fun CborParser.nextDouble(tag: ULong) = nextDouble(ulongArrayOf(tag))

private fun CborParser.nextString(tag: ULong) = nextString(ulongArrayOf(tag))

private fun CborParser.startArray(tag: ULong): Int = startArray(ulongArrayOf(tag))

private fun CborParser.startMap(tag: ULong) = startMap(ulongArrayOf(tag))

private fun CborParser.skipElement(singleTag: ULong) = skipElement(ulongArrayOf(singleTag))

private fun CborParser.skipElement() = skipElement(null)

private fun CborParser.expect(expected: String, tag: ULong? = null) {
    assertEquals(expected, actual = nextString(tag?.let { ulongArrayOf(it) }), "string")
}

private fun CborParser.expectMap(size: Int, tag: ULong? = null) {
    assertEquals(size, actual = startMap(tag?.let { ulongArrayOf(it) }), "map size")
}

private fun CborParser.expectEof() {
    assertTrue(isEof(), "Expected EOF.")
}
