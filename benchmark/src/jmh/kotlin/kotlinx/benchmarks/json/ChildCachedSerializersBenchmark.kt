@file:UseSerializers(UseSerializerOverheadBenchmark.DataClassSerializer::class, UseSerializerOverheadBenchmark.DataObjectSerializer::class)
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
open class ChildCachedSerializersBenchmark {

    @Serializable
    enum class TestEnum {
        ONE,
        TWO,
        THREE
    }

    @Serializable
    data class WithOneEnum(val enum: TestEnum)

    @Serializable
    data class WithOnePolymorphic(@Polymorphic val a: Any)

    @Serializable
    data class MultipleProperties(
        @Polymorphic val p0: Any,
        @Polymorphic val p1: Any,
        @Polymorphic val p2: Any,
        @Polymorphic val p3: Any,
        @Polymorphic val p4: Any,
        @Polymorphic val p5: Any,
        @Polymorphic val p6: Any,
        @Polymorphic val p7: Any,
        @Polymorphic val p8: Any,
        val p9: TestEnum
    )

    @Serializable
    class Child(val i: Int)

    private val json = Json {
        serializersModule = SerializersModule {
            polymorphic(Any::class) {
                subclass(Child::class, Child.serializer())
            }
        }
    }
    private val enumSerializer = WithOneEnum.serializer()
    private val oneSerializer = WithOnePolymorphic.serializer()
    private val multipleSerializer = MultipleProperties.serializer()

    @Benchmark
    fun enumTest() = json.encodeToString(enumSerializer,
        WithOneEnum(TestEnum.TWO)
    )

    @Benchmark
    fun oneTest() = json.encodeToString(oneSerializer,
        WithOnePolymorphic(Child(0))
    )

    @Benchmark
    fun multipleTest() = json.encodeToString(multipleSerializer,
        MultipleProperties(
            Child(0),
            Child(1),
            Child(2),
            Child(3),
            Child(4),
            Child(5),
            Child(6),
            Child(7),
            Child(8),
            TestEnum.ONE
        )
    )

}
