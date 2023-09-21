package kotlinx.serialization.json.internal

/**
 * Optimized version of StringBuilder that is specific to JSON-encoding.
 *
 * ## Implementation note
 *
 * In order to encode a single string, it should be processed symbol-per-symbol,
 * in order to detect and escape unicode symbols.
 *
 * Doing naively, it drastically slows down strings processing due to factors:
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
internal actual class JsonToStringWriter : InternalJsonWriter {
    private var array: CharArray = CharArrayPool.take()
    private var size = 0

    actual override fun writeLong(value: Long) {
        // Can be hand-rolled, but requires a lot of code and corner-cases handling
        write(value.toString())
    }

    actual override fun writeChar(char: Char) {
        ensureAdditionalCapacity(1)
        array[size++] = char
    }

    actual override fun write(text: String) {
        val length = text.length
        if (length == 0) return
        ensureAdditionalCapacity(length)
        text.toCharArray(array, size, 0, text.length)
        size += length
    }

    actual override fun writeQuoted(text: String) {
        ensureAdditionalCapacity(text.length + 2)
        val arr = array
        var sz = size
        arr[sz++] = '"'
        val length = text.length
        text.toCharArray(arr, sz, 0, length)
        for (i in sz until sz + length) {
            val ch = arr[i].code
            // Do we have unescaped symbols?
            if (ch < ESCAPE_MARKERS.size && ESCAPE_MARKERS[ch] != 0.toByte()) {
                // Go to slow path
                return appendStringSlowPath(i - sz, i, text)
            }
        }
        // Update the state
        // Capacity is not ensured because we didn't hit the slow path and thus guessed it properly in the beginning
        sz += length
        arr[sz++] = '"'
        size = sz
    }

    private fun appendStringSlowPath(firstEscapedChar: Int, currentSize: Int, string: String) {
        var sz = currentSize
        for (i in firstEscapedChar until string.length) {
            /*
             * We ar already on slow path and haven't guessed the capacity properly.
             * Reserve +2 for backslash-escaped symbols on each iteration
             */
            sz = ensureTotalCapacity(sz, 2)
            val ch = string[i].code
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
                        sz = ensureTotalCapacity(sz, escapedString.length)
                        escapedString.toCharArray(array, sz, 0, escapedString.length)
                        sz += escapedString.length
                        size = sz // Update size so the next resize will take it into account
                    }
                    else -> {
                        array[sz] = '\\'
                        array[sz + 1] = marker.toInt().toChar()
                        sz += 2
                        size = sz // Update size so the next resize will take it into account
                    }
                }
            } else {
                array[sz++] = ch.toChar()
            }
        }
        sz = ensureTotalCapacity(sz, 1)
        array[sz++] = '"'
        size = sz
    }

    actual override fun release() {
        CharArrayPool.release(array)
    }

    actual override fun toString(): String {
        return String(array, 0, size)
    }

    private fun ensureAdditionalCapacity(expected: Int) {
        ensureTotalCapacity(size, expected)
    }

    // Old size is passed and returned separately to avoid excessive [size] field read
    private fun ensureTotalCapacity(oldSize: Int, additional: Int): Int {
        val newSize = oldSize + additional
        if (array.size <= newSize) {
            array = array.copyOf(newSize.coerceAtLeast(oldSize * 2))
        }
        return oldSize
    }
}
