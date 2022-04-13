package kotlinx.serialization.json.internal

import okio.*

internal class JsonToOkioStreamWriter(private val target: BufferedSink) : JsonWriter {
    override fun writeLong(value: Long) {
        write(value.toString())
    }

    override fun writeChar(char: Char) {
        target.writeUtf8CodePoint(char.code)
    }

    override fun write(text: String) {
        target.writeUtf8(text)
    }

    override fun writeQuoted(text: String) {
        target.writeUtf8CodePoint('"'.code)
        var lastPos = 0
        for (i in text.indices) {
            val c = text[i].code
            if (c < ESCAPE_STRINGS.size && ESCAPE_STRINGS[c] != null) {
                target.writeUtf8(text, lastPos, i) // flush prev
                target.writeUtf8(ESCAPE_STRINGS[c]!!)
                lastPos = i + 1
            }
        }

        if (lastPos != 0) target.writeUtf8(text, lastPos, text.length)
        else target.writeUtf8(text)
        target.writeUtf8CodePoint('"'.code)
    }

    override fun release() {
        target.flush()
    }
}

internal class OkioSerialReader(private val source: BufferedSource): SerialReader {
    override fun read(buffer: CharArray, bufferOffset: Int, count: Int): Int {
        var i = 0
        while (i < count && !source.exhausted()) {
            buffer[i] = source.readUtf8CodePoint().toChar()
            i++
        }
        return if (i > 0) i else -1
    }
}

