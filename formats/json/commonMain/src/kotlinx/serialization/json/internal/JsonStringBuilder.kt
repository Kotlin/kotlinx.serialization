package kotlinx.serialization.json.internal

internal expect class JsonStringBuilder constructor() {
    fun append(value: Long)
    fun append(ch: Char)
    fun append(string: String)
    fun appendQuoted(string: String)
    override fun toString(): String
    fun release()
}
