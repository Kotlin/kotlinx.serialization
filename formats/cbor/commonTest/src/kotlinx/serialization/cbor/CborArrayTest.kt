package kotlinx.serialization.cbor

import kotlinx.serialization.*
import kotlin.test.*


class CborArrayTest {

    private val reference1 = ClassAs1Array(alg = -7)
    private val reference2 = ClassAs2Array(alg = -7, kid = "foo")
    private val reference3 = ClassWithArray(array = ClassAs2Array(alg = -7, kid = "bar"))

    /**
     * 81    # array(1)
     *    26 # negative(6)
     */
    private val reference1HexString = "8126"

    /**
     * C8              # tag(8)
     *    82           # array(2)
     *       26        # negative(6)
     *       63        # text(3)
     *          666F6F # "foo"
     */
    private val reference2HexString = "c8822663666f6f"

    /**
     * A1                 # map(1)
     *    65              # text(5)
     *       6172726179   # "array"
     *    C8              # tag(8)
     *       82           # array(2)
     *          26        # negative(6)
     *          63        # text(3)
     *             626172 # "bar"
     */
    private val reference3HexString = "a1656172726179c8822663626172"

    @Test
    fun writeReadVerifyArraySize1() {
        val cbor = Cbor {
            writeDefiniteLengths = true
        }
        assertEquals(reference1HexString, cbor.encodeToHexString(ClassAs1Array.serializer(), reference1))
        assertEquals(reference1, cbor.decodeFromHexString(ClassAs1Array.serializer(), reference1HexString))
    }

    @Test
    fun writeReadVerifyArraySize2() {
        val cbor = Cbor {
            writeDefiniteLengths = true
        }
        assertEquals(reference2HexString, cbor.encodeToHexString(ClassAs2Array.serializer(), reference2))
        assertEquals(reference2, cbor.decodeFromHexString(ClassAs2Array.serializer(), reference2HexString))
    }

    @Test
    fun writeReadVerifyClassWithArray() {
        val cbor = Cbor {
            writeDefiniteLengths = true
        }
        assertEquals(reference3HexString, cbor.encodeToHexString(ClassWithArray.serializer(), reference3))
        assertEquals(reference3, cbor.decodeFromHexString(ClassWithArray.serializer(), reference3HexString))
    }

    @CborArray
    @Serializable
    data class ClassAs1Array(
        @SerialName("alg")
        val alg: Int,
    )

    @CborArray(8U)
    @Serializable
    data class ClassAs2Array(
        @SerialName("alg")
        val alg: Int,
        @SerialName("kid")
        val kid: String,
    )

    @Serializable
    data class ClassWithArray(
        @SerialName("array")
        val array: ClassAs2Array,
    )
}

