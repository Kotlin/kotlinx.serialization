/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.benchmarks

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
open class GeneratedBenchmark {
    @JvmField
    val json = Json

    @Serializable
    class Fields1(
        val i1: Int
    )

    @Benchmark
    fun jsonRoundTrip1(): Fields1 {
        val instance = Fields1(1)
        val string = json.encodeToString(Fields1.serializer(), instance)
        return json.decodeFromString(Fields1.serializer(), string)
    }

    @Serializable
    class Fields2(
        val i1: Int, val i2: Int
    )

    @Benchmark
    fun jsonRoundTrip2(): Fields2 {
        val instance = Fields2(1, 2)
        val string = json.encodeToString(Fields2.serializer(), instance)
        return json.decodeFromString(Fields2.serializer(), string)
    }

    @Serializable
    class Fields3(
        val i1: Int, val i2: Int, val i3: Int
    )

    @Benchmark
    fun jsonRoundTrip3(): Fields3 {
        val instance = Fields3(1, 2, 3)
        val string = json.encodeToString(Fields3.serializer(), instance)
        return json.decodeFromString(Fields3.serializer(), string)
    }

    @Serializable
    class Fields4(
        val i1: Int, val i2: Int, val i3: Int, val i4: Int
    )

    @Benchmark
    fun jsonRoundTrip4(): Fields4 {
        val instance = Fields4(1, 2, 3, 4)
        val string = json.encodeToString(Fields4.serializer(), instance)
        return json.decodeFromString(Fields4.serializer(), string)
    }

    @Serializable
    class Fields5(
        val i1: Int, val i2: Int, val i3: Int, val i4: Int, val i5: Int
    )

    @Benchmark
    fun jsonRoundTrip5(): Fields5 {
        val instance = Fields5(1, 2, 3, 4, 5)
        val string = json.encodeToString(Fields5.serializer(), instance)
        return json.decodeFromString(Fields5.serializer(), string)
    }

    @Serializable
    class Fields10(
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

    @Benchmark
    fun jsonRoundTrip10(): Fields10 {
        val instance = Fields10(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
        val string = json.encodeToString(Fields10.serializer(), instance)
        return json.decodeFromString(Fields10.serializer(), string)
    }

    @Serializable
    class Fields20(
        val i1: Int,
        val i2: Int,
        val i3: Int,
        val i4: Int,
        val i5: Int,
        val i6: Int,
        val i7: Int,
        val i8: Int,
        val i9: Int,
        val i10: Int,
        val i11: Int,
        val i12: Int,
        val i13: Int,
        val i14: Int,
        val i15: Int,
        val i16: Int,
        val i17: Int,
        val i18: Int,
        val i19: Int,
        val i20: Int
    )

    @Benchmark
    fun jsonRoundTrip20(): Fields20 {
        val instance = Fields20(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20)
        val string = json.encodeToString(Fields20.serializer(), instance)
        return json.decodeFromString(Fields20.serializer(), string)
    }

    @Serializable
    class Fields50(
        val i1: Int,
        val i2: Int,
        val i3: Int,
        val i4: Int,
        val i5: Int,
        val i6: Int,
        val i7: Int,
        val i8: Int,
        val i9: Int,
        val i10: Int,
        val i11: Int,
        val i12: Int,
        val i13: Int,
        val i14: Int,
        val i15: Int,
        val i16: Int,
        val i17: Int,
        val i18: Int,
        val i19: Int,
        val i20: Int,
        val i21: Int,
        val i22: Int,
        val i23: Int,
        val i24: Int,
        val i25: Int,
        val i26: Int,
        val i27: Int,
        val i28: Int,
        val i29: Int,
        val i30: Int,
        val i31: Int,
        val i32: Int,
        val i33: Int,
        val i34: Int,
        val i35: Int,
        val i36: Int,
        val i37: Int,
        val i38: Int,
        val i39: Int,
        val i40: Int,
        val i41: Int,
        val i42: Int,
        val i43: Int,
        val i44: Int,
        val i45: Int,
        val i46: Int,
        val i47: Int,
        val i48: Int,
        val i49: Int,
        val i50: Int
    )

    @Benchmark
    fun jsonRoundTrip50(): Fields50 {
        val instance = Fields50(
            1,
            2,
            3,
            4,
            5,
            6,
            7,
            8,
            9,
            10,
            11,
            12,
            13,
            14,
            15,
            16,
            17,
            18,
            19,
            20,
            21,
            22,
            23,
            24,
            25,
            26,
            27,
            28,
            29,
            30,
            31,
            32,
            33,
            34,
            35,
            36,
            37,
            38,
            39,
            40,
            41,
            42,
            43,
            44,
            45,
            46,
            47,
            48,
            49,
            50
        )
        val string = json.encodeToString(Fields50.serializer(), instance)
        return json.decodeFromString(Fields50.serializer(), string)
    }
}


fun main() {
    val fields = listOf(1, 2, 3, 4, 5, 10, 20, 50)

    for (fieldsCount in fields) {
        val s = buildString {
            append(
                """
                @Serializable
                class Fields$fieldsCount(
            """.trimIndent()
            )
            val decl = (1..fieldsCount).joinToString(prefix = "\n", postfix = "\n)") { "val i$it: Int" }
            append(decl)
            appendln()
            appendln()
            val instance =
                (1..fieldsCount).joinToString(prefix = "Fields$fieldsCount(", postfix = ")", separator = ", ")
            append(
                """
                @Benchmark
                fun jsonRoundTrip$fieldsCount(): Fields$fieldsCount {
                    val instance = $instance
                    val string = json.stringify(Fields$fieldsCount.serializer(), instance)
                    return json.parse(Fields$fieldsCount.serializer(), string)
                }
                
            """.trimIndent()
            )
        }
        println(s)
    }
}


