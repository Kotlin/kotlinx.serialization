// This file was automatically generated from serialization-json-configuration.md by Knit tool. Do not edit.
package example.exampleJson10

val format = Json { allowSpecialFloatingPointValues = true }

@Serializable
class Data(
    val value: Double
)

fun main() {
    val data = Data(Double.NaN)
    println(format.encodeToString(data))
}
