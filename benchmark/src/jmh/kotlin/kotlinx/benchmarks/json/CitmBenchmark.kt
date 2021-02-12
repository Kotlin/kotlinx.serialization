package kotlinx.benchmarks.json

import kotlinx.benchmarks.model.*
import kotlinx.serialization.json.*
import org.openjdk.jmh.annotations.*
import java.util.concurrent.*

@Warmup(iterations = 7, time = 1)
@Measurement(iterations = 7, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(2)
open class CitmBenchmark {
    /*
     * For some reason Citm is kind of de-facto standard cross-language benchmark.
     * Order of magnitude: 200 ops/sec
     */
    private val citm = CitmBenchmark::class.java.getResource("/citm_catalog.json").readBytes().decodeToString()

    @Benchmark
    fun decodeCitm(): CitmCatalog = Json.decodeFromString(CitmCatalog.serializer(), citm)
}
