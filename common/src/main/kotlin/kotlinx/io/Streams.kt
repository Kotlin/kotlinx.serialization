package kotlinx.io

header open class IOException: Exception {
    constructor()
    constructor(message: String)
}

header abstract class InputStream {
    open fun available(): Int
    open fun close()
    abstract fun read(): Int
    open fun read(b: ByteArray): Int
    open fun read(b: ByteArray, offset: Int, len: Int): Int
    open fun skip(n: Long): Long
}

header class ByteArrayInputStream(buf: ByteArray): InputStream {
    override fun read(): Int
}

header abstract class OutputStream {
    open fun close()
    open fun flush()
    open fun write(buffer: ByteArray, offset: Int, count: Int)
    open fun write(buffer: ByteArray)
    abstract fun write(oneByte: Int)

}

@Suppress("NON_FINAL_MEMBER_IN_FINAL_CLASS") // KT-17944
header class ByteArrayOutputStream: OutputStream {
    override fun write(oneByte: Int)
    open fun toByteArray(): ByteArray
    open fun size(): Int
}