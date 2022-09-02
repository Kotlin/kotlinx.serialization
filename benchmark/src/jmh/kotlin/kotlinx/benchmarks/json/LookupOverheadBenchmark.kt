/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.benchmarks.json

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*
import org.openjdk.jmh.annotations.*
import java.util.concurrent.*

@Warmup(iterations = 7, time = 1)
@Measurement(iterations = 7, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(2)
open class LookupOverheadBenchmark {

    @Serializable
    class Holder(val a: String)

    @Serializable
    class Generic<T>(val a: T)

    @Serializable
    class DoubleGeneric<T1, T2>(val a: T1, val b: T2)

    @Serializable
    class PentaGeneric<T1, T2, T3, T4, T5>(val a: T1, val b: T2, val c: T3, val d: T4, val e: T5)

    private val data = """{"a":""}"""
    private val doubleData = """{"a":"","b":0}"""
    private val pentaData = """{"a":"","b":0,"c":1,"d":true,"e":" "}"""

    @Serializable
    object Object

    @Benchmark
    fun dataReified() = Json.decodeFromString<Holder>(data)

    @Benchmark
    fun dataPlain() = Json.decodeFromString(Holder.serializer(), data)

    @Benchmark
    fun genericReified() = Json.decodeFromString<Generic<String>>(data)

    @Benchmark
    fun genericPlain() = Json.decodeFromString(Generic.serializer(String.serializer()), data)

    @Benchmark
    fun doubleGenericReified() = Json.decodeFromString<DoubleGeneric<String, Int>>(doubleData)

    @Benchmark
    fun doubleGenericPlain() = Json.decodeFromString(DoubleGeneric.serializer(String.serializer(), Int.serializer()), doubleData)

    @Benchmark
    fun pentaGenericReified() = Json.decodeFromString<PentaGeneric<String, Int, Long, Boolean, Char>>(pentaData)

    @Benchmark
    fun pentaGenericPlain() = Json.decodeFromString(PentaGeneric.serializer(String.serializer(), Int.serializer(), Long.serializer(), Boolean.serializer(), Char.serializer()), pentaData)

    @Benchmark
    fun objectReified() = Json.decodeFromString<Object>("{}")

    @Benchmark
    fun objectPlain() = Json.decodeFromString(Object.serializer(), "{}")
}
