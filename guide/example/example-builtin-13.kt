// This file was automatically generated from builtin-classes.md by Knit tool. Do not edit.
package example.exampleBuiltin13

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
sealed class ParametrizedParent<out R> {
    @Serializable
    data class ChildWithoutParameter(val value: Int) : ParametrizedParent<Nothing>()
}

fun main() {
    println(Json.encodeToString(ParametrizedParent.ChildWithoutParameter(42)))
}
