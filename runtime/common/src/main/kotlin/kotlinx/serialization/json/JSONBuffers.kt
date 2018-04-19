package kotlinx.serialization.json

interface BufferEngine<T> {
    fun append(csq: String)
    fun append(csq: String, start: Int, end: Int)
    fun append(obj: Any)

    fun print(v: Char) = append(v.toString())
    fun print(v: String) = append(v)

    fun print(v: Float) = append(v)
    fun print(v: Double) = append(v)
    fun print(v: Byte) = append(v)
    fun print(v: Short) = append(v)
    fun print(v: Int) = append(v)
    fun print(v: Long) = append(v)
    fun print(v: Boolean) = append(v)

    fun result(): T
}

class StringEngine(private val sb: StringBuilder) : BufferEngine<String> {
    constructor() : this(StringBuilder())

    override fun print(v: Char) {
        sb.append(v)
    }

    override fun append(csq: String) {
        sb.append(csq)
    }

    override fun append(csq: String, start: Int, end: Int) {
        sb.append(csq, start, end)
    }

    override fun append(obj: Any) {
        sb.append(obj)
    }

    override fun print(v: String) {
        sb.append(v)
    }

    override fun result(): String {
        return sb.toString()
    }
}
