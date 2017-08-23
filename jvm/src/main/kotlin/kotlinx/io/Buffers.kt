package kotlinx.io

typealias JByteBuffer = java.nio.ByteBuffer
typealias JByteOrder = java.nio.ByteOrder

impl class ByteBuffer private constructor() {
    private lateinit var dw: JByteBuffer
    constructor(dw: JByteBuffer): this() {
        this.dw = dw
    }
    impl fun order(order: ByteOrder): ByteBuffer { dw.order(if (order == ByteOrder.LITTLE_ENDIAN) JByteOrder.LITTLE_ENDIAN else JByteOrder.BIG_ENDIAN); return this;}
    impl fun clear(): ByteBuffer { dw.clear(); return this;}
    impl fun flip(): ByteBuffer { dw.flip(); return this;}

    impl fun get(): Byte = dw.get()
    impl fun get(index: Int): Byte = dw.get(index)
    impl fun get(dst: ByteArray, offset: Int, cnt: Int): Unit {dw.get(dst, offset, cnt)}
    impl fun getChar(): Char = dw.getChar()
    impl fun getChar(index: Int): Char = dw.getChar(index)
    impl fun getShort(): Short = dw.getShort()
    impl fun getShort(index: Int): Short = dw.getShort(index)
    impl fun getInt(): Int = dw.getInt()
    impl fun getInt(index: Int): Int = dw.getInt(index)
    impl fun getLong(): Long = dw.getLong()
    impl fun getLong(index: Int): Long = dw.getLong(index)
    impl fun getFloat(): Float = dw.getFloat()
    impl fun getFloat(index: Int): Float = dw.getFloat(index)
    impl fun getDouble(): Double = dw.getDouble()
    impl fun getDouble(index: Int): Double = dw.getDouble(index)


    impl fun put(value: Byte): ByteBuffer {dw.put(value); return this}
    impl fun put(value: Byte, index: Int): ByteBuffer {dw.put(index, value); return this}
    impl fun put(src: ByteArray): ByteBuffer {dw.put(src); return this;}
    impl fun put(src: ByteArray, offset: Int, cnt: Int): ByteBuffer {dw.put(src, offset, cnt); return this;}

    impl fun putChar(value: Char): ByteBuffer {dw.putChar(value); return this}
    impl fun putChar(value: Char, index: Int): ByteBuffer {dw.putChar(index, value); return this}
    impl fun putShort(value: Short): ByteBuffer {dw.putShort(value); return this}
    impl fun putShort(value: Short, index: Int): ByteBuffer {dw.putShort(index, value); return this}
    impl fun putInt(value: Int): ByteBuffer {dw.putInt(value); return this}
    impl fun putInt(value: Int, index: Int): ByteBuffer {dw.putInt(index, value); return this}
    impl fun putLong(value: Long): ByteBuffer {dw.putLong(value); return this}
    impl fun putLong(value: Long, index: Int): ByteBuffer {dw.putLong(index, value); return this}
    impl fun putFloat(value: Float): ByteBuffer {dw.putFloat(value); return this}
    impl fun putFloat(value: Float, index: Int): ByteBuffer {dw.putFloat(index, value); return this}
    impl fun putDouble(value: Double): ByteBuffer {dw.putDouble(value); return this}
    impl fun putDouble(value: Double, index: Int): ByteBuffer {dw.putDouble(index, value); return this}

    impl fun array(): ByteArray = dw.array()
    impl companion object {
        impl fun allocate(capacity: Int) = ByteBuffer(java.nio.ByteBuffer.allocate(capacity))
    }
}
