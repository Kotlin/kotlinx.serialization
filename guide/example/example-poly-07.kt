// This file was automatically generated from polymorphism.md by Knit tool. Do not edit.
package example.examplePoly07

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
sealed class Response
                      
@Serializable
object EmptyResponse : Response()

@Serializable   
class TextResponse(val text: String) : Response()   

fun main() {
    val list = listOf(EmptyResponse, TextResponse("OK"))
    println(Json.encodeToString(list))
}  
