/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(ExperimentalUnsignedTypes::class)

package kotlinx.serialization.cbor

import kotlinx.serialization.*
import kotlinx.serialization.SimpleSealed.*
import kotlinx.serialization.cbor.internal.*
import kotlin.test.*

class CborDecoderTest {

    private val ignoreUnknownKeys = Cbor { ignoreUnknownKeys = true }

    @Test
    fun testDecodeSimpleObject() {
        val hex = "bf616163737472ff"
        val reference = Simple("str")
        assertEquals(reference, Cbor.decodeFromHexString(Simple.serializer(), hex))

        val struct = Cbor.decodeFromHexString<CborElement>(hex)
        assertEquals(reference, Cbor.decodeFromCborElement(Simple.serializer(), struct))

        assertEquals(hex, Cbor.encodeToHexString(CborElement.serializer(), struct))
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
        // with maps, lists & strings of indefinite length (note: this test vector did not correspond to proper encoding before, but decoded fine)
        // this collapsing bytes wrapped in a bytes string into a byte string could be an indicator of a buggy (as in: too lenient) decoder.
        val hex =
            "bf637374726d48656c6c6f2c20776f726c64216169182a686e756c6c61626c65f6646c6973749f61616162ff636d6170bf01f502f4ff65696e6e6572bf6161636c6f6cff6a696e6e6572734c6973749fbf6161636b656bffff6a62797465537472696e6742cafe696279746541727261799f383521ffff"
        assertEquals(
            test, Cbor.decodeFromHexString(
                TypesUmbrella.serializer(),
                hex
            )
        )

        val struct = Cbor.decodeFromHexString<CborElement>(hex)
        assertEquals(Cbor.encodeToCborElement(test), struct)
        assertEquals(test, Cbor.decodeFromCborElement(TypesUmbrella.serializer(), struct))

        assertEquals(hex, Cbor.encodeToHexString(TypesUmbrella.serializer(), test))
        assertEquals(hex, Cbor.encodeToHexString(CborElement.serializer(), struct))



        // with maps, lists & strings of definite length
        val hexDef =
            "a9646c6973748261616162686e756c6c61626c65f6636d6170a202f401f56169182a6a696e6e6572734c69737481a16161636b656b637374726d48656c6c6f2c20776f726c642165696e6e6572a16161636c6f6c6a62797465537472696e6742cafe6962797465417272617982383521"
        assertEquals(
            test, Cbor.decodeFromHexString(
                TypesUmbrella.serializer(),
                hexDef
            )
        )

        val structDef = Cbor.decodeFromHexString<CborElement>(hexDef)
        assertEquals(Cbor.encodeToCborElement(test), structDef)
        assertEquals(test, Cbor.decodeFromCborElement(TypesUmbrella.serializer(), structDef))

    }

    @Test
    fun testReadByteStringWhenNullable() {
        /* A1                         # map(1)
         *    6A                      # text(10)
         *       62797465537472696E67 # "byteString"
         *    44                      # bytes(4)
         *       01020304             # "\x01\x02\x03\x04"
         */
        val hex = "a16a62797465537472696e674401020304"
        val expected = NullableByteString(byteArrayOf(1, 2, 3, 4))
        assertEquals(
            expected = expected,
            actual = Cbor.decodeFromHexString(
                deserializer = NullableByteString.serializer(),
                hex = hex
            )
        )

        val struct = Cbor.decodeFromHexString<CborElement>(hex)
        assertEquals(expected, Cbor.decodeFromCborElement(NullableByteString.serializer(), struct))

        /* A1                         # map(1)
         *    6A                      # text(10)
         *       62797465537472696E67 # "byteString"
         *    F6                      # primitive(22)
         */
        val hexNull = "a16a62797465537472696e67f6"
        val expectedNull = NullableByteString(byteString = null)
        assertEquals(
            expected = expectedNull,
            actual = Cbor.decodeFromHexString(
                deserializer = NullableByteString.serializer(),
                hex = hexNull
            )
        )

        val structNull = Cbor.decodeFromHexString<CborElement>(hexNull)
        assertEquals(expectedNull, Cbor.decodeFromCborElement(NullableByteString.serializer(), structNull))
    }

