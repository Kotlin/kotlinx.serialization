// This file was automatically generated from formats.md by Knit tool. Do not edit.
package example.exampleFormats07

import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*

fun ByteArray.toAsciiHexString() = joinToString("") {
    if (it in 32..127) it.toInt().toChar().toString() else
        "{${it.toUByte().toString(16).padStart(2, '0').uppercase()}}"
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
class Data(
    @ProtoType(ProtoIntegerType.DEFAULT)
    val a: Int,
    @ProtoType(ProtoIntegerType.SIGNED)
    val b: Int,
    @ProtoType(ProtoIntegerType.FIXED)
    val c: Int
)

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val data = Data(1, -2, 3) 
    println(ProtoBuf.encodeToByteArray(data).toAsciiHexString())
}
