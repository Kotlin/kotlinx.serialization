/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.benchmarks.json

import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import java.util.concurrent.TimeUnit
import kotlin.io.encoding.Base64
import kotlin.random.Random

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@Fork(value = 1)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
open class RoundtripStringEncodingBenchmark {
    private val serializer = String.serializer()
    private val value = Base64.encode(
        ByteArray(8 * 1024).also { Random(239).nextBytes(it) }
    )

    @Benchmark
    fun roundTrip(): String {
        val encoded = Json.encodeToString(serializer, value)
        return Json.decodeFromString(serializer, encoded)
    }
}
