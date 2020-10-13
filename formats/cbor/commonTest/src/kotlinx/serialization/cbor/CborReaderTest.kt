/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.cbor

import kotlinx.serialization.*
import kotlinx.serialization.SimpleSealed.*
import kotlinx.serialization.cbor.internal.*
import kotlin.test.*

class CborReaderTest {

    private val ignoreUnknownKeys = Cbor { ignoreUnknownKeys = true }

    private fun withDecoder(input: String, block: CborDecoder.() -> Unit) {
        val bytes = HexConverter.parseHexBinary(input.toUpperCase())
        CborDecoder(ByteArrayInput(bytes)).block()
    }

    @Test
    fun testDecodeIntegers() {
        withDecoder("0C1903E8") {
            assertEquals(12L, nextNumber())
            assertEquals(1000L, nextNumber())
        }
        withDecoder("203903e7") {
            assertEquals(-1L, nextNumber())
            assertEquals(-1000L, nextNumber())
        }
    }

    @Test
    fun testDecodeStrings() {
        withDecoder("6568656C6C6F") {
            assertEquals("hello", nextString())
        }
        withDecoder("7828737472696E672074686174206973206C6F6E676572207468616E2032332063686172616374657273") {
            assertEquals("string that is longer than 23 characters", nextString())
        }
    }

    @Test
    fun testDecodeDoubles() {
        withDecoder("fb7e37e43c8800759c") {
            assertEquals(1e+300, nextDouble())
        }
        withDecoder("fa47c35000") {
            assertEquals(100000.0f, nextFloat())
        }
    }

    @Test
    fun testDecodeSimpleObject() {
        assertEquals(Simple("str"), Cbor.decodeFromHexString(Simple.serializer(), "bf616163737472ff"))
    }

    @Test
    fun testDecodeComplicatedObject() {
        val test = TypesUmbrella(
            "Hello, world!",
            42,
            null,
            listOf("a", "b"),
            mapOf(1 to true, 2 to false),
            Simple("lol"),
            listOf(Simple("kek")),
            HexConverter.parseHexBinary("cafe"),
            HexConverter.parseHexBinary("cafe")
        )
        // with maps, lists & strings of indefinite length
        assertEquals(test, Cbor.decodeFromHexString(
            TypesUmbrella.serializer(),
            "bf637374726d48656c6c6f2c20776f726c64216169182a686e756c6c61626c65f6646c6973749f61616162ff636d6170bf01f502f4ff65696e6e6572bf6161636c6f6cff6a696e6e6572734c6973749fbf6161636b656bffff6a62797465537472696e675f42cafeff696279746541727261799f383521ffff"
        )
        )
        // with maps, lists & strings of definite length
        assertEquals(test, Cbor.decodeFromHexString(
            TypesUmbrella.serializer(),
            "a9646c6973748261616162686e756c6c61626c65f6636d6170a202f401f56169182a6a696e6e6572734c69737481a16161636b656b637374726d48656c6c6f2c20776f726c642165696e6e6572a16161636c6f6c6a62797465537472696e6742cafe6962797465417272617982383521"
        )
        )
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
        withDecoder(input = "5F44aabbccdd43eeff99FF") {
            assertEquals(
                expected = "aabbccddeeff99",
                actual = HexConverter.printHexBinary(nextByteString(), lowerCase = true)
            )
        }
    }

    @Test
    fun testReadByteStringWhenNullable() {
        /* A1                         # map(1)
         *    6A                      # text(10)
         *       62797465537472696E67 # "byteString"
         *    44                      # bytes(4)
         *       01020304             # "\x01\x02\x03\x04"
         */
        assertEquals(
            expected = NullableByteString(byteArrayOf(1, 2, 3, 4)),
            actual = Cbor.decodeFromHexString(
                deserializer = NullableByteString.serializer(),
                hex = "a16a62797465537472696e674401020304"
            )
        )

        /* A1                         # map(1)
         *    6A                      # text(10)
         *       62797465537472696E67 # "byteString"
         *    F6                      # primitive(22)
         */
        assertEquals(
            expected = NullableByteString(byteString = null),
            actual = Cbor.decodeFromHexString(
                deserializer = NullableByteString.serializer(),
                hex = "a16a62797465537472696e67f6"
            )
        )
    }

