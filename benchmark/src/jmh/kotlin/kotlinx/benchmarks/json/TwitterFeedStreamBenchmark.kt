package kotlinx.benchmarks.json

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.benchmarks.model.MacroTwitterFeed
import kotlinx.benchmarks.model.MicroTwitterFeed
import kotlinx.serialization.json.*
import org.openjdk.jmh.annotations.*
import java.io.*
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
    val bytes = resource.readBytes()
    private val twitter = Json.decodeFromString(MacroTwitterFeed.serializer(), resource.readText())

    private val jsonIgnoreUnknwn = Json { ignoreUnknownKeys = true }
    private val objectMapper: ObjectMapper =
        jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)


    private val inputStream: InputStream
        get() = ByteArrayInputStream(bytes)
    private val outputStream: OutputStream
        get() = ByteArrayOutputStream()

    @Benchmark
    fun encodeTwitterWriteText(): OutputStream {
        return outputStream.use {
            it.bufferedWriter().write(Json.encodeToString(MacroTwitterFeed.serializer(), twitter))
            it
        }
    }

    @Benchmark
    fun encodeTwitterWriteStream(): OutputStream {
        return outputStream.use {
            Json.encodeToStream(MacroTwitterFeed.serializer(), twitter, it)
            it
        }
    }

    @Benchmark
    fun encodeTwitterJacksonStream(): OutputStream {
        return outputStream.use {
            objectMapper.writeValue(it, twitter)
            it
        }
    }

    @Benchmark
    fun decodeMicroTwitterReadText(): MicroTwitterFeed {
        return inputStream.use {
            jsonIgnoreUnknwn.decodeFromString(MicroTwitterFeed.serializer(), it.bufferedReader().readText())
        }
    }

    @Benchmark
    fun decodeMicroTwitterStream(): MicroTwitterFeed {
        return inputStream.use {
            jsonIgnoreUnknwn.decodeFromStream(MicroTwitterFeed.serializer(), it.buffered())
        }
    }

    @Benchmark
    fun decodeMicroTwitterJacksonStream(): MicroTwitterFeed {
        return inputStream.use {
            objectMapper.readValue(it, MicroTwitterFeed::class.java)
        }
    }
}
