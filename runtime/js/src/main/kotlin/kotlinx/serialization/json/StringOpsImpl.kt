package kotlinx.serialization.json

actual fun CharArray.createString(length: Int): String =
    joinToString(separator = "", limit = length, truncated = "")
