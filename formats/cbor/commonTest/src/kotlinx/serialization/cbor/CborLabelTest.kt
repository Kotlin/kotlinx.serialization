package kotlinx.serialization.cbor

import kotlinx.serialization.*
import kotlinx.serialization.cbor.internal.CborDecodingException
import kotlin.test.*


class CborLabelTest {

    private val reference = ClassWithCborLabel(alg = -7)


    /**
     * BF    # map(*)
     *    01 # unsigned(1)
     *    26 # negative(6)
     *    FF # primitive(*)
     */
    private val referenceHexLabelString = "bf0126ff"

    /**
     * BF           # map(*)
     *    63        # text(3)
     *       616C67 # "alg"
     *    26        # negative(6)
     *    FF        # primitive(*)
     */
    private val referenceHexNameString = "bf63616c6726ff"


    @Test
    fun writeReadVerifyCborLabel() {
        val cbor = Cbor {
            preferCborLabelsOverNames = true
        }
        assertEquals(referenceHexLabelString, cbor.encodeToHexString(ClassWithCborLabel.serializer(), reference))
        assertEquals(reference, cbor.decodeFromHexString(ClassWithCborLabel.serializer(), referenceHexLabelString))

        val struct = cbor.encodeToCborElement(ClassWithCborLabel.serializer(), reference)
        assertEquals(reference, cbor.decodeFromCborElement(ClassWithCborLabel.serializer(), struct))
        assertEquals(referenceHexLabelString, cbor.encodeToHexString(CborElement.serializer(), struct))
    }

    @Test
    fun writeReadVerifySerialName() {
        val cbor = Cbor {
            preferCborLabelsOverNames = false
        }
        assertEquals(referenceHexNameString, cbor.encodeToHexString(ClassWithCborLabel.serializer(), reference))
        assertEquals(reference, cbor.decodeFromHexString(ClassWithCborLabel.serializer(), referenceHexNameString))


        val struct = cbor.encodeToCborElement(ClassWithCborLabel.serializer(), reference)
        assertEquals(reference, cbor.decodeFromCborElement(ClassWithCborLabel.serializer(), struct))
        assertEquals(referenceHexNameString, cbor.encodeToHexString(CborElement.serializer(), struct))
    }

    @Test
    fun writeReadVerifyCborLabelWithTags() {
        val referenceWithTag = ClassWithCborLabelAndTag(alg = -7)
        /**
         * A1       # map(1)
         *    C5    # tag(5)
         *       01 # unsigned(1)
         *    26    # negative(6)
         */
        val referenceHexLabelWithTagString = "a1c50126"
        val cbor = Cbor {
            preferCborLabelsOverNames = true
            encodeKeyTags = true
            verifyKeyTags = true
            useDefiniteLengthEncoding = true
        }
        assertEquals(referenceHexLabelWithTagString, cbor.encodeToHexString(ClassWithCborLabelAndTag.serializer(), referenceWithTag))
        assertEquals(referenceWithTag, cbor.decodeFromHexString(ClassWithCborLabelAndTag.serializer(), referenceHexLabelWithTagString))

        val struct = cbor.encodeToCborElement(ClassWithCborLabelAndTag.serializer(), referenceWithTag)
        assertEquals(referenceWithTag, cbor.decodeFromCborElement(ClassWithCborLabelAndTag.serializer(), struct))
        assertEquals(referenceHexLabelWithTagString, cbor.encodeToHexString(CborElement.serializer(), struct))
    }

