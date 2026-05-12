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
    private val scalars = UnsignedScalars(200u, 32000u, 2147483648u, 9223372036854775808uL)
    private val array = UIntArray(64) { it.toUInt() }

    private val scalarBytes = Cbor.encodeToByteArray(UnsignedScalars.serializer(), scalars)
    private val arrayBytes = Cbor.encodeToByteArray(UIntArraySerializer(), array)

    @Benchmark
    fun encodeScalars() = Cbor.encodeToByteArray(UnsignedScalars.serializer(), scalars)

    @Benchmark
    fun decodeScalars() = Cbor.decodeFromByteArray(UnsignedScalars.serializer(), scalarBytes)

    @Benchmark
    fun encodeArray() = Cbor.encodeToByteArray(UIntArraySerializer(), array)

    @Benchmark
    fun decodeArray() = Cbor.decodeFromByteArray(UIntArraySerializer(), arrayBytes)
}
