/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal.lexer

import kotlinx.serialization.json.internal.*

internal class Json5Lexer(source: String): StringJsonLexer(source, json5 = true) {

    override fun startString(): Char {
        val source = source
        var current = currentPosition
        while (current != -1 && current < source.length) {
            val c = source[current++]
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') continue
            currentPosition = current
            if (c != STRING && c != STRING_SQUOTE) {
                failAndCheckForUnexpectedNullLiteral("start of the string: \" or '")
            }
            return c
        }
        currentPosition = -1 // for correct EOF reporting
        failAndCheckForUnexpectedNullLiteral("start of the string: \" or '")
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
            consumeQuotedString()
        }
        peekedString = string
        return string
    }

    override fun consumeValueString(): String {
        if (peekedString != null) {
            return takePeeked()
        }

        return consumeQuotedString()
    }

    override fun isQuotedStart(char: Char): Boolean {
        return char == STRING || char == STRING_SQUOTE
    }
}
