package kotlinx.serialization.cbor

import kotlinx.serialization.assertFailsWithMessage
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.HexConverter
import kotlinx.serialization.DuplicateKeyException
import kotlinx.serialization.Serializable
import kotlin.test.Test

class CborStrictModeTest {
    private val strict = Cbor { forbidDuplicateKeys = true }

    /** Duplicate keys are rejected in generic maps. */
    @Test
    fun testDuplicateKeysInMap() {
        val duplicateKeys = HexConverter.parseHexBinary("A2617805617806")
        assertFailsWithMessage<DuplicateKeyException>("Duplicate keys not allowed. Key appeared twice: x") {
            strict.decodeFromByteArray<Map<String, Long>>(duplicateKeys)
        }
    }

    @Serializable
    data class ExampleClass(val x: Long)

    /** Duplicate keys are rejected in classes. */
    @Test
    fun testDuplicateKeysInDataClass() {
        // {"x": 5, "x", 6}
        val duplicateKeys = HexConverter.parseHexBinary("A2617805617806")
        assertFailsWithMessage<DuplicateKeyException>("Duplicate keys not allowed. Key appeared twice: x") {
            strict.decodeFromByteArray<ExampleClass>(duplicateKeys)
        }
    }

    /** Duplicate unknown keys are rejected as well. */
    @Test
    fun testDuplicateUnknownKeys() {
        // {"a": 1, "a", 2, "x", 6}
        val duplicateKeys = HexConverter.parseHexBinary("A3616101616102617806")
        val cbor = Cbor(strict) { ignoreUnknownKeys = true }
        assertFailsWithMessage<DuplicateKeyException>("Duplicate keys not allowed. Key appeared twice: a") {
            cbor.decodeFromByteArray<ExampleClass>(duplicateKeys)
        }
    }
}
