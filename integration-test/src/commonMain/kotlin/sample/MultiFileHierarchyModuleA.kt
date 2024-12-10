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

@Serializable
open class GenericBox<E> {
    var contents: Map<String, E>? = null
}

// From #1264
@Serializable
sealed class TypedSealedClass<T>(val a: T) {
    @Serializable
    class Child(val y: Int) : TypedSealedClass<String>("10") {
        override fun toString(): String = "Child($a, $y)"
    }
}

// From #KT-43910
@Serializable
open class ValidatableValue<T : Any, V: Any>(
    var value: T? = null,
    var error: V? = null,
)
