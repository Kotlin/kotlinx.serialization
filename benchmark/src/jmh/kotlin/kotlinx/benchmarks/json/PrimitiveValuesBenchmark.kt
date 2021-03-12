package kotlinx.benchmarks.json

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.openjdk.jmh.annotations.*
import java.util.concurrent.*


@Suppress("unused", "BooleanLiteralArgument")
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(1)
open class PrimitiveValuesBenchmark {
    /*
     * Stresses int/long and boolean parser.
     * Order of magnitude: ~1.5 ops/us
     */

    @Serializable
    class BooleanHolder(
        val b1: Boolean, val b2: Boolean, val b3: Boolean, val b4: Boolean,
        val b5: Boolean, val b6: Boolean, val b7: Boolean, val b8: Boolean
    )

    private val booleanHolder = BooleanHolder(true, false, true, false, true, true, false, false)
    private val booleanValue = Json.encodeToString(booleanHolder)

    @Serializable
    class IntHolder(
        val b1: Int, val b2: Int, val b3: Int, val b4: Int,
        val b5: Int, val b6: Int, val b7: Int, val b8: Int
    )

    private val intHolder = IntHolder(239, step(1), step(2), step(3), step(4), step(5), step(6), step(7))
    private val intValue = Json.encodeToString(intHolder)

    private fun step(step: Int) = Int.MAX_VALUE / 8 * step

    @Serializable
    class LongHolder(
        val b1: Long, val b2: Long, val b3: Long, val b4: Long,
        val b5: Long, val b6: Long, val b7: Long, val b8: Long
    )

    private val longHolder = LongHolder(239, step(1L), step(2L), step(3L), step(4L), step(5L), step(6L), step(7L))
    private val longValue = Json.encodeToString(longHolder)

    private fun step(step: Long) = Long.MAX_VALUE / 8 * step

    @Benchmark
    fun decodeBoolean(): BooleanHolder = Json.decodeFromString(BooleanHolder.serializer(), booleanValue)

    @Benchmark
    fun decodeInt(): IntHolder = Json.decodeFromString(IntHolder.serializer(), intValue)

    @Benchmark
    fun decodeLong(): LongHolder = Json.decodeFromString(LongHolder.serializer(), longValue)
}
