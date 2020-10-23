/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package kotlinx.benchmarks.json

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.openjdk.jmh.annotations.*
import java.util.concurrent.*

@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(2)
open class CoerceInputValuesBenchmark {

    // Specific benchmark to isolate effect on #1156. Remove after release of 1.0.1

    @Serializable
    class Holder(
        val i1: Int,
        val i2: Int,
        val i3: Int,
        val i4: Int,
        val i5: Int,
        val i6: Int,
        val i7: Int,
        val i8: Int,
        val i9: Int,
        val i10: Int
    )

    @Serializable
    class NullableHolder(
        val i1: Int?,
        val i2: Int?,
        val i3: Int?,
        val i4: Int?,
        val i5: Int?,
        val i6: Int?,
        val i7: Int?,
        val i8: Int?,
        val i9: Int?,
        val i10: Int?
    )

    private val str = """{"i1":1,"i2":1,"i3":1,"i4":1,"i5":1,"i6":1,"i7":1,"i8":1,"i9":1,"i10":1}"""

    private val json = Json { coerceInputValues = false }
    private val coercingJson = Json { coerceInputValues = true }

    @Benchmark
    fun testNullableCoercing() = coercingJson.decodeFromString(NullableHolder.serializer(), str)

    @Benchmark
    fun testNullableRegular() = json.decodeFromString(NullableHolder.serializer(), str)


    @Benchmark
    fun testNonNullableCoercing() = coercingJson.decodeFromString(Holder.serializer(), str)

    @Benchmark
    fun testNonNullableRegular() = json.decodeFromString(Holder.serializer(), str)
}
