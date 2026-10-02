/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.benchmarks.protobuf

import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*
import org.openjdk.jmh.annotations.*
import java.util.concurrent.*

@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(1)
open class ProtoNestedMessageBenchmark {

    enum class Status { DRAFT, PUBLISHED, ARCHIVED }

    @Serializable
    class Author(
        @ProtoNumber(1) val id: Long,
        @ProtoNumber(2) val name: String,
        @ProtoNumber(3) val avatarUrl: String,
        @ProtoNumber(4) val verified: Boolean = false,
    )

    @Serializable
    class Rendition(
        @ProtoNumber(1) val id: String,
        @ProtoNumber(2) val extension: String,
        @ProtoNumber(3) val width: Int,
        @ProtoNumber(4) val height: Int,
        @ProtoNumber(5) val sizeBytes: Long,
        @ProtoNumber(6) val status: Status = Status.PUBLISHED,
    )

    @Serializable
    class Preview(
        @ProtoNumber(1) val id: String,
        @ProtoNumber(2) val bytes: ByteArray,
    )

    @Serializable
    class Document(
        @ProtoNumber(1) val id: String,
        @ProtoNumber(2) val title: String,
        @ProtoNumber(3) val summary: String,
        @ProtoNumber(4) val tags: List<String>,
        @ProtoNumber(5) val author: Author,
        @ProtoNumber(6) val status: Status,
        @ProtoNumber(7) val views: Int,
        @ProtoNumber(8) val renditions: Map<String, Rendition>,
        @ProtoNumber(9) val previews: Map<Int, Preview>,
        @ProtoNumber(10) val createdAt: Long,
        @ProtoNumber(11) val related: List<Author> = emptyList(),
        @ProtoNumber(12) val score: Double = 0.0,
        @ProtoNumber(13) val published: Boolean = true,
    )

    private val value = Document(
        id = "4f6c2a8e-6f1d-4b8a-9e2c-1d7b3f5a9c01",
        title = "A reasonably long document title",
        summary = "A short summary of the document that is a few dozen characters long.",
        tags = (1..12).map { "tag-$it" },
        author = Author(42, "Jane Doe", "https://example.com/avatars/42.png", verified = true),
        status = Status.PUBLISHED,
        views = 123_456,
        renditions = (1..20).associate { i ->
            "rendition-$i" to Rendition("8a1f0c4e-2b7d-4e5f-a3c9-${i.toString().padStart(12, '0')}", "jpg", 100 * i, 200 * i, 10_000L * i)
        },
        previews = (1..4).associateWith { i -> Preview("preview-$i", ByteArray(256) { it.toByte() }) },
        createdAt = 1_700_000_000_000L,
        related = (1..4).map { Author(it.toLong(), "Author $it", "https://example.com/avatars/$it.png") },
        score = 0.87,
    )

    private val bytes = ProtoBuf.encodeToByteArray(Document.serializer(), value)

    @Benchmark
    fun toBytes() = ProtoBuf.encodeToByteArray(Document.serializer(), value)

    @Benchmark
    fun fromBytes() = ProtoBuf.decodeFromByteArray(Document.serializer(), bytes)
}
