/*
 * Copyright 2017-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */


package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlin.test.*


class JsonErrorMessagesTest : JsonTestBase() {

    class DecodingExceptionAsserter(val mode: JsonTestingMode, val exception: JsonDecodingException) {
        private var hasPath = false
        private var hasOffset = false
        private var hasHint = false

        fun message(msg: String, alternativeForTree: String? = null) {
            val expected = alternativeForTree.takeIf { mode == JsonTestingMode.TREE } ?: msg
            assertContains(exception.message, expected, message = "Full message is incomplete")
            assertContains(
                expected,
                exception.shortMessage,
                message = "Short message is not substring of expected message"
            )
        }

        fun path(path: String) {
            assertContains(exception.message, " at path: $path", message = "Path is not substring of expected message")
            assertEquals(path, exception.path, message = "Path is not equal to expected path")
            hasPath = true
        }

        fun offset(offset: Int) {
            if (mode == JsonTestingMode.TREE && exception.offset == -1) return
            assertContains(
                exception.message,
                "Unexpected JSON token at offset $offset: ",
                message = "Offset is not substring of expected message"
            )
            assertEquals(offset, exception.offset, message = "Offset is not equal to expected offset")
            hasOffset = true
        }

        fun hint(hint: String) {
            assertContains(exception.message, hint, message = "Hint is not substring of expected message")
            assertContains(exception.hint!!, hint, message = "Hint is not substring of expected hint")
            hasHint = true
        }

        fun input(input: String) {
            if (mode == JsonTestingMode.TREE) return // Inputs generally show only current sub-object for TreeDecoder
            assertContains(
                exception.message,
                "JSON input: ",
                message = "Input pragma is not substring of expected message"
            )
            assertContains(
                exception.message,
                input,
                message = "Input content is not substring of expected message"
            )
            assertEquals(input, exception.input, message = "Input is not equal to expected input")
        }

        fun assertMissing() {
            if (!hasPath) assertNull(exception.path, "Path is not null")
            if (!hasOffset) assertEquals(-1, exception.offset, "Offset is not -1")
            if (!hasHint) assertNull(exception.hint, "Hint is not null")
        }

    }

    inline fun checkDecodingException(
        mode: JsonTestingMode,
        action: () -> Any?,
        assertions: DecodingExceptionAsserter.() -> Unit
    ) {
        val e = assertFailsWith(JsonDecodingException::class, action)
        val asserter = DecodingExceptionAsserter(mode, e)
        val result= runCatching {
            asserter.assertions()
            asserter.assertMissing()
        }
        if (result.isFailure) {
            println("Test failed for exception: ${asserter.exception.message}")
            asserter.exception.printStackTrace() // cannot do initCause outside of JVM
            throw result.exceptionOrNull()!!
        }
    }


    @Serializable
    @SerialName("app.Failure")
    sealed interface Failure {
        @Serializable
        @SerialName("a")
        data class A(val failure: Failure) : Failure
    }

    @Test
    fun testPolymorphicCastMessage() = parametrizedTest { mode ->
        val input = """{"type":"a", "failure":"wrong-input"}"""
        checkDecodingException(mode, {
            default.decodeFromString(
                Failure.serializer(),
                input,
                mode
            )
        }, {
            message("Expected JsonObject, but had JsonLiteral as the serialized body of app.Failure")
            path("$.failure")
            input("\"wrong-input\"") // For tree casting, input is the object alone
        })
    }

    @Test
    fun testPrimitiveInsteadOfObjectOrList() = parametrizedTest { mode ->
        val input = """{"boxed": 42}"""
        checkDecodingException(mode, {
            default.decodeFromString(Box.serializer(StringData.serializer()), input, mode)
        }, {
            message(
                "Expected start of the object '{', but had '4' instead",
                "Expected JsonObject, but had JsonLiteral as the serialized body of kotlinx.serialization.StringData",
            )
            offset(10)
            path("$.boxed")
            input(input)
        })

        checkDecodingException(mode, {
            default.decodeFromString(Box.serializer(ListSerializer(StringData.serializer())), input, mode)
        }, {
            message(
                "Expected start of the array '[', but had '4' instead",
                "Expected JsonArray, but had JsonLiteral as the serialized body of kotlin.collections.ArrayList",
            )
            offset(10)
            path("$.boxed")
            input(input)
        })
    }

    @Test
    fun testObjectOrListInsteadOfPrimitive() = parametrizedTest { mode ->
        val inputArr = """{"boxed": [1,2]}"""
        checkDecodingException(mode, {
            default.decodeFromString(Box.serializer(Int.serializer()), inputArr, mode)
        }, {
            message(
                "Expected numeric literal",
                "Expected JsonPrimitive, but had JsonArray as the serialized body of int",
            )
            offset(10)
            path("$.boxed")
            input(inputArr)
        })

        val inputObj = """{"boxed": {"x":"y"}}"""
        checkDecodingException(mode, {
            default.decodeFromString(Box.serializer(String.serializer()), inputObj, mode)
        }, {
            message(
                "Expected beginning of the string, but got {",
                "Expected JsonPrimitive, but had JsonObject as the serialized body of string",
            )
            offset(10)
            path("$.boxed")
            input(inputObj)
        })
    }

