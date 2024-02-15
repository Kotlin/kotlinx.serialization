/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal.lexer

internal class ByteStack {
    private var array: ByteArray = ByteArray(16)
    var size: Int = 0
        private set

    fun add(b: Byte) {
        if (size >= array.size) {
            array = array.copyOf(array.size * 2)
        }
        array[size++] = b
    }

    fun last(): Byte {
//        require(size > 0) { "Stack is empty" }
        return array[size - 1]
    }

    fun removeLast() {
//        require(size > 0) { "Stack is empty" }
        size--
    }

}
