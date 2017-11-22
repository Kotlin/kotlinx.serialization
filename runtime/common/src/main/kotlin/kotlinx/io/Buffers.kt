/*
 * Copyright 2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.io

expect class ByteBuffer private constructor(){

    fun order(order: ByteOrder): ByteBuffer
    fun clear(): ByteBuffer
    fun flip(): ByteBuffer

    fun get(): Byte
    fun get(index: Int): Byte
    fun get(dst: ByteArray, offset: Int, cnt: Int): Unit
    fun getChar(): Char
    fun getChar(index: Int): Char
    fun getShort(): Short
    fun getShort(index: Int): Short
    fun getInt(): Int
    fun getInt(index: Int): Int
    fun getLong(): Long
    fun getLong(index: Int): Long
    fun getFloat(): Float
    fun getFloat(index: Int): Float
    fun getDouble(): Double
    fun getDouble(index: Int): Double

    fun put(value: Byte): ByteBuffer
    fun put(value: Byte, index: Int): ByteBuffer
    fun put(src: ByteArray): ByteBuffer
    fun put(src: ByteArray, offset: Int, cnt: Int): ByteBuffer

    fun putChar(value: Char): ByteBuffer
    fun putChar(value: Char, index: Int): ByteBuffer
    fun putShort(value: Short): ByteBuffer
    fun putShort(value: Short, index: Int): ByteBuffer
    fun putInt(value: Int): ByteBuffer
    fun putInt(value: Int, index: Int): ByteBuffer
    fun putLong(value: Long): ByteBuffer
    fun putLong(value: Long, index: Int): ByteBuffer
    fun putFloat(value: Float): ByteBuffer
    fun putFloat(value: Float, index: Int): ByteBuffer
    fun putDouble(value: Double): ByteBuffer
    fun putDouble(value: Double, index: Int): ByteBuffer

    fun array(): ByteArray


    companion object {
        fun allocate(capacity: Int): ByteBuffer
    }
}

enum class ByteOrder {
    LITTLE_ENDIAN, BIG_ENDIAN
}