// This file was automatically generated from formats.md by Knit tool. Do not edit.
package example.exampleFormats06

import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*

fun ByteArray.toAsciiHexString() = joinToString("") {
    if (it in 32..127) it.toChar().toString() else
        "{${it.toUByte().toString(16).padStart(2, '0').toUpperCase()}}"
}

@Serializable
data class Data(
    val a: List<Int> = emptyList(),
    val b: List<Int> = emptyList()
)

fun main() {
    val data = Data(listOf(1, 2, 3), listOf())
    val bytes = ProtoBuf.encodeToByteArray(data)
    println(bytes.toAsciiHexString())
    println(ProtoBuf.decodeFromByteArray<Data>(bytes))
}
