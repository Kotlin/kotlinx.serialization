/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.benchmarks.json

import kotlinx.benchmarks.model.*
import kotlinx.serialization.json.*
import org.openjdk.jmh.annotations.*
import java.io.*
import java.util.concurrent.*

@Warmup(iterations = 7, time = 1)
@Measurement(iterations = 7, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(2)
open class TwitterFeedCommentsBenchmark {
    val inputBytes = TwitterFeedBenchmark::class.java.getResource("/twitter_macro.json").readBytes()
    private val input = inputBytes.decodeToString()
    private val inputWithComments = prepareInputWithComments(input)
    private val inputWithCommentsBytes = inputWithComments.encodeToByteArray()

    private val jsonComments = Json { ignoreUnknownKeys = true; allowComments = true; }
    private val jsonNoComments = Json { ignoreUnknownKeys = true; allowComments = false; }

    fun prepareInputWithComments(inp: String): String {
        val result = inp.lineSequence().map { s ->
            // "id", "in_...", "is_...", etc
            if (!s.trimStart().startsWith("\"i")) s else "$s // json comment"
        }.joinToString("\n")
        assert(result.contains("// json comment"))
        return result
    }

    @Setup
    fun init() {
        // Explicitly invoking both variants before benchmarking so we know that both parser implementation classes are loaded
        require("foobar" == jsonComments.decodeFromString<String>("\"foobar\""))
        require("foobar" == jsonNoComments.decodeFromString<String>("\"foobar\""))
    }

    // The difference with TwitterFeedBenchmark.decodeMicroTwitter shows if we slow down when both StringJsonLexer and CommentsJsonLexer
    // are loaded by JVM. Should be almost non-existent on modern JVMs (but on OpenJDK-Corretto-11.0.14.1 there is one. 17 is fine.)
    @Benchmark
    fun decodeMicroTwitter() = jsonNoComments.decodeFromString(MicroTwitterFeed.serializer(), input)

    // The difference with this.decodeMicroTwitter shows if we slow down when comments are enabled but no comments present
    // in the input. It is around 13% slower than without comments support, mainly because skipWhitespaces is a separate function
    // that sometimes is not inlined by JIT.
    @Benchmark
    fun decodeMicroTwitterCommentSupport() = jsonComments.decodeFromString(MicroTwitterFeed.serializer(), input)

    // Shows how much actual skipping of the comments takes: around 10%.
    @Benchmark
    fun decodeMicroTwitterCommentInData() = jsonComments.decodeFromString(MicroTwitterFeed.serializer(), inputWithComments)

    @Benchmark
    fun decodeMicroTwitterCommentSupportStream() = jsonComments.decodeFromStream(MicroTwitterFeed.serializer(), ByteArrayInputStream(inputBytes))

    @Benchmark
    fun decodeMicroTwitterCommentInDataStream() = jsonComments.decodeFromStream(MicroTwitterFeed.serializer(), ByteArrayInputStream(inputWithCommentsBytes))
}
