package kotlinx.serialization.base64.impl


internal fun decode(encoded: String): ByteArray {
    if (encoded.isBlank()) return ByteArray(0)
    val result = ByteArray(encoded.length)
    var resultSize = 0

    val backDictionary = backDictionary
    var buffer = 0
    var buffered = 0
    var index = 0

    while (index < encoded.length) {
        val ch = encoded[index++]
        if (ch <= ' ') continue
        if (ch == '=') {
            index--
            break
        }
        val value = backDictionary.getOrElse(ch.code) { -1 }
        if (value == -1) error("Unexpected character $ch (${ch.code})) in $encoded")

        buffer = buffer shl 6 or value
        buffered++

        if (buffered == 4) {
            result[resultSize] = (buffer shr 16).toByte()
            result[resultSize + 1] = (buffer shr 8 and 0xff).toByte()
            result[resultSize + 2] = (buffer and 0xff).toByte()
            resultSize += 3
            buffered = 0
            buffer = 0
        }
    }

    var padding = 0
    while (index < encoded.length) {
        val ch = encoded[index++]
        if (ch <= ' ') continue
        check(ch == '=')
        padding++
        buffer = buffer shl 6
        buffered++
    }

    if (buffered == 4) {
        result[resultSize] = (buffer shr 16).toByte()
        result[resultSize + 1] = (buffer shr 8 and 0xff).toByte()
        result[resultSize + 2] = (buffer and 0xff).toByte()
        resultSize += 3

        resultSize -= padding
        buffered = 0
    }

    check(buffered == 0) {
        "buffered: $buffered"
    }

    return when {
        resultSize < result.size -> result.copyOf(resultSize)
        else -> result
    }
}
