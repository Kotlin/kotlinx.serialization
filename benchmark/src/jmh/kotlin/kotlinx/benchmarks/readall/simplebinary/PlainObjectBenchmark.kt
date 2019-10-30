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
    data class Fields3(val i1: Float, val i2: Byte, val i3: Int)
    private val readByOneDecoder3 = ReadByOneBinaryDecoder(fixedData, 3)
    private val readAllExtraDecoder3 = ReadAllWithExtraDecodeElementIndex(fixedData, 3)

    @Benchmark
    fun fields03All(): Fields3 {
        readAllDecoder.reset()
        return readAllDecoder.decode(Fields3.serializer())
    }

    @Benchmark
    fun fields03AllExtra(): Fields3 {
        readAllExtraDecoder3.reset()
        return  readAllExtraDecoder3.decode(Fields3.serializer())
    }

    @Benchmark
    fun fields03ByOne(): Fields3 {
        readByOneDecoder3.reset()
        return readByOneDecoder3.decode(Fields3.serializer())
    }

    @Serializable
    data class Fields6(val i1: Float, val i2: Byte, val i3: Int, val i4: Float, val i5: Byte, val i6: Int)
    private val readByOneDecoder6 = ReadByOneBinaryDecoder(fixedData, 6)
    private val readAllExtraDecoder6 = ReadAllWithExtraDecodeElementIndex(fixedData, 6)

    @Benchmark
    fun fields06All(): Fields6 {
        readAllDecoder.reset()
        return readAllDecoder.decode(Fields6.serializer())
    }

    @Benchmark
    fun fields06AllExtra(): Fields6 {
        readAllExtraDecoder6.reset()
        return  readAllExtraDecoder6.decode(Fields6.serializer())
    }

    @Benchmark
    fun fields06ByOne(): Fields6 {
        readByOneDecoder6.reset()
        return readByOneDecoder6.decode(Fields6.serializer())
    }

    @Serializable
    data class Fields9(val i1: Float, val i2: Byte, val i3: Int, val i4: Float, val i5: Byte, val i6: Int, val i7: Float, val i8: Byte, val i9: Int)
    private val readByOneDecoder9 = ReadByOneBinaryDecoder(fixedData, 9)
    private val readAllExtraDecoder9 = ReadAllWithExtraDecodeElementIndex(fixedData, 9)

    @Benchmark
    fun fields09All(): Fields9 {
        readAllDecoder.reset()
        return readAllDecoder.decode(Fields9.serializer())
    }

    @Benchmark
    fun fields09AllExtra(): Fields9 {
        readAllExtraDecoder9.reset()
        return  readAllExtraDecoder9.decode(Fields9.serializer())
    }

    @Benchmark
    fun fields09ByOne(): Fields9 {
        readByOneDecoder9.reset()
        return readByOneDecoder9.decode(Fields9.serializer())
    }

    @Serializable
    data class Fields18(val i1: Float, val i2: Byte, val i3: Int, val i4: Float, val i5: Byte, val i6: Int, val i7: Float, val i8: Byte, val i9: Int, val i10: Float, val i11: Byte, val i12: Int, val i13: Float, val i14: Byte, val i15: Int, val i16: Float, val i17: Byte, val i18: Int)
    private val readByOneDecoder18 = ReadByOneBinaryDecoder(fixedData, 18)
    private val readAllExtraDecoder18 = ReadAllWithExtraDecodeElementIndex(fixedData, 18)

    @Benchmark
    fun fields18All(): Fields18 {
        readAllDecoder.reset()
        return readAllDecoder.decode(Fields18.serializer())
    }

    @Benchmark
    fun fields18AllExtra(): Fields18 {
        readAllExtraDecoder18.reset()
        return  readAllExtraDecoder18.decode(Fields18.serializer())
    }

    @Benchmark
    fun fields18ByOne(): Fields18 {
        readByOneDecoder18.reset()
        return readByOneDecoder18.decode(Fields18.serializer())
    }

    @Serializable
    data class Fields24(val i1: Float, val i2: Byte, val i3: Int, val i4: Float, val i5: Byte, val i6: Int, val i7: Float, val i8: Byte, val i9: Int, val i10: Float, val i11: Byte, val i12: Int, val i13: Float, val i14: Byte, val i15: Int, val i16: Float, val i17: Byte, val i18: Int, val i19: Float, val i20: Byte, val i21: Int, val i22: Float, val i23: Byte, val i24: Int)
    private val readByOneDecoder24 = ReadByOneBinaryDecoder(fixedData, 24)
    private val readAllExtraDecoder24 = ReadAllWithExtraDecodeElementIndex(fixedData, 24)

    @Benchmark
    fun fields24All(): Fields24 {
        readAllDecoder.reset()
        return readAllDecoder.decode(Fields24.serializer())
    }

    @Benchmark
    fun fields24AllExtra(): Fields24 {
        readAllExtraDecoder24.reset()
        return  readAllExtraDecoder24.decode(Fields24.serializer())
    }

    @Benchmark
    fun fields24ByOne(): Fields24 {
        readByOneDecoder24.reset()
        return readByOneDecoder24.decode(Fields24.serializer())
    }

    @Serializable
    data class Fields72(val i1: Float, val i2: Byte, val i3: Int, val i4: Float, val i5: Byte, val i6: Int, val i7: Float, val i8: Byte, val i9: Int, val i10: Float, val i11: Byte, val i12: Int, val i13: Float, val i14: Byte, val i15: Int, val i16: Float, val i17: Byte, val i18: Int, val i19: Float, val i20: Byte, val i21: Int, val i22: Float, val i23: Byte, val i24: Int, val i25: Float, val i26: Byte, val i27: Int, val i28: Float, val i29: Byte, val i30: Int, val i31: Float, val i32: Byte, val i33: Int, val i34: Float, val i35: Byte, val i36: Int, val i37: Float, val i38: Byte, val i39: Int, val i40: Float, val i41: Byte, val i42: Int, val i43: Float, val i44: Byte, val i45: Int, val i46: Float, val i47: Byte, val i48: Int, val i49: Float, val i50: Byte, val i51: Int, val i52: Float, val i53: Byte, val i54: Int, val i55: Float, val i56: Byte, val i57: Int, val i58: Float, val i59: Byte, val i60: Int, val i61: Float, val i62: Byte, val i63: Int, val i64: Float, val i65: Byte, val i66: Int, val i67: Float, val i68: Byte, val i69: Int, val i70: Float, val i71: Byte, val i72: Int)
    private val readByOneDecoder72 = ReadByOneBinaryDecoder(fixedData, 72)
    private val readAllExtraDecoder72 = ReadAllWithExtraDecodeElementIndex(fixedData, 72)

    @Benchmark
    fun fields72All(): Fields72 {
        readAllDecoder.reset()
        return readAllDecoder.decode(Fields72.serializer())
    }

    @Benchmark
    fun fields72AllExtra(): Fields72 {
        readAllExtraDecoder72.reset()
        return  readAllExtraDecoder72.decode(Fields72.serializer())
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
                private val readAllExtraDecoder$fields = ReadAllWithExtraDecodeElementIndex(fixedData, $fields)
                
                @Benchmark
                fun fields${cnt}All(): $clz {
                    readAllDecoder.reset()
                    return readAllDecoder.decode($clz.serializer())
                }  
                  
                @Benchmark
                fun fields${cnt}AllExtra(): $clz {
                     readAllExtraDecoder$fields.reset()
                    return  readAllExtraDecoder$fields.decode($clz.serializer())
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
