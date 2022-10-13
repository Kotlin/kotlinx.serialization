package kotlinx.benchmarks.json

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*
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
open class ContextualOverheadBenchmark {
    @Serializable
    data class Holder(val data: @Contextual Data)

    class Data(val a: Int, val b: String)

    object DataSerializer: KSerializer<Data> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Serializer") {
            element<Int>("a")
            element<String>("b")
        }

        override fun deserialize(decoder: Decoder): Data {
            return decoder.decodeStructure(descriptor) {
                var a = 0
                var b = ""
                while (true) {
                    when (val index = decodeElementIndex(descriptor)) {
                        0 -> a = decodeIntElement(descriptor, 0)
                        1 -> b = decodeStringElement(descriptor, 1)
                        CompositeDecoder.DECODE_DONE -> break
                        else -> error("Unexpected index: $index")
                    }
                }
                Data(a, b)
            }
        }

        override fun serialize(encoder: Encoder, value: Data) {
            encoder.encodeStructure(descriptor) {
                encodeIntElement(descriptor, 0, value.a)
                encodeStringElement(descriptor, 1, value.b)
            }
        }

    }

    private val module = SerializersModule {
        contextual(DataSerializer)
    }

    private val json = Json { serializersModule = module }

    private val holder = Holder(Data(1, "abc"))
    private val holderString = json.encodeToString(holder)
    private val holderSerializer = serializer<Holder>()

    @Benchmark
    fun decode() = json.decodeFromString(holderSerializer, holderString)

    @Benchmark
    fun encode() = json.encodeToString(holderSerializer, holder)

}
