/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf.internal

internal actual fun Int.reverseBytes(): Int = Integer.reverseBytes(this)

internal actual  fun Long.reverseBytes(): Long = java.lang.Long.reverseBytes(this)
