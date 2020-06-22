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
open class SampleBenchmark {

    @Serializable
    data class Pojo(val a: Int)

    private val value = Pojo(1)

    @Benchmark
    fun benchmarkStringify(): String {
        return Json.encodeToString(value)
    }
}
