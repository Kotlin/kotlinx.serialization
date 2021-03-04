package kotlinx.serialization.json.internal

/**
 * Optimized version of StringBuilder that is specific to JSON-encoding.
 *
 * ## Implementation note
 *
 * In order to encode a single string, it should be processed symbol-per-symbol,
 * in order to detect and escape unicode symbols.
 *
 * Doing naively, it drastically slows down strings processing due to to factors:
 * * Byte-by-byte copying that does not leverage optimized array copying
 * * A lot of range and flags checks due to Java's compact strings
 *
 * The following technique is used:
 * 1) Instead of storing intermediate result in `StringBuilder`, we store it in
 *    `CharArray` directly, skipping compact strings checks in `StringBuilder`
 * 2) Instead of copying symbols one-by-one, we optimistically copy it in batch using
 *    optimized and intrinsified `string.toCharArray(destination)`.
 *    It copies the content by up-to 8 times faster.
 *    Then we iterate over the char-array and execute single check over
 *    each character that is easily unrolled and vectorized by the inliner.
 *    If escape character is found, we fallback to per-symbol processing.
 *
 * 3) We pool char arrays in order to save excess resizes, allocations
 *    and nulls-out of arrays.
 */
internal actual class JsonStringBuilder {
    private var array = CharArrayPool.take()
    private var size = 0

    actual fun append(value: Long) {
        // Can be hand-rolled, but requires a lot of code and corner-cases handling
        append(value.toString())
    }

    actual fun append(ch: Char) {
        ensureAdditionalCapacity(1)
        array[size++] = ch
    }

    actual fun append(string: String) {
        val length = string.length
        ensureAdditionalCapacity(length)
        string.toCharArray(array, size, 0, string.length)
        size += length
    }

    actual fun appendQuoted(string: String) {
        ensureAdditionalCapacity(string.length + 2)
        val arr = array
        var sz = size
        arr[sz++] = '"'
        val length = string.length
        string.toCharArray(arr, sz, 0, length)
        for (i in sz until sz + length) {
            val ch = arr[i].toInt()
            // Do we have unescaped symbols?
            if (ch < ESCAPE_MARKERS.size && ESCAPE_MARKERS[ch] != 0.toByte()) {
                // Go to slow path
                return appendStringSlowPath(i - sz, i, string)
            }
        }
        // Update the state
        sz += length
        arr[sz++] = '"'
        size = sz
    }

    private fun appendStringSlowPath(firstEscapedChar: Int, currentSize: Int, string: String) {
        var sz = currentSize
        for (i in firstEscapedChar until string.length) {
            val ch = string[i].toInt()
            // Do we have unescaped symbols?
            if (ch < ESCAPE_MARKERS.size) {
                /*
                * Escape markers are populated for backslash-escaped symbols.
                * E.g. ESCAPE_MARKERS['\b'] == 'b'.toByte()
                * Everything else is populated with either zeros (no escapes)
                * or ones (unicode escape)
                */
                when (val marker = ESCAPE_MARKERS[ch]) {
                    0.toByte() -> {
                        array[sz++] = ch.toChar()
                    }
                    1.toByte() -> {
                        val escapedString = ESCAPE_STRINGS[ch]!!
                        ensureTotalCapacity(sz + escapedString.length)
                        escapedString.toCharArray(array, sz, 0, escapedString.length)
                        sz += escapedString.length
                    }
                    else -> {
                        array[sz] = '\\'
                        array[sz + 1] = marker.toChar()
                        sz += 2
                    }
                }
            } else {
                array[sz++] = ch.toChar()
            }
        }
        array[sz++] = '"'
        size = sz
    }

    actual override fun toString(): String {
        return String(array, 0, size)
    }

    private fun ensureAdditionalCapacity(expected: Int) {
        ensureTotalCapacity(size + expected)
    }

    private fun ensureTotalCapacity(newSize: Int) {
        if (array.size <= newSize) {
            array = array.copyOf(newSize.coerceAtLeast(size * 2))
        }
    }

    actual fun release() {
        CharArrayPool.release(array)
    }
}
