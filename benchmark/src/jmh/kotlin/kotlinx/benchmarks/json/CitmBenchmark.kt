package kotlinx.benchmarks.json

import kotlinx.benchmarks.model.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.json.Json.Default.decodeFromString
import kotlinx.serialization.json.Json.Default.encodeToString
import org.openjdk.jmh.annotations.*
import java.util.concurrent.*

@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(2)
open class CitmBenchmark {

    @Serializable
    data class Foo(val a: Int)

    @Serializable
    object Object

    @Benchmark
    fun objectS() = serializer<Object>()

    @Benchmark
    fun dataS() = serializer<Foo>()
}
