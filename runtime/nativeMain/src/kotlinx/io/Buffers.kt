/*
 *  Copyright 2018 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
@file:Suppress("DEPRECATION_ERROR")
package kotlinx.io

@Deprecated(message = message, level = DeprecationLevel.ERROR)
actual class ByteBuffer private constructor(private var backingArray: ByteArray) {

    private var idx = 0

    actual companion object {
        actual fun allocate(capacity: Int): ByteBuffer = ByteBuffer(ByteArray(capacity))
    }

    private var order: ByteOrder = ByteOrder.BIG_ENDIAN

    actual fun order(order: ByteOrder): ByteBuffer {
        this.order = order
        return this
    }

    actual fun clear(): ByteBuffer {
        backingArray = ByteArray(backingArray.size)
        idx = 0
        return this
    }

    actual fun flip(): ByteBuffer {
        idx = 0
        return this
    }

    actual fun get(): Byte {
        return backingArray[idx++]
    }

    actual fun getChar() = transformToInt(backingArray[idx++], backingArray[idx++], bytesToRead = 2).toChar()
    actual fun getShort() = transformToInt(backingArray[idx++], backingArray[idx++], bytesToRead = 2).toShort()
    actual fun getInt() = transformToInt(
            backingArray[idx++],
            backingArray[idx++],
            backingArray[idx++],
            backingArray[idx++],
            bytesToRead = 4
    )

    actual fun getFloat() = Float.fromBits(
            transformToInt(
                    backingArray[idx++],
                    backingArray[idx++],
                    backingArray[idx++],
                    backingArray[idx++],
                    bytesToRead = 4
            )
    )

    private fun transformToInt(vararg bytes: Byte, bytesToRead: Int): Int {
        if (order != ByteOrder.LITTLE_ENDIAN) {
            var r: Int = 0
            if (bytesToRead > 2) {
                r = r or (((bytes.getOrNull(0) ?: 0).toInt() and 0xFF) shl 24)
                r = r or (((bytes.getOrNull(1) ?: 0).toInt() and 0xFF) shl 16)
            }
            val offset = if (bytesToRead == 2) 0 else 2
            r = r or (((bytes.getOrNull(offset) ?: 0).toInt() and 0xFF) shl 8)
            r = r or (((bytes.getOrNull(offset + 1) ?: 0).toInt() and 0xFF) shl 0)
            return r
        } else {
            var r: Int = 0
            r = r or (((bytes.getOrNull(0) ?: 0).toInt() and 0xFF) shl 0)
            r = r or (((bytes.getOrNull(1) ?: 0).toInt() and 0xFF) shl 8)
            r = r or (((bytes.getOrNull(2) ?: 0).toInt() and 0xFF) shl 16)
            r = r or (((bytes.getOrNull(3) ?: 0).toInt() and 0xFF) shl 24)
            return r
        }
    }

    private fun transformFromInt(i: Int, bytesToWrite: Int) {
        val b0 = (i ushr 24).toByte()
        val b1 = (i ushr 16).toByte()
        val b2 = (i ushr 8).toByte()
        val b3 = (i).toByte()
        if (order != ByteOrder.LITTLE_ENDIAN) {
            if (bytesToWrite > 2) {
                backingArray[idx++] = b0
                backingArray[idx++] = b1
            }
            backingArray[idx++] = b2
            backingArray[idx++] = b3
        } else {
            backingArray[idx++] = b3
            if (bytesToWrite < 2) return
            backingArray[idx++] = b2
            if (bytesToWrite < 4) return
            backingArray[idx++] = b1
            backingArray[idx++] = b0
        }
    }

    actual fun put(value: Byte): ByteBuffer {
        backingArray[idx++] = value
        return this
    }

    actual fun putChar(value: Char): ByteBuffer {
        transformFromInt(value.toInt() and 0xFFFF, 2)
        return this
    }

    actual fun putShort(value: Short): ByteBuffer {
        transformFromInt(value.toInt() and 0xFFFF, 2)
        return this
    }

    actual fun putInt(value: Int): ByteBuffer {
        transformFromInt(value, 4)
        return this
    }

    actual fun putFloat(value: Float): ByteBuffer {
        transformFromInt(value.toBits(), 4)
        return this
    }

    actual fun array(): ByteArray {
        return backingArray
    }

    actual fun put(src: ByteArray): ByteBuffer {
        for (i in src) backingArray[idx++] = i
        return this
    }

    actual fun getLong(): Long {
        val baseOffset = idx
        var bytes: Long = 0
        if (order != ByteOrder.LITTLE_ENDIAN) {
            for (i in 0..7) {
                bytes = bytes shl 8
                bytes = bytes or (backingArray[baseOffset + i].toInt() and 0xFF).toLong()
            }
        } else {
            for (i in 7 downTo 0) {
                bytes = bytes shl 8
                bytes = bytes or (backingArray[baseOffset + i].toInt() and 0xFF).toLong()
            }
        }
        idx += 8
        return bytes
    }

    actual fun getDouble(): Double {
        return Double.fromBits(getLong())
    }

    @Suppress("NAME_SHADOWING")
    actual fun putLong(value: Long): ByteBuffer {
        var value = value
        val baseOffset = idx
        if (order != ByteOrder.LITTLE_ENDIAN) {
            for (i in 7 downTo 0) {
                backingArray[baseOffset + i] = (value and 0xFF).toByte()
                value = value shr 8
            }
        } else {
            for (i in 0..7) {
                backingArray[baseOffset + i] = (value and 0xFF).toByte()
                value = value shr 8
            }
        }
        idx += 8
        return this
    }

    actual fun putDouble(value: Double): ByteBuffer {
        return putLong(value.toBits())
    }

    actual fun get(index: Int): Byte {
        TODO("Native is not supported yet")
    }

    actual fun get(dst: ByteArray, offset: Int, cnt: Int) {
        TODO("Native is not supported yet")
    }

    actual fun getChar(index: Int): Char {
        TODO("Native is not supported yet")
    }

    actual fun getShort(index: Int): Short {
        TODO("Native is not supported yet")
    }

    actual fun getInt(index: Int): Int {
        TODO("Native is not supported yet")
    }

    actual fun getLong(index: Int): Long {
        TODO("Native is not supported yet")
    }

    actual fun getFloat(index: Int): Float {
        TODO("Native is not supported yet")
    }

    actual fun getDouble(index: Int): Double {
        TODO("Native is not supported yet")
    }

    actual fun put(value: Byte, index: Int): ByteBuffer {
        TODO("Native is not supported yet")
    }

    actual fun put(src: ByteArray, offset: Int, cnt: Int): ByteBuffer {
        TODO("Native is not supported yet")
    }

    actual fun putChar(value: Char, index: Int): ByteBuffer {
        TODO("Native is not supported yet")
    }

    actual fun putShort(value: Short, index: Int): ByteBuffer {
        TODO("Native is not supported yet")
    }

    actual fun putInt(value: Int, index: Int): ByteBuffer {
        TODO("Native is not supported yet")
    }

    actual fun putLong(value: Long, index: Int): ByteBuffer {
        TODO("Native is not supported yet")
    }

    actual fun putFloat(value: Float, index: Int): ByteBuffer {
        TODO("Native is not supported yet")
    }

    actual fun putDouble(value: Double, index: Int): ByteBuffer {
        TODO("Native is not supported yet")
    }
}
