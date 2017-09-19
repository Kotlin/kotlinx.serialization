package kotlinx.io

@Suppress("HEADER_WITHOUT_IMPLEMENTATION") // see KT-19848
header abstract class Writer protected constructor() {
    open fun write(ch: Int)
    open fun write(str: String)
    abstract fun write(src: CharArray, off: Int, len: Int)
    abstract fun flush()
    abstract fun close()
}

header open class PrintWriter(w: Writer) : Writer {
    open fun print(s: String)
    open fun print(ch: Char)
    open fun print(value: Float)
    open fun print(value: Double)
    open fun print(value: Boolean)
    open fun print(value: Int)
    open fun print(value: Long)
    open fun print(value: Any?)

    open fun println()
    open fun println(s: String)
    open fun println(ch: Char)
    open fun println(value: Float)
    open fun println(value: Double)
    open fun println(value: Boolean)
    open fun println(value: Int)
    open fun println(value: Long)
    open fun println(value: Any?)

    override fun write(src: CharArray, off: Int, len: Int)
    override fun flush()
    override fun close()
}

header class StringWriter: Writer {
    override fun toString(): String
    override fun write(src: CharArray, off: Int, len: Int)
    override fun flush()
    override fun close()
}

@Suppress("HEADER_WITHOUT_IMPLEMENTATION") // see KT-19848
header abstract class Reader protected constructor() {
    abstract fun read(): Int
    abstract fun read(dst: CharArray, off: Int, len: Int): Int
    open fun close()
}

header class StringReader(str: String): Reader {
    override fun read(): Int
    override fun read(dst: CharArray, off: Int, len: Int): Int
    override fun close()
}