package kotlinx.benchmarks.json

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.module.kotlin.*
import benchmarks.model.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.json.okio.encodeToBufferedSink
import okio.blackholeSink
import okio.buffer
import org.openjdk.jmh.annotations.*
import java.io.ByteArrayInputStream
import java.io.OutputStream
import java.util.concurrent.*

@Warmup(iterations = 7, time = 1)
@Measurement(iterations = 5, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(2)
open class JacksonComparisonBenchmark {

    private val objectMapper: ObjectMapper = jacksonObjectMapper()


    private val devNullSink = blackholeSink().buffer()
    private val devNullStream = object : OutputStream() {
        override fun write(b: Int) {}
        override fun write(b: ByteArray) {}
        override fun write(b: ByteArray, off: Int, len: Int) {}
    }

    private val stringData = Json.encodeToString(DefaultPixelEvent.serializer(), pixelEvent)
    private val utf8BytesData = stringData.toByteArray()

    @Serializable
    private class SmallDataClass(val id: Int, val name: String)

    private val smallData = SmallDataClass(42, "Vincent")

    @Benchmark
    fun jacksonToString(): String = objectMapper.writeValueAsString(pixelEvent)

    @Benchmark
    fun jacksonToStringWithEscapes(): String = objectMapper.writeValueAsString(pixelEventWithEscapes)

    @Benchmark
    fun jacksonSmallToString(): String = objectMapper.writeValueAsString(smallData)

    @Benchmark
    fun kotlinToString(): String = Json.encodeToString(DefaultPixelEvent.serializer(), pixelEvent)

    @Benchmark
    fun kotlinToStream() = Json.encodeToStream(DefaultPixelEvent.serializer(), pixelEvent, devNullStream)

    @Benchmark
    fun kotlinFromStream() = Json.decodeFromStream(DefaultPixelEvent.serializer(), ByteArrayInputStream(utf8BytesData))

    @Benchmark
    fun kotlinToOkio() = Json.encodeToBufferedSink(DefaultPixelEvent.serializer(), pixelEvent, devNullSink)

    @Benchmark
    fun kotlinToStringWithEscapes(): String = Json.encodeToString(DefaultPixelEvent.serializer(), pixelEventWithEscapes)

    @Benchmark
    fun kotlinSmallToString(): String = Json.encodeToString(SmallDataClass.serializer(), smallData)

    @Benchmark
    fun kotlinSmallToStream() = Json.encodeToStream(SmallDataClass.serializer(), smallData, devNullStream)

    @Benchmark
    fun kotlinSmallToOkio() = Json.encodeToBufferedSink(SmallDataClass.serializer(), smallData, devNullSink)

    @Benchmark
    fun jacksonFromString(): DefaultPixelEvent = objectMapper.readValue(stringData, DefaultPixelEvent::class.java)

    @Benchmark
    fun kotlinFromString(): DefaultPixelEvent = Json.decodeFromString(DefaultPixelEvent.serializer(), stringData)
}
