package kotlinx.benchmarks.json

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import java.util.concurrent.TimeUnit

@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(2)
open class TreeJsonNamesBenchmark {

    @Serializable
    data class Wide(
        val f00: String? = null, val f01: String? = null, val f02: String? = null,
        val f03: String? = null, val f04: String? = null, val f05: String? = null,
        val f06: String? = null, val f07: String? = null, val f08: String? = null,
        val f09: String? = null, val f10: String? = null, val f11: String? = null,
        val f12: String? = null, val f13: String? = null, val f14: String? = null,
        val f15: String? = null, val f16: String? = null, val f17: String? = null,
        val f18: String? = null, val f19: String? = null, val f20: String? = null,
        val f21: String? = null, val f22: String? = null, val f23: String? = null,
        val f24: String? = null, val f25: String? = null, val f26: String? = null,
        val f27: String? = null, val f28: String? = null, val f29: String? = null
    )

    private val default = Json                              // useAlternativeNames = true (default)
    private val noAltNames = Json { useAlternativeNames = false }

    private val sparse = """{"f00":"a","f07":"b","f13":"c","f21":"d","f29":"e"}"""
    private val element: JsonObject = Json.parseToJsonElement(sparse).jsonObject
    private val serializer = Wide.serializer()

    @Benchmark
    fun treeAltNames(): Wide = default.decodeFromJsonElement(serializer, element)

    @Benchmark
    fun treeNoAltNames(): Wide = noAltNames.decodeFromJsonElement(serializer, element)
}
