/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.benchmarks

import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*
import org.openjdk.jmh.annotations.*
import java.util.concurrent.*

@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(1)
open class ProtoListBenchmark {

    @Serializable
    class Holder(val a: Int, val b: Int, val c: Long, val d: Double)

    @Serializable
    class HolderList(val list: List<Holder>)

    private val h = Holder(1, 2, 3L, 4.0)
    private val value = HolderList(listOf(h, h, h, h, h))
    private val bytes = ProtoBuf.encodeToByteArray(value)

    @Benchmark
    fun toBytes() = ProtoBuf.encodeToByteArray(HolderList.serializer(), value)

    @Benchmark
    fun fromBytes() = ProtoBuf.decodeFromByteArray(HolderList.serializer(), bytes)
}
