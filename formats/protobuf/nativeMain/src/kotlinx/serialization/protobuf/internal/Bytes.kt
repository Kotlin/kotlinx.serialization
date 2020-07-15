/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf.internal

private fun Short.reverseBytes(): Short = (((this.toInt() and 0xff) shl 8) or ((this.toInt() and 0xffff) ushr 8)).toShort()

internal actual fun Int.reverseBytes(): Int =
    ((this and 0xffff).toShort().reverseBytes().toInt() shl 16) or ((this ushr 16).toShort().reverseBytes().toInt() and 0xffff)

internal actual  fun Long.reverseBytes(): Long =
    ((this and 0xffffffff).toInt().reverseBytes().toLong() shl 32) or ((this ushr 32).toInt()
        .reverseBytes().toLong() and 0xffffffff)
