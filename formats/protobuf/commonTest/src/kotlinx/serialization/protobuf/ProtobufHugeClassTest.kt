package kotlinx.serialization.protobuf

import kotlinx.serialization.HexConverter
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlin.test.Test
import kotlin.test.assertEquals

class ProtobufHugeClassTest {
    @Serializable
    data class Lists64(
        val field0: List<Int>,
        val field1: List<Int>,
        val field2: List<Int>,
        val field3: List<Int>,
        val field4: List<Int>,
        val field5: List<Int>,
        val field6: List<Int>,
        val field7: List<Int>,
        val field8: List<Int>,
        val field9: List<Int>,

        val field10: List<Int>,
        val field11: List<Int>,
        val field12: List<Int>,
        val field13: List<Int>,
        val field14: List<Int>,
        val field15: List<Int>,
        val field16: List<Int>,
        val field17: List<Int>,
        val field18: List<Int>,
        val field19: List<Int>,

        val field20: List<Int>,
        val field21: List<Int>,
        val field22: List<Int>,
        val field23: List<Int>,
        val field24: List<Int>,
        val field25: List<Int>,
        val field26: List<Int>,
        val field27: List<Int>,
        val field28: List<Int>,
        val field29: List<Int>,

        val field30: List<Int>,
        val field31: List<Int>,
        val field32: List<Int>,
        val field33: List<Int>,
        val field34: List<Int>,
        val field35: List<Int>,
        val field36: List<Int>,
        val field37: List<Int>,
        val field38: List<Int>,
        val field39: List<Int>,

        val field40: List<Int>,
        val field41: List<Int>,
        val field42: List<Int>,
        val field43: List<Int>,
        val field44: List<Int>,
        val field45: List<Int>,
        val field46: List<Int>,
        val field47: List<Int>,
        val field48: List<Int>,
        val field49: List<Int>,

        val field50: List<Int>,
        val field51: List<Int>,
        val field52: List<Int>,
        val field53: List<Int>,
        val field54: List<Int>,
        val field55: List<Int>,
        val field56: List<Int>,
        val field57: List<Int>,
        val field58: List<Int>,
        val field59: List<Int>,

        val field60: List<Int>,
        val field61: List<Int>,
        val field62: List<Int>,
        val field63: List<Int>
    )

    @Serializable
    data class Values70(
        val field0: Int?,
        val field1: Int?,
        val field2: Int?,
        val field3: Int?,
        val field4: Int?,
        val field5: Int?,
        val field6: Int?,
        val field7: Int?,
        val field8: Int?,
        val field9: Int?,

        val field10: Int?,
        val field11: Int?,
        val field12: Int?,
        val field13: Int?,
        val field14: Int?,
        val field15: Int?,
        val field16: Int?,
        val field17: Int?,
        val field18: Int?,
        val field19: Int?,

        val field20: Int?,
        val field21: Int?,
        val field22: Int?,
        val field23: Int?,
        val field24: Int?,
        val field25: Int?,
        val field26: Int?,
        val field27: Int?,
        val field28: Int?,
        val field29: Int?,

        val field30: Int?,
        val field31: Int?,
        val field32: Int?,
        val field33: Int?,
        val field34: Int?,
        val field35: Int?,
        val field36: Int?,
        val field37: Int?,
        val field38: Int?,
        val field39: Int?,

        val field40: Int?,
        val field41: Int?,
        val field42: Int?,
        val field43: Int?,
        val field44: Int?,
        val field45: Int?,
        val field46: Int?,
        val field47: Int?,
        val field48: Int?,
        val field49: Int?,

        val field50: Int?,
        val field51: Int?,
        val field52: Int?,
        val field53: Int?,
        val field54: Int?,
        val field55: Int?,
        val field56: Int?,
        val field57: Int?,
        val field58: Int?,
        val field59: Int?,

        val field60: Int?,
        val field61: Int?,
        val field62: Int?,
        val field63: Int?,
        val field64: Int?,
        val field65: Int?,
        val field66: Int?,
        val field67: Int?,
        val field68: Int?,
        val field69: Int?
    )

