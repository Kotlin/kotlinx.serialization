// This file was automatically generated from serializers.md by Knit tool. Do not edit.
package example.exampleSerializer03

import kotlinx.serialization.*

@Serializable
@SerialName("Color")
class Color(val rgb: Int)

@Serializable           
@SerialName("Box")
class Box<T>(val contents: T)    

fun main() {
    val boxedColorSerializer = Box.serializer(Color.serializer())
    println(boxedColorSerializer.descriptor)
} 
