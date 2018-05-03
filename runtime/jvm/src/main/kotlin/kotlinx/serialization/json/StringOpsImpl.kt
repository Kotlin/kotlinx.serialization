package kotlinx.serialization.json

actual fun CharArray.createString(length: Int): String =
    String(this, 0, length)
