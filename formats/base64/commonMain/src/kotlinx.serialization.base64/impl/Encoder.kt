package kotlinx.serialization.base64.impl

internal fun encode(array: ByteArray): String = buildString(capacity = (array.size / 3) * 4 + 1) {
    var index = 0

    while (index < array.size) {
        if (index + 3 > array.size) break

        val buffer = array[index].toInt() and 0xff shl 16 or
                (array[index + 1].toInt() and 0xff shl 8) or
                (array[index + 2].toInt() and 0xff shl 0)

        append(dictionary[buffer shr 18])
        append(dictionary[buffer shr 12 and 0x3f])
        append(dictionary[buffer shr 6 and 0x3f])
        append(dictionary[buffer and 0x3f])

        index += 3
    }

    if (index < array.size) {
        var buffer = 0
        while (index < array.size) {
            buffer = buffer shl 8 or (array[index].toInt() and 0xff)
            index++
        }
        val padding = 3 - (index % 3)
        buffer = buffer shl (padding * 8)

        append(dictionary[buffer shr 18])
        append(dictionary[buffer shr 12 and 0x3f])

        val a = dictionary[buffer shr 6 and 0x3f]
        val b = dictionary[buffer and 0x3f]

        when (padding) {
            0 -> {
                append(a)
                append(b)
            }
            1 -> {
                append(a)
                append('=')
            }
            2 -> {
                append("==")
            }
        }
    }
}
