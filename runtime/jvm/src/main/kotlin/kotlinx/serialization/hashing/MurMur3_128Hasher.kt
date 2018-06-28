package kotlinx.serialization.hashing

import com.google.common.primitives.UnsignedBytes.*
import kotlinx.serialization.*
import java.nio.*

class MurMur3_128Hasher : ElementValueOutput(), Hasher {

    private companion object {
        const val CHUNK_SIZE = 16
        const val C1: Long = -8663945395140668459L
        const val C2: Long = 5545529020109919103L
    }

    override val output: KOutput
        get() = this

    private var h1: Long = 0
    private var h2: Long = 0
    private var length: Int = 0
    private var rootDescriptor : KSerialClassDesc? = null

    private val buffer: ByteBuffer = ByteBuffer.allocate(CHUNK_SIZE + 7).order(ByteOrder.LITTLE_ENDIAN)

    override fun writeBegin(desc: KSerialClassDesc, vararg typeParams: KSerializer<*>): KOutput {
        if (rootDescriptor == null) {
            rootDescriptor = desc
        }

        return this
    }

    override fun writeIntValue(value: Int) {
        buffer.putInt(value)
        munchIfFull()
    }

    override fun writeLongValue(value: Long) {
        buffer.putLong(value)
        munchIfFull()
    }

    override fun writeStringValue(value: String) {
        for (byte in value.toByteArray(Charsets.UTF_8)) {
            writeByteValue(byte)
        }
    }

    override fun writeByteValue(value: Byte) {
        buffer.put(value)
        munchIfFull()
    }

    override fun writeEnd(desc: KSerialClassDesc) {
        if (desc !== rootDescriptor) {
            return
        }

        munch()
        buffer.flip()
        if (buffer.remaining() > 0) {
            processRemaining(buffer)
            buffer.position(buffer.limit())
        }
    }

    override fun makeLongHash(): Long = pad(makeByteArrayHash())