    @Test
    fun testNullables() {
        assertEquals(NullableByteStringDefaultNull(), Cbor.decodeFromHexString<NullableByteStringDefaultNull>("a0"))
        val struct = Cbor.decodeFromHexString<CborElement>("a0")
        assertEquals(NullableByteStringDefaultNull(), Cbor.decodeFromCborElement(NullableByteStringDefaultNull.serializer(), struct))
    }

    /**
     * CBOR hex data represents serialized versions of [TypesUmbrella] (which does **not** have a root property 'a') so
     * decoding to [Simple] (which has the field 'a') is expected to fail.
     */
    @Test
    fun testIgnoreUnknownKeysFailsWhenCborDataIsMissingKeysThatArePresentInKotlinClass() {
        // with maps & lists of indefinite length
        val hex =
            "bf637374726d48656c6c6f2c20776f726c64216169182a686e756c6c61626c65f6646c6973749f61616162ff636d6170bf01f502f4ff65696e6e6572bf6161636c6f6cff6a696e6e6572734c6973749fbf6161636b656bffffff"

        assertFailsWithMessage<SerializationException>("Field 'a' is required") {
            ignoreUnknownKeys.decodeFromHexString(
                Simple.serializer(),
                hex
            )
        }

        val struct = Cbor.decodeFromHexString<CborElement>(hex)
        assertFailsWithMessage<SerializationException>("Field 'a' is required") {
            ignoreUnknownKeys.decodeFromCborElement(
                Simple.serializer(),
                struct
            )
        }

        // with maps & lists of definite length
        val hexDef =
            "a7646c6973748261616162686e756c6c61626c65f6636d6170a202f401f56169182a6a696e6e6572734c69737481a16161636b656b637374726d48656c6c6f2c20776f726c642165696e6e6572a16161636c6f6c"
        assertFailsWithMessage<SerializationException>("Field 'a' is required") {
            ignoreUnknownKeys.decodeFromHexString(
                Simple.serializer(),
                hexDef
            )
        }

        val structDef = Cbor.decodeFromHexString<CborElement>(hexDef)
        assertFailsWithMessage<SerializationException>("Field 'a' is required") {
            ignoreUnknownKeys.decodeFromCborElement(
                Simple.serializer(),
                structDef
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
        val hex = "a36373747266737472696e676169006669676e6f7265"
        assertFailsWithMessage<CborDecodingException>("Unexpected EOF while skipping element") {
            ignoreUnknownKeys.decodeFromHexString(
                TypesUmbrella.serializer(),
                hex
            )
        }


        assertFailsWithMessage<CborDecodingException>("Unexpected EOF") {
            Cbor.decodeFromHexString<CborElement>(hex)
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
        val hex2 = "a36373747266737472696e676169006669676e6f7265a2"
        assertFailsWithMessage<CborDecodingException>("Unexpected EOF while skipping element") {
            ignoreUnknownKeys.decodeFromHexString(
                TypesUmbrella.serializer(),
                hex2
            )
        }

        assertFailsWithMessage<CborDecodingException>("Unexpected EOF") {
            Cbor.decodeFromHexString<CborElement>(hex2)
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
        val hex = "a36373747266737472696e676669676e6f7265ff"
        assertFailsWithMessage<CborDecodingException>("Expected next data item, but found FF") {
            ignoreUnknownKeys.decodeFromHexString(
                TypesUmbrella.serializer(),
                hex
            )
        }

        assertFailsWithMessage<CborDecodingException>("Invalid simple value or float type: FF") {
            Cbor.decodeFromHexString<CborElement>(hex)
        }
    }


    @Test
    fun testDecodeCborWithUnknownField() {
        val hex = "bf616163313233616263393837ff"
        val expected = Simple("123")
        assertEquals(
            expected = expected,
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
                hex = hex
            )
        )
        val struct = Cbor.decodeFromHexString<CborElement>(hex)
        assertEquals(expected, ignoreUnknownKeys.decodeFromCborElement(Simple.serializer(), struct))

    }

    @Test
    fun testDecodeCborWithUnknownNestedIndefiniteFields() {
        val hex = "bf6161633132336162bf7f6178ffa161790aff61639f010203ffff"
        val expected = Simple("123")
        assertEquals(
            expected = expected,
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
                hex = hex
            )
        )

        val struct = Cbor.decodeFromHexString<CborElement>(hex)
        assertEquals(expected, ignoreUnknownKeys.decodeFromCborElement(Simple.serializer(), struct))
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

        val expected = SealedBox(
            listOf(
                SubSealedA("a"),
                SubSealedB(1)
            )
        )
        val hex =
            "bf6565787472618309080765626f7865649f9f782d6b6f746c696e782e73657269616c697a6174696f6e2e53696d706c655365616c65642e5375625365616c656441bf61736161646e657741bf617801617902ffffff9f782d6b6f746c696e782e73657269616c697a6174696f6e2e53696d706c655365616c65642e5375625365616c656442bf616901ffffffff"
        assertEquals(
            expected = expected,
            actual = ignoreUnknownKeys.decodeFromHexString(
                SealedBox.serializer(),
                hex
            )
        )
        val struct = Cbor.decodeFromHexString<CborElement>(hex)
        assertEquals(expected, ignoreUnknownKeys.decodeFromCborElement(SealedBox.serializer(), struct))

    }

    @Test
    fun testReadCustomByteString() {
        val expected = TypeWithCustomByteString(CustomByteString(0x11, 0x22, 0x33))
        val hex = "bf617843112233ff"
        assertEquals(
            expected = expected,
            actual = Cbor.decodeFromHexString(hex)
        )
        val struct = Cbor.decodeFromHexString<CborElement>(hex)
        assertEquals(expected, Cbor.decodeFromCborElement(TypeWithCustomByteString.serializer(), struct))

    }

    @Test
    fun testReadNullableCustomByteString() {
        val hex = "bf617843112233ff"
        val expected = TypeWithNullableCustomByteString(CustomByteString(0x11, 0x22, 0x33))
        assertEquals(
            expected = expected,
            actual = Cbor.decodeFromHexString(hex)
        )
        val struct = Cbor.decodeFromHexString<CborElement>(hex)
        assertEquals(expected, Cbor.decodeFromCborElement(TypeWithNullableCustomByteString.serializer(), struct))

    }

    @Test
    fun testReadNullCustomByteString() {
        val hex = "bf6178f6ff"
        val expected = TypeWithNullableCustomByteString(null)
        assertEquals(
            expected = expected,
            actual = Cbor.decodeFromHexString(hex)
        )
        val struct = Cbor.decodeFromHexString<CborElement>(hex)
        assertEquals(expected, Cbor.decodeFromCborElement(TypeWithNullableCustomByteString.serializer(), struct))

    }

    @Test
    fun testReadValueClassWithByteString() {
        val expected = byteArrayOf(0x11, 0x22, 0x33)
        val hex = "43112233"
        assertContentEquals(
            expected = expected,
            actual = Cbor.decodeFromHexString<ValueClassWithByteString>(hex).x
        )
        val struct = Cbor.decodeFromHexString<CborElement>(hex)
        assertContentEquals(expected, Cbor.decodeFromCborElement(ValueClassWithByteString.serializer(), struct).x)

    }

    @Test
    fun testReadValueClassCustomByteString() {
        val expected = ValueClassWithCustomByteString(CustomByteString(0x11, 0x22, 0x33))
        val hex = "43112233"
        assertEquals(
            expected = expected,
            actual = Cbor.decodeFromHexString(hex)
        )
        val struct = Cbor.decodeFromHexString<CborElement>(hex)
        assertEquals(expected, Cbor.decodeFromCborElement(ValueClassWithCustomByteString.serializer(), struct))

    }

    @Test
    fun testReadValueClassWithUnlabeledByteString() {
        val expected = byteArrayOf(
            0x11,
            0x22,
            0x33
        )
        val hex = "43112233"
        assertContentEquals(
            expected = expected,
            actual = Cbor.decodeFromHexString<ValueClassWithUnlabeledByteString>(hex).x.x
        )
        val struct = Cbor.decodeFromHexString<CborElement>(hex)
        assertContentEquals(expected, Cbor.decodeFromCborElement(ValueClassWithUnlabeledByteString.serializer(), struct).x.x)

    }

    @Test
    fun testMismatchedByteStringRepresentationError() {
        @Serializable class BytesHolder(@ByteString val bytes: ByteArray)
        @Serializable class ArrayHolder(val bytes: ByteArray)

        assertFailsWithMessage<CborDecodingException>("Expected a start of array, but found 40, " +
            "which corresponds to the start of a byte string. " +
            "Make sure you correctly set 'alwaysUseByteString' setting " +
            "and/or 'kotlinx.serialization.cbor.ByteString' annotation.") {
            Cbor.decodeFromHexString<ArrayHolder>(Cbor.encodeToHexString(BytesHolder(ByteArray(0))))
        }

        assertFailsWithMessage<CborDecodingException>("Expected a start of a byte string, but found 9F, " +
            "which corresponds to the start of an array. " +
            "Make sure you correctly set 'alwaysUseByteString' setting " +
            "and/or 'kotlinx.serialization.cbor.ByteString' annotation.") {
            Cbor.decodeFromHexString<BytesHolder>(Cbor.encodeToHexString(ArrayHolder(ByteArray(0))))
        }
    }

    @Test
    fun testIndefiniteLengthTextStrings() {
        @Serializable
        class StringHolder(val string: String)

        assertEquals(
            "hello world",
            Cbor.decodeFromHexString<StringHolder>("BF66737472696E677F6568656C6C6F612065776F726C64FFFF").string
        )


        // Read a truncated sequence
        assertFailsWithMessage<CborDecodingException>("Unexpected end of encoded CBOR document") {
            Cbor.decodeFromHexString<StringHolder>("BF66737472696E677F6568656C6C6F612065776F726C64")
        }

        // Nested indefinite-length string (illegal as per https://www.rfc-editor.org/rfc/rfc8949.html#name-indefinite-length-byte-stri)
        // BF
        //   66 # key
        //     737472696E67
        //   7F # value, indefinite-length string
        //     65 # text chunk, 5 bytes
        //       68656C6C6F
        //     7F # indefinite length chunk
        //       61 # definite length cheunk
        //          20
        //     FF
        //     65 # text chunk, 5 bytes
        //       776F726C64
        //   FF # end of value
        // FF # end of object
        assertFailsWith<CborDecodingException> {
            Cbor.decodeFromHexString<StringHolder>("BF66737472696E677F6568656C6C6F7F6120FF65776F726C64FFFF")
        }

        // Indefinite-length string can only consist of definite-length string of the same major type
        // (see https://www.rfc-editor.org/rfc/rfc8949.html#name-indefinite-length-byte-stri).
        // Here, one of the chunks is an integer value.
        // BF
        //   66 # key
        //     737472696E67
        //   7F # value
        //     65 # text chunk, 5 bytes
        //       68656C6C6F
        //     01 # integer value, 1
        //     65 # text chunk, 5 bytes
        //       776F726C64
        //   FF
        // FF
        assertFailsWith<CborDecodingException> {
            Cbor.decodeFromHexString<StringHolder>("BF66737472696E677F6568656C6C6F0165776F726C64FFFF")
        }

        // Indefinite-length string can only consist of definite-length string of the same major type
        // (see https://www.rfc-editor.org/rfc/rfc8949.html#name-indefinite-length-byte-stri).
        // Here, one of the chunks is a byte-string.
        // BF
        //   66 # key
        //     737472696E67
        //   7F # value
        //     65 # text chunk, 5 bytes
        //       68656C6C6F
        //     41 # byte-string, 1 byte
        //       20
        //     65 # text chunk, 5 bytes
        //       776F726C64
        //   FF
        // FF
        assertFailsWith<CborDecodingException> {
            Cbor.decodeFromHexString<StringHolder>("BF66737472696E677F6568656C6C6F412065776F726C64FFFF")
        }

        // Indefinite-length string can only consist of definite-length string of the same major type
        // (see https://www.rfc-editor.org/rfc/rfc8949.html#name-indefinite-length-byte-stri).
        // Here, one of the chunks is a regular array.
        // BF
        //   66 # key
        //     737472696E67
        //   7F # value
        //     65 # text chunk, 5 bytes
        //       68656C6C6F
        //     81 # array
        //       1820
        //     65 # text chunk, 5 bytes
        //       776F726C64
        //   FF
        // FF
        assertFailsWith<CborDecodingException> {
            Cbor.decodeFromHexString<StringHolder>("BF66737472696E677F6568656C6C6F81182065776F726C64FFFF")
        }
    }

    @Test
    fun testIndefiniteLengthByteStrings() {
        @Serializable
        class BytesHolder(@ByteString val bytes: ByteArray)

        assertContentEquals(
            byteArrayOf(0, 1, 2),
            Cbor.decodeFromHexString<BytesHolder>("BF6562797465735F4100420102FFFF").bytes
        )

        // Read a truncated sequence
        assertFailsWithMessage<CborDecodingException>("Unexpected end of encoded CBOR document") {
            Cbor.decodeFromHexString<BytesHolder>("BF6562797465735F4100420102")
        }

        // Nested indefinite-length byte-strings are illegal
        // (see https://www.rfc-editor.org/rfc/rfc8949.html#name-indefinite-length-byte-stri).
        //
        // BF
        //   65 # key
        //     6279746573
        //   5F # value
        //     5F # nested byte-string
        //       41 # fixed-length chunk
        //         00
        //     FF
        //     42 # fixed-length chunk
        //       0102
        //   FF
        // FF
        assertFailsWith<CborDecodingException> {
            Cbor.decodeFromHexString<BytesHolder>("BF6562797465735F5F4100FF420102FFFF")
        }

        // Indefinite-length byte-string can only consist of definite-length string of the same major type
        // (see https://www.rfc-editor.org/rfc/rfc8949.html#name-indefinite-length-byte-stri).
        // Here, one of the chunks is an integer value.
        // BF
        //   65 # key
        //     6279746573
        //   5F # value
        //     00 # integer value (0)
        //     42 # fixed-length chunk
        //       0102
        //   FF
        // FF
        assertFailsWith<CborDecodingException> {
            Cbor.decodeFromHexString<BytesHolder>("BF6562797465735F00420102FFFF")
        }

        // Indefinite-length string can only consist of definite-length string of the same major type
        // (see https://www.rfc-editor.org/rfc/rfc8949.html#name-indefinite-length-byte-stri).
        // Here, one of the chunks is a text-string.
        // BF
        //   65 # key
        //     6279746573
        //   5F # value
        //     61 # text-string
        //       00
        //     42 # fixed-length chunk
        //       0102
        //   FF
        // FF
        assertFailsWith<CborDecodingException> {
            Cbor.decodeFromHexString<BytesHolder>("BF6562797465735F6100420102FFFF")
        }

        // Indefinite-length string can only consist of definite-length string of the same major type
        // (see https://www.rfc-editor.org/rfc/rfc8949.html#name-indefinite-length-byte-stri).
        // Here, one of the chunks is an array.
        // BF
        //   65 # key
        //     6279746573
        //   5F # value
        //     81 # array
        //       00
        //     42 # fixed-length chunk
        //       0102
        //   FF
        // FF
        assertFailsWith<CborDecodingException> {
            Cbor.decodeFromHexString<BytesHolder>("BF6562797465735F8100420102FFFF")
        }
    }

    private inline fun <reified T> checkDecodingException(inputHexString: String, exceptionMessage: String, cbor: BinaryFormat = Cbor) {
        assertFailsWithMessage<CborDecodingException>(exceptionMessage) {
            cbor.decodeFromHexString<T>(inputHexString)
        }
    }

    @Test
    fun testEOF() {
        val eof = "Unexpected end of encoded CBOR document"
        checkDecodingException<Int>("", eof)
        checkDecodingException<Boolean>("", eof)
        checkDecodingException<Double>("", eof)
        checkDecodingException<String>("", eof)
        checkDecodingException<ByteArray>("", eof)
        checkDecodingException<Map<String, String>>("", eof)
        // Fixed length map, missing the second key-value pair
        checkDecodingException<Map<String, String>>("A261616161", eof)
        // Fixed length map, missing the second key-value pair's key is truncated
        checkDecodingException<Map<String, String>>("A26161616161", "Unexpected EOF, available 0 bytes, requested: 1")
        // Fixed length map, missing the second key-value pair's value is missing
        checkDecodingException<Map<String, String>>("A2616161616161", eof)
        // Fixed length array, second element is missing
        checkDecodingException<List<String>>("826161", eof)
        // Fixed length array, second element is corrupted
        checkDecodingException<List<String>>("82616161", "Unexpected EOF, available 0 bytes, requested: 1")
        // Only tag is present
        checkDecodingException<Int>("C0", eof)
        // Only tag is present, but it's truncated
        checkDecodingException<Int>("D7", eof)
    }

    @Test
    fun testValueTruncation() {
        @Serializable data class IntHolder(val v: Int)
        @Serializable data class ShortHolder(val v: Short)
        @Serializable data class ByteHolder(val v: Byte)

        //          18: single-byte int \  / 2A
        val singleByteValue = "BF6176182AFF"
        //       39: two-byte int \   / -16162
        val twoByteValue = "BF6176393F21FF"
        //       19: two-byte int \   / C0DE
        val twoByteValueWithShortOverflow = "BF617619C0DEFF"
        //       1A: four-byte int \  / 0BADC0DE
        val fourByteValue = "BF61761A0BADC0DEFF"
        //       1B: eight-byte int \  / 0BADC0DE15BAD000
        val eightByteValue = "BF61761B0BADC0DE15BAD000FF"

        assertEquals(0x2A, Cbor.decodeFromHexString<IntHolder>(singleByteValue).v)
        assertEquals(0xC0DE, Cbor.decodeFromHexString<IntHolder>(twoByteValueWithShortOverflow).v)
        assertEquals(0xBADC0DE, Cbor.decodeFromHexString<IntHolder>(fourByteValue).v)
        assertFailsWith<CborDecodingException> { Cbor.decodeFromHexString<IntHolder>(eightByteValue) }

        assertEquals(0x2A, Cbor.decodeFromHexString<ShortHolder>(singleByteValue).v)
        assertEquals(0xC0DE.toShort(), Cbor.decodeFromHexString<ShortHolder>(twoByteValue).v)
        assertFailsWith<CborDecodingException> { Cbor.decodeFromHexString<ShortHolder>(twoByteValueWithShortOverflow) }
        assertFailsWith<CborDecodingException> { Cbor.decodeFromHexString<ShortHolder>(fourByteValue) }
        assertFailsWith<CborDecodingException> { Cbor.decodeFromHexString<ShortHolder>(eightByteValue) }

        assertEquals(0x2A, Cbor.decodeFromHexString<ByteHolder>(singleByteValue).v)
        assertFailsWith<CborDecodingException> { Cbor.decodeFromHexString<ByteHolder>(twoByteValue) }
        assertFailsWith<CborDecodingException> { Cbor.decodeFromHexString<ByteHolder>(fourByteValue) }
        assertFailsWith<CborDecodingException> { Cbor.decodeFromHexString<ByteHolder>(eightByteValue) }
    }

    // Check behavior for examples from https://www.rfc-editor.org/rfc/rfc8949.html#section-f.1
    @Test
    fun rfcWellFormednessComplianceTest() {
        val shortEOF = "Unexpected EOF"
        val longEOF = "Unexpected end of encoded CBOR document"
        val CborABS = Cbor { alwaysUseByteString = true }

        // End of input in a head
        checkDecodingException<Long>("18", shortEOF)
        checkDecodingException<Long>("19", shortEOF)
        checkDecodingException<Long>("1A", shortEOF)
        checkDecodingException<Long>("1B", shortEOF)
        checkDecodingException<Long>("1901", shortEOF)
        checkDecodingException<Long>("1A0102", shortEOF)
        checkDecodingException<Long>("1B01020304050607", shortEOF)
        checkDecodingException<Long>("38", shortEOF)
        checkDecodingException<ByteArray>("58", shortEOF, CborABS)
        checkDecodingException<String>("78", shortEOF)
        checkDecodingException<IntArray>("98", shortEOF)
        checkDecodingException<IntArray>("9A01FF00", shortEOF)
        checkDecodingException<Map<String, String>>("B8", shortEOF)
        checkDecodingException<Int>("D8", shortEOF)
        // we don't support arbitrary simple values
        // checkEof<Boolean>("F8") }
        checkDecodingException<Double>("F900", shortEOF)
        checkDecodingException<Double>("FA0000", shortEOF)
        checkDecodingException<Double>("FB000000", shortEOF)

        // Definite-length strings with short data
        checkDecodingException<ByteArray>("41", shortEOF, CborABS)
        checkDecodingException<String>("61", shortEOF)
        checkDecodingException<ByteArray>("5AFFFFFFFF00", "length for byte string is too large", CborABS)
        checkDecodingException<ByteArray>("5BFFFFFFFFFFFFFFFF010203", "negative length value was decoded for byte string", CborABS)
        checkDecodingException<String>("7AFFFFFFFF00", "length for string is too large")
        checkDecodingException<String>("7B7FFFFFFFFFFFFFFF010203", "length for string is too large")

        // Definite-length maps and arrays not closed with enough items:
        checkDecodingException<IntArray>("81", longEOF)
        checkDecodingException<Array<Array<Array<Array<Array<Array<Array<Array<Array<Int>>>>>>>>>>("818181818181818181", longEOF)
        checkDecodingException<IntArray>("8200", longEOF)
        checkDecodingException<Map<Int, Int>>("A1", longEOF)
        checkDecodingException<Map<Int, Int>>("A20102", longEOF)
        checkDecodingException<Map<Int, Int>>("A100", longEOF)
        checkDecodingException<Map<Int, Int>>("A2000000", longEOF)

        // Tag number not followed by tag content
        checkDecodingException<Long>("C0", longEOF)

        // Indefinite-length strings not closed by a "break" stop code:
        checkDecodingException<ByteArray>("5F4100", longEOF, CborABS)
        checkDecodingException<String>("7F6100", longEOF)

        // Indefinite-length maps and arrays not closed by a "break" stop code:
        checkDecodingException<IntArray>("9F", longEOF)
        checkDecodingException<IntArray>("9F0102", longEOF)
        checkDecodingException<Map<Int, Int>>("BF", longEOF)
        checkDecodingException<Map<Int, Int>>("BF01020102", longEOF)
        checkDecodingException<Array<Array<Int>>>("819F", longEOF)
        checkDecodingException<IntArray>("9F8000", "Expected an unsigned or negative integer, but found 80")
        checkDecodingException<IntArray>("9F9F9F9F9FFFFFFFFF", "Expected an unsigned or negative integer, but found 9F")
        checkDecodingException<IntArray>("9F819F819F9FFFFFFF", "Expected an unsigned or negative integer, but found 81")
    }

    @Test
    fun testTrailingBytesAfterDecoding() {
        @Serializable
        class BytesHolder(@ByteString val bytes: ByteArray)

        val paddedInput = "BF6562797465735F4100420102FFFFBADBADBADBAD"
        assertFailsWith<CborDecodingException> {
            Cbor.decodeFromHexString<BytesHolder>(paddedInput)
        }
    }
}
