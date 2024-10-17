// This file was automatically generated from alternative-serialization-formats.md by Knit tool. Do not edit.
package example.exampleFormats01

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.cbor.*

fun ByteArray.toAsciiHexString() = joinToString("") {
    // // Converts bytes to ASCII characters if printable, otherwise shows their hexadecimal value
    if (it in 32..127) it.toInt().toChar().toString() else
        "{${it.toUByte().toString(16).padStart(2, '0').uppercase()}}"
}

@Serializable
data class Project(val name: String, val language: String)

fun main() {
    val data = Project("kotlinx.serialization", "Kotlin")
    
    // Serializes the object into a CBOR binary array
    val bytes = Cbor.encodeToByteArray(data)

    // Converts the binary array to a human-readable hex string
    println(bytes.toAsciiHexString())
    // {BF}dnameukotlinx.serializationhlanguagefKotlin{FF}
    
    // Deserializes the binary array back into a Project object
    val obj = Cbor.decodeFromByteArray<Project>(bytes)
    println(obj)
    // Project(name=kotlinx.serialization, language=Kotlin)
}
