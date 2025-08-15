/*
 * Copyright 2017-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(ExperimentalUnsignedTypes::class)

package kotlinx.serialization.cbor

import kotlinx.serialization.*
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

    // See https://datatracker.ietf.org/doc/html/rfc8949#section-3.3
    @Test
    fun testSimpleValues() {
        withParser("F4F5") { // boolean values
            assertFalse(nextBoolean())
            assertTrue(nextBoolean())
        }
        withParser("F6") { // null
            assertNull(nextNull())
            expectEof()
        }
        // withParser("F7") { // undefined
        // TODO: how do we handle undefined values?
        // }
    }

    // See https://datatracker.ietf.org/doc/html/rfc8949#section-3.3
    @Test
    fun testUnsupportedSimpleValueEncodings() {
        fun CborParser.tryReadAllSimpleValues() {
            assertFailsWith<CborDecodingException> { nextBoolean() }
            assertFailsWith<CborDecodingException> { nextNull() }
            assertFailsWith<CborDecodingException> { nextFloat() }
            assertFailsWith<CborDecodingException> { nextDouble() }
        }

        // unassigned
        withParser("E0") { tryReadAllSimpleValues() }
        withParser("F3") { tryReadAllSimpleValues() }
        withParser("FC") { tryReadAllSimpleValues() }
        // break
        withParser("FF") { tryReadAllSimpleValues() }
        // two bytes, the second is missing
        withParser("F8") { tryReadAllSimpleValues() }
        // two bytes, unassigned
        withParser("F800") { tryReadAllSimpleValues() }
        withParser("F8FF") { tryReadAllSimpleValues() }
        // two bytes, reserved
        withParser("F818") { tryReadAllSimpleValues() }
    }

    // See https://datatracker.ietf.org/doc/html/rfc8949#section-3.3
    @Test
    fun testReadArbitraryByteSequenceAsSimpleValue() {
        withParser("17") { // that's not a simple value
            assertFailsWith<CborDecodingException> { nextBoolean() }
        }
        withParser("17") { // that's not a simple value
            assertFailsWith<CborDecodingException> { nextNull() }
        }
        withParser("6568656C6C6F6568656C6C6F") { // that's not a simple value
            assertFailsWith<CborDecodingException> { nextFloat() }
        }
        withParser("6568656C6C6F6568656C6C6F") { // that's not a simple value
            assertFailsWith<CborDecodingException> { nextDouble() }
        }
    }

    // See https://datatracker.ietf.org/doc/html/rfc8949#section-3.3
    @Test
    fun testFloatValues() {
        // 1.0 -> 0x3F800000
        // NaN -> 0x7FC00000
        // Inf -> 0x7F800000
        // -Inf -> 0xFF800000
        // 0.0 -> 0x0
        // -0.0 -> 0x80000000

        // read as float
        withParser("FA3F800000FA7FC00000FA7F800000FAFF800000FA00000000FA80000000") {
            assertEquals(1.0f, nextFloat(), 1e-6f)
            assertTrue(nextFloat().isNaN())
            assertEquals(Float.POSITIVE_INFINITY, nextFloat())
            assertEquals(Float.NEGATIVE_INFINITY, nextFloat())
            assertEquals(0.0f, nextFloat())
            assertEquals(0x80000000.toInt(), nextFloat().toBits())
        }

        // read as double
        withParser("FA3F800000FA7FC00000FA7F800000FAFF800000FA00000000FA80000000") {
            assertEquals(1.0, nextDouble(), 1e-6)
            assertTrue(nextDouble().isNaN())
            assertEquals(Double.POSITIVE_INFINITY, nextDouble())
            assertEquals(Double.NEGATIVE_INFINITY, nextDouble())
            assertEquals(0.0, nextDouble())
            assertEquals(0x8000000000000000UL.toLong(), nextDouble().toBits())
        }

        // read invalid data
        repeat(4) {
            withParser("FA" + "00".repeat(it)) {
                assertFailsWith<CborDecodingException> { nextFloat() }
            }
        }
        repeat(4) {
            withParser("FA" + "00".repeat(it)) {
                assertFailsWith<CborDecodingException> { nextDouble() }
            }
        }
    }

    // See https://datatracker.ietf.org/doc/html/rfc8949#section-3.3
    @Test
    fun testDoubleValues() {
        // 1.0 -> 0x3FF0000000000000
        // NaN -> 0x7FF8000000000000
        // Inf -> 0x7FF0000000000000
        // -Inf -> 0xFFF0000000000000
        // 0.0 -> 0x0
        // -0.0 -> 0x8000000000000000

        // read as double
        withParser("FB3FF0000000000000FB7FF8000000000000FB7FF0000000000000FBFFF0000000000000FB0000000000000000FB8000000000000000") {
            assertEquals(1.0, nextDouble(), 1e-6)
            assertTrue(nextDouble().isNaN())
            assertEquals(Double.POSITIVE_INFINITY, nextDouble())
            assertEquals(Double.NEGATIVE_INFINITY, nextDouble())
            assertEquals(0.0, nextDouble())
            assertEquals(0x8000000000000000UL.toLong(), nextDouble().toBits())
        }

        // read invalid data
        repeat(8) {
            withParser("FB" + "00".repeat(it)) {
                assertFailsWith<CborDecodingException> { nextFloat() }
            }
        }
        repeat(8) {
            withParser("FB" + "00".repeat(it)) {
                assertFailsWith<CborDecodingException> { nextDouble() }
            }
        }

        // read double as float
        withParser("FB3FF0000000000000") {
            assertFailsWith<CborDecodingException> { nextFloat() }
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

    // See https://www.rfc-editor.org/rfc/rfc8949#section-3.1
    @Test
    fun testPositiveIntegersImmediateEncoding() {
        withParser("000117") { // immediate encoding, values in range 0..23
            assertEquals(0L, nextNumber())
            assertEquals(1L, nextNumber())
            assertEquals(23L, nextNumber())
        }
        withParser("1C") { // additional information > 27 is illegal
            assertFailsWith<CborDecodingException> { nextNumber() }
        }
        withParser("1F") { // additional information > 27 is illegal
            assertFailsWith<CborDecodingException> { nextNumber() }
        }
    }

    // See https://www.rfc-editor.org/rfc/rfc8949#section-3.1
    @Test
    fun testPositiveIntegersSingleByteEncoding() {
        withParser("180018FF") { // single byte encoding
            assertEquals(0L, nextNumber())
            assertEquals(0xFFL, nextNumber())
        }
        withParser("18") { // single byte encoding, underflow
            assertFailsWith<CborDecodingException> { nextNumber() }
        }
    }

    // See https://www.rfc-editor.org/rfc/rfc8949#section-3.1
    @Test
    fun testPositiveIntegersTwoByteEncoding() {
        withParser("19000019FFFF") { // two byte encoding
            assertEquals(0L, nextNumber())
            assertEquals(0xFFFFL, nextNumber())
        }
        repeat(2) {
            withParser("19" + "00".repeat(it)) { // two bytes encoding, underflow
                assertFailsWith<CborDecodingException> { nextNumber() }
            }
        }
    }

    // See https://www.rfc-editor.org/rfc/rfc8949#section-3.1
    @Test
    fun testPositiveIntegersFourByteEncoding() {
        withParser("1A000000001AFFFFFFFF") { // four bytes encoding
            assertEquals(0L, nextNumber())
            assertEquals(0xFFFFFFFFL, nextNumber())
        }
        repeat(4) {
            withParser("1A" + "00".repeat(it)) { // four bytes encoding, underflow
                assertFailsWith<CborDecodingException> { nextNumber() }
            }
        }
    }

    // See https://www.rfc-editor.org/rfc/rfc8949#section-3.1
    @Test
    fun testPositiveIntegersEightByteEncoding() {
        withParser("1B00000000000000001B7FFFFFFFFFFFFFFF1BFFFFFFFFFFFFFFFF") { // four bytes encoding
            assertEquals(0L, nextNumber())
            assertEquals(Long.MAX_VALUE, nextNumber())
            // TODO: should it be an error?
            assertEquals(-1L, nextNumber())
        }
        repeat(8) {
            withParser("1B" + "00".repeat(it)) { // four bytes encoding, underflow
                assertFailsWith<CborDecodingException> { nextNumber() }
            }
        }
    }

    // See https://www.rfc-editor.org/rfc/rfc8949#section-3.1
    @Test
    fun testNegativeIntegersImmediateEncoding() {
        withParser("202137") { // immediate encoding, values in range -24..-1
            assertEquals(-1L, nextNumber())
            assertEquals(-2L, nextNumber())
            assertEquals(-24L, nextNumber())
        }
        withParser("3C") { // additional information > 27 is illegal
            assertFailsWith<CborDecodingException> { nextNumber() }
        }
        withParser("3F") { // additional information > 27 is illegal
            assertFailsWith<CborDecodingException> { nextNumber() }
        }
    }

    // See https://www.rfc-editor.org/rfc/rfc8949#section-3.1
    @Test
    fun testNegativeIntegersSingleByteEncoding() {
        withParser("380038FF") {
            assertEquals(-1L, nextNumber())
            assertEquals(-256L, nextNumber())
        }
        withParser("38") { // underflow
            assertFailsWith<CborDecodingException> { nextNumber() }
        }
    }

    // See https://www.rfc-editor.org/rfc/rfc8949#section-3.1
    @Test
    fun testNegativeIntegersTwoByteEncoding() {
        withParser("39000039FFFF") {
            assertEquals(-1L, nextNumber())
            assertEquals(-65536L, nextNumber())
        }
        repeat(2) {
            withParser("39" + "00".repeat(it)) { // underflow
                assertFailsWith<CborDecodingException> { nextNumber() }
            }
        }
    }

    // See https://www.rfc-editor.org/rfc/rfc8949#section-3.1
    @Test
    fun testNegativeIntegersFourByteEncoding() {
        withParser("3A000000003AFFFFFFFF") {
            assertEquals(-1L, nextNumber())
            assertEquals(-4294967296L, nextNumber())
        }
        repeat(4) {
            withParser("3A" + "00".repeat(it)) { // underflow
                assertFailsWith<CborDecodingException> { nextNumber() }
            }
        }
    }

    // See https://www.rfc-editor.org/rfc/rfc8949#section-3.1
    @Test
    fun testNegativeIntegersEightByteEncoding() {
        withParser("3B00000000000000003BFFFFFFFFFFFFFFFF") {
            assertEquals(-1L, nextNumber())
            // TODO: what should it be?
            assertEquals(0L, nextNumber())
        }
        repeat(8) {
            withParser("3B" + "00".repeat(it)) {
                assertFailsWith<CborDecodingException> { nextNumber() }
            }
        }
    }

    // See https://www.rfc-editor.org/rfc/rfc8949#section-3.1
    @Test
    fun testReadArbitraryByteSequenceAsIntegralNumber() {
        withParser("6568656C6C6F") { // string "hello"
            assertFailsWith<CborDecodingException> { nextNumber() }
        }
        withParser("FB400921FB54442EEA") { // PI
            assertFailsWith<CborDecodingException> { nextNumber() }
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

    @Test // from #2886
    fun testEofInsideIndefiniteByteString() {
        withParser("7f6100") {
            val msg = assertFailsWith<CborDecodingException> {
                nextString()
            }
            assertEquals("Unexpected end of encoded CBOR document", msg.message)
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

    @Test
    fun testTooLongByteString() {
        withParser("5B0000000080000000010203") { // Int overflow
            assertFailsWith<CborDecodingException> { nextByteString() }
        }
        withParser("5B0001000000000000010203") { // does not fit into Int
            assertFailsWith<CborDecodingException> { nextByteString() }
        }
        withParser("5B0001000000000000010203") { // does not fit into Int
            assertFailsWith<CborDecodingException> { skipElement() }
        }
        withParser("5B0000000070000000010203") { // EOF
            assertFailsWith<CborDecodingException> { nextByteString() }
        }
    }

    @Test
    fun testTooLongTextString() {
        withParser("7B00000000800000006C6F6E67") { // Int overflow
            assertFailsWith<CborDecodingException> { nextString() }
        }
        withParser("7B00010000000000006C6F6E67") { // does not fit into Int
            assertFailsWith<CborDecodingException> { nextString() }
        }
        withParser("7B00010000000000006C6F6E67") { // does not fit into Int
            assertFailsWith<CborDecodingException> { skipElement() }
        }
        withParser("7B0000000070000000000000") { // EOF
            assertFailsWith<CborDecodingException> { nextByteString() }
        }
    }

    @Test
    fun testTooLongList() {
        withParser("9B000000008000000001020304") { // Int overflow
            assertFailsWith<CborDecodingException> { startArray() }
        }
        withParser("9B000100000000000001020304") { // does not fit into Int
            assertFailsWith<CborDecodingException> { startArray() }
        }
        withParser("9B000100000000000001020304") { // does not fit into Int
            assertFailsWith<CborDecodingException> { skipElement() }
        }
    }

    @Test
    fun testTooLongMap() {
        withParser("BB0000000080000000616B6176") { // Int overflow
            assertFailsWith<CborDecodingException> { startMap() }
        }
        withParser("BB0000000040000000616B6176") { // we don't support maps with size larger than Int.MAX_VALUE / 2
            assertFailsWith<CborDecodingException> { startMap() }
        }
        withParser("BB0001000000000000616B6176") { // does not fit into Int
            assertFailsWith<CborDecodingException> { startMap() }
        }
        withParser("BB0001000000000000616B6176") { // does not fit into Int
            assertFailsWith<CborDecodingException> { skipElement() }
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
