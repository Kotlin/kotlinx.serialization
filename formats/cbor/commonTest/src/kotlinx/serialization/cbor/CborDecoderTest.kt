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
        assertEquals(
            test, Cbor.decodeFromHexString(
                TypesUmbrella.serializer(),
                "bf637374726d48656c6c6f2c20776f726c64216169182a686e756c6c61626c65f6646c6973749f61616162ff636d6170bf01f502f4ff65696e6e6572bf6161636c6f6cff6a696e6e6572734c6973749fbf6161636b656bffff6a62797465537472696e675f42cafeff696279746541727261799f383521ffff"
            )
        )
        // with maps, lists & strings of definite length
        assertEquals(
            test, Cbor.decodeFromHexString(
                TypesUmbrella.serializer(),
                "a9646c6973748261616162686e756c6c61626c65f6636d6170a202f401f56169182a6a696e6e6572734c69737481a16161636b656b637374726d48656c6c6f2c20776f726c642165696e6e6572a16161636c6f6c6a62797465537472696e6742cafe6962797465417272617982383521"
            )
        )
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

    @Test
    fun testNullables() {
        Cbor.decodeFromHexString<NullableByteStringDefaultNull>("a0")
    }

    /**
     * CBOR hex data represents serialized versions of [TypesUmbrella] (which does **not** have a root property 'a') so
     * decoding to [Simple] (which has the field 'a') is expected to fail.
     */
    @Test
    fun testIgnoreUnknownKeysFailsWhenCborDataIsMissingKeysThatArePresentInKotlinClass() {
        // with maps & lists of indefinite length
        assertFailsWithMessage<SerializationException>("Field 'a' is required") {
            ignoreUnknownKeys.decodeFromHexString(
                Simple.serializer(),
                "bf637374726d48656c6c6f2c20776f726c64216169182a686e756c6c61626c65f6646c6973749f61616162ff636d6170bf01f502f4ff65696e6e6572bf6161636c6f6cff6a696e6e6572734c6973749fbf6161636b656bffffff"
            )
        }

        // with maps & lists of definite length
        assertFailsWithMessage<SerializationException>("Field 'a' is required") {
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

    @Test
    fun testReadCustomByteString() {
        assertEquals(
            expected = TypeWithCustomByteString(CustomByteString(0x11, 0x22, 0x33)),
            actual = Cbor.decodeFromHexString("bf617843112233ff")
        )
    }

    @Test
    fun testReadNullableCustomByteString() {
        assertEquals(
            expected = TypeWithNullableCustomByteString(CustomByteString(0x11, 0x22, 0x33)),
            actual = Cbor.decodeFromHexString("bf617843112233ff")
        )
    }

    @Test
    fun testReadNullCustomByteString() {
        assertEquals(
            expected = TypeWithNullableCustomByteString(null),
            actual = Cbor.decodeFromHexString("bf6178f6ff")
        )
    }

    @Test
    fun testReadValueClassWithByteString() {
        assertContentEquals(
            expected = byteArrayOf(0x11, 0x22, 0x33),
            actual = Cbor.decodeFromHexString<ValueClassWithByteString>("43112233").x
        )
    }

    @Test
    fun testReadValueClassCustomByteString() {
        assertEquals(
            expected = ValueClassWithCustomByteString(CustomByteString(0x11, 0x22, 0x33)),
            actual = Cbor.decodeFromHexString("43112233")
        )
    }

    @Test
    fun testReadValueClassWithUnlabeledByteString() {
        assertContentEquals(
            expected = byteArrayOf(
                0x11,
                0x22,
                0x33
            ),
            actual = Cbor.decodeFromHexString<ValueClassWithUnlabeledByteString>("43112233").x.x
        )
    }

}
