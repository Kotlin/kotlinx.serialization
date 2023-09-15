package kotlinx.serialization.json.internal

internal actual open class JsonToStringWriter actual constructor(): InternalJsonWriter {
    private val sb = StringBuilder(128)

    actual override fun writeLong(value: Long) {
        sb.append(value)
    }

    actual override fun writeChar(char: Char) {
        sb.append(char)
    }

    actual override fun write(text: String) {
        sb.append(text)
    }

    actual override fun writeQuoted(text: String) {
        sb.printQuoted(text)
    }

    actual override fun release() {
        // nothing to flush
    }

    actual override fun toString(): String {
        return sb.toString()
    }
}
