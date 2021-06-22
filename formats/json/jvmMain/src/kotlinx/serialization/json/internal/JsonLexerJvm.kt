package kotlinx.serialization.json.internal

import java.io.*
import java.nio.CharBuffer
import java.nio.charset.Charset

internal const val BATCH_SIZE = 64 * DEFAULT_BUFFER_SIZE
private const val DEFAULT_THRESHOLD = 1024


/**
 * For some weird reason this hand-rolled implementation is faster than java.nio.CharBuffer.wrap(CharArray)
 */
internal class ArrayAsSequence(private val source: CharArray) : CharSequence {
    override val length: Int = source.size

    override fun get(index: Int): Char = source[index]

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        return String(source, startIndex, endIndex - startIndex)
    }
}

//internal fun ArrayAsSequence(s: CharArray, length: Int = s.size): CharSequence = CharBuffer.wrap(s, 0, length)

internal class JsonReaderLexer(
    private val reader: Reader,
    private var _source: CharArray = CharArray(BATCH_SIZE + DEFAULT_THRESHOLD)
) : JsonLexer(ArrayAsSequence(_source)) {
    private var threshold: Int = DEFAULT_THRESHOLD // chars

    constructor(i: InputStream, charset: Charset = Charsets.UTF_8) : this(i.bufferedReader(charset))

    init {
        preload(0)
    }

    private fun preload(spaceLeft: Int) {
        val buffer = _source
        System.arraycopy(buffer, currentPosition, buffer, 0, spaceLeft)
        var read = spaceLeft
        val sizeTotal = BATCH_SIZE + threshold
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

    override fun ensureHaveChars() {
        val cur = currentPosition
        val oldSize = _source.size
        val spaceLeft = oldSize - cur
        if (spaceLeft > threshold) return
        // warning: current position is not updated during string consumption
        // resizing
        preload(spaceLeft)
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
