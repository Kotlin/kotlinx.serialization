// This file was automatically generated from alternative-serialization-formats.md by Knit tool. Do not edit.
package example.exampleFormats02

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.cbor.*

// Sets ignoreUnknownKeys to true to allow unknown keys during deserialization
val format = Cbor { ignoreUnknownKeys = true }

@Serializable
data class Project(val name: String)

fun main() {
    val data = format.decodeFromHexString<Project>(
        // CBOR hex notation input with an extra, unknown "language" key
        "bf646e616d65756b6f746c696e782e73657269616c697a6174696f6e686c616e6775616765664b6f746c696eff"
    )
    // Prints the deserialized Project object, ignoring the language property
    println(data)
    // Project(name=kotlinx.serialization)
}
