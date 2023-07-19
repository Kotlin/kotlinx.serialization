package kotlinx.serialization.cbor

import kotlinx.serialization.*
import kotlin.test.*

class CborIsoTest {

    private val reference = DataClass(
        bytes = "foo".encodeToByteArray()
    )

    /**
     * A1               # map(1)
     *    65            # text(5)
     *       6279746573 # "bytes"
     *    43            # bytes(3)
     *       666F6F     # "foo"
     *
     */
    private val referenceHexString = "a165627974657343666f6f"

    @Test
    fun writeReadVerifyCoseSigned() {
        val cbor = Cbor {
            alwaysUseByteString = true
            writeDefiniteLengths = true
        }
        assertEquals(reference, cbor.decodeFromHexString(DataClass.serializer(), referenceHexString))
        assertEquals(referenceHexString, cbor.encodeToHexString(DataClass.serializer(), reference))
    }

    @Serializable
    data class DataClass(
        @SerialName("bytes")
        val bytes: ByteArray,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as DataClass

            return bytes.contentEquals(other.bytes)
        }

        override fun hashCode(): Int {
            return bytes.contentHashCode()
        }
    }
}