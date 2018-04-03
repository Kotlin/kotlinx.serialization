package kotlinx.serialization.internal

actual fun CharArray.createString(length: Int): String =
    StringBuilder().also {
        it.insert(0, this)
        it.length = length
    }.toString()
