package kotlinx.serialization.cbor

import kotlinx.serialization.*
import kotlin.test.*


class CborArrayTest {

    @Test
    fun writeReadVerifyArraySize1() {
        /**
         * 81    # array(1)
         *    26 # negative(6)
         */
        val referenceHexString = "8126"
        val reference = ClassAs1Array(alg = -7)

        val cbor = Cbor {
            writeDefiniteLengths = true
        }
        assertEquals(referenceHexString, cbor.encodeToHexString(ClassAs1Array.serializer(), reference))
        assertEquals(reference, cbor.decodeFromHexString(ClassAs1Array.serializer(), referenceHexString))
    }

    @Test
    fun writeReadVerifyArraySize2() {
        /**
         * C8              # tag(8)
         *    82           # array(2)
         *       26        # negative(6)
         *       63        # text(3)
         *          666F6F # "foo"
         */
        val referenceHexString = "c8822663666f6f"
        val reference = ClassAs2Array(alg = -7, kid = "foo")

        val cbor = Cbor {
            writeDefiniteLengths = true
        }
        assertEquals(referenceHexString, cbor.encodeToHexString(ClassAs2Array.serializer(), reference))
        assertEquals(reference, cbor.decodeFromHexString(ClassAs2Array.serializer(), referenceHexString))
    }

    @Test
    fun writeReadVerifyArraySize4Nullable() {
        /**
         * 84           # array(4)
         *    26        # negative(6)
         *    63        # text(3)
         *       626172 # "bar"
         *    F6        # primitive(22)
         *    A0        # map(0)
         */
        val referenceHexString = "842663626172f6a0"
        val reference = ClassAs4ArrayNullable(alg = -7, kid = "bar", iv = null, array = null)

        val cbor = Cbor {
            writeDefiniteLengths = true
            explicitNulls = true
        }

        assertEquals(referenceHexString, cbor.encodeToHexString(ClassAs4ArrayNullable.serializer(), reference))
        assertEquals(reference, cbor.decodeFromHexString(ClassAs4ArrayNullable.serializer(), referenceHexString))
    }

    @Test
    fun writeReadVerifyClassWithArray() {
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
        val referenceHexString = "a1656172726179c8822663626172"
        val reference = ClassWithArray(array = ClassAs2Array(alg = -7, kid = "bar"))

        val cbor = Cbor {
            writeDefiniteLengths = true
        }
        assertEquals(referenceHexString, cbor.encodeToHexString(ClassWithArray.serializer(), reference))
        assertEquals(reference, cbor.decodeFromHexString(ClassWithArray.serializer(), referenceHexString))
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

    @CborArray
    @Serializable
    data class ClassAs4ArrayNullable(
        @SerialName("alg")
        val alg: Int,
        @SerialName("kid")
        val kid: String,
        @SerialName("iv")
        @ByteString
        val iv: ByteArray?,
        @SerialName("array")
        val array: ClassWithArray?
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as ClassAs4ArrayNullable

            if (alg != other.alg) return false
            if (kid != other.kid) return false
            if (iv != null) {
                if (other.iv == null) return false
                if (!iv.contentEquals(other.iv)) return false
            } else if (other.iv != null) return false
            if (array != other.array) return false

            return true
        }

        override fun hashCode(): Int {
            var result = alg
            result = 31 * result + kid.hashCode()
            result = 31 * result + (iv?.contentHashCode() ?: 0)
            result = 31 * result + (array?.hashCode() ?: 0)
            return result
        }
    }


    @Serializable
    data class ClassWithArray(
        @SerialName("array")
        val array: ClassAs2Array,
    )
}

