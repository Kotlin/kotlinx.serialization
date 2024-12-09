// This file was automatically generated from json.md by Knit tool. Do not edit.
package example.exampleJson04

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@OptIn(ExperimentalSerializationApi::class) // JsonIgnoreUnknownKeys is an experimental annotation for now
@Serializable
@JsonIgnoreUnknownKeys
data class Outer(val a: Int, val inner: Inner)

@Serializable
data class Inner(val x: String)

fun main() {
    // 1
    println(Json.decodeFromString<Outer>("""{"a":1,"inner":{"x":"value"},"unknownKey":42}"""))
    println()
    // 2
    println(Json.decodeFromString<Outer>("""{"a":1,"inner":{"x":"value","unknownKey":"unknownValue"}}"""))
}
