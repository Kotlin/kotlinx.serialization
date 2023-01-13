@file:Suppress("UnstableApiUsage")

package kotlinx.serialization.hashing

import com.google.common.hash.Funnel
import com.google.common.hash.Hashing
import kotlinx.serialization.Serializable
import kotlinx.serialization.hashing.internal.MurMur3_128Hasher
import org.junit.Test
import kotlin.test.assertEquals

class GuavaMurMur3_128ConsistencyTest {
  @Test
  fun testHashConsistency() {
    val hashable = IntHolder(1)
    val guavaHash = Hashing.murmur3_128().newHasher().putObject(hashable, hashable.getFunnel())
    val kotlinHash = MurMur3_128Hasher().longHash(hashable)

    assertEquals(guavaHash.hash().asLong(), kotlinHash)
  }
}

interface Hashable<T : Hashable<T>> {
  fun getFunnel(): Funnel<T>
}

@Serializable
data class IntHolder(val i: Int) : Hashable<IntHolder> {
  override fun getFunnel(): Funnel<IntHolder> {
    return Funnel<IntHolder> { _, sink -> sink.putInt(i) }
  }
}