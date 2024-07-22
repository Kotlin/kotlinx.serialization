/*
 * Copyright 2017-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(ExperimentalUnsignedTypes::class)

package kotlinx.serialization.cbor

import kotlinx.serialization.*
import kotlinx.serialization.cbor.internal.*
import kotlin.test.*


@Serializable
data class DataWithTags(
    @ValueTags(12uL)
    val a: ULong,

    @KeyTags(34uL)
    val b: Int,

    @KeyTags(56uL)
    @ValueTags(78uL)
    @ByteString val c: ByteArray,

    @ValueTags(90uL, 12uL)
    val d: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DataWithTags

        if (a != other.a) return false
        if (b != other.b) return false
        if (!c.contentEquals(other.c)) return false
        return d == other.d
    }

    override fun hashCode(): Int {
        var result = a.hashCode()
        result = 31 * result + b
        result = 31 * result + c.contentHashCode()
        result = 31 * result + d.hashCode()
        return result
    }
}

class CborTaggedTest {

    private val reference = DataWithTags(
        a = 0xFFFFFFFuL,
        b = -1,
        c = byteArrayOf(0xCA.toByte(), 0xFE.toByte()),
        d = "Hello World"
    )

    /*
     * BF                                 # map(*)
     *    61                              # text(1)
     *       61                           # "a"
     *    CC                              # tag(12)
     *       1A 0FFFFFFF                  # unsigned(268435455)
     *    D8 22                           # tag(34)
     *       61                           # text(1)
     *          62                        # "b"
     *    20                              # negative(0)
     *    D8 38                           # tag(56)
     *       61                           # text(1)
     *          63                        # "c"
     *    D8 4E                           # tag(78)
     *       42                           # bytes(2)
     *          CAFE                      # "\xCA\xFE"
     *    61                              # text(1)
     *       64                           # "d"
     *    D8 5A                           # tag(90)
     *       CC                           # tag(12)
     *          6B                        # text(11)
     *             48656C6C6F20576F726C64 # "Hello World"
     *    FF                              # primitive(*)
     */
    private val referenceHexString =
        "bf6161cc1a0fffffffd822616220d8386163d84e42cafe6164d85acc6b48656c6c6f20576f726c64ff"

    /*
     * A4                                 # map(4)
     *    61                              # text(1)
     *       61                           # "a"
     *    CC                              # tag(12)
     *       1A 0FFFFFFF                  # unsigned(268435455)
     *    D8 22                           # tag(34)
     *       61                           # text(1)
     *          62                        # "b"
     *    20                              # negative(0)
     *    D8 38                           # tag(56)
     *       61                           # text(1)
     *          63                        # "c"
     *    D8 4E                           # tag(78)
     *       42                           # bytes(2)
     *          CAFE                      # "\xCA\xFE"
     *    61                              # text(1)
     *       64                           # "d"
     *    D8 5A                           # tag(90)
     *       CC                           # tag(12)
     *          6B                        # text(11)
     *             48656C6C6F20576F726C64 # "Hello World"
     */
    private val referenceHexStringDefLen =
        "a46161cc1a0fffffffd822616220d8386163d84e42cafe6164d85acc6b48656c6c6f20576f726c64"

    /*
     * BF                                 # map(*)
     *    61                              # text(1)
     *       61                           # "a"
     *    CC                              # tag(12)
     *       1A 0FFFFFFF                  # unsigned(268435455)
     *    61                              # text(1)
     *       62                           # "b"
     *    20                              # negative(0)
     *    61                              # text(1)
     *       63                           # "c"
     *    D8 4E                           # tag(78)
     *       42                           # bytes(2)
     *          CAFE                      # "\xCA\xFE"
     *    61                              # text(1)
     *       64                           # "d"
     *    D8 5A                           # tag(90)
     *       CC                           # tag(12)
     *          6B                        # text(11)
     *             48656C6C6F20576F726C64 # "Hello World"
     *    FF                              # primitive(*)
     */
    private val noKeyTags = "bf6161cc1a0fffffff6162206163d84e42cafe6164d85acc6b48656c6c6f20576f726c64ff"

    /*
     * A4                                 # map(4)
     *    61                              # text(1)
     *       61                           # "a"
     *    CC                              # tag(12)
     *       1A 0FFFFFFF                  # unsigned(268435455)
     *    61                              # text(1)
     *       62                           # "b"
     *    20                              # negative(0)
     *    61                              # text(1)
     *       63                           # "c"
     *    D8 4E                           # tag(78)
     *       42                           # bytes(2)
     *          CAFE                      # "\xCA\xFE"
     *    61                              # text(1)
     *       64                           # "d"
     *    D8 5A                           # tag(90)
     *       CC                           # tag(12)
     *          6B                        # text(11)
     *             48656C6C6F20576F726C64 # "Hello World"
     *
     *
     */
    private val noKeyTagsDefLen = "a46161cc1a0fffffff6162206163d84e42cafe6164d85acc6b48656c6c6f20576f726c64"

    /*
     * BF                           # map(*)
     *    61                        # text(1)
     *       61                     # "a"
     *    1A 0FFFFFFF               # unsigned(268435455)
     *    D8 22                     # tag(34)
     *       61                     # text(1)
     *          62                  # "b"
     *    20                        # negative(0)
     *    D8 38                     # tag(56)
     *       61                     # text(1)
     *          63                  # "c"
     *    42                        # bytes(2)
     *       CAFE                   # "\xCA\xFE"
     *    61                        # text(1)
     *       64                     # "d"
     *    6B                        # text(11)
     *       48656C6C6F20576F726C64 # "Hello World"
     *    FF                        # primitive(*)
     */
    private val noValueTags = "bf61611a0fffffffd822616220d838616342cafe61646b48656c6c6f20576f726c64ff"

    /*
     * BF                           # map(*)
     *    61                        # text(1)
     *       61                     # "a"
     *    1A 0FFFFFFF               # unsigned(268435455)
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
     *       48656C6C6F20576F726C64 # "Hello World"
     *    FF                        # primitive(*)
     *
     */
    private val noTags = "bf61611a0fffffff616220616342cafe61646b48656c6c6f20576f726c64ff"

    /*
     * A4                           # map(4)
     *    61                        # text(1)
     *       61                     # "a"
     *    1A 0FFFFFFF               # unsigned(268435455)
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
     *       48656C6C6F20576F726C64 # "Hello World"
     *
     */
    private val noTagsDefLen = "a461611a0fffffff616220616342cafe61646b48656c6c6f20576f726c64"

    @Test
    fun writeReadVerifyTaggedClass() {
        assertEquals(referenceHexString, Cbor {
            useDefiniteLengthEncoding = false
            encodeKeyTags = true
            encodeValueTags = true
            encodeObjectTags = true
            verifyKeyTags = true
            verifyValueTags = true
            verifyObjectTags = true
        }.encodeToHexString(DataWithTags.serializer(), reference))
        assertEquals(
            referenceHexStringDefLen,
            Cbor {
                useDefiniteLengthEncoding = true
                encodeKeyTags = true
                encodeValueTags = true
                encodeObjectTags = true
                verifyKeyTags = true
                verifyValueTags = true
                verifyObjectTags = true
            }.encodeToHexString(DataWithTags.serializer(), reference)
        )
        assertEquals(reference, Cbor.CoseCompliant.decodeFromHexString(DataWithTags.serializer(), referenceHexString))
        assertEquals(
            reference,
            Cbor.CoseCompliant.decodeFromHexString(DataWithTags.serializer(), referenceHexStringDefLen)
        )
    }

    @Test
    fun writeReadUntaggedKeys() {
        assertEquals(noKeyTags, Cbor {
            encodeKeyTags = false
            encodeValueTags = true
            encodeObjectTags = true
            verifyKeyTags = false
            verifyValueTags = true
            verifyObjectTags = true
        }.encodeToHexString(DataWithTags.serializer(), reference))
        assertEquals(
            noKeyTagsDefLen,
            Cbor {
                useDefiniteLengthEncoding = true
                encodeKeyTags = false
                encodeValueTags = true
                encodeObjectTags = true
                verifyKeyTags = true
                verifyValueTags = true
                verifyObjectTags = true
            }.encodeToHexString(
                DataWithTags.serializer(),
                reference
            )
        )
        assertEquals(reference, Cbor {
            encodeKeyTags = true
            encodeValueTags = true
            encodeObjectTags = true
            verifyValueTags = true
            verifyObjectTags = true
            verifyKeyTags = false
        }.decodeFromHexString(noKeyTags))
        assertEquals(reference, Cbor {
            encodeKeyTags = true
            encodeValueTags = true
            encodeObjectTags = true
            verifyValueTags = true
            verifyObjectTags = true
            verifyKeyTags = false
        }.decodeFromHexString(noKeyTagsDefLen))
        assertEquals(reference, Cbor {
            encodeKeyTags = true
            encodeValueTags = true
            encodeObjectTags = true
            verifyValueTags = true
            verifyObjectTags = true
            verifyKeyTags = false
        }.decodeFromHexString(referenceHexString))

        assertFailsWith(CborDecodingException::class) {
            Cbor.CoseCompliant.decodeFromHexString(
                DataWithTags.serializer(),
                noKeyTags
            )
        }

        assertFailsWith(CborDecodingException::class) {
            Cbor {
                encodeKeyTags = true
                encodeValueTags = true
                encodeObjectTags = true
                verifyValueTags = true
                verifyObjectTags = true
                verifyKeyTags = false
            }.decodeFromHexString(DataWithTags.serializer(), noValueTags)
        }
    }

    @Test
    fun writeReadUntaggedValues() {
        assertEquals(
            noValueTags,
            Cbor {
                encodeKeyTags = true
                encodeObjectTags = true
                verifyKeyTags = true
                verifyValueTags = true
                verifyObjectTags = true
                encodeValueTags = false
            }.encodeToHexString(DataWithTags.serializer(), reference)
        )
        assertEquals(reference, Cbor {
            encodeKeyTags = true
            encodeValueTags = true
            encodeObjectTags = true
            verifyKeyTags = true
            verifyObjectTags = true
            verifyValueTags = false
        }.decodeFromHexString(noValueTags))
        assertEquals(reference, Cbor {
            encodeKeyTags = true
            encodeValueTags = true
            encodeObjectTags = true
            verifyKeyTags = true
            verifyObjectTags = true
            verifyValueTags = false
        }.decodeFromHexString(referenceHexString))

        assertFailsWith(CborDecodingException::class) {
            Cbor {
                encodeKeyTags = true
                encodeValueTags = true
                encodeObjectTags = true
                verifyKeyTags = true
                verifyValueTags = true
                verifyObjectTags = true
            }.decodeFromHexString(
                DataWithTags.serializer(),
                noValueTags
            )
        }

        assertFailsWith(CborDecodingException::class) {
            Cbor {
                encodeKeyTags = true
                encodeValueTags = true
                encodeObjectTags = true
                verifyKeyTags = true
                verifyObjectTags = true
                verifyValueTags = false
            }.decodeFromHexString(
                DataWithTags.serializer(),
                noKeyTags
            )
        }

    }

    @Test
    fun writeReadUntaggedEverything() {
        assertEquals(
            noTags,
            Cbor {
                encodeObjectTags = true
                verifyKeyTags = true
                verifyValueTags = true
                verifyObjectTags = true
                encodeValueTags = false
                encodeKeyTags = false
            }.encodeToHexString(DataWithTags.serializer(), reference)
        )
        assertEquals(
            noTagsDefLen,
            Cbor {
                encodeObjectTags = true
                verifyKeyTags = true
                verifyValueTags = true
                verifyObjectTags = true
                encodeValueTags = false
                encodeKeyTags = false
                useDefiniteLengthEncoding = true
            }.encodeToHexString(DataWithTags.serializer(), reference)
        )

        assertEquals(reference, Cbor {
            encodeKeyTags = true
            encodeValueTags = true
            encodeObjectTags = true
            verifyObjectTags = true
            verifyKeyTags = false
            verifyValueTags = false
        }.decodeFromHexString(noTags))

        assertEquals(reference, Cbor {
            encodeKeyTags = true
            encodeValueTags = true
            encodeObjectTags = true
            verifyObjectTags = true
            verifyKeyTags = false
            verifyValueTags = false
        }.decodeFromHexString(noTagsDefLen))

        assertEquals(reference, Cbor {
            encodeKeyTags = true
            encodeValueTags = true
            encodeObjectTags = true
            verifyObjectTags = true
            verifyKeyTags = false
            verifyValueTags = false
            useDefiniteLengthEncoding = true
        }.decodeFromHexString(noTags))

        assertEquals(reference, Cbor {
            encodeKeyTags = true
            encodeValueTags = true
            encodeObjectTags = true
            verifyObjectTags = true
            verifyKeyTags = false
            verifyValueTags = false
            useDefiniteLengthEncoding = true
        }.decodeFromHexString(noTagsDefLen))

        assertEquals(reference, Cbor {
            encodeKeyTags = true
            encodeValueTags = true
            encodeObjectTags = true
            verifyObjectTags = true
            verifyKeyTags = false
            verifyValueTags = false
        }.decodeFromHexString(noKeyTags))

        assertEquals(reference, Cbor {
            encodeKeyTags = true
            encodeValueTags = true
            encodeObjectTags = true
            verifyObjectTags = true
            verifyKeyTags = false
            verifyValueTags = false
        }.decodeFromHexString(noValueTags))

        assertEquals(reference, Cbor {
            encodeKeyTags = true
            encodeValueTags = true
            encodeObjectTags = true
            verifyObjectTags = true
            verifyKeyTags = false
            verifyValueTags = false
        }.decodeFromHexString(referenceHexString))

        assertFailsWith(CborDecodingException::class) {
            Cbor {
                encodeKeyTags = true
                encodeValueTags = true
                encodeObjectTags = true
                verifyKeyTags = true
                verifyValueTags = true
                verifyObjectTags = true
            }.decodeFromHexString(
                DataWithTags.serializer(),
                noTags
            )
        }

    }

    @Test
    fun wrongTags() {
        val wrongTag55ForPropertyC = "A46161CC1A0FFFFFFFD822616220D8376163D84E42CAFE6164D85ACC6B48656C6C6F20576F726C64"
        listOf(
            Cbor {
                encodeKeyTags = true
                encodeValueTags = true
                encodeObjectTags = true
                verifyKeyTags = true
                verifyValueTags = true
                verifyObjectTags = true
            },
            Cbor {
                encodeKeyTags = true
                encodeValueTags = true
                encodeObjectTags = true
                verifyKeyTags = true
                verifyValueTags = true
                verifyObjectTags = true
                useDefiniteLengthEncoding = true
            }).forEach { cbor ->

            assertContains(
                assertFailsWith(
                    CborDecodingException::class,
                    message = "CBOR tags [55] do not match declared tags [56]"
                ) {
                    cbor.decodeFromHexString(
                        DataWithTags.serializer(),
                        wrongTag55ForPropertyC
                    )
                }.message ?: "", "CBOR tags [55] do not match expected tags [56]"
            )
        }
        listOf(
            Cbor {
                encodeKeyTags = true
                encodeValueTags = true
                encodeObjectTags = true
                verifyValueTags = true
                verifyObjectTags = true
                verifyKeyTags = false
                useDefiniteLengthEncoding = true
            },
            Cbor {
                encodeKeyTags = true
                encodeValueTags = true
                encodeObjectTags = true
                verifyValueTags = true
                verifyObjectTags = true
                verifyKeyTags = false
            }).forEach { cbor ->
            assertEquals(reference, cbor.decodeFromHexString(wrongTag55ForPropertyC))
        }
    }


    @Test
    fun objectTags() {
        /**
         * D9 0539         # tag(1337)
         *    BF           # map(*)
         *       63        # text(3)
         *          616C67 # "alg"
         *       13        # unsigned(19)
         *       FF        # primitive(*)
         */
        val referenceHexString = "d90539bf63616c6713ff"
        val untaggedHexString = "bf63616c6713ff" //no ObjectTags
        val reference = ClassAsTagged(19)

        val cbor = Cbor {
            encodeKeyTags = true
            encodeValueTags = true
            verifyKeyTags = true
            verifyValueTags = true
            useDefiniteLengthEncoding = false
            verifyObjectTags = true
            encodeObjectTags = true
        }

        assertEquals(referenceHexString, cbor.encodeToHexString(ClassAsTagged.serializer(), reference))
        assertEquals(reference, cbor.decodeFromHexString(ClassAsTagged.serializer(), referenceHexString))

        assertEquals(
            reference,
            Cbor { verifyObjectTags = false }.decodeFromHexString(ClassAsTagged.serializer(), referenceHexString)
        )

        assertEquals(
            untaggedHexString,
            Cbor { encodeObjectTags = false }.encodeToHexString(ClassAsTagged.serializer(), reference)
        )


        assertEquals(
            reference,
            Cbor { verifyObjectTags = false }.decodeFromHexString(ClassAsTagged.serializer(), untaggedHexString)
        )

        assertContains(
            assertFailsWith(CborDecodingException::class) {
                cbor.decodeFromHexString(ClassAsTagged.serializer(), untaggedHexString)
            }.message ?: "", "do not match expected tags"
        )

        /**
         * 81                 # array(1)
         *    D9 0539         # tag(1337)
         *       A1           # map(1)
         *          63        # text(3)
         *             616C67 # "alg"
         *          13        # unsigned(19)
         */
        val listOfObjectTagged = listOf(reference)
        assertEquals("81d90539a163616c6713", Cbor.CoseCompliant.encodeToHexString(listOfObjectTagged))


    }


    @Test
    fun nestedObjectTags() {
        /**
         * BF                                 # map(*)
         *    63                              # text(3)
         *       616C67                       # "alg"
         *    0D                              # unsigned(13)
         *    64                              # text(4)
         *       696E7473                     # "ints"
         *    D3                              # tag(19)
         *       9F                           # array(*)
         *          18 1A                     # unsigned(26)
         *          18 18                     # unsigned(24)
         *          FF                        # primitive(*)
         *    69                              # text(9)
         *       6F626A546167676564           # "objTagged"
         *    D8 2A                           # tag(42)
         *       D9 0539                      # tag(1337)
         *          BF                        # map(*)
         *             63                     # text(3)
         *                616C67              # "alg"
         *             13                     # unsigned(19)
         *             FF                     # primitive(*)
         *    6E                              # text(14)
         *       6F626A5461676765644172726179 # "objTaggedArray"
         *    D8 2A                           # tag(42)
         *       9F                           # array(*)
         *          D9 0539                   # tag(1337)
         *             BF                     # map(*)
         *                63                  # text(3)
         *                   616C67           # "alg"
         *                19 03E8             # unsigned(1000)
         *                FF                  # primitive(*)
         *          FF                        # primitive(*)
         *    FF                              # primitive(*)
         */
        val referenceHexString =
            "bf63616c670d64696e7473d39f181a1818ff696f626a546167676564d82ad90539bf63616c6713ff6e6f626a5461676765644172726179d9038f9fd90539bf63616c671903e8ffffff"
        val referenceHexStringWithBogusTag =
            "bf63616c670d64696e7473d3d49f181a1818ff696f626a546167676564d82ad90539bf63616c6713ff6e6f626a5461676765644172726179d9038f9fd90539bf63616c671903e8ffffff"
        val referenceHexStringWithMissingTag =
            "bf63616c670d64696e74739f181a1818ff696f626a546167676564d82ad90539bf63616c6713ff6e6f626a5461676765644172726179d9038f9fd90539bf63616c671903e8ffffff"

        val superfluousTagged =
            "bf63616c670d64696e7473d39f181a1818ff696f626a546167676564d82ad90540d90539bf63616c6713ff6e6f626a5461676765644172726179d9038f9fd90539bf63616c671903e8ffffff"
        val superfluousWrongTaggedTagged =
            "bf63616c670d64696e7473d39f181a1818ff696f626a546167676564d82bd82ad90540d90539bf63616c6713ff6e6f626a5461676765644172726179d9038f9fd90539bf63616c671903e8ffffff"
        val untaggedHexString =
            "bf63616c670d64696e7473d39f181a1818ff696f626a546167676564d82abf63616c6713ff6e6f626a5461676765644172726179d9038f9fbf63616c671903e8ffffff" //no ObjectTags
        val reference = NestedTagged(
            alg = 13,
            ints = intArrayOf(26, 24),
            objTagged = ClassAsTagged(19),
            objTaggedArray = listOf((ClassAsTagged(1000)))
        )
        val cbor = Cbor {
            encodeKeyTags = true
            verifyKeyTags = true
            verifyValueTags = true
            useDefiniteLengthEncoding = false
            verifyObjectTags = true
            encodeObjectTags = true
            encodeValueTags = true
        }
        assertEquals(referenceHexString, cbor.encodeToHexString(NestedTagged.serializer(), reference))
        assertEquals(reference, cbor.decodeFromHexString(NestedTagged.serializer(), referenceHexString))

        assertEquals(
            "More tags found than the 1 tags specified",
            assertFailsWith(CborDecodingException::class, message = "More tags found than the 1 tags specified") {
                cbor.decodeFromHexString(NestedTagged.serializer(), referenceHexStringWithBogusTag)
            }.message
        )

        assertEquals(
            "CBOR tags null do not match expected tags [19]",
            assertFailsWith(CborDecodingException::class, message = "CBOR tags null do not match expected tags [19]") {
                cbor.decodeFromHexString(NestedTagged.serializer(), referenceHexStringWithMissingTag)
            }.message
        )


        assertEquals(
            reference,
            Cbor {
                encodeKeyTags = true
                encodeValueTags = true
                encodeObjectTags = true
                verifyKeyTags = true
                verifyValueTags = true
                verifyObjectTags = false
            }.decodeFromHexString(NestedTagged.serializer(), referenceHexString)
        )

        assertEquals(
            reference,
            Cbor {
                encodeKeyTags = true
                encodeValueTags = true
                encodeObjectTags = true
                verifyKeyTags = true
                verifyValueTags = true
                verifyObjectTags = false
            }.decodeFromHexString(NestedTagged.serializer(), superfluousTagged)
        )

        assertEquals(
            untaggedHexString,
            Cbor {
                encodeKeyTags = true
                encodeValueTags = true
                verifyKeyTags = true
                verifyValueTags = true
                verifyObjectTags = true
                encodeObjectTags = false
            }.encodeToHexString(NestedTagged.serializer(), reference)
        )


        assertEquals(
            reference,
            Cbor {
                encodeKeyTags = true
                encodeValueTags = true
                encodeObjectTags = true
                verifyKeyTags = true
                verifyValueTags = true
                verifyObjectTags = false
            }.decodeFromHexString(NestedTagged.serializer(), untaggedHexString)
        )

        assertContains(
            assertFailsWith(CborDecodingException::class) {
                cbor.decodeFromHexString(NestedTagged.serializer(), untaggedHexString)
            }.message ?: "", "do not match expected tags"
        )

        assertContains(
            assertFailsWith(CborDecodingException::class) {
                Cbor {
                    encodeKeyTags = true
                    encodeValueTags = true
                    encodeObjectTags = true
                    verifyKeyTags = true
                    verifyValueTags = true
                    verifyObjectTags = false
                }.decodeFromHexString(
                    NestedTagged.serializer(),
                    superfluousWrongTaggedTagged
                )
            }.message ?: "", "do not start with specified tags"
        )


    }

    @ObjectTags(1337uL)
    @Serializable
    data class ClassAsTagged(
        @SerialName("alg")
        val alg: Int,
    )

    @Serializable
    data class NestedTagged(
        @SerialName("alg")
        val alg: Int,
        @ValueTags(19u)
        val ints: IntArray,

        @ValueTags(42u)
        val objTagged: ClassAsTagged,
        @ValueTags(911u)
        val objTaggedArray: List<ClassAsTagged>
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is NestedTagged) return false

            if (alg != other.alg) return false
            if (!ints.contentEquals(other.ints)) return false
            if (objTagged != other.objTagged) return false
            if (objTaggedArray != other.objTaggedArray) return false

            return true
        }

        override fun hashCode(): Int {
            var result = alg
            result = 31 * result + ints.contentHashCode()
            result = 31 * result + objTagged.hashCode()
            result = 31 * result + objTaggedArray.hashCode()
            return result
        }
    }


}