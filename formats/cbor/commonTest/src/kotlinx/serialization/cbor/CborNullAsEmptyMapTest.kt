package kotlinx.serialization.cbor

import kotlinx.serialization.*
import kotlin.test.*


class CborNullAsEmptyMapTest {


    @Test
    fun nullableAsMap() {
        /**
         * a1                     # map(1)
         *    68                  #   text(8)
         *       6e756c6c61626c65 #     "nullable"
         *    a0                  #   map(0)
         */
        val referenceHexString = "a1686e756c6c61626c65a0"
        val reference = ClassWNullableAsMap(nullable = null)

        val cbor = Cbor.CoseCompliant

        assertEquals(referenceHexString, cbor.encodeToHexString(ClassWNullableAsMap.serializer(), reference))
        assertEquals(reference, cbor.decodeFromHexString(ClassWNullableAsMap.serializer(), referenceHexString))
    }

    @Test
    fun nullableAsNull() {
        /**
         * a1                     # map(1)
         *    68                  #   text(8)
         *       6e756c6c61626c65 #     "nullable"
         *    f6                  #   null, simple(22)
         */
        val referenceHexString = "a1686e756c6c61626c65f6"
        val reference = ClassWNullableAsNull(nullable = null)


        val cbor = Cbor.CoseCompliant

        assertEquals(referenceHexString, cbor.encodeToHexString(ClassWNullableAsNull.serializer(), reference))
        assertEquals(reference, cbor.decodeFromHexString(ClassWNullableAsNull.serializer(), referenceHexString))
    }

    @Test
    fun nullableAsMapWithDefaultNull() {
        /**
         * a1                     # map(1)
         *    68                  #   text(8)
         *       6e756c6c61626c65 #     "nullable"
         *    a0                  #   map(0)
         */
        val referenceHexString = "a1686e756c6c61626c65a0"
        val reference = ClassWNullableAsMapWithDefaultValueNull()

        val cbor = Cbor {
            useDefiniteLengthEncoding = true
            encodeDefaults = true
        }

        assertEquals(
            referenceHexString,
            cbor.encodeToHexString(ClassWNullableAsMapWithDefaultValueNull.serializer(), reference)
        )
        assertEquals(
            reference,
            cbor.decodeFromHexString(ClassWNullableAsMapWithDefaultValueNull.serializer(), referenceHexString)
        )
    }

    @Test
    fun nullableAsNullWithDefaultNull() {
        /**
         * a1                     # map(1)
         *    68                  #   text(8)
         *       6e756c6c61626c65 #     "nullable"
         *    f6                  #   null, simple(22)
         */
        val referenceHexString = "a1686e756c6c61626c65f6"
        val reference = ClassWNullableAsNullWithDefaultValueNull()


        val cbor = Cbor {
            useDefiniteLengthEncoding = true
            encodeDefaults = true
        }

        assertEquals(
            referenceHexString,
            cbor.encodeToHexString(ClassWNullableAsNullWithDefaultValueNull.serializer(), reference)
        )
        assertEquals(
            reference,
            cbor.decodeFromHexString(ClassWNullableAsNullWithDefaultValueNull.serializer(), referenceHexString)
        )
    }
    @Test
    fun nullableAsMapWithDefaultNullNoEncodeDefaults() {
        /**
         * a0                     # map(0)
         */
        val referenceHexString = "a0"
        val reference = ClassWNullableAsMapWithDefaultValueNull()

        val cbor = Cbor {
            useDefiniteLengthEncoding = true
            encodeDefaults = false
        }

        assertEquals(
            referenceHexString,
            cbor.encodeToHexString(ClassWNullableAsMapWithDefaultValueNull.serializer(), reference)
        )
        assertEquals(
            reference,
            cbor.decodeFromHexString(ClassWNullableAsMapWithDefaultValueNull.serializer(), referenceHexString)
        )
    }

    @Test
    fun nullableAsNullWithDefaultNullNoEncodeDefaults() {
        /**
         * a0                     # map(0)
         */
        val referenceHexString = "a0"
        val reference = ClassWNullableAsNullWithDefaultValueNull()


        val cbor = Cbor {
            useDefiniteLengthEncoding = true
            encodeDefaults = false
        }

        assertEquals(
            referenceHexString,
            cbor.encodeToHexString(ClassWNullableAsNullWithDefaultValueNull.serializer(), reference)
        )
        assertEquals(
            reference,
            cbor.decodeFromHexString(ClassWNullableAsNullWithDefaultValueNull.serializer(), referenceHexString)
        )
    }
}

@Serializable
data class ClassWNullableAsMap(
    @SerialName("nullable")
    @CborNullAsEmptyMap
    val nullable: NullableClass?
)

@Serializable
data class ClassWNullableAsMapWithDefaultValueNull(
    @SerialName("nullable")
    @CborNullAsEmptyMap
    val nullable: NullableClass? = null
)


@Serializable
data class ClassWNullableAsNull(
    @SerialName("nullable")
    val nullable: NullableClass?
)

@Serializable
data class ClassWNullableAsNullWithDefaultValueNull(
    @SerialName("nullable")
    val nullable: NullableClass? = null
)

@Serializable
data class NullableClass(
    val property: String
)



