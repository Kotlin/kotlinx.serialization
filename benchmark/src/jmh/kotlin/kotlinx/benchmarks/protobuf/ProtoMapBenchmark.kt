/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.benchmarks.protobuf

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(1)
open class ProtoMapBenchmark {

    @Serializable
    class Holder(val map: Map<String, Int>)

    private val value = Holder((0..128).associateBy { it.toString() })
    private val bytes = ProtoBuf.encodeToByteArray(value)

    @Benchmark
    fun toBytes() = ProtoBuf.encodeToByteArray(Holder.serializer(), value)

    @Benchmark
    fun fromBytes() = ProtoBuf.decodeFromByteArray(Holder.serializer(), bytes)
}
