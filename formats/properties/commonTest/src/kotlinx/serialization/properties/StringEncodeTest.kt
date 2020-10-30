package kotlinx.serialization.properties

import kotlin.test.*

internal class StringEncodeTest {
    private fun test(input: Map<String, String>, expected: String, escapeUnicode: Boolean = false) {
        val result = input.encodeAsString(escapeUnicode).trimEnd('\n')
        assertEquals(expected, result, "Map encoded as Properties")
    }

    @Test fun empty_map_is_encoded_as_empty_string() {
        test(emptyMap(), expected = "")
    }

    @Test fun key_and_values_are_separated_by_equals() {
        test(
            input = mapOf("akey" to "avalue"),
            expected = "akey=avalue"
        )
    }

    @Test fun special_characters_are_escaped_in_keys_and_values() {
        test(
            input = mapOf("my key\t" to """\my:val="""),
            expected = """my\ key\t=\\my\:val\="""
        )
    }

    @Test fun non_printable_characters_are_not_converted_to_unicode_when_escape_is_disabled() {
        test(
            input = mapOf("a\u0005" to "\u000fb"),
            expected = "a\u0005=\u000fb",
            escapeUnicode = false
        )
    }

    @Test fun non_printable_characters_are_converted_to_unicode_when_escape_is_enabled() {
        test(
            input = mapOf("a\u0005" to "\u000fb"),
            expected = """a\u0005=\u000fb""",
            escapeUnicode = true
        )
    }
}