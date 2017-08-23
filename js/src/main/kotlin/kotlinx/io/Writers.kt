package kotlinx.io

impl abstract class Writer protected constructor() {
    impl open fun write(ch: Int) {
        write(charArrayOf(ch.toChar()), 0, 1)
    }
    impl open fun write(str: String) {
        write(str.toList().toCharArray(), 0, str.length)
    }
    impl abstract fun write(src: CharArray, off: Int, len: Int)
    impl abstract fun flush()
    impl abstract fun close()
}

impl open class PrintWriter(val w: Writer): Writer() {
    impl open fun print(s: String) = w.write(s)
    impl open fun print(ch: Char) = w.write(ch.toInt())
    impl open fun print(value: Float)= print(value.toString())
    impl open fun print(value: Double)= print(value.toString())
    impl open fun print(value: Boolean)= print(value.toString())
    impl open fun print(value: Int)= print(value.toString())
    impl open fun print(value: Long)= print(value.toString())

    impl open fun print(value: Any?) = print(value.toString())

    impl override fun write(src: CharArray, off: Int, len: Int) {
        w.write(src, off, len)
    }
    impl override fun flush() {}
    impl override fun close() {}
}

impl class StringWriter: Writer() {
    private val sb = StringBuilder()

    impl override fun toString(): String = sb.toString()
    impl override fun write(src: CharArray, off: Int, len: Int) {
        src.slice(off until off+len).forEach { sb.append(it) }
    }
    impl override fun flush() {}
    impl override fun close() {}
}

@Suppress("IMPLEMENTATION_WITHOUT_HEADER")
impl abstract class Reader protected constructor() {
    impl abstract fun read(): Int
    impl abstract fun read(dst: CharArray, off: Int, len: Int): Int
    impl open fun close() {}
}
impl class StringReader(val str: String): Reader() {

    private var position: Int = 0

    impl override fun read(): Int = str[position++].toInt()

    impl override fun read(dst: CharArray, off: Int, len: Int): Int {
        for (i in off until off+len) {
            dst[i] = str[position++]
        }
        return len
    }

    impl override fun close() {}
}