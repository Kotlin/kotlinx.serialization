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
    }

    @Test
    fun writeReadVerifySerialName() {
        val cbor = Cbor {
            preferCborLabelsOverNames = false
        }
        assertEquals(referenceHexNameString, cbor.encodeToHexString(ClassWithCborLabel.serializer(), reference))
        assertEquals(reference, cbor.decodeFromHexString(ClassWithCborLabel.serializer(), referenceHexNameString))
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
            writeKeyTags = true
            verifyKeyTags = true
            writeDefiniteLengths = true
        }
        assertEquals(referenceHexLabelWithTagString, cbor.encodeToHexString(ClassWithCborLabelAndTag.serializer(), referenceWithTag))
        assertEquals(referenceWithTag, cbor.decodeFromHexString(ClassWithCborLabelAndTag.serializer(), referenceHexLabelWithTagString))
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
            writeKeyTags = true
            verifyKeyTags = true
            writeDefiniteLengths = true
        }
        assertFailsWith(CborDecodingException::class) {
            cbor.decodeFromHexString(ClassWithCborLabelAndTag.serializer(), referenceHexLabelWithTagString)
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
            writeKeyTags = true
            verifyKeyTags = true
            ignoreUnknownKeys = true
            writeDefiniteLengths = true
        }
        assertEquals(referenceWithTag, cbor.decodeFromHexString(ClassWithCborLabelAndTag.serializer(), referenceHexLabelWithTagString))
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

}

