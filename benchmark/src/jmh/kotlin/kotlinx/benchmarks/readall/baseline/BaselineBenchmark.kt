/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.benchmarks.readall.baseline

import kotlinx.serialization.*
import org.openjdk.jmh.annotations.*
import java.util.concurrent.*


@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(1, jvmArgs = ["-XX:+UseParallelGC"])
open class BaselineBenchmark {
    private val fixedData = IntArray(100) { it }
    private val readAllDecoder = ReadAllIntDecoder(fixedData)

    @Serializable
    data class Fields1(val i1: Int)
    private val readByOneDecoder1 = ReadByOneIntDecoder(fixedData, 1)

    @Benchmark
    fun fields01All(): Fields1 {
        readAllDecoder.reset()
        return readAllDecoder.decode(Fields1.serializer())
    }

    @Benchmark
    fun fields01ByOne(): Fields1 {
        readByOneDecoder1.reset()
        return readByOneDecoder1.decode(Fields1.serializer())
    }

    @Serializable
    data class Fields2(val i1: Int, val i2: Int)
    private val readByOneDecoder2 = ReadByOneIntDecoder(fixedData, 2)

    @Benchmark
    fun fields02All(): Fields2 {
        readAllDecoder.reset()
        return readAllDecoder.decode(Fields2.serializer())
    }

    @Benchmark
    fun fields02ByOne(): Fields2 {
        readByOneDecoder2.reset()
        return readByOneDecoder2.decode(Fields2.serializer())
    }

    @Serializable
    data class Fields3(val i1: Int, val i2: Int, val i3: Int)
    private val readByOneDecoder3 = ReadByOneIntDecoder(fixedData, 3)

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
    data class Fields4(val i1: Int, val i2: Int, val i3: Int, val i4: Int)
    private val readByOneDecoder4 = ReadByOneIntDecoder(fixedData, 4)

    @Benchmark
    fun fields04All(): Fields4 {
        readAllDecoder.reset()
        return readAllDecoder.decode(Fields4.serializer())
    }

    @Benchmark
    fun fields04ByOne(): Fields4 {
        readByOneDecoder4.reset()
        return readByOneDecoder4.decode(Fields4.serializer())
    }

    @Serializable
    data class Fields5(val i1: Int, val i2: Int, val i3: Int, val i4: Int, val i5: Int)
    private val readByOneDecoder5 = ReadByOneIntDecoder(fixedData, 5)

    @Benchmark
    fun fields05All(): Fields5 {
        readAllDecoder.reset()
        return readAllDecoder.decode(Fields5.serializer())
    }

    @Benchmark
    fun fields05ByOne(): Fields5 {
        readByOneDecoder5.reset()
        return readByOneDecoder5.decode(Fields5.serializer())
    }

    @Serializable
    data class Fields10(val i1: Int, val i2: Int, val i3: Int, val i4: Int, val i5: Int, val i6: Int, val i7: Int, val i8: Int, val i9: Int, val i10: Int)
    private val readByOneDecoder10 = ReadByOneIntDecoder(fixedData, 10)

    @Benchmark
    fun fields10All(): Fields10 {
        readAllDecoder.reset()
        return readAllDecoder.decode(Fields10.serializer())
    }

    @Benchmark
    fun fields10ByOne(): Fields10 {
        readByOneDecoder10.reset()
        return readByOneDecoder10.decode(Fields10.serializer())
    }

    @Serializable
    data class Fields25(val i1: Int, val i2: Int, val i3: Int, val i4: Int, val i5: Int, val i6: Int, val i7: Int, val i8: Int, val i9: Int, val i10: Int, val i11: Int, val i12: Int, val i13: Int, val i14: Int, val i15: Int, val i16: Int, val i17: Int, val i18: Int, val i19: Int, val i20: Int, val i21: Int, val i22: Int, val i23: Int, val i24: Int, val i25: Int)
    private val readByOneDecoder25 = ReadByOneIntDecoder(fixedData, 25)

    @Benchmark
    fun fields25All(): Fields25 {
        readAllDecoder.reset()
        return readAllDecoder.decode(Fields25.serializer())
    }

    @Benchmark
    fun fields25ByOne(): Fields25 {
        readByOneDecoder25.reset()
        return readByOneDecoder25.decode(Fields25.serializer())
    }
}

fun main() {
    generateBenchmarks()
}

private fun generateBenchmarks() {
    for (fields in listOf(1, 2, 3, 4, 5, 10, 25)) {
        val cnt = if (fields >= 10) fields.toString() else "0$fields"
        val s = buildString {
            append("@Serializable\n")
            val clz = "Fields$fields"
            append("data class $clz(")
            append((1..fields).joinToString(", ") { "val i$it: Int" })
            append(")")
            append("\n")

            append(
                """
                private val readByOneDecoder$fields = ReadByOneIntDecoder(fixedData, $fields)

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
