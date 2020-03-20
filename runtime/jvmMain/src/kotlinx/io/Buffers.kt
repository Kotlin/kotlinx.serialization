/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:Suppress(
    "DEPRECATION_ERROR", "NO_EXPLICIT_VISIBILITY_IN_API_MODE_WARNING",
    "NO_EXPLICIT_RETURN_TYPE_IN_API_MODE_WARNING"
)
package kotlinx.io

@Deprecated(message = message, level = DeprecationLevel.ERROR)
typealias JByteBuffer = java.nio.ByteBuffer
@Deprecated(message = message, level = DeprecationLevel.ERROR)
typealias JByteOrder = java.nio.ByteOrder

@Deprecated(message = message, level = DeprecationLevel.ERROR)
actual class ByteBuffer private constructor() {
    private lateinit var dw: JByteBuffer
    constructor(dw: JByteBuffer): this() {
        this.dw = dw
    }
    actual fun order(order: ByteOrder): ByteBuffer { dw.order(if (order == ByteOrder.LITTLE_ENDIAN) JByteOrder.LITTLE_ENDIAN else JByteOrder.BIG_ENDIAN); return this;}
    actual fun clear(): ByteBuffer { dw.clear(); return this;}
    actual fun flip(): ByteBuffer { dw.flip(); return this;}

    actual fun get(): Byte = dw.get()
    actual fun get(index: Int): Byte = dw.get(index)
    actual fun get(dst: ByteArray, offset: Int, cnt: Int): Unit {dw.get(dst, offset, cnt)}
    actual fun getChar(): Char = dw.getChar()
    actual fun getChar(index: Int): Char = dw.getChar(index)
    actual fun getShort(): Short = dw.getShort()
    actual fun getShort(index: Int): Short = dw.getShort(index)
    actual fun getInt(): Int = dw.getInt()
    actual fun getInt(index: Int): Int = dw.getInt(index)
    actual fun getLong(): Long = dw.getLong()
    actual fun getLong(index: Int): Long = dw.getLong(index)
    actual fun getFloat(): Float = dw.getFloat()
    actual fun getFloat(index: Int): Float = dw.getFloat(index)
    actual fun getDouble(): Double = dw.getDouble()
    actual fun getDouble(index: Int): Double = dw.getDouble(index)


    actual fun put(value: Byte): ByteBuffer {dw.put(value); return this}
    actual fun put(value: Byte, index: Int): ByteBuffer {dw.put(index, value); return this}
    actual fun put(src: ByteArray): ByteBuffer {dw.put(src); return this;}
    actual fun put(src: ByteArray, offset: Int, cnt: Int): ByteBuffer {dw.put(src, offset, cnt); return this;}

    actual fun putChar(value: Char): ByteBuffer {dw.putChar(value); return this}
    actual fun putChar(value: Char, index: Int): ByteBuffer {dw.putChar(index, value); return this}
    actual fun putShort(value: Short): ByteBuffer {dw.putShort(value); return this}
    actual fun putShort(value: Short, index: Int): ByteBuffer {dw.putShort(index, value); return this}
    actual fun putInt(value: Int): ByteBuffer {dw.putInt(value); return this}
    actual fun putInt(value: Int, index: Int): ByteBuffer {dw.putInt(index, value); return this}
    actual fun putLong(value: Long): ByteBuffer {dw.putLong(value); return this}
    actual fun putLong(value: Long, index: Int): ByteBuffer {dw.putLong(index, value); return this}
    actual fun putFloat(value: Float): ByteBuffer {dw.putFloat(value); return this}
    actual fun putFloat(value: Float, index: Int): ByteBuffer {dw.putFloat(index, value); return this}
    actual fun putDouble(value: Double): ByteBuffer {dw.putDouble(value); return this}
    actual fun putDouble(value: Double, index: Int): ByteBuffer {dw.putDouble(index, value); return this}

    actual fun array(): ByteArray = dw.array()
    actual companion object {
        actual fun allocate(capacity: Int) = ByteBuffer(java.nio.ByteBuffer.allocate(capacity))
    }
}
