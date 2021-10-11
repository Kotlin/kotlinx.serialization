// This file was automatically generated from formats.md by Knit tool. Do not edit.
package example.exampleFormats02

import kotlinx.serialization.*
import kotlinx.serialization.cbor.*

val format = Cbor { ignoreUnknownKeys = true }

@Serializable
data class Project(val name: String)

fun main() {
    val data = format.decodeFromHexString<Project>(
        "bf646e616d65756b6f746c696e782e73657269616c697a6174696f6e686c616e6775616765664b6f746c696eff"
    )
    println(data)
}
