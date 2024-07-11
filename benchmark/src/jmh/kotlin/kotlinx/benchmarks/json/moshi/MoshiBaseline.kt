/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.benchmarks.json.moshi


import com.squareup.moshi.*
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import benchmarks.model.*import kotlinx.serialization.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.okio.*
import okio.*
import org.openjdk.jmh.annotations.*
import java.io.*
import java.util.concurrent.*


/**
 * Note: these benchmarks were not checked to compare anything meaningful.
 * It's just a baseline to simplify Moshi configuration for a more intricated comparisons.
 * // M3, 1.7.1, Corretto 17.0.7
 *
 * Benchmark                        Mode  Cnt     Score    Error   Units
 * MoshiBaseline.kotlinFromSource  thrpt    5   283.587 ± 10.556  ops/ms
 * MoshiBaseline.kotlinFromString  thrpt    5  1518.012 ± 47.191  ops/ms
 * MoshiBaseline.kotlinToOkio      thrpt    5  1055.492 ± 48.782  ops/ms
 * MoshiBaseline.kotlinToString    thrpt    5  2264.407 ± 88.324  ops/ms
 *
 * MoshiBaseline.moshiFromSource   thrpt    5  1280.841 ± 81.180  ops/ms
 * MoshiBaseline.moshiFromString   thrpt    5  1137.416 ± 63.516  ops/ms
 * MoshiBaseline.moshiToOkio       thrpt    5   963.130 ± 50.316  ops/ms
 * MoshiBaseline.moshiToString     thrpt    5  1061.251 ± 10.217  ops/ms
 */
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(1)
open class MoshiBaseline {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val jsonAdapter = moshi.adapter(DefaultPixelEvent::class.java)

    private val devNullSink = blackholeSink().buffer()

    private val source = Buffer().writeUtf8(pixelEventJson)

    // Moshi

    @Benchmark
    fun moshiToString(): String = jsonAdapter.toJson(pixelEvent)

    @Benchmark
    fun moshiToOkio() = jsonAdapter.toJson(devNullSink, pixelEvent)

    @Benchmark
    fun moshiFromString(): DefaultPixelEvent = jsonAdapter.fromJson(pixelEventJson)!!

    @Benchmark
    fun moshiFromSource(): DefaultPixelEvent = jsonAdapter.fromJson(source.copy())!!

    // Kx

    @Benchmark
    fun kotlinToString(): String = Json.encodeToString(DefaultPixelEvent.serializer(), pixelEvent)

    @Benchmark
    fun kotlinToOkio() = Json.encodeToBufferedSink(DefaultPixelEvent.serializer(), pixelEvent, devNullSink)

    @Benchmark
    fun kotlinFromString(): DefaultPixelEvent = Json.decodeFromString(DefaultPixelEvent.serializer(), pixelEventJson)

    @Benchmark
    fun kotlinFromSource(): DefaultPixelEvent = Json.decodeFromBufferedSource(DefaultPixelEvent.serializer(), source.copy())
}
