package kotlinx.serialization.hashing

import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer

public interface Hasher {
  public val encoder: Encoder
  public fun makeLongHash(): Long
}

public inline fun <reified T : Any> Hasher.longHash(value: T): Long {
  T::class.serializer().serialize(encoder, value)
  return makeLongHash()
}