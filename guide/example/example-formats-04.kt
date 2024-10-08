// This file was automatically generated from formats.md by Knit tool. Do not edit.
package example.exampleFormats04

import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*

fun ByteArray.toAsciiHexString() = joinToString("") {
    if (it in 32..127) it.toInt().toChar().toString() else
        "{${it.toUByte().toString(16).padStart(2, '0').uppercase()}}"
}

@Serializable
data class Project(val name: String, val language: String)

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val data = Project("kotlinx.serialization", "Kotlin") 
    val bytes = ProtoBuf.encodeToByteArray(data)   
    println(bytes.toAsciiHexString())
    val obj = ProtoBuf.decodeFromByteArray<Project>(bytes)
    println(obj)
}
