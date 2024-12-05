package kotlinx.serialization.cbor

import kotlinx.serialization.*
import kotlin.test.*

class CborSkipTagAndEmptyTest {

    /**
     * A3                                      # map(3)
     *    67                                   # text(7)
     *       76657273696F6E                    # ""version""
     *    63                                   # text(3)
     *       312E30                            # ""1.0""
     *    69                                   # text(9)
     *       646F63756D656E7473                # ""documents""
     *    81                                   # array(1)
     *       A1                                # map(1)
     *          6C                             # text(12)
     *             6465766963655369676E6564    # ""deviceSigned""
     *          A2                             # map(2)
     *             6A                          # text(10)
     *                6E616D65537061636573     # ""nameSpaces""
     *             D8 18                       # tag(24) <------------------- Testing this skips properly
     *                41                       # bytes(1)
     *                   A0                    # ""\xA0""
     *             6A                          # text(10)
     *                64657669636541757468     # ""deviceAuth""
     *             A1                          # map(1)
     *                69                       # text(9)
     *                   6465766963654D6163    # ""deviceMac""
     *                84                       # array(4)
     *                   43                    # bytes(3)
     *                      A10105
     *                   A0                    # map(0) <------------------- Testing this skips properly
     *                   F6                    # primitive(22)
     *                   58 20                 # bytes(32)
     *                      E99521A85AD7891B806A07F8B5388A332D92C189A7BF293EE1F543405AE6824D
     *    66                                   # text(6)
     *       737461747573                      # ""status""
     *    00                                   # unsigned(0)
     */
    private val referenceHexString = "A36776657273696F6E63312E3069646F63756D656E747381A16C6465766963655369676E6564A26A6E616D65537061636573D81841A06A64657669636541757468A1696465766963654D61638443A10105A0F65820E99521A85AD7891B806A07F8B5388A332D92C189A7BF293EE1F543405AE6824D6673746174757300"

    @Test
    fun deserializesCorrectly() {
        // Specifically, skipping keys with descendants that contain tags and empty maps
        val cbor = Cbor{
            ignoreUnknownKeys = true
        }
        // Prior exception:
        // Field 'status' is required for type with serial name 'kotlinx.serialization.cbor.CborSkipTagAndEmptyTest.DataClass', but it was missing
        val target = cbor.decodeFromHexString(DataClass.serializer(), referenceHexString)
        assertEquals(0, target.status)
    }

    @Serializable
    data class DataClass(
        val status: Int,
    )
}