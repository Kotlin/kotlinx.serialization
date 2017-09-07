package kotlinx.io

import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.DataView


impl class ByteBuffer(val capacity: Int) {
    private impl constructor(): this(16) //don't use, only for matching header

    init {
        require(capacity >= 0)
    }

    private val dw = DataView(ArrayBuffer(capacity), 0, capacity)

    var limit: Int = capacity
        set(value) {
            require(value in 0..capacity)
            field = value
            if (position > value) {
                position = value
            }
        }

    var position: Int = 0
        set(newPosition) {
            require(newPosition in 0..limit)
            field = newPosition
        }

    impl fun clear(): ByteBuffer {
        position = 0
        limit = capacity
        return this
    }

    impl fun flip(): ByteBuffer {
        limit = position
        position = 0
        return this
    }

    val hasRemaining: Boolean
        get() = position < limit

    val remaining: Int
        get() = limit - position

    fun rewind(): ByteBuffer {
        position = 0
        return this
    }

    var order: ByteOrder = ByteOrder.BIG_ENDIAN
    impl fun order(order: ByteOrder): ByteBuffer {
        this.order = order
        return this
    }

    private fun idx(index: Int, size: Int): Int {
        val i = if (index == -1) {
            position += size
            position - size
        } else index
        if (i > limit) throw IllegalArgumentException()
        return i
    }

    impl fun get(): Byte = get(-1)
    impl fun get(index: Int): Byte {
        val i = idx(index, 1)
        return dw.getInt8(i)
    }

    impl fun get(dst: ByteArray, offset: Int, cnt: Int): Unit {
        val pos = idx(-1, cnt)
        for (i in 0 until cnt) {
            dst[offset + i] = dw.getInt8(pos + i)
        }
    }

    impl fun getChar() = getChar(-1)
    impl fun getChar(index: Int): Char {
        val i = idx(index, 2)
        return dw.getUint16(i, order == ByteOrder.LITTLE_ENDIAN).toChar()
    }

    impl fun getShort() = getShort(-1)
    impl fun getShort(index: Int): Short {
        val i = idx(index, 2)
        return dw.getInt16(i, order == ByteOrder.LITTLE_ENDIAN)
    }

    impl fun getInt() = getInt(-1)
    impl fun getInt(index: Int): Int {
        val i = idx(index, 4)
        return dw.getInt32(i, order == ByteOrder.LITTLE_ENDIAN)
    }

    impl fun getLong() = getLong(-1)
    impl fun getLong(index: Int): Long {
        val low:Int
        val high:Int
        val scndIdx = if (index == -1) -1 else index + 4
        if (order == ByteOrder.LITTLE_ENDIAN) {
            low = getInt(index)
            high = getInt(scndIdx)
        } else {
            high = getInt(index)
            low = getInt(scndIdx)
        }
        return ((high.toLong() shl 32) or (low.toLong() and 0xFFFFFFFF))
    }

    impl fun getFloat() = getFloat(-1)
    impl fun getFloat(index: Int): Float {
        val i = idx(index, 4)
        return dw.getFloat32(i, order == ByteOrder.LITTLE_ENDIAN)
    }

    impl fun getDouble() = getDouble(-1)
    impl fun getDouble(index: Int): Double {
        val i = idx(index, 8)
        return dw.getFloat64(i, order == ByteOrder.LITTLE_ENDIAN)
    }

    impl fun put(value: Byte) = put(value, -1)
    impl fun put(value: Byte, index: Int): ByteBuffer {
        val i = idx(index, 1)
        dw.setInt8(i, value)
        return this
    }

    impl fun put(src: ByteArray) = put(src, 0, src.size)
    impl fun put(src: ByteArray, offset: Int, cnt: Int): ByteBuffer {
        val pos = idx(-1, cnt)
        for (i in 0 until cnt) {
            dw.setInt8(pos + i, src[offset + i])
        }
        return this
    }

    impl fun putChar(value: Char) = putChar(value, -1)
    impl fun putChar(value: Char, index: Int): ByteBuffer {
        val i = idx(index, 2)
        dw.setUint16(i, value.toShort(), order == ByteOrder.LITTLE_ENDIAN)
        return this
    }

    impl fun putShort(value: Short) = putShort(value, -1)
    impl fun putShort(value: Short, index: Int): ByteBuffer {
        val i = idx(index, 2)
        dw.setInt16(i, value, order == ByteOrder.LITTLE_ENDIAN)
        return this
    }

    impl fun putInt(value: Int) = putInt(value, -1)
    impl fun putInt(value: Int, index: Int): ByteBuffer {
        val i = idx(index, 4)
        dw.setInt32(i, value, order == ByteOrder.LITTLE_ENDIAN)
        return this
    }

    impl fun putLong(value: Long) = putLong(value, -1)
    impl fun putLong(value: Long, index: Int): ByteBuffer {
        val high = (value shr 32).toInt()
        val low = (value and 0xFFFFFFFFL).toInt()
        val scndIdx = if (index == -1) -1 else index + 4
        if (order == ByteOrder.LITTLE_ENDIAN) {
            putInt(low, index)
            putInt(high, scndIdx)
        } else {
            putInt(high, index)
            putInt(low, scndIdx)
        }
        return this
    }

    impl fun putFloat(value: Float) = putFloat(value, -1)
    impl fun putFloat(value: Float, index: Int): ByteBuffer {
        val i = idx(index, 4)
        dw.setFloat32(i, value, order == ByteOrder.LITTLE_ENDIAN)
        return this
    }

    impl fun putDouble(value: Double) = putDouble(value, -1)
    impl fun putDouble(value: Double, index: Int): ByteBuffer {
        val i = idx(index, 8)
        dw.setFloat64(i, value, order == ByteOrder.LITTLE_ENDIAN)
        return this
    }

    impl fun array(): ByteArray {
        val out = ByteArray(limit)
        get(out, 0, limit)
        return out
    }

    impl companion object {
        impl fun allocate(capacity: Int) = ByteBuffer(capacity)
    }
}
