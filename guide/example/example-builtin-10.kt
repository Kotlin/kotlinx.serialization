// This file was automatically generated from builtin-classes.md by Knit tool. Do not edit.
package example.exampleBuiltin10

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
data class Data(
    val a: List<Int>,
    val b: Set<Int>
)
     
fun main() {
    val data = Json.decodeFromString<Data>("""
        {
            "a": [42, 42],
            "b": [42, 42]
        }
    """)
    println(data)
}
