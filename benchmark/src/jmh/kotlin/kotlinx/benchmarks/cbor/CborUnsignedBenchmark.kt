/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:OptIn(ExperimentalUnsignedTypes::class, ExperimentalSerializationApi::class)

package kotlinx.benchmarks.cbor

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.*
import kotlinx.serialization.cbor.*
import org.openjdk.jmh.annotations.*
import org.openjdk.jmh.infra.Blackhole
import java.util.concurrent.*

@Serializable
data class UnsignedScalars(
    val a: UByte,
    val b: UShort,
    val c: UInt,
    val d: ULong,
)

@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(1)
open class CborUnsignedBenchmark {
    val scalars = UnsignedScalars(200u, 32000u, 2147483648u, 9223372036854775808uL)
    val scalarBytes: ByteArray = Cbor.encodeToByteArray(UnsignedScalars.serializer(), scalars)

    // Held as Any so JMH's bytecode generator does not reject the inline-class getter name.
    val array: Any = UIntArray(64) { it.toUInt() }
    val arrayBytes: ByteArray = Cbor.encodeToByteArray(UIntArraySerializer(), array as UIntArray)

    @Benchmark
    fun encodeScalars(): ByteArray = Cbor.encodeToByteArray(UnsignedScalars.serializer(), scalars)

    @Benchmark
    fun decodeScalars(): UnsignedScalars = Cbor.decodeFromByteArray(UnsignedScalars.serializer(), scalarBytes)

    @Benchmark
    fun encodeArray(): ByteArray = Cbor.encodeToByteArray(UIntArraySerializer(), array as UIntArray)

    @Benchmark
    fun decodeArray(bh: Blackhole) {
        bh.consume(Cbor.decodeFromByteArray(UIntArraySerializer(), arrayBytes))
    }
}
