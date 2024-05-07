package kotlinx.serialization.cbor

import kotlinx.serialization.assertFailsWithMessage
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.HexConverter
import kotlinx.serialization.DuplicateMapKeyException
import kotlin.test.Test

class CborStrictModeTest {
    private val strict = Cbor { allowDuplicateKeys = false }

    /** Duplicate keys are rejected in generic maps. */
    @Test
    fun testDuplicateKeysInMap() {
        val duplicateKeys = HexConverter.parseHexBinary("A2617805617806")
        assertFailsWithMessage<DuplicateMapKeyException>("Duplicate keys not allowed in maps. Key appeared twice: x") {
            strict.decodeFromByteArray<Map<String, Long>>(duplicateKeys)
        }
    }
}
