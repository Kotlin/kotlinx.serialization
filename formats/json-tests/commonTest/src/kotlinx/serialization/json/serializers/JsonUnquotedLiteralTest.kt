package kotlinx.serialization.json.serializers

import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.json.*
import kotlinx.serialization.test.assertFailsWithSerialMessage
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonUnquotedLiteralTest : JsonTestBase() {

    private fun assertRawJsonValueEncoded(inputValue: String) = parametrizedTest { mode ->
        val rawElement = JsonUnquotedLiteral(inputValue)

        assertEquals(inputValue, rawElement.toString(), "expect JsonElement.toString() returns the raw input value")
        assertEquals(inputValue, default.encodeToString(JsonElement.serializer(), rawElement, mode))
    }

    @Test
    fun testRawJsonNumbers() {
        assertRawJsonValueEncoded("1")
        assertRawJsonValueEncoded("-1")
        assertRawJsonValueEncoded("100.0")
        assertRawJsonValueEncoded("-100.0")

        assertRawJsonValueEncoded("9999999999999999999999999999999999999999999999999999999.9999999999999999999999999999999999999999999999999999999")
        assertRawJsonValueEncoded("-9999999999999999999999999999999999999999999999999999999.9999999999999999999999999999999999999999999999999999999")

        assertRawJsonValueEncoded("99999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999")
        assertRawJsonValueEncoded("-99999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999")

        assertRawJsonValueEncoded("2.99792458e8")
        assertRawJsonValueEncoded("-2.99792458e8")

        assertRawJsonValueEncoded("2.99792458E8")
        assertRawJsonValueEncoded("-2.99792458E8")

        assertRawJsonValueEncoded("11.399999999999")
        assertRawJsonValueEncoded("0.30000000000000004")
        assertRawJsonValueEncoded("0.1000000000000000055511151231257827021181583404541015625")
    }

    @Test
    fun testRawJsonWhitespaceStrings() {
        assertRawJsonValueEncoded("")
        assertRawJsonValueEncoded("         ")
        assertRawJsonValueEncoded("\t")
        assertRawJsonValueEncoded("\t\t\t")
        assertRawJsonValueEncoded("\r\n")
        assertRawJsonValueEncoded("\n")
        assertRawJsonValueEncoded("\n\n\n")
    }

    @Test
    fun testRawJsonStrings() {
        assertRawJsonValueEncoded("lorem")
        assertRawJsonValueEncoded(""""lorem"""")
        assertRawJsonValueEncoded(
            """
                Well, my name is Freddy Kreuger
                I've got the Elm Street blues
                I've got a hand like a knife rack
                And I die in every film!
            """.trimIndent()
        )
    }

    @Test
    fun testRawJsonObjects() {
        assertRawJsonValueEncoded("""{"some":"json"}""")
        assertRawJsonValueEncoded("""{"some":"json","object":true,"count":1,"array":[1,2.0,-333,"4",boolean]}""")
    }

    @Test
    fun testRawJsonArrays() {
        assertRawJsonValueEncoded("""[1,2,3]""")
        assertRawJsonValueEncoded("""["a","b","c"]""")
        assertRawJsonValueEncoded("""[true,false]""")
        assertRawJsonValueEncoded("""[1,2.0,-333,"4",boolean]""")
        assertRawJsonValueEncoded("""[{"some":"json","object":true,"count":1,"array":[1,2.0,-333,"4",boolean]}]""")
        assertRawJsonValueEncoded("""[{"some":"json","object":true,"count":1,"array":[1,2.0,-333,"4",boolean]},{"some":"json","object":true,"count":1,"array":[1,2.0,-333,"4",boolean]}]""")
    }

    @Test
    fun testRawJsonNull() {
        assertEquals(JsonNull, JsonUnquotedLiteral(null))
    }

    @Test
    fun testRawJsonNullString() {
        fun test(block: () -> Unit) {
            assertFailsWithSerialMessage(
                "JsonEncodingException",
                "It is impossible to create a literal unquoted value of 'null'. If you want to create JSON null literal, use JsonNull object, otherwise, use JsonPrimitive",
                block = block,
            )
        }

        test { JsonUnquotedLiteral("null") }
        test { JsonUnquotedLiteral(JsonNull.content) }
    }

    @Test
    fun testRawJsonInvalidMapKeyIsEscaped() {
        val mapSerializer = MapSerializer(
            JsonPrimitive.serializer(),
            JsonPrimitive.serializer(),
        )

        fun test(expected: String, input: String) = parametrizedTest { mode ->
            val data = mapOf(JsonUnquotedLiteral(input) to JsonPrimitive("invalid key"))

            assertEquals(
                """ {"$expected":"invalid key"} """.trim(),
                default.encodeToString(mapSerializer, data, mode),
            )
        }

        test(" ", " ")
        test(
            """ \\\"\\\" """.trim(),
            """  \"\"    """.trim(),
        )
        test(
            """  \\\\\\\"  """.trim(),
            """  \\\"      """.trim(),
        )
        test(
            """  {\\\"I'm not a valid JSON object key\\\"}  """.trim(),
            """  {\"I'm not a valid JSON object key\"}      """.trim(),
        )
    }
}
