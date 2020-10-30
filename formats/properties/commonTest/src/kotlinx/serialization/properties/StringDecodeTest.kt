package kotlinx.serialization.properties

import kotlin.test.*

internal class StringDecodeTest {
    private fun test(expected: Map<String, String>, input: String) {
        val result = input.decodeAsProperties()
        assertEquals(expected, result, "Result of parsing Properties string")
    }

    @Test fun empty_string_is_converted_to_empty_list() {
        test(emptyMap(), "")
    }

    @Test fun single_string_is_converted_to_key_with_empty_string_value() {
        test(mapOf("someData" to ""),"someData")
    }

    @Test fun arbitrary_number_of_whitespaces_can_separate_key_value_pairs() {
        // values 3 and 4 are separated by \t
        val input = """
            ks1 svI
            ks2     svII
            ks3	svIII
            ks4			svIV
        """.trimIndent()
        val expected = mapOf(
            "ks1" to "svI",
            "ks2" to "svII",
            "ks3" to "svIII",
            "ks4" to "svIV"
        )
        test(expected, input)
    }

    @Test fun equals_sign_with_arbitrary_number_of_whitespaces_can_separate_key_value_pairs() {
        val input = """
            ke1=veI
            ke2 =veII
            ke3= veIII
            ke4 = veIIII
            ke5     =   veV
        """.trimIndent()
        val expected = mapOf(
            "ke1" to "veI",
            "ke2" to "veII",
            "ke3" to "veIII",
            "ke4" to "veIIII",
            "ke5" to "veV",
        )

        test(expected, input)
    }

    @Test fun colon_with_arbitrary_number_of_whitespaces_can_separate_key_value_pairs() {
        val input = """
            kc1:veI
            kc2 :veII
            kc3: veIII
            kc4 : veIIII
            kc5       :    veV
        """.trimIndent()
        val expected = mapOf(
            "kc1" to "veI",
            "kc2" to "veII",
            "kc3" to "veIII",
            "kc4" to "veIIII",
            "kc5" to "veV",
        )

        test(expected, input)
    }

    @Test fun escaped_spaces_do_not_separate_keys_from_values() {
        val input = """some\ escaped\ key some value"""
        val expected = mapOf(
            "some escaped key" to "some value"
        )
        test(expected, input)
    }

    @Test fun escaped_whitespaces_are_correctly_interpreted() {
        val input = """a\nquasi\tmulti\ line\fkey some\ value\nwith\twhitespaces\rinside"""
        val expected = mapOf(
            "a\nquasi\tmulti line\u000ckey" to "some value\nwith\twhitespaces\rinside"
        )
        test(expected, input)
    }

    // This is the explicit behaviour of java.util.Properties::load method
    @Test fun slashes_before_non_escapable_characters_are_dropped() {
        val input = """m\y\ \key=\my \value"""
        val expected = mapOf(
            "my key" to "my value"
        )
        test(expected, input)
    }

    @Test fun unicode_escaped_characters_are_correctly_converted() {
        val input = """\u0061\u004bey=\u0056A\u004C"""
        val expected = mapOf(
            "\u0061\u004bey" to "\u0056A\u004C"
        )
        test(expected, input)
    }

    @Test fun multi_line_values_with_all_line_breaks_sequences_are_supported() {
        val input = "key=multi\\\nline\\\rvalue\\\r\nstring\notherKey=val"
        val expected = mapOf(
            "key" to "multilinevaluestring",
            "otherKey" to "val"
        )
        test(expected, input)
    }

    @Test fun subsequent_separators_become_part_of_the_value() {
        val input = "something =  = somethingElse"
        val expected = mapOf(
            "something" to "= somethingElse"
        )
        test(expected, input)
    }

    @Test fun comment_lines_are_ignored() {
        val input = """
            # some comment
            data=value
            !another comment
            otherdata
            #and another
        """.trimIndent()
        val expected = mapOf(
            "data" to "value",
            "otherdata" to ""
        )
        test(expected, input)
    }

    @Test fun invalid_unicode_format_cause_IllegalArgumentExceptions(){
        assertFailsWith(IllegalArgumentException::class) {
            "\\u12".decodeAsProperties()
        }
        assertFailsWith(IllegalArgumentException::class) {
            "a\\u000Z=v".decodeAsProperties()
        }
        assertFailsWith(IllegalArgumentException::class) {
            "\\u01 val".decodeAsProperties()
        }
    }
}