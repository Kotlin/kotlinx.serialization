package kotlinx.serialization.json.internal

internal expect class JsonToStringWriter constructor() : JsonWriter {
    override fun writeChar(char: Char)
    override fun writeLong(value: Long)
    override fun write(text: String)
    override fun writeQuoted(text: String)
    override fun toString(): String
    override fun release()
}
