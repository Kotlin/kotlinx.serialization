package kotlinx.serialization.cbor

import kotlinx.serialization.*
import kotlin.test.*

@Serializable
data class CoseHeader(
    @SerialLabel(4)
    @SerialName("kid")
    val kid: String? = null,
)

class CborSerialLabelTest {

    private val reference = CoseHeader(
        kid = "11"
    )

    /*
        BF         # map(*)
           04      # unsigned(4)
           62      # text(2)
              3131 # "11"
           FF      # primitive(*)
     */
    private val referenceHexString = "bf04623131ff"


    @Test
    fun writeReadVerifyCoseHeader() {
        assertEquals(referenceHexString, Cbor.encodeToHexString(CoseHeader.serializer(), reference))
        assertEquals(reference, Cbor.decodeFromHexString(CoseHeader.serializer(), referenceHexString))
    }
}