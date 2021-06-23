package kotlinx.benchmarks.json

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.benchmarks.model.MacroTwitterFeed
import kotlinx.benchmarks.model.MicroTwitterFeed
import kotlinx.serialization.json.*
import org.openjdk.jmh.annotations.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.deleteIfExists
import kotlin.io.path.outputStream

@Warmup(iterations = 7, time = 1)
@Measurement(iterations = 7, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(2)
open class TwitterFeedStreamBenchmark {
    val resource = TwitterFeedBenchmark::class.java.getResource("/twitter_macro.json")!!
    private val twitter = Json.decodeFromString(MacroTwitterFeed.serializer(), resource.readText())

    private val jsonIgnoreUnknwn = Json { ignoreUnknownKeys = true }
    private val objectMapper: ObjectMapper =
        jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)


    private var file: Path? = null

    @Setup
    fun init() {
        file = Files.createTempFile("json_benchmark", "tmp")
    }

    @TearDown
    fun tearDown() {
        file?.deleteIfExists()
    }

    @Benchmark
    fun encodeTwitterWriteText() {
        file?.outputStream()?.use {
            it.bufferedWriter().write(Json.encodeToString(MacroTwitterFeed.serializer(), twitter))
        }
    }

    @Benchmark
    fun encodeTwitterWriteStream() {
        file?.outputStream()?.use {
            Json.encodeToStream(MacroTwitterFeed.serializer(), twitter, it)
        }
    }

    @Benchmark
    fun encodeTwitterJacksonStream() {
        file?.outputStream()?.use {
            objectMapper.writeValue(it, twitter)
        }
    }

    @Benchmark
    fun decodeMicroTwitterReadText(): MicroTwitterFeed {
        return resource.openStream().use {
            jsonIgnoreUnknwn.decodeFromString(MicroTwitterFeed.serializer(), it.bufferedReader().readText())
        }
    }

    @Benchmark
    fun decodeMicroTwitterStream(): MicroTwitterFeed {
        return resource.openStream().use {
            jsonIgnoreUnknwn.decodeFromStream(MicroTwitterFeed.serializer(), it.buffered())
        }
    }

    @Benchmark
    fun decodeMicroTwitterJacksonStream(): MicroTwitterFeed {
        return resource.openStream().use {
            objectMapper.readValue(it, MicroTwitterFeed::class.java)
        }
    }
}
