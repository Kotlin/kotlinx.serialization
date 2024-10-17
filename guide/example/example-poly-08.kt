// This file was automatically generated from serialization-polymorphism.md by Knit tool. Do not edit.
package example.examplePoly08

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
sealed class Response

// Declares an object that extends Response
@Serializable
object EmptyResponse : Response()

// Declares a class that extends Response
@Serializable   
class TextResponse(val text: String) : Response()

// Serializes a list of different responses
fun main() {
    val list = listOf(EmptyResponse, TextResponse("OK"))
    println(Json.encodeToString(list))
    // [{"type":"EmptyResponse"},{"type":"TextResponse","text":"OK"}]
}
