// This file was automatically generated from serializers.md by Knit tool. Do not edit.
package example.exampleSerializer04

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*

fun main() {
    val intSerializer: KSerializer<Int> = Int.serializer()
    println(intSerializer.descriptor)
}
