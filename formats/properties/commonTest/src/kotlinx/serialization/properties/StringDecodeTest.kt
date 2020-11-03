package kotlinx.serialization.properties

import kotlin.test.*

internal class StringDecodeTest {
    private fun test(input: String, expected: Map<String, String>) {
        val result = input.decodeAsProperties()
        assertEquals(expected, result, "Result of parsing Properties string")
    }

    @Test fun empty_string_is_converted_to_empty_list() = test(
        input = "",
        expected = emptyMap()
    )

    @Test fun single_string_is_converted_to_key_with_empty_string_value() = test(
        input = "someData",
        expected = mapOf("someData" to "")
    )

    @Test fun arbitrary_number_of_whitespaces_can_separate_key_value_pairs() = test(
        // values 3 and 4 are separated by \t
        input = """
            ks1 svI
            ks2     svII
            ks3	svIII
            ks4			svIV
        """.trimIndent(),
        expected = mapOf(
            "ks1" to "svI",
            "ks2" to "svII",
            "ks3" to "svIII",
            "ks4" to "svIV"
        )
    )

    @Test fun equals_sign_with_arbitrary_number_of_whitespaces_can_separate_key_value_pairs() = test(
        input = """
            ke1=veI
            ke2 =veII
            ke3= veIII
            ke4 = veIIII
            ke5     =   veV
        """.trimIndent(),
        expected = mapOf(
            "ke1" to "veI",
            "ke2" to "veII",
            "ke3" to "veIII",
            "ke4" to "veIIII",
            "ke5" to "veV",
        )
    )

    @Test fun colon_with_arbitrary_number_of_whitespaces_can_separate_key_value_pairs() = test(
        input = """
            kc1:veI
            kc2 :veII
            kc3: veIII
            kc4 : veIIII
            kc5       :    veV
        """.trimIndent(),
        expected = mapOf(
            "kc1" to "veI",
            "kc2" to "veII",
            "kc3" to "veIII",
            "kc4" to "veIIII",
            "kc5" to "veV",
        )
    )

    @Test fun escaped_spaces_do_not_separate_keys_from_values() = test(
        input = """some\ escaped\ key some value""",
        expected = mapOf(
            "some escaped key" to "some value"
        )
    )

    @Test fun escaped_whitespaces_are_correctly_interpreted() = test(
        input = """a\nquasi\tmulti\ line\fkey some\ value\nwith\twhitespaces\rinside""",
        expected = mapOf(
            "a\nquasi\tmulti line\u000ckey" to "some value\nwith\twhitespaces\rinside"
        )
    )

    // This is the explicit behaviour of java.util.Properties::load method
    @Test fun slashes_before_non_escapable_characters_are_dropped() = test(
        input = """m\y\ \key=\my \value""",
        expected = mapOf(
            "my key" to "my value"
        )
    )

    @Test fun unicode_escaped_characters_are_correctly_converted() = test(
        input = """\u0061\u004bey=\u0056A\u004C""",
        expected = mapOf(
            "\u0061\u004bey" to "\u0056A\u004C"
        )
    )

    @Test fun multi_line_values_with_all_line_breaks_sequences_are_supported() = test(
        input = "key=multi\\\nline\\\rvalue\\\r\nstring\notherKey=val",
        expected = mapOf(
            "key" to "multilinevaluestring",
            "otherKey" to "val"
        )
    )

    @Test fun subsequent_separators_become_part_of_the_value() = test(
        input = "something =  = somethingElse",
        expected = mapOf(
            "something" to "= somethingElse"
        )
    )

    @Test fun comment_lines_are_ignored() = test(
        input = """
            # some comment
            data=value
            !another comment
            otherdata
            #and another
        """.trimIndent(),
        expected = mapOf(
            "data" to "value",
            "otherdata" to ""
        )
    )

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