/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.benchmarks.readall.simplebinary

import kotlinx.serialization.*
import org.openjdk.jmh.annotations.*
import java.util.concurrent.*


@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(1, jvmArgs = ["-XX:+UseParallelGC"])
open class NestedObjectBenchmark {

    val data = ByteArray(1000) { it.toByte() }
    private val readAll = ReadAllBinaryDecoder(data)
    private val readByOne = ReadByOneBinaryDecoder(data, 17)

    @Serializable
    data class Outer(val i: Int, val j: Float, val inner: InnerNested)
    @Serializable
    data class InnerNested(val i: Int, val nested: InnerNested2)
    @Serializable
    data class InnerNested2(val i: Int, val nested: InnerNested3)
    @Serializable
    data class InnerNested3(val i: Int, val nested: InnerNested4)
    @Serializable
    data class InnerNested4(val i: Int, val nested: InnerNested5)
    @Serializable
    data class InnerNested5(val i: Int, val nested: InnerNested6)
    @Serializable
    data class InnerNested6(val i: Int, val nested: InnerNested7)
    @Serializable
    data class InnerNested7(val i: Int, val nested: InnerNested?)

    @Benchmark
    fun nestedAll(): Outer {
        readAll.reset()
        return readAll.decode(Outer.serializer())
    }

//    @Benchmark // Doesn't work
    fun nestedByOne(): Outer {
        readByOne.reset()
        return readByOne.decode(Outer.serializer())
    }
}
