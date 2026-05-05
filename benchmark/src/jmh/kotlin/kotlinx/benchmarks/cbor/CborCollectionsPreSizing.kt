/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.benchmarks.cbor

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

@Warmup(iterations = 7, time = 1)
@Measurement(iterations = 7, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(2)
@OptIn(ExperimentalSerializationApi::class)
open class CborCollectionsPreSizing {
    @Param("1", "7", "17", "31")
    var collectionSize: Int = 0
    @Param("true", "false")
    var definiteLengthEncoding: Boolean = false

    private var cbor: Cbor = Cbor
    private var encodedIntArray: ByteArray = ByteArray(0)
    private var encodedIntMap: ByteArray = ByteArray(0)

    @Setup
    fun initializeCollections() {
        cbor = Cbor { useDefiniteLengthEncoding = definiteLengthEncoding }
        encodedIntArray = cbor.encodeToByteArray(IntArray(collectionSize) { it })
        encodedIntMap = cbor.encodeToByteArray(IntArray(collectionSize) { it }.associateWith { it })
    }

    @Benchmark
    fun array(): IntArray = cbor.decodeFromByteArray(encodedIntArray)

    @Benchmark
    fun map(): Map<Int, Int> = cbor.decodeFromByteArray(encodedIntMap)
}
