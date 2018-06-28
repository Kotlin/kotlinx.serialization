package kotlinx.serialization.hashing

import com.google.common.hash.*
import kotlinx.serialization.*
import org.junit.Test
import org.junit.runner.*
import org.junit.runners.*
import kotlin.test.*

@Suppress("UNCHECKED_CAST")
@RunWith(Parameterized::class)
class GuavaConsistencyTest(val hashable: Hashable<*>) {
    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}")
        fun parameters(): Array<Array<Any>> {
            return arrayOf<Any>(
                IntHolder(1),
                IntHolder(42),
                IntHolder(Int.MIN_VALUE),
                IntHolder(Int.MAX_VALUE),

                LongHolder(1),
                LongHolder(42),
                LongHolder(Long.MIN_VALUE),
                LongHolder(Long.MAX_VALUE),

                NumbersHolder(
                    1, 1L, 2, Long.MIN_VALUE, Int.MAX_VALUE, 3L, 0,
                    Long.MAX_VALUE - 1, Int.MIN_VALUE + 1, 0x444, 0xfff, -1
                ),

                IdempotentStringsHolder("a", ""),
                IdempotentStringsHolder("", "a"),
                IdempotentStringsHolder("", ""),
                IdempotentStringsHolder("axc".repeat(42), "бдыщ".repeat(3)),
                IdempotentStringsHolder("some medium size", "string"),

                MixedHolder("1234", 42L, "fadfs"),

                NestedHolder("goo.gl/zPOD", MixedHolder("these literals", 239L, "are so boring"), LongHolder(314L))

            ).map { arrayOf(it) }.toTypedArray()
        }
    }

    @Test
    fun testHashConsistency() {
        val guavaHash =
            @Suppress("UNCHECKED_CAST")
            Hashing.murmur3_128().newHasher().putObject(hashable, hashable.getFunnel() as Funnel<in Hashable<*>>)

        val serializer = (null).klassSerializer(hashable.javaClass::kotlin.get())
        val out = MurMur3_128Hasher()
        serializer.save(out, hashable)
        assertEquals(guavaHash.hash().asLong(), out.makeLongHash())
    }

}

interface Hashable<T : Hashable<T>> {
    fun getFunnel(): Funnel<T>
}

@Serializable
data class IntHolder(val i: Int) : Hashable<IntHolder> {
    override fun getFunnel() = Funnel<IntHolder> { _, sink -> sink.putInt(i) }
}

@Serializable
data class LongHolder(val l: Long) : Hashable<LongHolder> {
    override fun getFunnel() = Funnel<LongHolder> { _, sink -> sink.putLong(l) }
}

@Serializable
data class NumbersHolder(
    val i1: Int, val l1: Long,
    val i2: Int, val l2: Long,
    val i3: Int, val l3: Long,
    val i4: Int, val l4: Long,
    val i5: Int, val l5: Long,
    val i6: Int, val l6: Long
) : Hashable<NumbersHolder> {
    override fun getFunnel() = Funnel<NumbersHolder> { _, sink ->
        sink.putInt(i1).putLong(l1)
            .putInt(i2).putLong(l2)
            .putInt(i3).putLong(l3)
            .putInt(i4).putLong(l4)
            .putInt(i5).putLong(l5)
            .putInt(i6).putLong(l6)

    }
}

@Serializable
data class IdempotentStringsHolder(val s1: String, val s2: String) : Hashable<IdempotentStringsHolder> {
    override fun getFunnel() = Funnel<IdempotentStringsHolder> { _, into -> into.putString(s1 + s2, Charsets.UTF_8) }
}

@Serializable
data class MixedHolder(val s1: String, val l: Long, val s2: String) : Hashable<MixedHolder> {
    override fun getFunnel() =
        Funnel<MixedHolder> { _, sink ->
            sink.putString(s1, Charsets.UTF_8).putLong(l).putString(s2, Charsets.UTF_8)
        }
}

@Serializable
data class NestedHolder(val s: String, val mixedHolder: MixedHolder, val longHolder: LongHolder) :
    Hashable<NestedHolder> {
    override fun getFunnel() = Funnel<NestedHolder> { _, sink ->
        sink.putString(s, Charsets.UTF_8)
        mixedHolder.getFunnel().funnel(mixedHolder, sink)
        longHolder.getFunnel().funnel(longHolder, sink)
    }
}
