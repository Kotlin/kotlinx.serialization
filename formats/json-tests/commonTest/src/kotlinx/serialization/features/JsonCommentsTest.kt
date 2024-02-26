/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.test.*

class JsonCommentsTest: JsonTestBase() {
    val json = Json(default) {
        allowComments = true
        allowTrailingComma = true
    }

    val withLenient = Json(json) {
        isLenient = true
        ignoreUnknownKeys = true
    }

    @Test
    fun testBasic() = parametrizedTest { mode ->
        val inputBlock = """{"data": "b" /*value b*/ }"""
        val inputLine = "{\"data\": \"b\" // value b \n }"
        assertEquals(StringData("b"), json.decodeFromString(inputBlock, mode))
        assertEquals(StringData("b"), json.decodeFromString(inputLine, mode))
    }

    @Serializable
    data class Target(val key: String, val key2: List<Int>, val key3: NestedTarget, val key4: String)

    @Serializable
    data class NestedTarget(val nestedKey: String)

    private fun target(key4: String): Target = Target("value", listOf(1, 2), NestedTarget("foo"), key4)

    @Test
    fun testAllBlocks() = parametrizedTest { mode ->
        val input = """{ /*beginning*/
            /*before key*/ "key" /*after key*/ : /*after colon*/ "value" /*before comma*/,
            "key2": [ /*array1*/ 1, /*array2*/ 2, /*end array*/],
            "key3": { /*nested obj*/ "nestedKey": "foo"} /*after nested*/,
            "key4": "/*comment inside quotes is a part of value*/",
            /*before end*/
        }"""
        assertEquals(target("/*comment inside quotes is a part of value*/"), json.decodeFromString(input, mode))
    }

    @Test
    fun testAllLines() = parametrizedTest { mode ->
        val input = """{ //beginning
            //before key
            "key" // after key
             : // after colon
              "value" //before comma
              ,
            "key2": [ //array1
             1, //array2
              2, //end array
              ],
            "key3": { //nested obj
            "nestedKey": "foo"
            } , //after nested
            "key4": "//comment inside quotes is a part of value",
            //before end
        }"""
        assertEquals(target("//comment inside quotes is a part of value"), json.decodeFromString(input, mode))
    }

    @Test
    fun testMixed() = parametrizedTest { mode ->
        val input = """{ // begin
           "key": "value", // after
            "key2": /* array */ [1, 2],
            "key3": /* //this is a block comment */ { "nestedKey": // /*this is a line comment*/ "bar"
                "foo" },
            "key4": /* nesting block comments /* not supported */ "*/"
        /* end */}"""
        assertEquals(target("*/"), json.decodeFromString(input, mode))
    }

    @Test
    fun testWithLenient() = parametrizedTest { mode ->
        val input = """{ //beginning
            //before key
            key // after key
             : // after colon
              value //before comma
              ,
            key2: [ //array1
             1, //array2
              2, //end array
              ],
            key3: { //nested obj
            nestedKey: "foo"
            } , //after nested
            key4: value//comment_cannot_break_value_apart, 
            key5: //comment without quotes where new token expected is still a comment
            value5,
            //before end
        }"""
        assertEquals(target("value//comment_cannot_break_value_apart"), withLenient.decodeFromString(input, mode))
    }

    @Test
    fun testUnclosedCommentsErrorMsg() = parametrizedTest { mode ->
        val input = """{"data": "x"} // no newline"""
        assertEquals(StringData("x"),  json.decodeFromString<StringData>(input, mode))
        val input2 = """{"data": "x"} /* no endblock"""
        assertFailsWith<SerializationException>("Expected end of the block comment: \"*/\", but had EOF instead at path: \$") {
            json.decodeFromString<StringData>(input2, mode)
        }
    }

    @Test
    fun testVeryLargeComments() = parametrizedTest { mode ->
        // 16 * 1024 is ReaderJsonLexer.BATCH_SIZE
        val strLen = 16 * 1024 * 2 + 42
        val inputLine = """{"data":  //a""" + "a".repeat(strLen) + "\n\"x\"}"
        assertEquals(StringData("x"),  json.decodeFromString<StringData>(inputLine, mode))
        val inputBlock = """{"data":  /*a""" + "a".repeat(strLen) + "*/\"x\"}"
        assertEquals(StringData("x"),  json.decodeFromString<StringData>(inputBlock, mode))
    }

}
