/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package kotlinx.serialization.json.internal

// Operates with lexer's batch size
internal expect object JsonLexerBufferPool {
    fun take(): CharArray
    fun release(array: CharArray)
}
