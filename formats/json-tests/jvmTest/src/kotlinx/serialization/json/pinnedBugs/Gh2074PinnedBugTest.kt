/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.pinnedBugs

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.io.InputStream
import kotlin.test.*

/**
 * PINNED BUG. Related issue: https://github.com/Kotlin/kotlinx.serialization/issues/2074
 * Pins the current behavior that surfaced in the specific issue.
 *
 * Json.decodeToSequence(inputStream, WHITESPACE_SEPARATED) is expected to yield each complete entry
 * as soon as it has been read off the stream. But tries to eagerly fill the internal read buffer
 * before the parsing starts, which may block on the live stream.
 */
class Gh2074PinnedBugTest {
    @Serializable
    data class Data(val contents: String)

    private class WouldBlockException : RuntimeException("Read requested while no more data is available")

    private class LimitedStream(private val data: ByteArray) : InputStream() {
        private var position = 0

        override fun read(): Int = throw UnsupportedOperationException()

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (position >= data.size) throw WouldBlockException()
            val count = minOf(len, data.size - position)
            data.copyInto(b, off, position, position + count)
            position += count
            return count
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun test() {
        // Doesnt block, when number of elements is bigger then the internal buffer
        val data = (1..5).joinToString(separator = "") { Json.encodeToString(Data("data")) }
        assertFailsWith<WouldBlockException> {
            val _ = Json.decodeToSequence<Data>(
                LimitedStream(data.toByteArray()),
                DecodeSequenceMode.WHITESPACE_SEPARATED
            ).first()
        }
    }
}
