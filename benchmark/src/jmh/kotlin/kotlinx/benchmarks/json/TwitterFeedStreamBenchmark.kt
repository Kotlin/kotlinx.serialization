package kotlinx.benchmarks.json

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.benchmarks.model.MacroTwitterFeed
import kotlinx.benchmarks.model.MicroTwitterFeed
import kotlinx.io.*
import kotlinx.serialization.json.*
import kotlinx.serialization.json.io.*
import kotlinx.serialization.json.okio.*
import okio.*
import org.openjdk.jmh.annotations.*
import java.io.*
import java.util.concurrent.TimeUnit
import kotlin.io.use
import okio.Buffer as OkioBuffer
import okio.Sink as OkioSink

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


    @Setup
    fun init() {
        // Explicitly invoking decodeFromStream before benchmarking so we know that both parser implementation classes are loaded
        require("foobar" == Json.decodeFromStream<String>(ByteArrayInputStream("\"foobar\"".encodeToByteArray())))
        require("foobar" == Json.decodeFromString<String>("\"foobar\""))
    }


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
    fun encodeTwitterOkioStream(): OkioSink {
        val b = OkioBuffer()
        Json.encodeToBufferedSink(MacroTwitterFeed.serializer(), twitter, b)
        return b
    }

    @Benchmark
    fun encodeTwitterKotlinxIoStream(): Sink {
        val b = Buffer()
        Json.encodeToSink(MacroTwitterFeed.serializer(), twitter, b)
        return b
    }

    /**
     * While encode* benchmarks use MacroTwitterFeed model to output as many bytes as possible,
     * decode* benchmarks use MicroTwitterFeed model to also factor for skipping over unnecessary data.
     */

    // Difference with TwitterFeedBenchmark.decodeMicroTwitter shows how heavy Java's standard UTF-8 decoding actually is.
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

    @Benchmark
    fun decodeMicroTwitterOkioStream(): MicroTwitterFeed {
        // It seems there is no optimal way to reuse `bytes` between benchmark, so we are forced
        // to write them to buffer every time.
        // Note that it makes comparison with Jackson and InputStream integration much less meaningful.
        val b = OkioBuffer()
        b.write(bytes)
        return jsonIgnoreUnknwn.decodeFromBufferedSource(MicroTwitterFeed.serializer(), b)
    }

    @Benchmark
    fun decodeMicroTwitterKotlinxIoStream(): MicroTwitterFeed {
        // It seems there is no way to reuse filled buffer between benchmark iterations, so we are forced
        // to write bytes to buffer every time.
        // Note that it makes comparison with Jackson and InputStream integration much less meaningful.
        val b = Buffer()
        b.write(bytes)
        return jsonIgnoreUnknwn.decodeFromSource(MicroTwitterFeed.serializer(), b)
    }
}
