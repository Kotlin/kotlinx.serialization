package kotlinx.serialization

import kotlinx.serialization.internal.HexConverter
import kotlin.test.Test
import kotlin.test.assertEquals

class StringTest {
    val str = "i ♥ u"
    val hex = "6920E299A52075"

    @Test
    fun toUtf8() {
        val bytes = str.toUtf8Bytes()
        assertEquals(hex, HexConverter.printHexBinary(bytes, false))
    }

    @Test
    fun fromUtf8() {
        val s = stringFromUtf8Bytes(HexConverter.parseHexBinary(hex))
        assertEquals(str, s)
    }
}