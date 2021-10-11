/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package kotlinx.benchmarks.protobuf

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.*
import org.openjdk.jmh.annotations.*
import java.util.concurrent.*

@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(1)
open class ProtoHuge {

    @Serializable
    data class Values130(
        val field0: Int,
        val field1: Int,
        val field2: Int,
        val field3: Int,
        val field4: Int,
        val field5: Int,
        val field6: Int,
        val field7: Int,
        val field8: Int,
        val field9: Int,

        val field10: Int,
        val field11: Int,
        val field12: Int,
        val field13: Int,
        val field14: Int,
        val field15: Int,
        val field16: Int,
        val field17: Int,
        val field18: Int,
        val field19: Int,

        val field20: Int,
        val field21: Int,
        val field22: Int,
        val field23: Int,
        val field24: Int,
        val field25: Int,
        val field26: Int,
        val field27: Int,
        val field28: Int,
        val field29: Int,

        val field30: Int,
        val field31: Int,
        val field32: Int,
        val field33: Int,
        val field34: Int,
        val field35: Int,
        val field36: Int,
        val field37: Int,
        val field38: Int,
        val field39: Int,

        val field40: Int,
        val field41: Int,
        val field42: Int,
        val field43: Int,
        val field44: Int,
        val field45: Int,
        val field46: Int,
        val field47: Int,
        val field48: Int,
        val field49: Int,

        val field50: Int,
        val field51: Int,
        val field52: Int,
        val field53: Int,
        val field54: Int,
        val field55: Int,
        val field56: Int,
        val field57: Int,
        val field58: Int,
        val field59: Int,

        val field60: Int,
        val field61: Int,
        val field62: Int,
        val field63: Int,
        val field64: Int,
        val field65: Int,
        val field66: Int,
        val field67: Int,
        val field68: Int,
        val field69: Int,

        val field70: Int,
        val field71: Int,
        val field72: Int,
        val field73: Int,
        val field74: Int,
        val field75: Int,
        val field76: Int,
        val field77: Int,
        val field78: Int,
        val field79: Int,

        val field80: Int,
        val field81: Int,
        val field82: Int,
        val field83: Int,
        val field84: Int,
        val field85: Int,
        val field86: Int,
        val field87: Int,
        val field88: Int,
        val field89: Int,

        val field90: Int,
        val field91: Int,
        val field92: Int,
        val field93: Int,
        val field94: Int,
        val field95: Int,
        val field96: Int,
        val field97: Int,
        val field98: Int,
        val field99: Int,

        val field100: Int,
        val field101: Int,
        val field102: Int,
        val field103: Int,
        val field104: Int,
        val field105: Int,
        val field106: Int,
        val field107: Int,
        val field108: Int,
        val field109: Int,

        val field110: Int,
        val field111: Int,
        val field112: Int,
        val field113: Int,
        val field114: Int,
        val field115: Int,
        val field116: Int,
        val field117: Int,
        val field118: Int,
        val field119: Int,

        val field120: Int,
        val field121: Int,
        val field122: Int,
        val field123: Int,
        val field124: Int,
        val field125: Int,
        val field126: Int,
        val field127: Int,
        val field128: Int,
        val field129: Int
    )

    private val values130 = Values130(
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9,
        10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
        20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
        30, 31, 32, 33, 34, 35, 36, 37, 38, 39,
        40, 41, 42, 43, 44, 45, 46, 47, 48, 49,
        50, 51, 52, 53, 54, 55, 56, 57, 58, 59,
        60, 61, 62, 63, 64, 65, 66, 67, 68, 69,
        70, 71, 72, 73, 74, 75, 76, 77, 78, 79,
        80, 81, 82, 83, 84, 85, 86, 87, 88, 89,
        90, 91, 92, 93, 94, 95, 96, 97, 98, 99,
        100, 101, 102, 103, 104, 105, 106, 107, 108, 109,
        110, 111, 112, 113, 114, 115, 116, 117, 118, 119,
        120, 121, 122, 123, 124, 125, 126, 127, 128, 129
    )

    private val values130Bytes = ProtoBuf.encodeToByteArray(Values130.serializer(), values130)

    @Benchmark
    fun toBytes130() = ProtoBuf.encodeToByteArray(Values130.serializer(), values130)

    @Benchmark
    fun fromBytes130() = ProtoBuf.decodeFromByteArray(Values130.serializer(), values130Bytes)

}
