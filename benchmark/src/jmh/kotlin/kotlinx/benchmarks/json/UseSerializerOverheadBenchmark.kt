@file:UseSerializers(UseSerializerOverheadBenchmark.DataClassSerializer::class, UseSerializerOverheadBenchmark.DataObjectSerializer::class)
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
open class UseSerializerOverheadBenchmark {
    @Serializable
    data class HolderForClass(val data: DataForClass)

    @Serializable
    data class HolderForObject(val data: DataForObject)

    class DataForClass(val a: Int, val b: String)

    class DataForObject(val a: Int, val b: String)

    object DataClassSerializer: KSerializer<DataForClass> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ClassSerializer") {
            element<Int>("a")
            element<String>("b")
        }

        override fun deserialize(decoder: Decoder): DataForClass {
            return decoder.decodeStructure(descriptor) {
                var a = 0
                var b = ""
                while (true) {
                    when (val index = decodeElementIndex(ContextualOverheadBenchmark.DataSerializer.descriptor)) {
                        0 -> a = decodeIntElement(ContextualOverheadBenchmark.DataSerializer.descriptor, 0)
                        1 -> b = decodeStringElement(ContextualOverheadBenchmark.DataSerializer.descriptor, 1)
                        CompositeDecoder.DECODE_DONE -> break
                        else -> error("Unexpected index: $index")
                    }
                }
                DataForClass(a, b)
            }
        }

        override fun serialize(encoder: Encoder, value: DataForClass) {
            encoder.encodeStructure(descriptor) {
                encodeIntElement(descriptor, 0, value.a)
                encodeStringElement(descriptor, 1, value.b)
            }
        }
    }

    object DataObjectSerializer: KSerializer<DataForObject> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("ObjectSerializer") {
            element<Int>("a")
            element<String>("b")
        }

        override fun deserialize(decoder: Decoder): DataForObject {
            return decoder.decodeStructure(descriptor) {
                var a = 0
                var b = ""
                while (true) {
                    when (val index = decodeElementIndex(ContextualOverheadBenchmark.DataSerializer.descriptor)) {
                        0 -> a = decodeIntElement(ContextualOverheadBenchmark.DataSerializer.descriptor, 0)
                        1 -> b = decodeStringElement(ContextualOverheadBenchmark.DataSerializer.descriptor, 1)
                        CompositeDecoder.DECODE_DONE -> break
                        else -> error("Unexpected index: $index")
                    }
                }
                DataForObject(a, b)
            }
        }

        override fun serialize(encoder: Encoder, value: DataForObject) {
            encoder.encodeStructure(descriptor) {
                encodeIntElement(descriptor, 0, value.a)
                encodeStringElement(descriptor, 1, value.b)
            }
        }
    }

    private val module = SerializersModule {
        contextual(DataClassSerializer)
    }

    private val json = Json { serializersModule = module }

    private val classHolder = HolderForClass(DataForClass(1, "abc"))
    private val classHolderString = json.encodeToString(classHolder)
    private val classHolderSerializer = serializer<HolderForClass>()

    private val objectHolder = HolderForObject(DataForObject(1, "abc"))
    private val objectHolderString = json.encodeToString(objectHolder)
    private val objectHolderSerializer = serializer<HolderForObject>()

    @Benchmark
    fun decodeForClass() = json.decodeFromString(classHolderSerializer, classHolderString)

    @Benchmark
    fun encodeForClass() = json.encodeToString(classHolderSerializer, classHolder)

    /*
    Any optimizations should not affect the speed of these tests.
    It doesn't make sense to cache singleton (`object`) serializer, because the object is accessed instantly
     */

    @Benchmark
    fun decodeForObject() = json.decodeFromString(objectHolderSerializer, objectHolderString)

    @Benchmark
    fun encodeForObject() = json.encodeToString(objectHolderSerializer, objectHolder)

}
