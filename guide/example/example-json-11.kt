// This file was automatically generated from serialization-json-configuration.md by Knit tool. Do not edit.
package example.exampleJson11

import kotlinx.serialization.*
import kotlinx.serialization.json.*

// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

// The @JsonClassDiscriminator annotation is inheritable, so all subclasses of Base will have the same discriminator
@Serializable
@JsonClassDiscriminator("message_type")
sealed class Base

// Class discriminator is inherited from Base
@Serializable
sealed class ErrorClass: Base()

// Defines a class that combines a message and an optional error
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
    // The discriminator from the Base class is used
    println(format.encodeToString(data))
    // {"message":{"message_type":"my.app.BaseMessage","message":"not found"},"error":{"message_type":"my.app.GenericError","error_code":404}}
}
