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
        val cbor = Cbor {
            useDefiniteLengthEncoding = false
            encodeKeyTags = true
            encodeValueTags = true
            encodeObjectTags = true
            verifyKeyTags = true
            verifyValueTags = true
            verifyObjectTags = true
        }
        assertEquals(referenceHexString, cbor.encodeToHexString(DataWithTags.serializer(), reference))
        val structFromHex = cbor.decodeFromHexString(CborElement.serializer(), referenceHexString)
        val struct = cbor.encodeToCborElement(DataWithTags.serializer(), reference)
        assertEquals(struct, structFromHex)
        assertEquals(reference, cbor.decodeFromCborElement(DataWithTags.serializer(), struct))
        assertEquals(referenceHexString, cbor.encodeToHexString(CborElement.serializer(), struct))

        val cborDef = Cbor {
            useDefiniteLengthEncoding = true
            encodeKeyTags = true
            encodeValueTags = true
            encodeObjectTags = true
            verifyKeyTags = true
            verifyValueTags = true
            verifyObjectTags = true
        }
        assertEquals(referenceHexStringDefLen, cborDef.encodeToHexString(DataWithTags.serializer(), reference))
        val structDefFromHex = cborDef.decodeFromHexString(CborElement.serializer(), referenceHexStringDefLen)
        val structDef = cborDef.encodeToCborElement(DataWithTags.serializer(), reference)
        assertEquals(structDef, structDefFromHex)
        assertEquals(reference, cborDef.decodeFromCborElement(DataWithTags.serializer(), structDef))
        assertEquals(referenceHexStringDefLen, cborDef.encodeToHexString(CborElement.serializer(), structDef))


        assertEquals(reference, Cbor.CoseCompliant.decodeFromHexString(DataWithTags.serializer(), referenceHexString))
        val structCoseFromHex = Cbor.CoseCompliant.decodeFromHexString(CborElement.serializer(), referenceHexString)
        val structCose = Cbor.CoseCompliant.encodeToCborElement(DataWithTags.serializer(), reference)
        assertEquals(structCose, structCoseFromHex)
        assertEquals(reference, Cbor.CoseCompliant.decodeFromCborElement(DataWithTags.serializer(), structCose))

        assertEquals(
            reference,
            Cbor.CoseCompliant.decodeFromHexString(DataWithTags.serializer(), referenceHexStringDefLen)
        )
        val structCoseFromHexDef =
            Cbor.CoseCompliant.decodeFromHexString(CborElement.serializer(), referenceHexStringDefLen)
        val structCoseDef = Cbor.CoseCompliant.encodeToCborElement(DataWithTags.serializer(), reference)
        assertEquals(structCoseDef, structCoseFromHexDef)
        assertEquals(reference, Cbor.CoseCompliant.decodeFromCborElement(DataWithTags.serializer(), structCoseDef))

    }

    @Test
    fun writeReadUntaggedKeys() {
        val cborNoKeyTags = Cbor {
            encodeKeyTags = false
            encodeValueTags = true
            encodeObjectTags = true
            verifyKeyTags = false
            verifyValueTags = true
            verifyObjectTags = true
        }
        assertEquals(noKeyTags, cborNoKeyTags.encodeToHexString(DataWithTags.serializer(), reference))
        (cborNoKeyTags to noKeyTags).let { (cbor, hex) ->
            val struct = cbor.encodeToCborElement(DataWithTags.serializer(), reference)
            assertEquals(struct, cbor.decodeFromHexString(CborElement.serializer(), hex))
            assertEquals(reference, cbor.decodeFromCborElement(DataWithTags.serializer(), struct))
        }

        val cborNoKeyTagsDefLen = Cbor {
            useDefiniteLengthEncoding = true
            encodeKeyTags = false
            encodeValueTags = true
            encodeObjectTags = true
            verifyKeyTags = true
            verifyValueTags = true
            verifyObjectTags = true
        }
        assertEquals(noKeyTagsDefLen, cborNoKeyTagsDefLen.encodeToHexString(DataWithTags.serializer(), reference))
        (cborNoKeyTagsDefLen to noKeyTagsDefLen).let { (cbor, hex) ->
            val struct = cbor.encodeToCborElement(DataWithTags.serializer(), reference)
            assertEquals(struct, cbor.decodeFromHexString(CborElement.serializer(), hex))
            // this must fail, because encoding/decoding is not symmetric with the current config (the struct does not have the tags, but the hex string does)
            assertFailsWith(CborDecodingException::class) {
                assertEquals(reference, cbor.decodeFromCborElement(DataWithTags.serializer(), struct))
            }
        }

        val cborEncodingKeyTags = Cbor {
            encodeKeyTags = true
            encodeValueTags = true
            encodeObjectTags = true
            verifyValueTags = true
            verifyObjectTags = true
            verifyKeyTags = false
        }
        assertEquals(reference, cborEncodingKeyTags.decodeFromHexString(noKeyTags))
        (cborEncodingKeyTags to noKeyTags).let { (cbor, hex) ->
            val struct = cbor.encodeToCborElement(DataWithTags.serializer(), reference)
            // this must not be equal, because the scruct has the tags, but the hex string doesn't
            assertNotEquals(struct, cbor.decodeFromHexString(CborElement.serializer(), hex))
            assertEquals(reference, cbor.decodeFromCborElement(DataWithTags.serializer(), struct))
        }

        assertEquals(reference, cborEncodingKeyTags.decodeFromHexString(noKeyTagsDefLen))
        (cborEncodingKeyTags to noKeyTagsDefLen).let { (cbor, hex) ->
            val struct = cbor.encodeToCborElement(DataWithTags.serializer(), reference)
            // this must not be equals, because the scruct has the tags, but the hex string doesn't (as above)
            assertNotEquals(struct, cbor.decodeFromHexString(CborElement.serializer(), hex))
            assertEquals(reference, cbor.decodeFromCborElement(DataWithTags.serializer(), struct))
        }


        assertEquals(reference, cborEncodingKeyTags.decodeFromHexString(referenceHexString))
        (cborNoKeyTags to noKeyTags).let { (cbor, hex) ->
            val struct = cbor.encodeToCborElement(DataWithTags.serializer(), reference)
            assertEquals(struct, cbor.decodeFromHexString(CborElement.serializer(), hex))
            assertEquals(reference, cbor.decodeFromCborElement(DataWithTags.serializer(), struct))
        }

        assertFailsWith(CborDecodingException::class) {
            //Tested against struct inside one of the above let-blocks
            Cbor.CoseCompliant.decodeFromHexString(
                DataWithTags.serializer(),
                noKeyTags
            )
        }

        assertFailsWith(CborDecodingException::class) {
            //Tested against struct inside one of the above let-blocks
            cborEncodingKeyTags.decodeFromHexString(DataWithTags.serializer(), noValueTags)
        }
    }

    @Test
    fun writeReadUntaggedValues() {
        val cborNoValueTags = Cbor {
            encodeKeyTags = true
            encodeObjectTags = true
            verifyKeyTags = true
            verifyValueTags = true
            verifyObjectTags = true
            encodeValueTags = false
        }
        assertEquals(noValueTags, cborNoValueTags.encodeToHexString(DataWithTags.serializer(), reference))
        (cborNoValueTags to noValueTags).let { (cbor, hex) ->
            val struct = cbor.encodeToCborElement(DataWithTags.serializer(), reference)
            assertEquals(struct, cbor.decodeFromHexString(CborElement.serializer(), hex))
            // no value tags are written to the struct, so this will fail
            assertFailsWith(CborDecodingException::class) {
                assertEquals(reference, cbor.decodeFromCborElement(DataWithTags.serializer(), struct))
            }
        }


        val cborEncodingValueTags = Cbor {
            encodeKeyTags = true
            encodeValueTags = true
            encodeObjectTags = true
            verifyKeyTags = true
            verifyObjectTags = true
            verifyValueTags = false
        }
        assertEquals(reference, cborEncodingValueTags.decodeFromHexString(noValueTags))
        (cborEncodingValueTags to noValueTags).let { (cbor, hex) ->
            val struct = cbor.encodeToCborElement(DataWithTags.serializer(), reference)
            //hex is missing the tags, struct has them from the serializer
            assertNotEquals(struct, cbor.decodeFromHexString(CborElement.serializer(), hex))
            assertEquals(reference, cbor.decodeFromCborElement(DataWithTags.serializer(), struct))
        }


        assertEquals(reference, cborEncodingValueTags.decodeFromHexString(referenceHexString))
        (cborEncodingValueTags to referenceHexString).let { (cbor, hex) ->
            val struct = cbor.encodeToCborElement(DataWithTags.serializer(), reference)
            assertEquals(struct, cbor.decodeFromHexString(CborElement.serializer(), hex))
            assertEquals(reference, cbor.decodeFromCborElement(DataWithTags.serializer(), struct))
        }

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
            //Struct stuff has been tested in the above let blocks already
        }

        assertFailsWith(CborDecodingException::class) {
            cborEncodingValueTags.decodeFromHexString(
                DataWithTags.serializer(),
                noKeyTags
            )
            //Struct stuff has been tested already
        }

    }

    @Test
    fun writeReadUntaggedEverything() {
        val cborNoTags = Cbor {
            encodeObjectTags = true
            verifyKeyTags = true
            verifyValueTags = true
            verifyObjectTags = true
            encodeValueTags = false
            encodeKeyTags = false
        }
        assertEquals(noTags, cborNoTags.encodeToHexString(DataWithTags.serializer(), reference))
        (cborNoTags to noTags).let { (cbor, hex) ->
            val struct = cbor.encodeToCborElement(DataWithTags.serializer(), reference)
            assertEquals(struct, cbor.decodeFromHexString(CborElement.serializer(), hex))
            //struct is missing the tags
            assertFailsWith(CborDecodingException::class) {
                assertEquals(reference, cbor.decodeFromCborElement(DataWithTags.serializer(), struct))
            }
        }

        val cborNoTagsDefLen = Cbor {
            encodeObjectTags = true
            verifyKeyTags = true
            verifyValueTags = true
            verifyObjectTags = true
            encodeValueTags = false
            encodeKeyTags = false
            useDefiniteLengthEncoding = true
        }
        assertEquals(noTagsDefLen, cborNoTagsDefLen.encodeToHexString(DataWithTags.serializer(), reference))
        (cborNoTagsDefLen to noTagsDefLen).let { (cbor, hex) ->
            val struct = cbor.encodeToCborElement(DataWithTags.serializer(), reference)
            assertEquals(struct, cbor.decodeFromHexString(CborElement.serializer(), hex))
            //struct is missing the tags
            assertFailsWith(CborDecodingException::class) {
                assertEquals(reference, cbor.decodeFromCborElement(DataWithTags.serializer(), struct))
            }
        }

        val cborEncodingAllVerifyingObjectTags = Cbor {
            encodeKeyTags = true
            encodeValueTags = true
            encodeObjectTags = true
            verifyObjectTags = true
            verifyKeyTags = false
            verifyValueTags = false
        }
        assertEquals(reference, cborEncodingAllVerifyingObjectTags.decodeFromHexString(noTags))
        (cborEncodingAllVerifyingObjectTags to noTags).let { (cbor, hex) ->
            val struct = cbor.encodeToCborElement(DataWithTags.serializer(), reference)
            //hex has not tags but the current config encodes them into the struct
            assertNotEquals(struct, cbor.decodeFromHexString(CborElement.serializer(), hex))
            assertEquals(reference, cbor.decodeFromCborElement(DataWithTags.serializer(), struct))
        }

        assertEquals(reference, cborEncodingAllVerifyingObjectTags.decodeFromHexString(noTagsDefLen))
        (cborEncodingAllVerifyingObjectTags to noTagsDefLen).let { (cbor, hex) ->
            val struct = cbor.encodeToCborElement(DataWithTags.serializer(), reference)
            //hex has not tags but the current config encodes them into the struct
            assertNotEquals(struct, cbor.decodeFromHexString(CborElement.serializer(), hex))
            assertEquals(reference, cbor.decodeFromCborElement(DataWithTags.serializer(), struct))
        }


        val cborEncodingAllDefLen = Cbor {
            encodeKeyTags = true
            encodeValueTags = true
            encodeObjectTags = true
            verifyObjectTags = true
            verifyKeyTags = false
            verifyValueTags = false
            useDefiniteLengthEncoding = true
        }
        assertEquals(reference, cborEncodingAllDefLen.decodeFromHexString(noTags))
        (cborEncodingAllDefLen to noTags).let { (cbor, hex) ->
            val struct = cbor.encodeToCborElement(DataWithTags.serializer(), reference)
            //hex has not tags but the current config encodes them into the struct
            assertNotEquals(struct, cbor.decodeFromHexString(CborElement.serializer(), hex))
            assertEquals(reference, cbor.decodeFromCborElement(DataWithTags.serializer(), struct))
        }

        assertEquals(reference, cborEncodingAllDefLen.decodeFromHexString(noTagsDefLen))
        (cborEncodingAllDefLen to noTagsDefLen).let { (cbor, hex) ->
            val struct = cbor.encodeToCborElement(DataWithTags.serializer(), reference)
            //hex has not tags but the current config encodes them into the struct
            assertNotEquals(struct, cbor.decodeFromHexString(CborElement.serializer(), hex))
            assertEquals(reference, cbor.decodeFromCborElement(DataWithTags.serializer(), struct))
        }

        assertEquals(reference, cborEncodingAllVerifyingObjectTags.decodeFromHexString(noKeyTags))
        (cborEncodingAllVerifyingObjectTags to noKeyTags).let { (cbor, hex) ->
            val struct = cbor.encodeToCborElement(DataWithTags.serializer(), reference)
            //hex has not tags but the current config encodes them into the struct
            assertNotEquals(struct, cbor.decodeFromHexString(CborElement.serializer(), hex))
            assertEquals(reference, cbor.decodeFromCborElement(DataWithTags.serializer(), struct))
        }

        assertEquals(reference, cborEncodingAllVerifyingObjectTags.decodeFromHexString(noValueTags))
        (cborEncodingAllVerifyingObjectTags to noValueTags).let { (cbor, hex) ->
            val struct = cbor.encodeToCborElement(DataWithTags.serializer(), reference)
            //hex has not tags but the current config encodes them into the struct
            assertNotEquals(struct, cbor.decodeFromHexString(CborElement.serializer(), hex))
            assertEquals(reference, cbor.decodeFromCborElement(DataWithTags.serializer(), struct))
        }

        assertEquals(reference, cborEncodingAllVerifyingObjectTags.decodeFromHexString(referenceHexString))
        (cborEncodingAllVerifyingObjectTags to referenceHexString).let { (cbor, hex) ->
            val struct = cbor.encodeToCborElement(DataWithTags.serializer(), reference)
            assertEquals(struct, cbor.decodeFromHexString(CborElement.serializer(), hex))
            assertEquals(reference, cbor.decodeFromCborElement(DataWithTags.serializer(), struct))
        }

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
            //the struct stuff is already tested before
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

            assertContains(
                assertFailsWith(
                    CborDecodingException::class,
                    message = "CBOR tags [55] do not match declared tags [56]"
                ) {
                    cbor.decodeFromCborElement(
                        DataWithTags.serializer(), cbor.decodeFromHexString(
                            CborElement.serializer(),
                            wrongTag55ForPropertyC
                        )
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
            assertEquals(reference, cbor.decodeFromCborElement(cbor.decodeFromHexString(wrongTag55ForPropertyC)))
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

        (cbor to referenceHexString).let { (cbor, hexString) ->
            val struct = cbor.encodeToCborElement(reference)
            assertEquals(hexString, cbor.encodeToHexString(CborElement.serializer(), struct))
            assertEquals(cbor.decodeFromHexString<CborElement>(hexString), struct)
            assertEquals(reference, cbor.decodeFromCborElement(ClassAsTagged.serializer(), struct))
        }


        val cborNoVerifyObjTags = Cbor { verifyObjectTags = false }
        assertEquals(
            reference,
            cborNoVerifyObjTags.decodeFromHexString(ClassAsTagged.serializer(), referenceHexString)
        )
        (cborNoVerifyObjTags to referenceHexString).let { (cbor, hexString) ->
            val struct = cbor.encodeToCborElement(reference)
            // NEQ: the ref string has object tags, but here we don't encode them
            assertNotEquals(hexString, cbor.encodeToHexString(CborElement.serializer(), struct))
            // NEW, the hex string has the tags, so they are decoded, but the struct, created without object tags does not
            assertNotEquals(cbor.decodeFromHexString<CborElement>(hexString), struct)
            assertEquals(reference, cbor.decodeFromCborElement(ClassAsTagged.serializer(), struct))
        }

        val cborNoEncodeObjTags = Cbor { encodeObjectTags = false }
        assertEquals(
            untaggedHexString,
            cborNoEncodeObjTags.encodeToHexString(ClassAsTagged.serializer(), reference)
        )
        (cborNoEncodeObjTags to untaggedHexString).let { (cbor, hexString) ->
            val struct = cbor.encodeToCborElement(reference)
            assertEquals(hexString, cbor.encodeToHexString(CborElement.serializer(), struct))
            assertEquals(cbor.decodeFromHexString<CborElement>(hexString), struct)
            assertEquals(reference, cbor.decodeFromCborElement(ClassAsTagged.serializer(), struct))
        }


        assertEquals(
            reference,
            cborNoVerifyObjTags.decodeFromHexString(ClassAsTagged.serializer(), untaggedHexString)
        )
        (cborNoVerifyObjTags to untaggedHexString).let { (cbor, hexString) ->
            val struct = cbor.encodeToCborElement(reference)
            assertEquals(hexString, cbor.encodeToHexString(CborElement.serializer(), struct))
            assertEquals(cbor.decodeFromHexString<CborElement>(hexString), struct)
            assertEquals(reference, cbor.decodeFromCborElement(ClassAsTagged.serializer(), struct))
        }

        assertContains(
            assertFailsWith(CborDecodingException::class) {
                cbor.decodeFromHexString(ClassAsTagged.serializer(), untaggedHexString)
            }.message ?: "", "do not match expected tags"
        )
        assertContains(
            assertFailsWith(CborDecodingException::class) {
                cbor.decodeFromCborElement(
                    ClassAsTagged.serializer(),
                    cbor.decodeFromHexString(CborElement.serializer(), untaggedHexString)
                )
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
        assertEquals(
            "81d90539a163616c6713",
            Cbor.CoseCompliant.encodeToHexString(Cbor.CoseCompliant.encodeToCborElement(listOfObjectTagged))
        )


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
        (cbor to referenceHexString).let { (cbor, hex) ->
            val struct = cbor.encodeToCborElement(reference)
            assertEquals(struct, cbor.decodeFromHexString<CborElement>(hex))
            assertEquals(reference, cbor.decodeFromCborElement(struct))
        }


        assertEquals(
            "More tags found than the 1 tags specified",
            assertFailsWith(CborDecodingException::class, message = "More tags found than the 1 tags specified") {
                cbor.decodeFromHexString(NestedTagged.serializer(), referenceHexStringWithBogusTag)
            }.message
        )
        assertEquals(
            "CBOR tags [19, 20] do not match expected tags [19]",
            assertFailsWith(
                CborDecodingException::class,
                message = "CBOR tags [19, 20] do not match expected tags [19]"
            ) {
                cbor.decodeFromCborElement(
                    NestedTagged.serializer(),
                    cbor.decodeFromHexString(CborElement.serializer(), referenceHexStringWithBogusTag)
                )
            }.message
        )

        assertEquals(
            "CBOR tags null do not match expected tags [19]",
            assertFailsWith(CborDecodingException::class, message = "CBOR tags null do not match expected tags [19]") {
                cbor.decodeFromHexString(NestedTagged.serializer(), referenceHexStringWithMissingTag)
            }.message
        )
        assertEquals(
            "CBOR tags null do not match expected tags [19]",
            assertFailsWith(CborDecodingException::class, message = "CBOR tags null do not match expected tags [19]") {
                cbor.decodeFromCborElement(
                    NestedTagged.serializer(),
                    cbor.decodeFromHexString(CborElement.serializer(), referenceHexStringWithMissingTag)
                )
            }.message
        )


        val cborNoVerifying = Cbor {
            encodeKeyTags = true
            encodeValueTags = true
            encodeObjectTags = true
            verifyKeyTags = true
            verifyValueTags = true
            verifyObjectTags = false
        }
        assertEquals(
            reference,
            cborNoVerifying.decodeFromHexString(NestedTagged.serializer(), referenceHexString)
        )
        (cborNoVerifying to referenceHexString).let { (cbor, hex) ->
            val struct = cbor.encodeToCborElement(reference)
            assertEquals(struct, cbor.decodeFromHexString<CborElement>(hex))
            assertEquals(reference, cbor.decodeFromCborElement(struct))
        }

        assertEquals(
            reference,
            cborNoVerifying.decodeFromHexString(NestedTagged.serializer(), superfluousTagged)
        )
        (cborNoVerifying to superfluousTagged).let { (cbor, hex) ->
            val struct = cbor.encodeToCborElement(reference)
            //there are more tags in the string
            assertNotEquals(struct, cbor.decodeFromHexString<CborElement>(hex))
            assertEquals(reference, cbor.decodeFromCborElement(struct))
        }

        val cborNoEncode = Cbor {
            encodeKeyTags = true
            encodeValueTags = true
            verifyKeyTags = true
            verifyValueTags = true
            verifyObjectTags = false
            encodeObjectTags = false
        }
        assertEquals(
            untaggedHexString,
            cborNoEncode.encodeToHexString(NestedTagged.serializer(), reference)
        )
        (cborNoEncode to untaggedHexString).let { (cbor, hex) ->
            val struct = cbor.encodeToCborElement(reference)
            assertEquals(struct, cbor.decodeFromHexString<CborElement>(hex))
            assertEquals(reference, cbor.decodeFromCborElement(struct))
        }


        assertEquals(
            reference,
            cborNoVerifying.decodeFromHexString(NestedTagged.serializer(), untaggedHexString)
        )
        (cborNoVerifying to untaggedHexString).let { (cbor, hex) ->
            val struct = cbor.encodeToCborElement(reference)
            //NEQ: decoding from an untagged string means no tags coming in while encoding path above creates those tags
            assertNotEquals(struct, cbor.decodeFromHexString<CborElement>(hex))
            assertEquals(reference, cbor.decodeFromCborElement(struct))
        }

        assertContains(
            assertFailsWith(CborDecodingException::class) {
                cbor.decodeFromHexString(NestedTagged.serializer(), untaggedHexString)
            }.message ?: "", "do not match expected tags"
        )

        assertContains(
            assertFailsWith(CborDecodingException::class) {
                cbor.decodeFromCborElement(
                    NestedTagged.serializer(),
                    cbor.decodeFromHexString(CborElement.serializer(), untaggedHexString)
                )
            }.message ?: "", "do not match expected tags"
        )

        assertContains(
            assertFailsWith(CborDecodingException::class) {
                cborNoVerifying.decodeFromHexString(
                    NestedTagged.serializer(),
                    superfluousWrongTaggedTagged
                )
            }.message ?: "", "do not start with specified tags"
        )
    }

    // See https://www.rfc-editor.org/rfc/rfc8949.html#name-self-described-cbor
    @Test
    fun testSelfDescribedCborParsing() {
        val selfDescribedCborTag = "d9d9f7"
        @Serializable
        data class Datum(val l: String, val v: Int)

        val original = Datum("Kotlin", 2)
        val serialized = selfDescribedCborTag + Cbor.encodeToHexString(original)
        val deserialized = Cbor.decodeFromHexString<Datum>(serialized)
        assertEquals(original, deserialized)
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
