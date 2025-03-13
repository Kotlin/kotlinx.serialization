package kotlinx.serialization.cbor

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.HexConverter
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromHexString
import kotlinx.serialization.encodeToByteArray
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails

class CborArrayNullWithOptionalClassElement {

    /**
     * 82           # array(2)
     *    63        # text(3)
     *       666F6F # "foo"
     *    A0        # map(0) <- notice this is an empty map
     */
    private val withEmptyMap = "8263666F6FA0"


    /**
     * 82           # array(2)
     *    63        # text(3)
     *       666F6F # "foo"
     *    80        # array(0) <- notice this is an empty array
     */
    private val withEmptyArray = "8263666F6F80"


    /**
     * 82           # array(2)
     *    63        # text(3)
     *       666F6F # "foo"
     *    F6        # null <- notice this null
     */
    private val withNullElement = "8263666F6FF6"


    @Test
    fun withNullEncodesAndDecodes() {
        val cbor = Cbor {
            useDefiniteLengthEncoding = true
            treatNullComplexObjectsAsNull = true
        }

        val structureWithNull = SessionTranscript(
            someValue = "foo",
            nested = null
        )

        val decodedStructureWithNull =
            cbor.decodeFromHexString<SessionTranscript>(withNullElement)
        // currently functions and this assert passes
        assertEquals(structureWithNull, decodedStructureWithNull)

        val encodedStructureWithNull =
            cbor.encodeToByteArray<SessionTranscript>(structureWithNull)
        // currently, this assert fails - as the last byte is 0xA0 rather than 0xF6
        assertContentEquals(HexConverter.parseHexBinary(withNullElement), encodedStructureWithNull)
    }

    @Test
    fun withEmptyMap() {
        val cbor = Cbor {
            useDefiniteLengthEncoding = true
            treatNullComplexObjectsAsNull = true
        }

        assertFails {
            cbor.decodeFromHexString<SessionTranscript>(withEmptyMap)
        }
    }

    @Test
    fun withEmptyArray() {
        val cbor = Cbor {
            useDefiniteLengthEncoding = true
            treatNullComplexObjectsAsNull = true
        }

        assertFails {
            cbor.decodeFromHexString<SessionTranscript>(withEmptyArray)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @CborArray
    @Serializable
    data class SessionTranscript(
        val someValue: String,
        val nested: OtherKind?
    )

    @OptIn(ExperimentalSerializationApi::class)
    @CborArray
    @Serializable
    data class OtherKind(
        val otherValue: String,
    )
}