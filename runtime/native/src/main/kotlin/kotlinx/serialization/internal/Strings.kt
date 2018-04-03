package kotlinx.serialization.internal

actual fun CharArray.createString(length: Int): String =
    joinToString(separator = "", limit = length, truncated = "")
