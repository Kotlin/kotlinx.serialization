package kotlinx.serialization.hashing.internal

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.hashing.Hasher
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

@PublishedApi
internal class MurMur3_128Hasher : Encoder, CompositeEncoder, Hasher {

  companion object {
    private const val CHUNK_SIZE = 16
    private const val C1 = -0x783c846eeebdac2bL
    private const val C2 = 0x4cf5ad432745937fL
  }

  private var h1 = 0L
  private var h2 = 0L
  private var length = 0

  private val buffer = ByteBuffer.allocate(CHUNK_SIZE).order(ByteOrder.LITTLE_ENDIAN)

  private var rootDescriptor: SerialDescriptor? = null

  override val encoder: Encoder
    get() = this

  override fun makeLongHash(): Long = pad(makeByteArrayHash())

  override val serializersModule: SerializersModule
    get() = EmptySerializersModule()

  override fun endStructure(descriptor: SerialDescriptor) {
    if (descriptor !== rootDescriptor) {
      return
    }

    munch()
    buffer.flip()
    if (buffer.remaining() > 0) {
      processRemaining(buffer)
      buffer.position(buffer.limit())
    }
  }

  override fun encodeBooleanElement(descriptor: SerialDescriptor, index: Int, value: Boolean) {
    TODO("Not yet implemented")
  }

  override fun encodeByteElement(descriptor: SerialDescriptor, index: Int, value: Byte) {
    buffer.put(value)
    munchIfFull()
  }

  override fun encodeShortElement(descriptor: SerialDescriptor, index: Int, value: Short) {
    buffer.putShort(value)
    munchIfFull()
  }

  override fun encodeCharElement(descriptor: SerialDescriptor, index: Int, value: Char) {
    buffer.putChar(value)
    munchIfFull()
  }

  override fun encodeIntElement(descriptor: SerialDescriptor, index: Int, value: Int) {
    buffer.putInt(value)
    munchIfFull()
  }

  override fun encodeLongElement(descriptor: SerialDescriptor, index: Int, value: Long) {
    buffer.putLong(value)
    munchIfFull()
  }

  override fun encodeFloatElement(descriptor: SerialDescriptor, index: Int, value: Float) {
    buffer.putFloat(value)
    munchIfFull()
  }

  override fun encodeDoubleElement(descriptor: SerialDescriptor, index: Int, value: Double) {
    buffer.putDouble(value)
    munchIfFull()
  }