    @Test
    fun testJsonTokensAreProperlyReported1() = parametrizedTest { mode ->
        val input = """{"boxed":4}"""
        val serString = serializer<Box<String>>()

        checkDecodingException(mode, {
            default.decodeFromString(serString, input, mode)
        }, {
            message(
                "Expected quotation mark '\"', but had '4' instead",
                "String literal for value of key 'boxed' should be quoted",
            )
            offset(9)
            path("$.boxed")
            input(input)
            if (mode == JsonTestingMode.TREE) hint("isLenient = true")
        })
    }

    @Test
    fun testJsonTokensAreProperlyReported2() = parametrizedTest { mode ->
        val input = """{"boxed":"str"}"""
        val serInt = serializer<Box<Int>>()

        checkDecodingException(mode, {
            default.decodeFromString(serInt, input, mode)
        }, {
            message(
                "Unexpected symbol 's' in numeric literal",
                "Failed to parse literal '\"str\"' as an int value",
            )
            offset(10)
            path("$.boxed")
            input(input)
        })
    }

    @Test
    fun testMissingClosingQuote1() = parametrizedTest { mode ->
        val input = """{"boxed:4}"""
        val serInt = serializer<Box<Int>>()

        checkDecodingException(mode, {
            default.decodeFromString(serInt, input, mode)
        }, {
            // For discussion:
            // Technically, both of these messages are correct despite them being completely different.
            // A `:` instead of `"` is a good guess, but `:`/`}` is a perfectly valid token inside Json string — for example,
            // it can be some kind of path `{"foo:bar:baz":"my:resource:locator:{123}"}` or even URI used as a string key/value.
            // So if the closing quote is missing, there's really no way to correctly tell where the key or value is supposed to end.
            // Although we may try to unify these messages for consistency.
            if (mode in setOf(JsonTestingMode.STREAMING, JsonTestingMode.TREE)) {
                message("Expected quotation mark '\"', but had ':' instead")
                offset(7)
            } else {
                message("Unexpected EOF")
            }
            path("$")
            input(input)
        })
    }

    @Test
    fun testMissingClosingQuote2() = parametrizedTest { mode ->
        val input = """{"boxed":"str}"""
        val serString = serializer<Box<String>>()

        checkDecodingException(mode, {
            default.decodeFromString(serString, input, mode)
        }, {
            if (mode in setOf(JsonTestingMode.STREAMING, JsonTestingMode.TREE)) {
                message("Expected quotation mark '\"', but had '}' instead")
                offset(13)
                if (mode == JsonTestingMode.STREAMING) {
                    path("$.boxed")
                } else {
                    // FIXME? When we are reading Json in 'raw tree' (parseToJsonElement) mode, lexer does not provide path information on syntax errors because there are no descriptors.
                    // It's probably better to cut out path completely or try to build it without descriptors
                    path("$")
                }
            } else {
                path("$.boxed")
                message("Unexpected EOF")
            }
            input(input)
        })
    }

    @Test
    fun testMissingClosingQuote3() = parametrizedTest { mode ->
        val input = """{"boxed:"str"}"""
        val serString = serializer<Box<String>>()

        checkDecodingException(mode, {
            default.decodeFromString(serString, input, mode)
        }, {
            message("Expected colon ':', but had 's' instead")
            offset(9)
            path("$")
            input(input)
        })
    }

    @Test
    fun testUnquoted() = parametrizedTest { mode ->
        val input1 = """{boxed:str}"""
        val input2 = """{"boxed":str}"""
        val ser = serializer<Box<String>>()

        checkDecodingException(mode, {
            default.decodeFromString(ser, input1, mode)
        }, {
            message("""Expected quotation mark '"', but had 'b' instead""")
            offset(1)
            path("$")
            input(input1)
        })

        checkDecodingException(mode, {
            default.decodeFromString(ser, input2, mode)
        }, {
            message(
                """Expected quotation mark '"', but had 's' instead""",
                "String literal for value of key 'boxed' should be quoted",
            )
            offset(9)
            path("$.boxed")
            input(input2)
            if (mode == JsonTestingMode.TREE) hint("isLenient = true")
        })
    }

    @Test
    fun testNullLiteralForNotNull() = parametrizedTest { mode ->
        val input = """{"boxed":null}"""
        val ser = serializer<Box<String>>()
        checkDecodingException(mode, {
            default.decodeFromString(ser, input, mode)
        }, {
            message(
                "Expected string literal but 'null' literal was found",
                "Expected string value for a non-null key 'boxed', got null literal instead",
            )
            offset(9)
            path("$.boxed")
            input(input)
            hint("coerceInputValues = true")
        })
    }

    @Test
    fun testNullLiteralForNotNullNumber() = parametrizedTest { mode ->
        val input = """{"boxed":null}"""
        val ser = serializer<Box<Int>>()
        checkDecodingException(mode, {
            default.decodeFromString(ser, input, mode)
        }, {
            message(
                "Unexpected symbol 'n' in numeric literal",
                "Failed to parse literal 'null' as an int value",
            )
            if (mode != JsonTestingMode.TREE) offset(9)
            path("$.boxed")
            input(input)
        })
    }

    @Test
    fun testEof() = parametrizedTest { mode ->
        val input = """{"boxed":"""
        checkDecodingException(mode, {
            default.decodeFromString<Box<String>>(input, mode)
        }, {
            message(
                "Expected quotation mark '\"', but had 'EOF' instead",
                "Cannot read Json element because of unexpected end of the input",
            )
            if (mode != JsonTestingMode.TREE) path("$.boxed") else path("$") // see comment in testMissingClosingQuote2
            input(input)
        })

    }
}