    /**
     * CBOR hex data represents serialized versions of [TypesUmbrella] (which does **not** have a root property 'a') so
     * decoding to [Simple] (which has the field 'a') is expected to fail.
     */
    @Test
    fun testIgnoreUnknownKeysFailsWhenCborDataIsMissingKeysThatArePresentInKotlinClass() {
        // with maps & lists of indefinite length
        assertFailsWithMessage<SerializationException>("Field 'a' is required, but it was missing") {
            ignoreUnknownKeys.decodeFromHexString(
                Simple.serializer(),
                "bf637374726d48656c6c6f2c20776f726c64216169182a686e756c6c61626c65f6646c6973749f61616162ff636d6170bf01f502f4ff65696e6e6572bf6161636c6f6cff6a696e6e6572734c6973749fbf6161636b656bffffff"
            )
        }

        // with maps & lists of definite length
        assertFailsWithMessage<SerializationException>("Field 'a' is required, but it was missing") {
            ignoreUnknownKeys.decodeFromHexString(
                Simple.serializer(),
                "a7646c6973748261616162686e756c6c61626c65f6636d6170a202f401f56169182a6a696e6e6572734c69737481a16161636b656b637374726d48656c6c6f2c20776f726c642165696e6e6572a16161636c6f6c"
            )
        }
    }

    @Test
    fun testIgnoreUnknownKeysFailsWhenDecodingIncompleteCbor() {
        /* A3                 # map(3)
         *    63              # text(3)
         *       737472       # "str"
         *    66              # text(6)
         *       737472696E67 # "string"
         *    61              # text(1)
         *       69           # "i"
         *    00              # unsigned(0)
         *    66              # text(6)
         *       69676E6F7265 # "ignore"
         * (missing value associated with "ignore" key)
         */
        assertFailsWithMessage<CborDecodingException>("Unexpected EOF while skipping element") {
            ignoreUnknownKeys.decodeFromHexString(
                TypesUmbrella.serializer(),
                "a36373747266737472696e676169006669676e6f7265"
            )
        }

        /* A3                 # map(3)
         *    63              # text(3)
         *       737472       # "str"
         *    66              # text(6)
         *       737472696E67 # "string"
         *    61              # text(1)
         *       69           # "i"
         *    00              # unsigned(0)
         *    66              # text(6)
         *       69676E6F7265 # "ignore"
         *    A2              # map(2)
         * (missing map contents associated with "ignore" key)
         */
        assertFailsWithMessage<CborDecodingException>("Unexpected EOF while skipping element") {
            ignoreUnknownKeys.decodeFromHexString(
                TypesUmbrella.serializer(),
                "a36373747266737472696e676169006669676e6f7265a2"
            )
        }
    }

