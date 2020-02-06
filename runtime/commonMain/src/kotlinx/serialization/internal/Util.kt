/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.io.*
import kotlinx.serialization.*

@InternalSerializationApi
public fun InputStream.readExactNBytes(bytes: Int): ByteArray {
    val array = ByteArray(bytes)
    var read = 0
    while (read < bytes) {
        val i = this.read(array, read, bytes - read)
        if (i == -1) error("Unexpected EOF")
        read += i
    }
    return array
}

@PublishedApi // reified BinaryFormat.dumps/loads
internal object InternalHexConverter {
    private const val hexCode = "0123456789ABCDEF"

    fun parseHexBinary(s: String): ByteArray {
        val len = s.length
        require(len % 2 == 0) { "HexBinary string must be even length" }
        val bytes = ByteArray(len / 2)
        var i = 0

        while (i < len) {
            val h = hexToInt(s[i])
            val l = hexToInt(s[i + 1])
            require(!(h == -1 || l == -1)) { "Invalid hex chars: ${s[i]}${s[i+1]}" }

            bytes[i / 2] = ((h shl 4) + l).toByte()
            i += 2
        }

        return bytes
    }

    private fun hexToInt(ch: Char): Int = when (ch) {
        in '0'..'9' -> ch - '0'
        in 'A'..'F' -> ch - 'A' + 10
        in 'a'..'f' -> ch - 'a' + 10
        else -> -1
    }

    fun printHexBinary(data: ByteArray, lowerCase: Boolean = false): String {
        val r = StringBuilder(data.size * 2)
        for (b in data) {
            r.append(hexCode[b.toInt() shr 4 and 0xF])
            r.append(hexCode[b.toInt() and 0xF])
        }
        return if (lowerCase) r.toString().toLowerCase() else r.toString()
    }

    fun toHexString(n: Int): String {
        val arr = ByteArray(4)
        for (i in 0 until 4) {
            arr[i] = (n shr (24 - i * 8)).toByte()
        }
        return printHexBinary(arr, true).trimStart('0').takeIf { it.isNotEmpty() } ?: "0"
    }
}

@InternalSerializationApi
@Deprecated(
    level = DeprecationLevel.WARNING,
    message = "HexConverter slipped into public API surface accidentally and will be removed in the future releases. " +
            "You can copy-paste it to your project or (better) find a polished implementation that initially was intended for public uses."
)
object HexConverter {
    private const val hexCode = "0123456789ABCDEF"
    fun parseHexBinary(s: String): ByteArray = InternalHexConverter.parseHexBinary(s)
    fun printHexBinary(data: ByteArray, lowerCase: Boolean = false): String = InternalHexConverter.printHexBinary(data, lowerCase)
    fun toHexString(n: Int): String = InternalHexConverter.toHexString(n)
}

internal fun SerialDescriptor.cachedSerialNames(): Set<String> {
    @Suppress("DEPRECATION_ERROR")
    if (this is PluginGeneratedSerialDescriptor) return namesSet
    val result = HashSet<String>(elementsCount)
    for (i in 0 until elementsCount) {
        result += getElementName(i)
    }
    return result
}

/**
 * Returns serial descriptor that delegates all the calls to descriptor returned by [deferred] block.
 * Used to resolve cyclic dependencies between recursive serializable structures.
 */
internal fun defer(deferred: () -> SerialDescriptor): SerialDescriptor = object : SerialDescriptor {

    private val original: SerialDescriptor by lazy(deferred)

    override val serialName: String
        get() = original.serialName
    override val kind: SerialKind
        get() = original.kind
    override val elementsCount: Int
        get() = original.elementsCount

    override fun getElementName(index: Int): String = original.getElementName(index)
    override fun getElementIndex(name: String): Int = original.getElementIndex(name)
    override fun getElementAnnotations(index: Int): List<Annotation> = original.getElementAnnotations(index)
    override fun getElementDescriptor(index: Int): SerialDescriptor = original.getElementDescriptor(index)
    override fun isElementOptional(index: Int): Boolean = original.isElementOptional(index)
}
