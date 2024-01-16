/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal.lexer

import kotlinx.serialization.json.internal.*

internal class Json5Lexer(source: String): StringJsonLexer(source, allowLeadingPlusSign = true) {

    override fun startString(): Char {
        val source = source
        while (currentPosition != -1 && currentPosition < source.length) {
            val c = source[currentPosition++]
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') continue
            if (c != STRING && c != STRING_SQUOTE) {
                failAndCheckForUnexpectedNullLiteral("start of the string: \" or '")
            }
            return c
        }
        currentPosition = -1 // for correct EOF reporting
        failAndCheckForUnexpectedNullLiteral("start of the string: \" or '") // EOF
    }

    override fun consumeKeyString(): String {
        return consumeUnquotedString()
    }

    override fun peekString(isLenient: Boolean, isKey: Boolean): String? {
        skipWhitespaces()
        val cur = source[currentPosition]
        val string = if (isKey) {
            if (cur != STRING && cur != STRING_SQUOTE && charToTokenClass(cur) != TC_OTHER) return null
            consumeUnquotedString()
        } else {
            if (cur != STRING && cur != STRING_SQUOTE) return null
            consumeQuotedStringBase()
        }
        peekedString = string
        return string
    }

    // Should be a basic building block for Json5
    // (while the basic building block for Json is consumeKeyString)
    // because we can express unquoted strings via parser for quoted.
    override fun consumeValueString(): String {
        if (peekedString != null) {
            return takePeeked()
        }

        return consumeQuotedStringBase()
    }

    private fun isValidUnquotedValue(char: Char): Boolean {
        val token = charToTokenClass(char)
        return token == TC_OTHER || token == TC_STRING_ESC // unicode escapes are allowed inside unquoted Json5 keys.
    }

    override fun consumeUnquotedString(): String {
        if (peekedString != null) {
            return takePeeked()
        }
        var current = skipWhitespaces()
        if (current >= source.length || current == -1) fail("EOF", current)
        var char = source[current]
        if (char == STRING || char == STRING_SQUOTE) {
            return consumeValueString()
        }
        if (!isValidUnquotedValue(char)) {
            fail("Expected beginning of the string, but got ${source[current]}")
        }
        // Todo: copypasta of consumeStringRest and consumeUnquotedString
        // However, `while` condition there is completely different, so some performance evaluation is required.
        var lastPosition = current
        var usedAppend = false
        while (isValidUnquotedValue(char)) {
            if (char == STRING_ESC) {
                usedAppend = true
                current = prefetchOrEof(appendEscape(lastPosition, current))
                if (current == -1)
                    fail("Unexpected EOF", current)
                lastPosition = current
            } else if (++current >= source.length) {
                usedAppend = true
                // end of chunk
                appendRange(lastPosition, current)
                current = prefetchOrEof(current)
                if (current == -1) {
                    // to handle plain lenient strings, such as top-level
                    currentPosition = current
                    return decodedString(0, 0)
                }
            }
            char = source[current]
        }

        val string = if (!usedAppend) {
            // there was no escaped chars
            substring(lastPosition, current)
        } else {
            // some escaped chars were there
            decodedString(lastPosition, current)
        }
        this.currentPosition = current
        return string
    }
}
