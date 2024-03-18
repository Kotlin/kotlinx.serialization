/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

/*
 Implementations of these two classes are nearly identical. However, there are several reasons why it can't be unified
 and merged into one lexer:

 1. Making skipWhitespaces() a separate method slows deserialization down for about 10-15%. It is faster to have
 `if (c == ' ' || c == '\n' || c == '\r' || c == '\t')` handling copy-pasted instead of separate method; however, copy-pasting comment
 handling will make the code too big and hardly possible to maintain. Therefore, implementation without comment support should not be delegated to skipWhitespaces().

 2. We assume that most users do not need comment support and therefore the only implementation of lexer will be StringJsonLexer.
 JIT therefore will be able to devirtualize and inline calls well; however, if there is more than one class loaded, it will
 be harder to predict if performance is stable. Consequently, StringJsonLexer should inherit AbstractJsonLexer directly and we can't move
 generalized implementation to some CommentJsonLexer in between.
 */

internal class StringJsonLexerWithComments(source: String): StringJsonLexer(source) {
    override fun consumeNextToken(): Byte {
        val source = source
        val cpos = skipWhitespaces()
        if (cpos >= source.length || cpos == -1) return TC_EOF
        currentPosition = cpos + 1
        return charToTokenClass(source[cpos])
    }

    override fun canConsumeValue(): Boolean {
        val current = skipWhitespaces()
        if (current >= source.length || current == -1) return false
        return isValidValueStart(source[current])
    }

    override fun consumeNextToken(expected: Char) {
        val source = source
        val current = skipWhitespaces()
        if (current >= source.length || current == -1) {
            currentPosition = -1 // for correct EOF reporting
            unexpectedToken(expected) // EOF
        }
        val c = source[current]
        currentPosition = current + 1
        if (c == expected) return
        else unexpectedToken(expected)
    }

    override fun peekNextToken(): Byte {
        val source = source
        val cpos = skipWhitespaces()
        // skipWhitespaces() calls prefetch() on every iteration, so we can be sure that there's at least THRESHOLD-1 chars in buf when it returns.
        if (cpos >= source.length || cpos == -1) return TC_EOF
        currentPosition = cpos // only difference with consumeNextToken(), actually
        return charToTokenClass(source[cpos])
    }

    override fun skipWhitespaces(): Int {
        var current = currentPosition
        if (current == -1) return current
        val source = source
        // Skip whitespaces
        while (current < source.length) {
            val c = source[current]
            // Faster than char2TokenClass actually
            if (c.isWs()) {
                ++current
                continue
            }
            if (c == '/' && current + 1 < source.length) { // potential comment start
                when(source[current + 1]) {
                    '/' -> {
                        current = source.indexOf('\n', current + 2)
                        if (current == -1) current = source.length else current++ // skip char itself
                        continue
                    }
                    '*' -> {
                        current = source.indexOf("*/", current + 2)
                        if (current == -1) {
                            currentPosition = source.length
                            fail("Expected end of the block comment: \"*/\", but had EOF instead")
                        } else {
                            current += 2 // skip */ chars
                        }
                        continue
                    }
                }
            }
            break
        }
        currentPosition = current
        return current
    }
}

internal class ReaderJsonLexerWithComments(reader: InternalJsonReader, buffer: CharArray): ReaderJsonLexer(reader, buffer) {
    override fun consumeNextToken(expected: Char) {
        ensureHaveChars()
        val source = source
        val current = skipWhitespaces()
        // skipWhitespaces() calls prefetch() on every iteration, so we can be sure that there's at least THRESHOLD-1 chars in buf when it returns.
        if (current >= source.length || current == -1) {
            currentPosition = -1 // for correct EOF reporting
            unexpectedToken(expected) // EOF
        }
        val c = source[current]
        currentPosition = current + 1
        if (c == expected) return
        else unexpectedToken(expected)
    }

    override fun canConsumeValue(): Boolean {
        ensureHaveChars()
        val current = skipWhitespaces()
        // skipWhitespaces() calls prefetch() on every iteration, so we can be sure that there's at least THRESHOLD-1 chars in buf when it returns.
        if (current >= source.length || current == -1) return false
        return isValidValueStart(source[current])
    }

    override fun consumeNextToken(): Byte {
        ensureHaveChars()
        val source = source
        val cpos = skipWhitespaces()
        if (cpos >= source.length || cpos == -1) return TC_EOF
        currentPosition = cpos + 1
        return charToTokenClass(source[cpos])
    }

    override fun peekNextToken(): Byte {
        ensureHaveChars()
        val source = source
        val cpos = skipWhitespaces()
        // skipWhitespaces() calls prefetch() on every iteration, so we can be sure that there's at least THRESHOLD-1 chars in buf when it returns.
        if (cpos >= source.length || cpos == -1) return TC_EOF
        currentPosition = cpos // only difference with consumeNextToken(), actually
        return charToTokenClass(source[cpos])
    }

    private fun handleComment(position: Int): Pair<Int, Boolean> {
        var current = position
        var startIndex = current + 2
        when (source[current + 1]) {
            '/' -> {
                while(current != -1) {
                    current = source.indexOf('\n', startIndex)
                    if (current == -1) {
                        current = prefetchOrEof(source.length)
                        startIndex = current
                    } else {
                        return current + 1 to true
                    }
                }
                // reached end of stream.
                return -1 to true
            }

            '*' -> {
                var rareCaseHit = false
                while (current != -1) {
                    current = source.indexOf("*/", startIndex)
                    if (current != -1) {
                        return current + 2 to true
                    } else if (source[source.length - 1] != '*') {
                        current = prefetchOrEof(source.length)
                        startIndex = current
                    } else {
                        // Rare case: */ got split by batch boundary (see JsonCommentsTest.testCommentsOnThresholdEdge)
                        // In this case, we should manually force next batch loading with 1 char left in buffer
                        current = prefetchWithinThreshold(source.length - 1)
                        // However, we also can stuck in a situation where comment is unclosed
                        // and * without / is a last char in the buffer. So to avoid checking it in infinite cycle,
                        // there's an escape hatch:
                        if (rareCaseHit) {
                            break
                        }
                        rareCaseHit = true
                        startIndex = current
                    }
                }
                // reached end of stream.
                currentPosition = source.length
                fail("Expected end of the block comment: \"*/\", but had EOF instead")
            }
        }
        return current to false
    }

    private fun prefetchWithinThreshold(position: Int): Int {
        if (source.length - position > threshold) return position
        currentPosition = position
        ensureHaveChars()
        if (currentPosition != 0 || source.isEmpty()) return -1 // if something was loaded, then it would be zero.
        return 0
    }

    override fun skipWhitespaces(): Int {
        var current = currentPosition
        // Skip whitespaces
        while (true) {
            current = prefetchOrEof(current)
            if (current == -1) break
            val c = source[current]
            // Faster than char2TokenClass actually
            if (c.isWs()) {
                ++current
                continue
            }
            if (c == '/' && current + 1 < source.length) { // potential comment start
                val (new, cont) = handleComment(current)
                current = new
                if (cont) continue
            }
            break
        }
        currentPosition = current
        return current
    }
}
