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
open class SimpleBinaryFormatBenchmark {
    val fixedData = ByteArray(1000) { it.toByte() }
    private val readAllDecoder = ReadAllBinaryDecoder(fixedData)

    @Serializable
    data class Fields3(val i1: Byte, val i2: Float, val i3: Int)
    private val readByOneDecoder3 = ReadByOneBinaryDecoder(fixedData, 3)

    @Benchmark
    fun fields03All(): Fields3 {
        readAllDecoder.reset()
        return readAllDecoder.decode(Fields3.serializer())
    }

    @Benchmark
    fun fields03ByOne(): Fields3 {
        readByOneDecoder3.reset()
        return readByOneDecoder3.decode(Fields3.serializer())
    }

    @Serializable
    data class Fields6(val i1: Byte, val i2: Float, val i3: Int, val i4: Byte, val i5: Float, val i6: Int)
    private val readByOneDecoder6 = ReadByOneBinaryDecoder(fixedData, 6)

    @Benchmark
    fun fields06All(): Fields6 {
        readAllDecoder.reset()
        return readAllDecoder.decode(Fields6.serializer())
    }

    @Benchmark
    fun fields06ByOne(): Fields6 {
        readByOneDecoder6.reset()
        return readByOneDecoder6.decode(Fields6.serializer())
    }

    @Serializable
    data class Fields9(val i1: Byte, val i2: Float, val i3: Int, val i4: Byte, val i5: Float, val i6: Int, val i7: Byte, val i8: Float, val i9: Int)
    private val readByOneDecoder9 = ReadByOneBinaryDecoder(fixedData, 9)

    @Benchmark
    fun fields09All(): Fields9 {
        readAllDecoder.reset()
        return readAllDecoder.decode(Fields9.serializer())
    }

    @Benchmark
    fun fields09ByOne(): Fields9 {
        readByOneDecoder9.reset()
        return readByOneDecoder9.decode(Fields9.serializer())
    }

    @Serializable
    data class Fields18(val i1: Byte, val i2: Float, val i3: Int, val i4: Byte, val i5: Float, val i6: Int, val i7: Byte, val i8: Float, val i9: Int, val i10: Byte, val i11: Float, val i12: Int, val i13: Byte, val i14: Float, val i15: Int, val i16: Byte, val i17: Float, val i18: Int)
    private val readByOneDecoder18 = ReadByOneBinaryDecoder(fixedData, 18)

    @Benchmark
    fun fields18All(): Fields18 {
        readAllDecoder.reset()
        return readAllDecoder.decode(Fields18.serializer())
    }

    @Benchmark
    fun fields18ByOne(): Fields18 {
        readByOneDecoder18.reset()
        return readByOneDecoder18.decode(Fields18.serializer())
    }

    @Serializable
    data class Fields24(val i1: Byte, val i2: Float, val i3: Int, val i4: Byte, val i5: Float, val i6: Int, val i7: Byte, val i8: Float, val i9: Int, val i10: Byte, val i11: Float, val i12: Int, val i13: Byte, val i14: Float, val i15: Int, val i16: Byte, val i17: Float, val i18: Int, val i19: Byte, val i20: Float, val i21: Int, val i22: Byte, val i23: Float, val i24: Int)
    private val readByOneDecoder24 = ReadByOneBinaryDecoder(fixedData, 24)

    @Benchmark
    fun fields24All(): Fields24 {
        readAllDecoder.reset()
        return readAllDecoder.decode(Fields24.serializer())
    }

    @Benchmark
    fun fields24ByOne(): Fields24 {
        readByOneDecoder24.reset()
        return readByOneDecoder24.decode(Fields24.serializer())
    }

    @Serializable
    data class Fields72(val i1: Byte, val i2: Float, val i3: Int, val i4: Byte, val i5: Float, val i6: Int, val i7: Byte, val i8: Float, val i9: Int, val i10: Byte, val i11: Float, val i12: Int, val i13: Byte, val i14: Float, val i15: Int, val i16: Byte, val i17: Float, val i18: Int, val i19: Byte, val i20: Float, val i21: Int, val i22: Byte, val i23: Float, val i24: Int, val i25: Byte, val i26: Float, val i27: Int, val i28: Byte, val i29: Float, val i30: Int, val i31: Byte, val i32: Float, val i33: Int, val i34: Byte, val i35: Float, val i36: Int, val i37: Byte, val i38: Float, val i39: Int, val i40: Byte, val i41: Float, val i42: Int, val i43: Byte, val i44: Float, val i45: Int, val i46: Byte, val i47: Float, val i48: Int, val i49: Byte, val i50: Float, val i51: Int, val i52: Byte, val i53: Float, val i54: Int, val i55: Byte, val i56: Float, val i57: Int, val i58: Byte, val i59: Float, val i60: Int, val i61: Byte, val i62: Float, val i63: Int, val i64: Byte, val i65: Float, val i66: Int, val i67: Byte, val i68: Float, val i69: Int, val i70: Byte, val i71: Float, val i72: Int)
    private val readByOneDecoder72 = ReadByOneBinaryDecoder(fixedData, 72)

    @Benchmark
    fun fields72All(): Fields72 {
        readAllDecoder.reset()
        return readAllDecoder.decode(Fields72.serializer())
    }

    @Benchmark
    fun fields72ByOne(): Fields72 {
        readByOneDecoder72.reset()
        return readByOneDecoder72.decode(Fields72.serializer())
    }
}


fun main() {
    generateBenchmarks()
}

private fun generateBenchmarks() {
    fun Int.type(): String {
        val mod = this % 3
        return when (mod) {
            0 -> "Int"
            1 -> "Float"
            2 -> "Byte"
            else -> error("")
        }
    }

    for (fields in listOf(3, 6, 9, 18, 24, 24 * 3)) {
        val cnt = if (fields >= 10) fields.toString() else "0$fields"
        val s = buildString {
            append("@Serializable\n")
            val clz = "Fields$fields"
            append("data class $clz(")
            append((1..fields).joinToString(", ") { "val i$it: ${it.type()}" })
            append(")")
            append("\n")

            append(
                """
                private val readByOneDecoder$fields = ReadByOneBinaryDecoder(fixedData, $fields)

                @Benchmark
                fun fields${cnt}All(): $clz {
                    readAllDecoder.reset()
                    return readAllDecoder.decode($clz.serializer())
                }    

                @Benchmark
                fun fields${cnt}ByOne(): $clz {
                    readByOneDecoder$fields.reset()
                    return readByOneDecoder$fields.decode($clz.serializer())
                } 
                """.trimIndent()
            )
        }
        println(s + "\n")
    }
}