    @Test
    fun testIgnoreUnknownKeysFailsWhenEncounteringPreemptiveBreak() {
        /* A3                 # map(3)
         *    63              # text(3)
         *       737472       # "str"
         *    66              # text(6)
         *       737472696E67 # "string"
         *    66              # text(6)
         *       69676E6F7265 # "ignore"
         *    FF              # primitive(*)
         */
        assertFailsWithMessage<CborDecodingException>("Expected next data item, but found FF") {
            ignoreUnknownKeys.decodeFromHexString(
                TypesUmbrella.serializer(),
                "a36373747266737472696e676669676e6f7265ff"
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
        withDecoder("a461611bffffffffffffffff616220616342cafe61646b48656c6c6f20776f726c64") {
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
        withDecoder("a2616140616260") {
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
        withDecoder("a26161830118ff1a000100006162a26178676b6f746c696e7861796d73657269616c697a6174696f6e") {
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
        withDecoder("a26161806162a0") {
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
        withDecoder("a461615f42cafe43010203ff61627f6648656c6c6f2065776f726c64ff61639f676b6f746c696e786d73657269616c697a6174696f6eff6164bf613101613202613303ff") {
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

    @Test
    fun testDecodeCborWithUnknownField() {
        assertEquals(
            expected = Simple("123"),
            actual = ignoreUnknownKeys.decodeFromHexString(
                deserializer = Simple.serializer(),

                /* BF           # map(*)
                 *    61        # text(1)
                 *       61     # "a"
                 *    63        # text(3)
                 *       313233 # "123"
                 *    61        # text(1)
                 *       62     # "b"
                 *    63        # text(3)
                 *       393837 # "987"
                 *    FF        # primitive(*)
                 */
                hex = "bf616163313233616263393837ff"
            )
        )
    }

    @Test
    fun testDecodeCborWithUnknownNestedIndefiniteFields() {
        assertEquals(
            expected = Simple("123"),
            actual = ignoreUnknownKeys.decodeFromHexString(
                deserializer = Simple.serializer(),

                /* BF             # map(*)
                 *    61          # text(1)
                 *       61       # "a"
                 *    63          # text(3)
                 *       313233   # "123"
                 *    61          # text(1)
                 *       62       # "b"
                 *    BF          # map(*)
                 *       7F       # text(*)
                 *          61    # text(1)
                 *             78 # "x"
                 *          FF    # primitive(*)
                 *       A1       # map(1)
                 *          61    # text(1)
                 *             79 # "y"
                 *          0A    # unsigned(10)
                 *       FF       # primitive(*)
                 *    61          # text(1)
                 *       63       # "c"
                 *    9F          # array(*)
                 *       01       # unsigned(1)
                 *       02       # unsigned(2)
                 *       03       # unsigned(3)
                 *       FF       # primitive(*)
                 *    FF          # primitive(*)
                 */
                hex = "bf6161633132336162bf7f6178ffa161790aff61639f010203ffff"
            )
        )
    }

    /**
     * The following CBOR diagnostic output demonstrates the additional fields (prefixed with `+` in front of each line)
     * present in the encoded CBOR data that does not have associated fields in the Kotlin classes (they will be skipped
     * over with `ignoreUnknownKeys` is enabled).
     *
     * ```diff
     *   {
     * +   "extra": [
     * +     9,
     * +     8,
     * +     7
     * +   ],
     *     "boxed": [
     *       [
     *         "kotlinx.serialization.SimpleSealed.SubSealedA",
     *         {
     *           "s": "a",
     * +         "newA": {
     * +           "x": 1,
     * +           "y": 2
     * +         }
     *         }
     *       ],
     *       [
     *         "kotlinx.serialization.SimpleSealed.SubSealedB",
     *         {
     *           "i": 1
     *         }
     *       ]
     *     ]
     *   }
     * ```
     */
    @Test
    fun testDecodeCborWithUnknownKeysInSealedClasses() {
        /* BF                      # map(*)
         *    65                   # text(5)
         *       6578747261        # "extra"
         *    83                   # array(3)
         *       09                # unsigned(9)
         *       08                # unsigned(8)
         *       07                # unsigned(7)
         *    65                   # text(5)
         *       626F786564        # "boxed"
         *    9F                   # array(*)
         *       9F                # array(*)
         *          78 2D          # text(45)
         *             6B6F746C696E782E73657269616C697A6174696F6E2E53696D706C655365616C65642E5375625365616C656441 # "kotlinx.serialization.SimpleSealed.SubSealedA"
         *          BF             # map(*)
         *             61          # text(1)
         *                73       # "s"
         *             61          # text(1)
         *                61       # "a"
         *             64          # text(4)
         *                6E657741 # "newA"
         *             BF          # map(*)
         *                61       # text(1)
         *                   78    # "x"
         *                01       # unsigned(1)
         *                61       # text(1)
         *                   79    # "y"
         *                02       # unsigned(2)
         *                FF       # primitive(*)
         *             FF          # primitive(*)
         *          FF             # primitive(*)
         *       9F                # array(*)
         *          78 2D          # text(45)
         *             6B6F746C696E782E73657269616C697A6174696F6E2E53696D706C655365616C65642E5375625365616C656442 # "kotlinx.serialization.SimpleSealed.SubSealedB"
         *          BF             # map(*)
         *             61          # text(1)
         *                69       # "i"
         *             01          # unsigned(1)
         *             FF          # primitive(*)
         *          FF             # primitive(*)
         *       FF                # primitive(*)
         *    FF                   # primitive(*)
         */

        assertEquals(
            expected = SealedBox(
                listOf(
                    SubSealedA("a"),
                    SubSealedB(1)
                )
            ),
            actual = ignoreUnknownKeys.decodeFromHexString(
                SealedBox.serializer(),
                "bf6565787472618309080765626f7865649f9f782d6b6f746c696e782e73657269616c697a6174696f6e2e53696d706c655365616c65642e5375625365616c656441bf61736161646e657741bf617801617902ffffff9f782d6b6f746c696e782e73657269616c697a6174696f6e2e53696d706c655365616c65642e5375625365616c656442bf616901ffffffff"
            )
        )
    }
}

private fun CborDecoder.expect(expected: String) {
    assertEquals(expected, actual = nextString(), "string")
}

private fun CborDecoder.expectMap(size: Int) {
    assertEquals(size, actual = startMap(), "map size")
}

private fun CborDecoder.expectEof() {
    assertTrue(isEof(), "Expected EOF.")
}
