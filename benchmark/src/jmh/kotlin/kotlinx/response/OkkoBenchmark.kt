package kotlinx.response

import kotlinx.serialization.json.*
import org.openjdk.jmh.annotations.*
import response.*
import java.util.concurrent.*

@Warmup(iterations = 7, time = 1)
@Measurement(iterations = 7, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(2)
open class OkkoBenchmark {

    lateinit var json: Json
    lateinit var southPark: String
    lateinit var movies: String
    lateinit var seizad: String

    @Setup
    fun setup() {
        json = Json {
            ignoreUnknownKeys = true
        }
        southPark = requireNotNull(javaClass.getResourceAsStream("/south_park_response.json")).bufferedReader().readText()
        movies = requireNotNull(javaClass.getResourceAsStream("/movies_response.json")).bufferedReader().readText()
        seizad = requireNotNull(javaClass.getResourceAsStream("/seizad_response.json")).bufferedReader().readText()
    }


    @Benchmark
    fun southPark() = json.decodeFromString(ScreenApiResponse.serializer(), southPark)

    @Benchmark
    fun movies()  = json.decodeFromString(ScreenApiResponse.serializer(), movies)


    @Benchmark
    fun seizad() = json.decodeFromString(ScreenApiResponse.serializer(), seizad)
}
