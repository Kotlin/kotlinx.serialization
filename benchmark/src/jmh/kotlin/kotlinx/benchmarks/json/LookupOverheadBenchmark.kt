package kotlinx.benchmarks.json

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*
import org.openjdk.jmh.annotations.*
import java.util.concurrent.*

@Warmup(iterations = 7, time = 1)
@Measurement(iterations = 7, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(2)
open class LookupOverheadBenchmark {

    @Serializable
    class Holder(val a: String)

    @Serializable
    class Generic<T>(val a: T)

    private val data = """{"a":""}"""

    @Serializable
    object Object

    @Benchmark
    fun dataReified() = Json.decodeFromString<Holder>(data)

    @Benchmark
    fun dataPlain() = Json.decodeFromString(Holder.serializer(), data)

    @Benchmark
    fun genericReified() = Json.decodeFromString<Generic<String>>(data)

    @Benchmark
    fun genericPlain() = Json.decodeFromString(Generic.serializer(String.serializer()), data)

    @Benchmark
    fun objectReified() = Json.decodeFromString<Object>("{}")

    @Benchmark
    fun objectPlain() = Json.decodeFromString(Object.serializer(), "{}")
}
