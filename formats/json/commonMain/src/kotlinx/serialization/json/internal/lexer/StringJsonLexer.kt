/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

internal class StringJsonLexer(override val source: String) : AbstractJsonLexer() {

    override fun prefetchOrEof(position: Int): Int = if (position < source.length) position else -1

    override fun consumeNextToken(): Byte {
        val source = source
        while (currentPosition != -1 && currentPosition < source.length) {
            val ch = source[currentPosition++]
            return when (val tc = charToTokenClass(ch)) {
                TC_WHITESPACE -> continue
                else -> tc
            }
        }
        return TC_EOF
    }

    override fun tryConsumeComma(): Boolean {
        val current = skipWhitespaces()
        if (current == source.length || current == -1) return false
        if (source[current] == ',') {
            ++currentPosition
            return true
        }
        return false
    }

    override fun canConsumeValue(): Boolean {
        var current = currentPosition
        if (current == -1) return false
        while (current < source.length) {
            val c = source[current]
            // Inlined skipWhitespaces without field spill and nested loop. Also faster then char2TokenClass
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                ++current
                continue
            }
            currentPosition = current
            return isValidValueStart(c)
        }
        currentPosition = current
        return false
    }

    override fun skipWhitespaces(): Int {
        var current = currentPosition
        if (current == -1) return current
        // Skip whitespaces
        while (current < source.length) {
            val c = source[current]
            // Faster than char2TokenClass actually
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') {
                ++current
            } else {
                break
            }
        }
        currentPosition = current
        return current
    }

    override fun consumeNextToken(expected: Char) {
        if (currentPosition == -1) unexpectedToken(expected)
        val source = source
        while (currentPosition < source.length) {
            val c = source[currentPosition++]
            if (c == ' ' || c == '\n' || c == '\r' || c == '\t') continue
            if (c == expected) return
            unexpectedToken(expected)
        }
        unexpectedToken(expected) // EOF
    }

    override fun consumeKeyString(): String {
        /*
         * For strings we assume that escaped symbols are rather an exception, so firstly
         * we optimistically scan for closing quote via intrinsified and blazing-fast 'indexOf',
         * than do our pessimistic check for backslash and fallback to slow-path if necessary.
         */
        consumeNextToken(STRING)
        val current = currentPosition
        val closingQuote = source.indexOf('"', current)
        if (closingQuote == -1) fail(TC_STRING)
        // Now we _optimistically_ know where the string ends (it might have been an escaped quote)
        for (i in current until closingQuote) {
            // Encountered escape sequence, should fallback to "slow" path and symbolic scanning
            if (source[i] == STRING_ESC) {
                return consumeString(source, currentPosition, i)
            }
        }
        this.currentPosition = closingQuote + 1
        return source.substring(current, closingQuote)
    }

    override fun consumeStringChunked(isLenient: Boolean, consumeChunk: (stringChunk: String) -> Unit) {
        (if (isLenient) consumeStringLenient() else consumeString()).chunked(BATCH_SIZE).forEach(consumeChunk)
    }

    override fun peekLeadingMatchingValue(keyToMatch: String, isLenient: Boolean): String? {
        val positionSnapshot = currentPosition
        try {
            if (consumeNextToken() != TC_BEGIN_OBJ) return null // Malformed JSON, bailout
            val firstKey = peekString(isLenient)
            if (firstKey != keyToMatch) return null
            discardPeeked() // consume firstKey
            if (consumeNextToken() != TC_COLON) return null
            return peekString(isLenient)
        } finally {
            // Restore the position
            currentPosition = positionSnapshot
            discardPeeked()
        }
    }
}

