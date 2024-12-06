// This file was automatically generated from json.md by Knit tool. Do not edit.
package example.exampleJson13

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@OptIn(ExperimentalSerializationApi::class) // JsonClassDiscriminator is an experimental annotation for now
@Serializable
@JsonClassDiscriminator("message_type")
sealed class Base

@Serializable // Class discriminator is inherited from Base
sealed class ErrorClass: Base()

@Serializable
data class Message(val message: Base, val error: ErrorClass?)

@Serializable
@SerialName("my.app.BaseMessage")
data class BaseMessage(val message: String) : Base()

@Serializable
@SerialName("my.app.GenericError")
data class GenericError(@SerialName("error_code") val errorCode: Int) : ErrorClass()


val format = Json { classDiscriminator = "#class" }

fun main() {
    val data = Message(BaseMessage("not found"), GenericError(404))
    println(format.encodeToString(data))
}