    @Test
    fun writeReadVerifyCborLabelWithTagsThrowing() {
        /**
         * A1       # map(1)
         *    C6    # tag(6)        // wrong tag: declared is 5U, meaning C5 in hex
         *       01 # unsigned(1)
         *    26    # negative(6)
         */
        val referenceHexLabelWithTagString = "a1c60126"
        val cbor = Cbor {
            preferCborLabelsOverNames = true
            encodeKeyTags = true
            verifyKeyTags = true
            useDefiniteLengthEncoding = true
        }
        assertFailsWith(CborDecodingException::class) {
            cbor.decodeFromHexString(ClassWithCborLabelAndTag.serializer(), referenceHexLabelWithTagString)
        }

        //we can encode and decode since it is a valid structure
        val struct = cbor.decodeFromHexString(CborElement.serializer(), referenceHexLabelWithTagString)
        assertEquals(referenceHexLabelWithTagString, cbor.encodeToHexString(CborElement.serializer(), struct))

        //we cannot deserialize from the struct since it does not match the class structure
        assertFailsWith(CborDecodingException::class) {
            cbor.decodeFromCborElement(ClassWithCborLabelAndTag.serializer(), struct)
        }
    }

    @Test
    fun writeReadVerifyCborLabelWithTagsAndUnknownKeys() {
        val referenceWithTag = ClassWithCborLabelAndTag(alg = -7)
        /**
         * A2           # map(2)
         *    C5        # tag(5)
         *       01     # unsigned(1)
         *    26        # negative(6)
         *    02        # unsigned(2)
         *    63        # text(3)
         *       62617A # "baz"
         */
        val referenceHexLabelWithTagString = "a2c50126026362617a"
        val cbor = Cbor {
            preferCborLabelsOverNames = true
            encodeKeyTags = true
            verifyKeyTags = true
            ignoreUnknownKeys = true
            useDefiniteLengthEncoding = true
        }
        assertEquals(referenceWithTag, cbor.decodeFromHexString(ClassWithCborLabelAndTag.serializer(), referenceHexLabelWithTagString))

        //no unknown props
        val struct = cbor.encodeToCborElement(ClassWithCborLabelAndTag.serializer(), referenceWithTag)

        //with unknown props
        val structFromString = cbor.decodeFromHexString(CborElement.serializer(), referenceHexLabelWithTagString)
        //must obv mismatch
        assertNotEquals(struct, structFromString)
        assertNotEquals(referenceHexLabelWithTagString, cbor.encodeToHexString(CborElement.serializer(), struct))

        assertEquals(referenceWithTag, cbor.decodeFromCborElement(ClassWithCborLabelAndTag.serializer(), struct))
        assertEquals(referenceHexLabelWithTagString, cbor.encodeToHexString(CborElement.serializer(), structFromString))
    }

    @Test
    fun writeClassWithoutLabelBuPreferLabel() {

        //only serialName is present, no label, so fallback to serialName
        val referenceWithoutLabel = ClassWithoutCborLabel(algorithm = 9)
        /**
         * BF           # map(*)
         *    63        # text(3)
         *       616C67 # "alg"
         *    09        # unsigned(9)
         *    FF        # primitive(*)
         */

        val referenceHexStringWithoutLabel = "bf63616c6709ff"
        val cbor = Cbor {
            preferCborLabelsOverNames = true
        }

        assertEquals(referenceWithoutLabel, cbor.decodeFromHexString(ClassWithoutCborLabel.serializer(), referenceHexStringWithoutLabel))

        val struct = cbor.encodeToCborElement(ClassWithoutCborLabel.serializer(), referenceWithoutLabel)
        assertEquals(referenceWithoutLabel, cbor.decodeFromCborElement(ClassWithoutCborLabel.serializer(), struct))
        assertEquals(referenceHexStringWithoutLabel, cbor.encodeToHexString(CborElement.serializer(), struct))

    }

    @Serializable
    data class ClassWithCborLabel(
        @CborLabel(1)
        @SerialName("alg")
        val alg: Int
    )

    @Serializable
    data class ClassWithCborLabelAndTag(
        @CborLabel(1)
        @SerialName("alg")
        @KeyTags(5U)
        val alg: Int
    )

    @Serializable
    data class ClassWithoutCborLabel(
        @SerialName("alg")
        val algorithm: Int
    )

}

