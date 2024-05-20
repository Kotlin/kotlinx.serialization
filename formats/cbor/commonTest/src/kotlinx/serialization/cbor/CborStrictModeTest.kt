package kotlinx.serialization.cbor

import kotlinx.serialization.assertFailsWithMessage
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.HexConverter
import kotlinx.serialization.DuplicateKeyException
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
}
