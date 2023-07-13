package kotlinx.serialization.cbor

import kotlinx.serialization.*
import kotlin.test.*


class CborSerialLabelTest {

    private val reference = ClassWithSerialLabel(alg = -7)

    /**
     * A1    # map(1)
     *    01 # unsigned(1)
     *    26 # negative(6)
     */
    private val referenceHexLabelString = "a10126"

    /**
     * A1           # map(1)
     *    63        # text(3)
     *       616C67 # "alg"
     *    26        # negative(6)
     */
    private val referenceHexNameString = "a163616c6726"


    @Test
    fun writeReadVerifySerialLabel() {
        val cbor = Cbor {
            preferSerialLabelsOverNames = true
            writeDefiniteLengths = true
        }
        assertEquals(referenceHexLabelString, cbor.encodeToHexString(ClassWithSerialLabel.serializer(), reference))
        assertEquals(reference, cbor.decodeFromHexString(ClassWithSerialLabel.serializer(), referenceHexLabelString))
    }

    @Test
    fun writeReadVerifySerialName() {
        val cbor = Cbor {
            preferSerialLabelsOverNames = false
            writeDefiniteLengths = true
        }
        assertEquals(referenceHexNameString, cbor.encodeToHexString(ClassWithSerialLabel.serializer(), reference))
        assertEquals(reference, cbor.decodeFromHexString(ClassWithSerialLabel.serializer(), referenceHexNameString))
    }


    @Serializable
    data class ClassWithSerialLabel(
        @SerialLabel(1)
        @SerialName("alg")
        val alg: Int
    )

}

