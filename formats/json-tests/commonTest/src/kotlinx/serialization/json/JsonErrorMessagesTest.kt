/*
 * Copyright 2017-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */


package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.test.*
import kotlin.test.*


class JsonErrorMessagesTest : JsonTestBase() {

    @Test
    fun testJsonTokensAreProperlyReported() = parametrizedTest { mode ->
        val input1 = """{"boxed":4}"""
        val input2 = """{"boxed":"str"}"""

        val serString = serializer<Box<String>>()
        val serInt = serializer<Box<Int>>()

        checkSerializationException({
            default.decodeFromString(serString, input1, mode)
        }, { message ->
            if (mode == JsonTestingMode.TREE)
                assertContains(message, "String literal for key 'boxed' should be quoted.")
            else
                assertContains(
                    message,
                    "Unexpected JSON token at offset 9: Expected quotation mark '\"', but had '4' instead at path: \$.boxed"
                )
        })

        checkSerializationException({
            default.decodeFromString(serInt, input2, mode)
        }, { message ->
            if (mode != JsonTestingMode.TREE)
            // we allow number values to be quoted, so the message pointing to 's' is correct
                assertContains(
                    message,
                    "Unexpected JSON token at offset 9: Unexpected symbol 's' in numeric literal at path: \$.boxed"
                )
            else
                assertContains(message, "Failed to parse literal as 'int' value")
        })
    }

    @Test
    fun testMissingClosingQuote() = parametrizedTest { mode ->
        val input1 = """{"boxed:4}"""
        val input2 = """{"boxed":"str}"""
        val input3 = """{"boxed:"str"}"""
        val serString = serializer<Box<String>>()
        val serInt = serializer<Box<Int>>()

        checkSerializationException({
            default.decodeFromString(serInt, input1, mode)
        }, { message ->
            // For discussion:
            // Technically, both of these messages are correct despite them being completely different.
            // A `:` instead of `"` is a good guess, but `:`/`}` is a perfectly valid token inside Json string — for example,
            // it can be some kind of path `{"foo:bar:baz":"my:resource:locator:{123}"}` or even URI used as a string key/value.
            // So if the closing quote is missing, there's really no way to correctly tell where the key or value is supposed to end.
            // Although we may try to unify these messages for consistency.
            if (mode in setOf(JsonTestingMode.STREAMING, JsonTestingMode.TREE))
                assertContains(
                    message,
                    "Unexpected JSON token at offset 7: Expected quotation mark '\"', but had ':' instead at path: \$"
                )
            else
                assertContains(
                    message, "Unexpected EOF at path: \$"
                )
        })

        checkSerializationException({
            default.decodeFromString(serString, input2, mode)
        }, { message ->
            if (mode in setOf(JsonTestingMode.STREAMING, JsonTestingMode.TREE))
                assertContains(
                    message,
                    "Unexpected JSON token at offset 13: Expected quotation mark '\"', but had '}' instead at path: \$"
                )
            else
                assertContains(message, "Unexpected EOF at path: \$.boxed")
        })

        checkSerializationException({
            default.decodeFromString(serString, input3, mode)
        }, { message ->
            assertContains(
                message,
                "Unexpected JSON token at offset 9: Expected colon ':', but had 's' instead at path: \$"
            )
        })
    }

    @Test
    fun testUnquoted() = parametrizedTest { mode ->
        val input1 = """{boxed:str}"""
        val input2 = """{"boxed":str}"""
        val ser = serializer<Box<String>>()

        checkSerializationException({
            default.decodeFromString(ser, input1, mode)
        }, { message ->
            assertContains(
                message,
                """Unexpected JSON token at offset 1: Expected quotation mark '"', but had 'b' instead at path: ${'$'}"""
            )
        })

        checkSerializationException({
            default.decodeFromString(ser, input2, mode)
        }, { message ->
            if (mode == JsonTestingMode.TREE) assertContains(
                message,
                """String literal for key 'boxed' should be quoted."""
            )
            else assertContains(
                message,
                """Unexpected JSON token at offset 9: Expected quotation mark '"', but had 's' instead at path: ${'$'}.boxed"""
            )
        })
    }

    @Test
    fun testNullLiteralForNotNull() = parametrizedTest { mode ->
        val input = """{"boxed":null}"""
        val ser = serializer<Box<String>>()
        checkSerializationException({
            default.decodeFromString(ser, input, mode)
        }, { message ->
            if (mode == JsonTestingMode.TREE)
                assertContains(message, "Unexpected 'null' literal when non-nullable string was expected")
            else
                assertContains(
                    message,
                    "Unexpected JSON token at offset 9: Expected string literal but 'null' literal was found at path: \$.boxed"
                )
        })
    }

    @Test
    fun testEof() = parametrizedTest { mode ->
        val input = """{"boxed":"""
        checkSerializationException({
            default.decodeFromString<Box<String>>(input, mode)
        }, { message ->
            if (mode == JsonTestingMode.TREE)
                assertContains(message, "Cannot read Json element because of unexpected end of the input at path: $")
            else
                assertContains(message, "Expected quotation mark '\"', but had 'EOF' instead at path: \$.boxed")

        })

    }
}
