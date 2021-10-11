package sample

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
abstract class EmptyBase

@Serializable
class EmptyClassA : EmptyBase()

@Serializable
open class Vehicle {
    var name: String? = null
    var color: String? = null
}

@Serializable
abstract class Snippet(
    @SerialName("objectFieldName") val objectFieldName: String,
    @SerialName("aaa") val aaa: String
)

@Serializable
abstract class NotInConstructorBase {
    // b should precede a for testing
    val b = "val b"
    val a = "val a"
}