  override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String) {
    TODO("Not yet implemented")
  }

  override fun encodeInlineElement(descriptor: SerialDescriptor, index: Int): Encoder {
    TODO("Not yet implemented")
  }

  override fun <T> encodeSerializableElement(
    descriptor: SerialDescriptor,
    index: Int,
    serializer: SerializationStrategy<T>,
    value: T
  ) {
    TODO("Not yet implemented")
  }

  @ExperimentalSerializationApi
  override fun <T : Any> encodeNullableSerializableElement(
    descriptor: SerialDescriptor,
    index: Int,
    serializer: SerializationStrategy<T>,
    value: T?
  ) {
    TODO("Not yet implemented")
  }

  @ExperimentalSerializationApi
  override fun encodeNull() {
    TODO("Not yet implemented")
  }

  override fun encodeBoolean(value: Boolean) {
    TODO("Not yet implemented")
  }

  override fun encodeByte(value: Byte) {
    buffer.put(value)
    munchIfFull()
  }

  override fun encodeShort(value: Short) {
    buffer.putShort(value)
    munchIfFull()
  }

  override fun encodeChar(value: Char) {
    buffer.putChar(value)
    munchIfFull()
  }

  override fun encodeInt(value: Int) {
    buffer.putInt(value)
    munchIfFull()
  }

  override fun encodeLong(value: Long) {
    buffer.putLong(value)
    munchIfFull()
  }

  override fun encodeFloat(value: Float) {
    buffer.putFloat(value)
    munchIfFull()
  }

  override fun encodeDouble(value: Double) {
    buffer.putDouble(value)
    munchIfFull()
  }

  override fun encodeString(value: String) {
    TODO("Not yet implemented")
  }

  override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) {
    TODO("Not yet implemented")
  }

  override fun encodeInline(descriptor: SerialDescriptor): Encoder {
    TODO("Not yet implemented")
  }

  override fun beginStructure(descriptor: SerialDescriptor): CompositeEncoder {
    if (rootDescriptor == null) {
      rootDescriptor = descriptor
    }

    return this
  }

  private fun processRemaining(bb: ByteBuffer) {
    var k1: Long = 0
    var k2: Long = 0
    length += bb.remaining()
    when (bb.remaining()) {
      15 -> {
        k2 = k2 xor (bb.get(14).toInt().toLong() shl 48) // fall through
        k2 = k2 xor (bb.get(13).toInt().toLong() shl 40) // fall through
        k2 = k2 xor (bb.get(12).toInt().toLong() shl 32) // fall through
        k2 = k2 xor (bb.get(11).toInt().toLong() shl 24) // fall through
        k2 = k2 xor (bb.get(10).toInt().toLong() shl 16) // fall through
        k2 = k2 xor (bb.get(9).toInt().toLong() shl 8) // fall through
        k2 = k2 xor bb.get(8).toInt().toLong() // fall through
        k1 = k1 xor bb.long
      }

      14 -> {
        k2 = k2 xor (bb.get(13).toInt().toLong() shl 40)
        k2 = k2 xor (bb.get(12).toInt().toLong() shl 32)
        k2 = k2 xor (bb.get(11).toInt().toLong() shl 24)
        k2 = k2 xor (bb.get(10).toInt().toLong() shl 16)
        k2 = k2 xor (bb.get(9).toInt().toLong() shl 8)
        k2 = k2 xor bb.get(8).toInt().toLong()
        k1 = k1 xor bb.long
      }

      13 -> {
        k2 = k2 xor (bb.get(12).toInt().toLong() shl 32)
        k2 = k2 xor (bb.get(11).toInt().toLong() shl 24)
        k2 = k2 xor (bb.get(10).toInt().toLong() shl 16)
        k2 = k2 xor (bb.get(9).toInt().toLong() shl 8)
        k2 = k2 xor bb.get(8).toInt().toLong()
        k1 = k1 xor bb.long
      }

      12 -> {
        k2 = k2 xor (bb.get(11).toInt().toLong() shl 24)
        k2 = k2 xor (bb.get(10).toInt().toLong() shl 16)
        k2 = k2 xor (bb.get(9).toInt().toLong() shl 8)
        k2 = k2 xor bb.get(8).toInt().toLong()
        k1 = k1 xor bb.long
      }

      11 -> {
        k2 = k2 xor (bb.get(10).toInt().toLong() shl 16)
        k2 = k2 xor (bb.get(9).toInt().toLong() shl 8)
        k2 = k2 xor bb.get(8).toInt().toLong()
        k1 = k1 xor bb.long
      }

      10 -> {
        k2 = k2 xor (bb.get(9).toInt().toLong() shl 8)
        k2 = k2 xor bb.get(8).toInt().toLong()
        k1 = k1 xor bb.long
      }

      9 -> {
        k2 = k2 xor bb.get(8).toInt().toLong()
        k1 = k1 xor bb.long
      }

      8 -> k1 = k1 xor bb.long
      7 -> {
        k1 = k1 xor (bb.get(6).toInt().toLong() shl 48) // fall through
        k1 = k1 xor (bb.get(5).toInt().toLong() shl 40) // fall through
        k1 = k1 xor (bb.get(4).toInt().toLong() shl 32) // fall through
        k1 = k1 xor (bb.get(3).toInt().toLong() shl 24) // fall through
        k1 = k1 xor (bb.get(2).toInt().toLong() shl 16) // fall through
        k1 = k1 xor (bb.get(1).toInt().toLong() shl 8) // fall through
        k1 = k1 xor bb.get(0).toInt().toLong()
      }

      6 -> {
        k1 = k1 xor (bb.get(5).toInt().toLong() shl 40)
        k1 = k1 xor (bb.get(4).toInt().toLong() shl 32)
        k1 = k1 xor (bb.get(3).toInt().toLong() shl 24)
        k1 = k1 xor (bb.get(2).toInt().toLong() shl 16)
        k1 = k1 xor (bb.get(1).toInt().toLong() shl 8)
        k1 = k1 xor bb.get(0).toInt().toLong()
      }

      5 -> {
        k1 = k1 xor (bb.get(4).toInt().toLong() shl 32)
        k1 = k1 xor (bb.get(3).toInt().toLong() shl 24)
        k1 = k1 xor (bb.get(2).toInt().toLong() shl 16)
        k1 = k1 xor (bb.get(1).toInt().toLong() shl 8)
        k1 = k1 xor bb.get(0).toInt().toLong()
      }

      4 -> {
        k1 = k1 xor (bb.get(3).toInt().toLong() shl 24)
        k1 = k1 xor (bb.get(2).toInt().toLong() shl 16)
        k1 = k1 xor (bb.get(1).toInt().toLong() shl 8)
        k1 = k1 xor bb.get(0).toInt().toLong()
      }

      3 -> {
        k1 = k1 xor (bb.get(2).toInt().toLong() shl 16)
        k1 = k1 xor (bb.get(1).toInt().toLong() shl 8)
        k1 = k1 xor bb.get(0).toInt().toLong()
      }

      2 -> {
        k1 = k1 xor (bb.get(1).toInt().toLong() shl 8)
        k1 = k1 xor bb.get(0).toInt().toLong()
      }

      1 -> k1 = k1 xor bb.get(0).toInt().toLong()
      else -> throw AssertionError("Should never get here.")
    }
    h1 = h1 xor mixK1(k1)
    h2 = h2 xor mixK2(k2)
  }

  private fun munchIfFull() {
    if (buffer.remaining() < 8) {
      munch()
    }
  }

  private fun munch() {
    buffer.flip()
    while (buffer.remaining() >= CHUNK_SIZE) {
      process(buffer)
    }
    buffer.compact()
  }

  private fun process(bb: ByteBuffer) {
    val k1 = bb.long
    val k2 = bb.long
    bmix64(k1, k2)
    length += CHUNK_SIZE
  }

  private fun bmix64(k1: Long, k2: Long) {
    h1 = h1 xor mixK1(k1)

    h1 = java.lang.Long.rotateLeft(h1, 27)
    h1 += h2
    h1 = h1 * 5 + 0x52dce729

    h2 = h2 xor mixK2(k2)

    h2 = java.lang.Long.rotateLeft(h2, 31)
    h2 += h1
    h2 = h2 * 5 + 0x38495ab5
  }


  private fun mixK1(k1: Long): Long {
    var result = k1
    result *= C1
    result = java.lang.Long.rotateLeft(result, 31)
    result *= C2
    return result
  }

  private fun mixK2(k2: Long): Long {
    var result = k2
    result *= C2
    result = java.lang.Long.rotateLeft(result, 33)
    result *= C1
    return result
  }

  private fun pad(arr: ByteArray): Long {
    var retVal = arr[0].toLong() and 0xFF
    for (i in 1 until min(arr.size, 8)) {
      retVal = retVal or (arr[i].toLong() and 0xFFL shl i * 8)
    }
    return retVal
  }

  private fun makeByteArrayHash(): ByteArray {
    h1 = h1 xor length.toLong()
    h2 = h2 xor length.toLong()

    h1 += h2
    h2 += h1

    h1 = fmix64(h1)
    h2 = fmix64(h2)

    h1 += h2
    h2 += h1

    return ByteBuffer.wrap(ByteArray(CHUNK_SIZE))
      .order(ByteOrder.LITTLE_ENDIAN)
      .putLong(h1)
      .putLong(h2)
      .array()
  }

  private fun fmix64(k: Long): Long {
    var result = k
    result = result xor result.ushr(33)
    result *= -49064778989728563L
    result = result xor result.ushr(33)
    result *= -4265267296055464877L
    result = result xor result.ushr(33)
    return result
  }
}