    override fun makeByteArrayHash(): ByteArray {
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

    override fun reset() {
        h1 = 0
        h2 = 0
        length = 0
        buffer.clear()
    }

    private fun processRemaining(bb: ByteBuffer) {
        var k1: Long = 0
        var k2: Long = 0
        length += bb.remaining()
        when (bb.remaining()) {
            15 -> {
                k2 = k2 xor (toInt(bb.get(14)).toLong() shl 48) // fall through
                k2 = k2 xor (toInt(bb.get(13)).toLong() shl 40) // fall through
                k2 = k2 xor (toInt(bb.get(12)).toLong() shl 32) // fall through
                k2 = k2 xor (toInt(bb.get(11)).toLong() shl 24) // fall through
                k2 = k2 xor (toInt(bb.get(10)).toLong() shl 16) // fall through
                k2 = k2 xor (toInt(bb.get(9)).toLong() shl 8) // fall through
                k2 = k2 xor toInt(bb.get(8)).toLong() // fall through
                k1 = k1 xor bb.long
            }
            14 -> {
                k2 = k2 xor (toInt(bb.get(13)).toLong() shl 40)
                k2 = k2 xor (toInt(bb.get(12)).toLong() shl 32)
                k2 = k2 xor (toInt(bb.get(11)).toLong() shl 24)
                k2 = k2 xor (toInt(bb.get(10)).toLong() shl 16)
                k2 = k2 xor (toInt(bb.get(9)).toLong() shl 8)
                k2 = k2 xor toInt(bb.get(8)).toLong()
                k1 = k1 xor bb.long
            }
            13 -> {
                k2 = k2 xor (toInt(bb.get(12)).toLong() shl 32)
                k2 = k2 xor (toInt(bb.get(11)).toLong() shl 24)
                k2 = k2 xor (toInt(bb.get(10)).toLong() shl 16)
                k2 = k2 xor (toInt(bb.get(9)).toLong() shl 8)
                k2 = k2 xor toInt(bb.get(8)).toLong()
                k1 = k1 xor bb.long
            }
            12 -> {
                k2 = k2 xor (toInt(bb.get(11)).toLong() shl 24)
                k2 = k2 xor (toInt(bb.get(10)).toLong() shl 16)
                k2 = k2 xor (toInt(bb.get(9)).toLong() shl 8)
                k2 = k2 xor toInt(bb.get(8)).toLong()
                k1 = k1 xor bb.long
            }
            11 -> {
                k2 = k2 xor (toInt(bb.get(10)).toLong() shl 16)
                k2 = k2 xor (toInt(bb.get(9)).toLong() shl 8)
                k2 = k2 xor toInt(bb.get(8)).toLong()
                k1 = k1 xor bb.long
            }
            10 -> {
                k2 = k2 xor (toInt(bb.get(9)).toLong() shl 8)
                k2 = k2 xor toInt(bb.get(8)).toLong()
                k1 = k1 xor bb.long
            }
            9 -> {
                k2 = k2 xor toInt(bb.get(8)).toLong()
                k1 = k1 xor bb.long
            }
            8 -> k1 = k1 xor bb.long
            7 -> {
                k1 = k1 xor (toInt(bb.get(6)).toLong() shl 48) // fall through
                k1 = k1 xor (toInt(bb.get(5)).toLong() shl 40) // fall through
                k1 = k1 xor (toInt(bb.get(4)).toLong() shl 32) // fall through
                k1 = k1 xor (toInt(bb.get(3)).toLong() shl 24) // fall through
                k1 = k1 xor (toInt(bb.get(2)).toLong() shl 16) // fall through
                k1 = k1 xor (toInt(bb.get(1)).toLong() shl 8) // fall through
                k1 = k1 xor toInt(bb.get(0)).toLong()
            }
            6 -> {
                k1 = k1 xor (toInt(bb.get(5)).toLong() shl 40)
                k1 = k1 xor (toInt(bb.get(4)).toLong() shl 32)
                k1 = k1 xor (toInt(bb.get(3)).toLong() shl 24)
                k1 = k1 xor (toInt(bb.get(2)).toLong() shl 16)
                k1 = k1 xor (toInt(bb.get(1)).toLong() shl 8)
                k1 = k1 xor toInt(bb.get(0)).toLong()
            }
            5 -> {
                k1 = k1 xor (toInt(bb.get(4)).toLong() shl 32)
                k1 = k1 xor (toInt(bb.get(3)).toLong() shl 24)
                k1 = k1 xor (toInt(bb.get(2)).toLong() shl 16)
                k1 = k1 xor (toInt(bb.get(1)).toLong() shl 8)
                k1 = k1 xor toInt(bb.get(0)).toLong()
            }
            4 -> {
                k1 = k1 xor (toInt(bb.get(3)).toLong() shl 24)
                k1 = k1 xor (toInt(bb.get(2)).toLong() shl 16)
                k1 = k1 xor (toInt(bb.get(1)).toLong() shl 8)
                k1 = k1 xor toInt(bb.get(0)).toLong()
            }
            3 -> {
                k1 = k1 xor (toInt(bb.get(2)).toLong() shl 16)
                k1 = k1 xor (toInt(bb.get(1)).toLong() shl 8)
                k1 = k1 xor toInt(bb.get(0)).toLong()
            }
            2 -> {
                k1 = k1 xor (toInt(bb.get(1)).toLong() shl 8)
                k1 = k1 xor toInt(bb.get(0)).toLong()
            }
            1 -> k1 = k1 xor toInt(bb.get(0)).toLong()
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

    private fun fmix64(k: Long): Long {
        var result = k
        result = result xor result.ushr(33)
        result *= -49064778989728563L
        result = result xor result.ushr(33)
        result *= -4265267296055464877L
        result = result xor result.ushr(33)
        return result
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
        for (i in 1 until Math.min(arr.size, 8)) {
            retVal = retVal or (arr[i].toLong() and 0xFFL shl i * 8)
        }
        return retVal
    }
}