    @Serializable
    data class Values128(
        val field0: Int?,
        val field1: Int?,
        val field2: Int?,
        val field3: Int?,
        val field4: Int?,
        val field5: Int?,
        val field6: Int?,
        val field7: Int?,
        val field8: Int?,
        val field9: Int?,

        val field10: Int?,
        val field11: Int?,
        val field12: Int?,
        val field13: Int?,
        val field14: Int?,
        val field15: Int?,
        val field16: Int?,
        val field17: Int?,
        val field18: Int?,
        val field19: Int?,

        val field20: Int?,
        val field21: Int?,
        val field22: Int?,
        val field23: Int?,
        val field24: Int?,
        val field25: Int?,
        val field26: Int?,
        val field27: Int?,
        val field28: Int?,
        val field29: Int?,

        val field30: Int?,
        val field31: Int?,
        val field32: Int?,
        val field33: Int?,
        val field34: Int?,
        val field35: Int?,
        val field36: Int?,
        val field37: Int?,
        val field38: Int?,
        val field39: Int?,

        val field40: Int?,
        val field41: Int?,
        val field42: Int?,
        val field43: Int?,
        val field44: Int?,
        val field45: Int?,
        val field46: Int?,
        val field47: Int?,
        val field48: Int?,
        val field49: Int?,

        val field50: Int?,
        val field51: Int?,
        val field52: Int?,
        val field53: Int?,
        val field54: Int?,
        val field55: Int?,
        val field56: Int?,
        val field57: Int?,
        val field58: Int?,
        val field59: Int?,

        val field60: Int?,
        val field61: Int?,
        val field62: Int?,
        val field63: Int?,
        val field64: Int?,
        val field65: Int?,
        val field66: Int?,
        val field67: Int?,
        val field68: Int?,
        val field69: Int?,

        val field70: Int?,
        val field71: Int?,
        val field72: Int?,
        val field73: Int?,
        val field74: Int?,
        val field75: Int?,
        val field76: Int?,
        val field77: Int?,
        val field78: Int?,
        val field79: Int?,

        val field80: Int?,
        val field81: Int?,
        val field82: Int?,
        val field83: Int?,
        val field84: Int?,
        val field85: Int?,
        val field86: Int?,
        val field87: Int?,
        val field88: Int?,
        val field89: Int?,

        val field90: Int?,
        val field91: Int?,
        val field92: Int?,
        val field93: Int?,
        val field94: Int?,
        val field95: Int?,
        val field96: Int?,
        val field97: Int?,
        val field98: Int?,
        val field99: Int?,

        val field100: Int?,
        val field101: Int?,
        val field102: Int?,
        val field103: Int?,
        val field104: Int?,
        val field105: Int?,
        val field106: Int?,
        val field107: Int?,
        val field108: Int?,
        val field109: Int?,

        val field110: Int?,
        val field111: Int?,
        val field112: Int?,
        val field113: Int?,
        val field114: Int?,
        val field115: Int?,
        val field116: Int?,
        val field117: Int?,
        val field118: Int?,
        val field119: Int?,

        val field120: Int?,
        val field121: Int?,
        val field122: Int?,
        val field123: Int?,
        val field124: Int?,
        val field125: Int?,
        val field126: Int?,
        val field127: Int?
    )
    @Serializable
    data class Values130(
        val field0: Int?,
        val field1: Int?,
        val field2: Int?,
        val field3: Int?,
        val field4: Int?,
        val field5: Int?,
        val field6: Int?,
        val field7: Int?,
        val field8: Int?,
        val field9: Int?,

        val field10: Int?,
        val field11: Int?,
        val field12: Int?,
        val field13: Int?,
        val field14: Int?,
        val field15: Int?,
        val field16: Int?,
        val field17: Int?,
        val field18: Int?,
        val field19: Int?,

        val field20: Int?,
        val field21: Int?,
        val field22: Int?,
        val field23: Int?,
        val field24: Int?,
        val field25: Int?,
        val field26: Int?,
        val field27: Int?,
        val field28: Int?,
        val field29: Int?,

        val field30: Int?,
        val field31: Int?,
        val field32: Int?,
        val field33: Int?,
        val field34: Int?,
        val field35: Int?,
        val field36: Int?,
        val field37: Int?,
        val field38: Int?,
        val field39: Int?,

        val field40: Int?,
        val field41: Int?,
        val field42: Int?,
        val field43: Int?,
        val field44: Int?,
        val field45: Int?,
        val field46: Int?,
        val field47: Int?,
        val field48: Int?,
        val field49: Int?,

        val field50: Int?,
        val field51: Int?,
        val field52: Int?,
        val field53: Int?,
        val field54: Int?,
        val field55: Int?,
        val field56: Int?,
        val field57: Int?,
        val field58: Int?,
        val field59: Int?,

        val field60: Int?,
        val field61: Int?,
        val field62: Int?,
        val field63: Int?,
        val field64: Int?,
        val field65: Int?,
        val field66: Int?,
        val field67: Int?,
        val field68: Int?,
        val field69: Int?,

        val field70: Int?,
        val field71: Int?,
        val field72: Int?,
        val field73: Int?,
        val field74: Int?,
        val field75: Int?,
        val field76: Int?,
        val field77: Int?,
        val field78: Int?,
        val field79: Int?,

        val field80: Int?,
        val field81: Int?,
        val field82: Int?,
        val field83: Int?,
        val field84: Int?,
        val field85: Int?,
        val field86: Int?,
        val field87: Int?,
        val field88: Int?,
        val field89: Int?,

        val field90: Int?,
        val field91: Int?,
        val field92: Int?,
        val field93: Int?,
        val field94: Int?,
        val field95: Int?,
        val field96: Int?,
        val field97: Int?,
        val field98: Int?,
        val field99: Int?,

        val field100: Int?,
        val field101: Int?,
        val field102: Int?,
        val field103: Int?,
        val field104: Int?,
        val field105: Int?,
        val field106: Int?,
        val field107: Int?,
        val field108: Int?,
        val field109: Int?,

        val field110: Int?,
        val field111: Int?,
        val field112: Int?,
        val field113: Int?,
        val field114: Int?,
        val field115: Int?,
        val field116: Int?,
        val field117: Int?,
        val field118: Int?,
        val field119: Int?,

        val field120: Int?,
        val field121: Int?,
        val field122: Int?,
        val field123: Int?,
        val field124: Int?,
        val field125: Int?,
        val field126: Int?,
        val field127: Int?,
        val field128: Int?,
        val field129: Int?
    )

