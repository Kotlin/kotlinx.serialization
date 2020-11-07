package kotlinx.serialization.properties

import kotlin.test.*

internal class StringEncodeTest {
    private fun test(input: Map<String, String>, expected: String, escapeUnicode: Boolean = false) {
        val result = input.encodeAsString(escapeUnicode = escapeUnicode)
        assertEquals(expected, result, "Map encoded as Properties")
    }

    @Test fun emptyMapIsEncodedAsEmptyString() = test(
        input = emptyMap(),
        expected = ""
    )

    @Test fun keyAndValuesAreSeparatedByEquals() = test(
        input = mapOf("akey" to "avalue"),
        expected = "akey=avalue"
    )

    @Test fun specialCharactersAreEscapedInKeysAndValues() = test(
        input = mapOf("my key\t" to """\my:val="""),
        expected = """my\ key\t=\\my\:val\="""
    )

    @Test fun nonPrintableCharactersAreNotConvertedToUnicodeWhenEscapeIsDisabled() = test(
        input = mapOf("a\u0005" to "\u000fb"),
        expected = "a\u0005=\u000fb",
        escapeUnicode = false
    )

    @Test fun nonPrintableCharactersAreConvertedToUnicodeWhenEscapeIsEnabled() = test(
        input = mapOf("a\u0005" to "\u000fb"),
        expected = """a\u0005=\u000fb""",
        escapeUnicode = true
    )
}