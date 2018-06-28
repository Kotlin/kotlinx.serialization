package kotlinx.benchmarks.hashing

import com.google.common.hash.*
import kotlinx.serialization.*
import kotlinx.serialization.hashing.*
import org.openjdk.jmh.annotations.*
import java.util.concurrent.*

@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(1)
open class HashingBenchmark {

    private val instance = Pojo(1, 42L)

    private val guavaHasher = Hashing.murmur3_128()
    private val funnel = PojoFunnel()
    private val kxHasher = MurMur3_128Hasher()
    private val serializer = (null).klassSerializer(Pojo::class)

    @Serializable // TODO strings are not used because I'm too lazy to implement its processing same was as in Guava
    data class Pojo(val i: Int, val l: Long)

    class PojoFunnel : Funnel<Pojo> {
        override fun funnel(from: Pojo, into: PrimitiveSink) {
            into.putInt(from.i).putLong(from.l)
        }
    }

    @Benchmark
    fun kxHashReusable(): Long {
        kxHasher.reset()
        serializer.save(kxHasher, instance)
        return kxHasher.makeLongHash()
    }

    @Benchmark
    fun kxHash(): Long {
        val hasher = MurMur3_128Hasher()
        serializer.save(hasher, instance)
        return hasher.makeLongHash()
    }

    @Benchmark
    fun guavaHash(): Long {
        // Hashers are uncacheable :(
        return guavaHasher.hashObject(instance, funnel).asLong()
    }
}
