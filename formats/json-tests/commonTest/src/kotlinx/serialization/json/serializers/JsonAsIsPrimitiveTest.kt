package kotlinx.serialization.json.serializers

import kotlinx.serialization.json.*
import kotlin.test.*

class JsonAsIsPrimitiveTest : JsonTestBase() {
    private val asIs = Json { encodeAsIs = true }
    private val disabled = Json { encodeAsIs = false }

    private fun assertAsIsLiteralSerialized(inputValue: String) {
        assertSerialized(inputValue, inputValue, asIs)
    }

    private fun assertDisabledSerialized(expectedValue: String, inputValue: String) {
        assertSerialized(expectedValue, inputValue, disabled)
    }

    private fun assertSerialized(exceptedValue: String, inputValue: String, json: Json) {
        val normalElement = default.decodeFromString(JsonPrimitive.serializer(), inputValue)

        assertEquals(
            inputValue,
            normalElement.toString(),
            "Decoded JsonElement.toString() not returning original value."
        )

        parametrizedTest { mode ->
            assertEquals(exceptedValue, json.encodeToString(JsonElement.serializer(), normalElement, mode))
        }
    }

    @Test
    fun testJsonNumbersAsIs() {
        assertAsIsLiteralSerialized("true")
        assertAsIsLiteralSerialized("false")
        assertAsIsLiteralSerialized("null")
        assertAsIsLiteralSerialized("1")
        assertAsIsLiteralSerialized("-1")
        assertAsIsLiteralSerialized("100.0")
        assertAsIsLiteralSerialized("-100.0")

        assertAsIsLiteralSerialized("9999999999999999999999999999999999999999999999999999999.9999999999999999999999999999999999999999999999999999999")
        assertAsIsLiteralSerialized("-9999999999999999999999999999999999999999999999999999999.9999999999999999999999999999999999999999999999999999999")

        assertAsIsLiteralSerialized("99999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999")
        assertAsIsLiteralSerialized("-99999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999")

        assertAsIsLiteralSerialized("2.99792458e8")
        assertAsIsLiteralSerialized("-2.99792458e8")

        assertAsIsLiteralSerialized("2.99792458E8")
        assertAsIsLiteralSerialized("-2.99792458E8")

        assertAsIsLiteralSerialized("11.399999999999")
        assertAsIsLiteralSerialized("0.30000000000000004")
        assertAsIsLiteralSerialized("0.1000000000000000055511151231257827021181583404541015625")
        assertAsIsLiteralSerialized("1.000000000000000000000000001")
        assertAsIsLiteralSerialized("-5.0000000000000000000000000000000000000000000000000000000000000000000000000000")
        assertAsIsLiteralSerialized("123.12312312312312300000000000000000000000000000000000000000000000000000000000")

    }

    @Test
    fun testJsonStringsSerializable() {
        assertAsIsLiteralSerialized("\"hello world\"")
        assertAsIsLiteralSerialized("\"string\"")
        assertAsIsLiteralSerialized("\"\\n\"")
    }

    @Test
    fun testDisabledTruncated() {
        assertDisabledSerialized("1.0", "1.00")
        assertDisabledSerialized("1.0", "1.00000000")
        assertDisabledSerialized("1.0", "1.000000000000000000000")
        assertDisabledSerialized("-1.0", "-1.000000000000000000000")
        assertDisabledSerialized("-1.0", "-1.0")
        assertDisabledSerialized("-1.0", "-1.0000000")

        assertDisabledSerialized("1", "1")
        assertDisabledSerialized("10", "10")

        assertDisabledSerialized("-1", "-1")
        assertDisabledSerialized("-10", "-10")
    }
}
