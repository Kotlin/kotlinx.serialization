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
        assertEquals(referenceHexString, Cbor.encodeToHexString(DataWithTags.serializer(), reference))
        assertEquals(
            referenceHexStringDefLen,
            Cbor { writeDefiniteLengths = true }.encodeToHexString(DataWithTags.serializer(), reference)
        )
        assertEquals(reference, Cbor.decodeFromHexString(DataWithTags.serializer(), referenceHexString))
        assertEquals(reference, Cbor.decodeFromHexString(DataWithTags.serializer(), referenceHexStringDefLen))
    }

    @Test
    fun writeReadUntaggedKeys() {
        assertEquals(noKeyTags, Cbor { writeKeyTags = false }.encodeToHexString(DataWithTags.serializer(), reference))
        assertEquals(
            noKeyTagsDefLen,
            Cbor { writeKeyTags = false;writeDefiniteLengths = true }.encodeToHexString(
                DataWithTags.serializer(),
                reference
            )
        )
        assertEquals(reference, Cbor { verifyKeyTags = false }.decodeFromHexString(noKeyTags))
        assertEquals(reference, Cbor { verifyKeyTags = false }.decodeFromHexString(noKeyTagsDefLen))
        assertEquals(reference, Cbor { verifyKeyTags = false }.decodeFromHexString(referenceHexString))
        assertFailsWith(CborDecodingException::class) { Cbor.decodeFromHexString(DataWithTags.serializer(), noKeyTags) }
        assertFailsWith(CborDecodingException::class) {
            Cbor {
                verifyKeyTags = false
            }.decodeFromHexString(DataWithTags.serializer(), noValueTags)
        }
    }

    @Test
    fun writeReadUntaggedValues() {
        assertEquals(
            noValueTags,
            Cbor { writeValueTags = false }.encodeToHexString(DataWithTags.serializer(), reference)
        )
        assertEquals(reference, Cbor { verifyValueTags = false }.decodeFromHexString(noValueTags))
        assertEquals(reference, Cbor { verifyValueTags = false }.decodeFromHexString(referenceHexString))

        assertFailsWith(CborDecodingException::class) {
            Cbor.decodeFromHexString(
                DataWithTags.serializer(),
                noValueTags
            )
        }

        assertFailsWith(CborDecodingException::class) {
            Cbor { verifyValueTags = false }.decodeFromHexString(
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
                writeValueTags = false
                writeKeyTags = false
            }.encodeToHexString(DataWithTags.serializer(), reference)
        )
        assertEquals(
            noTagsDefLen,
            Cbor {
                writeValueTags = false
                writeKeyTags = false
                writeDefiniteLengths = true
            }.encodeToHexString(DataWithTags.serializer(), reference)
        )

        assertEquals(reference, Cbor {
            verifyKeyTags = false
            verifyValueTags = false
        }.decodeFromHexString(noTags))

        assertEquals(reference, Cbor {
            verifyKeyTags = false
            verifyValueTags = false
        }.decodeFromHexString(noTagsDefLen))

        assertEquals(reference, Cbor {
            verifyKeyTags = false
            verifyValueTags = false
            writeDefiniteLengths = true
        }.decodeFromHexString(noTags))

        assertEquals(reference, Cbor {
            verifyKeyTags = false
            verifyValueTags = false
            writeDefiniteLengths = true
        }.decodeFromHexString(noTagsDefLen))

        assertEquals(reference, Cbor {
            verifyKeyTags = false
            verifyValueTags = false
        }.decodeFromHexString(noKeyTags))

        assertEquals(reference, Cbor {
            verifyKeyTags = false
            verifyValueTags = false
        }.decodeFromHexString(noValueTags))

        assertEquals(reference, Cbor {
            verifyKeyTags = false
            verifyValueTags = false
        }.decodeFromHexString(referenceHexString))

        assertFailsWith(CborDecodingException::class) {
            Cbor.decodeFromHexString(
                DataWithTags.serializer(),
                noTags
            )
        }

    }

    @Test
    fun wrongTags() {
        val wrongTag55ForPropertyC = "A46161CC1A0FFFFFFFD822616220D8376163D84E42CAFE6164D85ACC6B48656C6C6F20576F726C64"
        listOf(
            Cbor,
            Cbor { writeDefiniteLengths = true }).forEach { cbor ->

            assertFailsWith(CborDecodingException::class, message = "CBOR tags [55] do not match expected tags [56]") {
                cbor.decodeFromHexString(
                    DataWithTags.serializer(),
                    wrongTag55ForPropertyC
                )
            }
        }
        listOf(
            Cbor { verifyKeyTags = false; writeDefiniteLengths = true },
            Cbor { verifyKeyTags = false }).forEach { cbor ->
            assertEquals(reference, cbor.decodeFromHexString(wrongTag55ForPropertyC))
        }
    }
}