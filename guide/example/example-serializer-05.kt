// This file was automatically generated from serializers.md by Knit tool. Do not edit.
package example.exampleSerializer05

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*

fun main() {   
    val stringListSerializer: KSerializer<List<String>> = ListSerializer(String.serializer()) 
    println(stringListSerializer.descriptor)
}