    private val lists64: Lists64 =
        Lists64(
            emptyList(),
            listOf(1, 42),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),

            emptyList(),
            emptyList(),
            listOf(12, 43),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),

            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),

            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),

            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),

            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),
            emptyList(),

            emptyList(),
            emptyList(),
            emptyList(),
            emptyList()
        )

    private val values70: Values70 = Values70(
        null, null, null, 3, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null, null,
        null, null, 42, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, 66, null, null, null
    )

    private val values128: Values128 = Values128(
        null, null, null, 3, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null, null,
        null, null, 42, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, 66, null, null, null,
        null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, 115, null, null, null, null,
        null, null, null, null, null, null, null, null
    )

    private val values130: Values130 = Values130(
        null, null, null, 3, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null, null,
        null, null, 42, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, 66, null, null, null,
        null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, 115, null, null, null, null,
        null, null, null, null, null, null, null, null, 128, null
    )

    @Test
    fun testLists64() {
        val bytes = ProtoBuf.encodeToByteArray(lists64)
        println(HexConverter.printHexBinary(bytes))

        val decoded = ProtoBuf.decodeFromByteArray<Lists64>(bytes)
        assertEquals(lists64, decoded)
    }

    @Test
    fun testValues70() {
        val bytes = ProtoBuf.encodeToByteArray(values70)
        println(HexConverter.printHexBinary(bytes))

        val decoded = ProtoBuf.decodeFromByteArray<Values70>(bytes)
        assertEquals(values70, decoded)
    }

    @Test
    fun testValues128() {
        val bytes = ProtoBuf.encodeToByteArray(values128)
        println(HexConverter.printHexBinary(bytes))

        val decoded = ProtoBuf.decodeFromByteArray<Values128>(bytes)
        assertEquals(values128, decoded)
    }

    @Test
    fun testValues130() {
        val bytes = ProtoBuf.encodeToByteArray(values130)
        println(HexConverter.printHexBinary(bytes))

        val decoded = ProtoBuf.decodeFromByteArray<Values130>(bytes)
        assertEquals(values130, decoded)
    }
}
