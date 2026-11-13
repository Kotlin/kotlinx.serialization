/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.benchmarks.json

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.GroupThreads
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import java.util.concurrent.TimeUnit

@Warmup(iterations = 7, time = 1)
@Measurement(iterations = 5, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(3)
open class MemoryPoolBenchmark {
    @Serializable
    data class Response(val code: Int = 200)

    val doc = Response()

    @Benchmark
    @GroupThreads(1)
    fun encodeT1(): String = Json.encodeToString(doc)

    @Benchmark
    @GroupThreads(2)
    fun encodeT2(): String = Json.encodeToString(doc)

    @Benchmark
    @GroupThreads(4)
    fun encodeT4(): String = Json.encodeToString(doc)

    @Benchmark
    @GroupThreads(8)
    fun encodeT8(): String = Json.encodeToString(doc)

    @Benchmark
    @GroupThreads(12)
    fun encodeT12(): String = Json.encodeToString(doc)

    @Benchmark
    @GroupThreads(16)
    fun encodeT16(): String = Json.encodeToString(doc)

    @Benchmark
    @GroupThreads(24)
    fun encodeT24(): String = Json.encodeToString(doc)
}
