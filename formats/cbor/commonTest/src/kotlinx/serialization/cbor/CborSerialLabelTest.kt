package kotlinx.serialization.cbor

import kotlinx.serialization.*
import kotlin.test.*


class CborSerialLabelTest {

    private val reference = ClassWithSerialLabel(alg = -7)

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
    fun writeReadVerifySerialLabel() {
        val cbor = Cbor {
            preferSerialLabelsOverNames = true
        }
        assertEquals(referenceHexLabelString, cbor.encodeToHexString(ClassWithSerialLabel.serializer(), reference))
        assertEquals(reference, cbor.decodeFromHexString(ClassWithSerialLabel.serializer(), referenceHexLabelString))
    }

    @Test
    fun writeReadVerifySerialName() {
        val cbor = Cbor {
            preferSerialLabelsOverNames = false
        }
        assertEquals(referenceHexNameString, cbor.encodeToHexString(ClassWithSerialLabel.serializer(), reference))
        assertEquals(reference, cbor.decodeFromHexString(ClassWithSerialLabel.serializer(), referenceHexNameString))
    }

    @Test
    fun writeReadVerifySerialLabelAndUnknownKeys() {
        val referenceWithTag = ClassWithSerialLabel(alg = -7)

        /**
         * A2           # map(2)
         *    01        # unsigned(1)
         *    26        # negative(6)
         *    02        # unsigned(2)
         *    63        # text(3)
         *       62617A # "baz"
         */
        val referenceHexLabelWithTagString = "a20126026362617a"
        val cbor = Cbor {
            preferSerialLabelsOverNames = true
            ignoreUnknownKeys = true
        }
        assertEquals(
            referenceWithTag,
            cbor.decodeFromHexString(ClassWithSerialLabel.serializer(), referenceHexLabelWithTagString)
        )
    }

    @Serializable
    data class ClassWithSerialLabel(
        @SerialLabel(1)
        @SerialName("alg")
        val alg: Int
    )

}

