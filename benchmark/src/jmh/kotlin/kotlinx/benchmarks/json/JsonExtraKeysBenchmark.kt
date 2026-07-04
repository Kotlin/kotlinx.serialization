/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

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
open class JsonExtraKeysBenchmark {

    @Serializable
    class Plain(
        val i1: Int, val i2: Int, val i3: Int, val i4: Int, val i5: Int,
        val i6: Int, val i7: Int, val i8: Int, val i9: Int, val i10: Int
    )

    @Serializable
    class WithMapBucket(
        val i1: Int, val i2: Int, val i3: Int, val i4: Int, val i5: Int,
        val i6: Int, val i7: Int, val i8: Int, val i9: Int, val i10: Int,
        @JsonExtraKeys val extras: Map<String, JsonElement> = emptyMap()
    )

    @Serializable
    class WithObjectBucket(
        val i1: Int, val i2: Int, val i3: Int, val i4: Int, val i5: Int,
        val i6: Int, val i7: Int, val i8: Int, val i9: Int, val i10: Int,
        @JsonExtraKeys val extras: JsonObject = JsonObject(emptyMap())
    )

    private val noExtrasInput = """{"i1":1,"i2":1,"i3":1,"i4":1,"i5":1,"i6":1,"i7":1,"i8":1,"i9":1,"i10":1}"""
    private val extrasInput =
        """{"i1":1,"i2":1,"i3":1,"i4":1,"i5":1,"i6":1,"i7":1,"i8":1,"i9":1,"i10":1,"e1":2,"e2":2,"e3":2}"""

    private val json = Json // useExtraKeys = false by default
    private val extraKeysJson = Json { useExtraKeys = true }
    private val ignoringJson = Json { ignoreUnknownKeys = true }

    private val mapValue = extraKeysJson.decodeFromString(WithMapBucket.serializer(), extrasInput)
    private val objectValue = extraKeysJson.decodeFromString(WithObjectBucket.serializer(), extrasInput)
    private val plainValue = json.decodeFromString(Plain.serializer(), noExtrasInput)

    // Baseline with the default configuration (useExtraKeys = false). Compare
    // against the same benchmark on master to detect hot-path regressions from
    // the feature's mere existence in the code.
    @Benchmark
    fun decodePlain() = json.decodeFromString(Plain.serializer(), noExtrasInput)

    @Benchmark
    fun encodePlain() = json.encodeToString(Plain.serializer(), plainValue)

    // Flag enabled, but the class declares no bucket: the per-object cost of
    // the enabled feature for classes that do not use it.
    @Benchmark
    fun decodePlainExtraKeysOn() = extraKeysJson.decodeFromString(Plain.serializer(), noExtrasInput)

    @Benchmark
    fun encodePlainExtraKeysOn() = extraKeysJson.encodeToString(Plain.serializer(), plainValue)

    // Bucket declared, input contains no unknown keys: the price of declaring
    // a bucket on the happy path.
    @Benchmark
    fun decodeMapBucketNoExtras() = extraKeysJson.decodeFromString(WithMapBucket.serializer(), noExtrasInput)

    @Benchmark
    fun decodeObjectBucketNoExtras() = extraKeysJson.decodeFromString(WithObjectBucket.serializer(), noExtrasInput)

    // Today's alternative: unknown keys silently dropped.
    @Benchmark
    fun decodeIgnoringExtras() = ignoringJson.decodeFromString(Plain.serializer(), extrasInput)

    // Capture cost with 3 unknown keys.
    @Benchmark
    fun decodeMapBucketExtras() = extraKeysJson.decodeFromString(WithMapBucket.serializer(), extrasInput)

    @Benchmark
    fun decodeObjectBucketExtras() = extraKeysJson.decodeFromString(WithObjectBucket.serializer(), extrasInput)

    // Write-back cost: 3 captured entries validated and appended after the
    // regular properties.
    @Benchmark
    fun encodeMapBucketExtras() = extraKeysJson.encodeToString(WithMapBucket.serializer(), mapValue)

    @Benchmark
    fun encodeObjectBucketExtras() = extraKeysJson.encodeToString(WithObjectBucket.serializer(), objectValue)
}
