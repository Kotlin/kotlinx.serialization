/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.benchmarks.cbor

import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.*
import org.openjdk.jmh.annotations.*
import java.util.concurrent.*

@Serializable
data class KTestAllTypes(
        val i32: Int,
        val i64: Long,
        val f: Float,
        val d: Double,
        val s: String,
        val b: Boolean = false,
    )

@Serializable
data class KTestOuterMessage(
        val a: Int,
        val b: Double,
        val inner: KTestAllTypes,
        val s: String,
        val ss: List<String>
    )

@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(1)
open class CborBaseline {
        val baseMessage = KTestOuterMessage(
                42,
                256123123412.0,
                s = "string",
                ss = listOf("a", "b", "c"),
                inner = KTestAllTypes(-123124512, 36253671257312, Float.MIN_VALUE, -23e15, "foobarbaz")
                )

        val cbor = Cbor {
                encodeDefaults = true
                writeKeyTags = false
                writeValueTags = false
                writeDefiniteLengths = false
                preferCborLabelsOverNames = false
            }

        val baseBytes = cbor.encodeToByteArray(KTestOuterMessage.serializer(), baseMessage)

        @Benchmark
        fun toBytes() = cbor.encodeToByteArray(KTestOuterMessage.serializer(), baseMessage)

        @Benchmark
        fun fromBytes() = cbor.decodeFromByteArray(KTestOuterMessage.serializer(), baseBytes)

    }
