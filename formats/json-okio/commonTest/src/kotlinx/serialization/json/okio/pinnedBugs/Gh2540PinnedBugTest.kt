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
 * Json.decodeBufferedSourceToSequence() doesn't work with infinite streams: it always reads until
 * its internal buffer is filled, or the source hits EOF. It is expected to read lazily, only when
 * the next sequence value is supposed to be evaluated.
 */
class Gh2540PinnedBugTest {
    @Serializable
    data class Message(val id: Int)

    @Test
    fun decodeBufferedSourceToSequenceReadsEverythingBeforeEmittingAnything() {
        val log = mutableListOf<String>()
        val chunks = listOf("{\"id\":1} ", "{\"id\":2} ", "{\"id\":3} ")
        var index = 0

        val source = object : Source {
            override fun read(sink: Buffer, byteCount: Long): Long {
                if (index >= chunks.size) {
                    log += "read-eof"
                    return -1L
                }
                log += "read-${index + 1}"
                val bytes = chunks[index++].encodeToByteArray()
                sink.write(bytes)
                return bytes.size.toLong()
            }

            override fun timeout(): Timeout = Timeout.NONE
            override fun close() {}
        }.buffer()

        Json.decodeBufferedSourceToSequence<Message>(source)
            .forEach { log += "emit-${it.id}" }

        assertEquals(
            listOf("read-1", "read-2", "read-3", "read-eof", "read-eof", "emit-1", "emit-2", "emit-3"),
            log
        )
    }
}
