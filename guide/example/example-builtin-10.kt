// This file was automatically generated from serialization-serialize-builtin-types.md by Knit tool. Do not edit.
package example.exampleBuiltin10

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
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
    // No duplicate values in data.b property, because it is a Set
    println(data)
    // Data(a=[42, 42], b=[42])
}
