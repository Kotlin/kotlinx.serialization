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
open class ProtoBaseline {

    @Serializable
    class Holder(val a: Int, val b: Int, val c: Long, val d: Double)

    @Serializable
    class HolderExplicit(@ProtoNumber(1) val a: Int, @ProtoNumber(2) val b: Int, @ProtoNumber(3) val c: Long, @ProtoNumber(4) val d: Double)

    private val holder = Holder(1, 2, 3L, 4.0)
    private val holderBytes = ProtoBuf.encodeToByteArray(Holder.serializer(), holder)

    private val holderExplicit = HolderExplicit(1, 2, 3L, 4.0)
    private val holderHolderExplicitBytes = ProtoBuf.encodeToByteArray(HolderExplicit.serializer(), holderExplicit)

    @Benchmark
    fun toBytes() = ProtoBuf.encodeToByteArray(Holder.serializer(), holder)

    @Benchmark
    fun fromBytes() = ProtoBuf.decodeFromByteArray(Holder.serializer(), holderBytes)

    @Benchmark
    fun toBytesExplicit() = ProtoBuf.encodeToByteArray(HolderExplicit.serializer(), holderExplicit)

    @Benchmark
    fun fromBytesExplicit() = ProtoBuf.decodeFromByteArray(HolderExplicit.serializer(), holderHolderExplicitBytes)
}
