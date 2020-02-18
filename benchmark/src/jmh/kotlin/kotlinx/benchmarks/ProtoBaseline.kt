/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.benchmarks

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.*
import org.openjdk.jmh.annotations.*
import java.util.concurrent.*

@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(1)
open class ProtoBaseline {

    @Serializable
    class Holder(val a: Int, val b: Int, val c: Long, val d: Double)

    @Serializable
    class HolderExplicit(@ProtoId(1) val a: Int, @ProtoId(2) val b: Int, @ProtoId(3) val c: Long, @ProtoId(4) val d: Double)

    private val holder = Holder(1, 2, 3L, 4.0)
    private val holderBytes = ProtoBuf.dump(Holder.serializer(), holder)

    private val holderExplicit = HolderExplicit(1, 2, 3L, 4.0)
    private val holderHolderExplicitBytes = ProtoBuf.dump(HolderExplicit.serializer(), holderExplicit)

    @Benchmark
    fun toBytes() = ProtoBuf.dump(Holder.serializer(), holder)

    @Benchmark
    fun fromBytes() = ProtoBuf.load(Holder.serializer(), holderBytes)

    @Benchmark
    fun toBytesExplicit() = ProtoBuf.dump(HolderExplicit.serializer(), holderExplicit)

    @Benchmark
    fun fromBytesExplicit() = ProtoBuf.load(HolderExplicit.serializer(), holderHolderExplicitBytes)
}
