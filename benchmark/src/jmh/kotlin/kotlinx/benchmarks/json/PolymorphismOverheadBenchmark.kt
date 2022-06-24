package kotlinx.benchmarks.json

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import org.openjdk.jmh.annotations.*
import java.util.concurrent.*

@Warmup(iterations = 7, time = 1)
@Measurement(iterations = 5, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(1)
open class PolymorphismOverheadBenchmark {

    @Serializable
    @JsonClassDiscriminator("poly")
    data class PolymorphicWrapper(val i: @Polymorphic Poly, val i2: Impl) // amortize the cost a bit

    @Serializable
    data class BaseWrapper(val i: Impl, val i2: Impl)

    @JsonClassDiscriminator("poly")
    interface Poly

    @Serializable
    @JsonClassDiscriminator("poly")
    class Impl(val a: Int, val b: String) : Poly

    private val impl = Impl(239, "average_size_string")
    private val module = SerializersModule {
        polymorphic(Poly::class) {
            subclass(Impl.serializer())
        }
    }

    private val json = Json { serializersModule = module }
    private val implString = json.encodeToString(impl)
    private val polyString = json.encodeToString<Poly>(impl)
    private val serializer = serializer<Poly>()

    // 5000
    @Benchmark
    fun base() = json.decodeFromString(Impl.serializer(), implString)

    // As of 1.3.x
    // Baseline -- 1500
    // v1, no skip -- 2000
    // v2, with skip -- 3000 [withdrawn]
    @Benchmark
    fun poly() = json.decodeFromString(serializer, polyString)

}
