package kotlinx.serialization.json.serializers

import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.json.*
import kotlinx.serialization.test.assertFailsWithSerialMessage
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonUnquotedLiteralTest : JsonTestBase() {

    private fun assertUnquotedLiteralEncoded(inputValue: String) {
        val unquotedElement = JsonUnquotedLiteral(inputValue)

        assertEquals(
            inputValue,
            unquotedElement.toString(),
            "expect JsonElement.toString() returns the unquoted input value"
        )

        parametrizedTest { mode ->
            assertEquals(inputValue, default.encodeToString(JsonElement.serializer(), unquotedElement, mode))
        }
    }

    @Test
    fun testUnquotedJsonNumbers() {
        assertUnquotedLiteralEncoded("1")
        assertUnquotedLiteralEncoded("-1")
        assertUnquotedLiteralEncoded("100.0")
        assertUnquotedLiteralEncoded("-100.0")

        assertUnquotedLiteralEncoded("9999999999999999999999999999999999999999999999999999999.9999999999999999999999999999999999999999999999999999999")
        assertUnquotedLiteralEncoded("-9999999999999999999999999999999999999999999999999999999.9999999999999999999999999999999999999999999999999999999")

        assertUnquotedLiteralEncoded("99999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999")
        assertUnquotedLiteralEncoded("-99999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999999")

        assertUnquotedLiteralEncoded("2.99792458e8")
        assertUnquotedLiteralEncoded("-2.99792458e8")

        assertUnquotedLiteralEncoded("2.99792458E8")
        assertUnquotedLiteralEncoded("-2.99792458E8")

        assertUnquotedLiteralEncoded("11.399999999999")
        assertUnquotedLiteralEncoded("0.30000000000000004")
        assertUnquotedLiteralEncoded("0.1000000000000000055511151231257827021181583404541015625")
    }

    @Test
    fun testUnquotedJsonWhitespaceStrings() {
        assertUnquotedLiteralEncoded("")
        assertUnquotedLiteralEncoded("         ")
        assertUnquotedLiteralEncoded("\t")
        assertUnquotedLiteralEncoded("\t\t\t")
        assertUnquotedLiteralEncoded("\r\n")
        assertUnquotedLiteralEncoded("\n")
        assertUnquotedLiteralEncoded("\n\n\n")
    }

    @Test
    fun testUnquotedJsonStrings() {
        assertUnquotedLiteralEncoded("lorem")
        assertUnquotedLiteralEncoded(""""lorem"""")
        assertUnquotedLiteralEncoded(
            """
                Well, my name is Freddy Kreuger
                I've got the Elm Street blues
                I've got a hand like a knife rack
                And I die in every film!
            """.trimIndent()
        )
    }

    @Test
    fun testUnquotedJsonObjects() {
        assertUnquotedLiteralEncoded("""{"some":"json"}""")
        assertUnquotedLiteralEncoded("""{"some":"json","object":true,"count":1,"array":[1,2.0,-333,"4",boolean]}""")
    }

    @Test
    fun testUnquotedJsonArrays() {
        assertUnquotedLiteralEncoded("""[1,2,3]""")
        assertUnquotedLiteralEncoded("""["a","b","c"]""")
        assertUnquotedLiteralEncoded("""[true,false]""")
        assertUnquotedLiteralEncoded("""[1,2.0,-333,"4",boolean]""")
        assertUnquotedLiteralEncoded("""[{"some":"json","object":true,"count":1,"array":[1,2.0,-333,"4",boolean]}]""")
        assertUnquotedLiteralEncoded("""[{"some":"json","object":true,"count":1,"array":[1,2.0,-333,"4",boolean]},{"some":"json","object":true,"count":1,"array":[1,2.0,-333,"4",boolean]}]""")
    }

    @Test
    fun testUnquotedJsonNull() {
        assertEquals(JsonNull, JsonUnquotedLiteral(null))
    }

    @Test
    fun testUnquotedJsonNullString() {
        fun test(block: () -> Unit) {
            assertFailsWithSerialMessage(
                exceptionName = "JsonEncodingException",
                message = "Creating a literal unquoted value of 'null' is forbidden. If you want to create JSON null literal, use JsonNull object, otherwise, use JsonPrimitive",
                block = block,
            )
        }

        test { JsonUnquotedLiteral("null") }
        test { JsonUnquotedLiteral(JsonNull.content) }
        test { buildJsonObject { put("key", JsonUnquotedLiteral("null")) } }
    }

    @Test
    fun testUnquotedJsonInvalidMapKeyIsEscaped() {
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
