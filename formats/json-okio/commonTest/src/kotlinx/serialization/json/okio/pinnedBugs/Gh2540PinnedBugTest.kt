/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.okio.pinnedBugs

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.json.okio.*
import okio.*
import kotlin.test.*

/**
 * PINNED BUG. Related issue: https://github.com/Kotlin/kotlinx.serialization/issues/2540
 * Pins the current behavior that surfaced in the specific issue.
 *
 * Json.decodeBufferedSourceToSequence() doesn't work with infinite streams: it reads until
 * internal buffer is filled, or the source hits EOF.
 * It is expected to read lazily, only when the next sequence value is supposed to be evaluated.
 */
class Gh2540PinnedBugTest {
    @Serializable
    data class Data(val id: Int)

    private class WouldBlockException : RuntimeException("Read requested while no more data is available")

    private class LimitedSource(private val data: ByteArray) : Source {
        private var position = 0

        override fun read(sink: Buffer, byteCount: Long): Long {
            if (position >= data.size) throw WouldBlockException()
            val count = minOf(byteCount, (data.size - position).toLong()).toInt()
            sink.write(data, position, count)
            position += count
            return count.toLong()
        }

        override fun timeout(): Timeout = Timeout.NONE
        override fun close() {}
    }

    @Test
    fun test() {
        val data = (1..3).joinToString(separator = " ") { Json.encodeToString(Data(it)) }
        val source = LimitedSource(data.encodeToByteArray()).buffer()
        assertFailsWith<WouldBlockException> {
            val _ = Json.decodeBufferedSourceToSequence<Data>(
                source,
                DecodeSequenceMode.WHITESPACE_SEPARATED
            ).first()
        }
    }
}
