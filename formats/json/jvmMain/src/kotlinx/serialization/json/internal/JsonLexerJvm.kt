package kotlinx.serialization.json.internal

import java.io.*
import java.nio.charset.Charset

internal const val BATCH_SIZE = 16 * 1024
private const val DEFAULT_THRESHOLD = 128


// This size of buffered reader is very important here, because utf-8 decoding is slow.
// Jackson and Moshi are faster because they have specialized UTF-8 parser directly over InputStream
internal const val READER_BUF_SIZE = 16 * BATCH_SIZE


/**
 * For some reason this hand-rolled implementation is faster than
 * fun ArrayAsSequence(s: CharArray): CharSequence = java.nio.CharBuffer.wrap(s, 0, length)
 */
private class ArrayAsSequence(private val source: CharArray) : CharSequence {
    override val length: Int = source.size

    override fun get(index: Int): Char = source[index]

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        return String(source, startIndex, endIndex - startIndex)
    }
}

internal class ReaderJsonLexer(
    private val reader: Reader,
    private var _source: CharArray = CharArray(BATCH_SIZE)
) : AbstractJsonLexer() {
    private var threshold: Int = DEFAULT_THRESHOLD // chars

    constructor(i: InputStream, charset: Charset) : this(i.reader(charset).buffered(READER_BUF_SIZE))

    override var source: CharSequence = ArrayAsSequence(_source)

    init {
        preload(0)
    }

    override fun tryConsumeComma(): Boolean {
        val current = skipWhitespaces()
        if (current >= source.length || current == -1) return false
        if (source[current] == ',') {
            ++currentPosition
            return true
        }
        return false
    }

    override fun canConsumeValue(): Boolean {
        ensureHaveChars()
        var current = currentPosition
        while (true) {
            current = definitelyNotEof(current)
            if (current == -1) break // could be inline function but KT-1436
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

    private fun preload(spaceLeft: Int) {
        val buffer = _source
        System.arraycopy(buffer, currentPosition, buffer, 0, spaceLeft)
        var read = spaceLeft
        val sizeTotal = _source.size
        while (read != sizeTotal) {
            val actual = reader.read(buffer, read, sizeTotal - read)
            if (actual == -1) {
                // EOF, resizing the array so it matches input size
                // Can also be done by extracting source.length to a separate var
                _source = _source.copyOf(read)
                source = ArrayAsSequence(_source)
                threshold = -1
                break
            }
            read += actual
        }
        currentPosition = 0
    }

    override fun definitelyNotEof(position: Int): Int {
        if (position < source.length) return position
        currentPosition = position
        ensureHaveChars()
        if (currentPosition != 0) return -1 // if something was loaded, then it would be zero.
        return 0
    }

    override fun consumeNextToken(): Byte {
        ensureHaveChars()
        val source = source
        var cpos = currentPosition
        while (true) {
            cpos = definitelyNotEof(cpos)
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

    override fun ensureHaveChars() {
        val cur = currentPosition
        val oldSize = _source.size
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
            current = definitelyNotEof(current)
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
        val src = _source
        for (i in startPos until src.size) {
            if (src[i] == char) return i
        }
        return -1
    }

    override fun substring(startPos: Int, endPos: Int): String {
        return String(_source, startPos, endPos - startPos)
    }

    override fun appendRange(fromIndex: Int, toIndex: Int) {
        escapedString.append(_source, fromIndex, toIndex - fromIndex)
    }
}
