package kotlinx.serialization.json.internal

internal actual class JsonStringBuilder actual constructor() {
    private val sb = StringBuilder(128)

    actual fun append(value: Long) {
        sb.append(value)
    }

    actual fun append(ch: Char) {
        sb.append(ch)
    }

    actual fun append(string: String) {
        sb.append(string)
    }

    actual fun appendQuoted(string: String) {
        sb.printQuoted(string)
    }

    actual override fun toString(): String {
        return sb.toString()
    }

    actual fun release() {
    }
}
