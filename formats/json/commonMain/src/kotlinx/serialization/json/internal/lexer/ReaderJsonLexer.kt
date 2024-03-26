/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.jvm.*
import kotlin.math.*

internal const val BATCH_SIZE: Int = 16 * 1024
private const val DEFAULT_THRESHOLD = 128

/**
 * For some reason this hand-rolled implementation is faster than
 * fun ArrayAsSequence(s: CharArray): CharSequence = java.nio.CharBuffer.wrap(s, 0, length)
 */
internal class ArrayAsSequence(internal val buffer: CharArray) : CharSequence {
    override var length: Int = buffer.size

    override fun get(index: Int): Char = buffer[index]

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        return buffer.concatToString(startIndex, minOf(endIndex, length))
    }

    fun substring(startIndex: Int, endIndex: Int): String {
        return buffer.concatToString(startIndex, minOf(endIndex, length))
    }

    fun trim(newSize: Int) {
        length = minOf(buffer.size, newSize)
    }

    // source.toString() is used in JsonDecodingException
    override fun toString(): String = substring(0, length)
}

@OptIn(ExperimentalSerializationApi::class)
internal fun ReaderJsonLexer(json: Json, reader: InternalJsonReader, buffer: CharArray = CharArrayPoolBatchSize.take()) =
    if (!json.configuration.allowComments) ReaderJsonLexer(reader, buffer) else ReaderJsonLexerWithComments(reader, buffer)

internal open class ReaderJsonLexer(
    val reader: InternalJsonReader,
    val buffer: CharArray = CharArrayPoolBatchSize.take()
) : AbstractJsonLexer() {

    @JvmField
    protected var threshold: Int = DEFAULT_THRESHOLD // chars

    override val source: ArrayAsSequence = ArrayAsSequence(buffer)

    init {
        preload(0)
    }

    override fun canConsumeValue(): Boolean {
        ensureHaveChars()
        var current = currentPosition
        while (true) {
            current = prefetchOrEof(current)
            if (current == -1) break // could be inline function but KT-1436
            val c = source[current]
            // Inlined skipWhitespaces without field spill and nested loop. Also faster then char2TokenClass
            if (c.isWs()) {
                ++current
                continue
            }
            currentPosition = current
            return isValidValueStart(c)
        }
        currentPosition = current
        return false
    }

    private fun preload(unprocessedCount: Int) {
        val buffer = source.buffer
        if (unprocessedCount != 0) {
            buffer.copyInto(buffer, 0, currentPosition, currentPosition + unprocessedCount)
        }
        var filledCount = unprocessedCount
        val sizeTotal = source.length
        while (filledCount != sizeTotal) {
            val actual = reader.read(buffer, filledCount, sizeTotal - filledCount)
            if (actual == -1) {
                // EOF, resizing the array so it matches input size
                source.trim(filledCount)
                threshold = -1
                break
            }
            filledCount += actual
        }
        currentPosition = 0
    }

    override fun prefetchOrEof(position: Int): Int {
        if (position < source.length) return position
        currentPosition = position
        ensureHaveChars()
        if (currentPosition != 0 || source.isEmpty()) return -1 // if something was loaded, then it would be zero.
        return 0
    }

    override fun consumeNextToken(): Byte {
        ensureHaveChars()
        val source = source
        var cpos = currentPosition
        while (true) {
            cpos = prefetchOrEof(cpos)
            if (cpos == -1) break
            val ch = source[cpos++]
            return when (val tc = charToTokenClass(ch)) {
                TC_WHITESPACE -> continue
                else -> {
                    currentPosition = cpos
                    tc
                }
            }
        }
        currentPosition = cpos
        return TC_EOF
    }

    override fun consumeNextToken(expected: Char) {
        ensureHaveChars()
        val source = source
        var cpos = currentPosition
        while (true) {
            cpos = prefetchOrEof(cpos)
            if (cpos == -1) break // could be inline function but KT-1436
            val c = source[cpos++]
            if (c.isWs()) continue
            currentPosition = cpos
            if (c == expected) return
            unexpectedToken(expected)
        }
        currentPosition = cpos
        unexpectedToken(expected) // EOF
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
            } else {
                break
            }
        }
        currentPosition = current
        return current
    }

    override fun ensureHaveChars() {
        val cur = currentPosition
        val oldSize = source.length
        val spaceLeft = oldSize - cur
        if (spaceLeft > threshold) return
        // warning: current position is not updated during string consumption
        // resizing
        preload(spaceLeft)
    }

    override fun consumeKeyString(): String {
        /*
         * For strings we assume that escaped symbols are rather an exception, so firstly
         * we optimistically scan for closing quote via intrinsified and blazing-fast 'indexOf',
         * than do our pessimistic check for backslash and fallback to slow-path if necessary.
         */
        consumeNextToken(STRING)
        var current = currentPosition
        val closingQuote = indexOf('"', current)
        if (closingQuote == -1) {
            current = prefetchOrEof(current)
            if (current == -1) fail(TC_STRING)
            // it's also possible just to resize buffer,
            // instead of falling back to slow path,
            // not sure what is better
            else return consumeString(source, currentPosition, current)
        }
        // Now we _optimistically_ know where the string ends (it might have been an escaped quote)
        for (i in current until closingQuote) {
            // Encountered escape sequence, should fallback to "slow" path and symmbolic scanning
            if (source[i] == STRING_ESC) {
                return consumeString(source, currentPosition, i)
            }
        }
        this.currentPosition = closingQuote + 1
        return substring(current, closingQuote)
    }

    override fun indexOf(char: Char, startPos: Int): Int {
        val src = source
        for (i in startPos until src.length) {
            if (src[i] == char) return i
        }
        return -1
    }

    override fun substring(startPos: Int, endPos: Int): String {
        return source.substring(startPos, endPos)
    }

    override fun appendRange(fromIndex: Int, toIndex: Int) {
        escapedString.appendRange(source.buffer, fromIndex, toIndex)
    }

    // Can be carefully implemented but postponed for now
    override fun peekLeadingMatchingValue(keyToMatch: String, isLenient: Boolean): String? = null

    fun release() {
        CharArrayPoolBatchSize.release(buffer)
    }
}
