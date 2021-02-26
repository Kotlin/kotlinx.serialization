package kotlinx.serialization.json.internal

/*
 * Optimized version of StringBuilder that is specific to JSON-encoding
 */
internal actual class JsonStringBuilder {
    private var array = CharArrayPool.take()
    private var size = 0

    actual fun append(value: Long) {
        ensureAdditionalCapacity(20) // Long length
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
            if (ch < ESCAPE_MARKERS.size && ESCAPE_MARKERS[ch] != 0.toChar()) {
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
            var marker: Char = 0.toChar()
            if (ch < ESCAPE_MARKERS.size && ESCAPE_MARKERS[ch].also { marker = it } != 0.toChar()) {
                if (marker != 0.toChar()) {
                    array[sz] = '\\'
                    array[sz + 1] = marker
                    sz += 2
                } else {
                    val escapedString = ESCAPE_STRINGS[ch]!!
                    ensureTotalCapacity(sz + escapedString.length)
                    escapedString.toCharArray(array, sz, 0, escapedString.length)
                    sz += escapedString.length
                }
            } else {
                array[sz++] = string[i]
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
