package sample

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class EmptyClassB : EmptyBase()


@Serializable
open class Car : Vehicle() {
    var maxSpeed: Int = 100

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Car) return false
        if (name != other.name) return false
        if (color != other.color) return false
        if (maxSpeed != other.maxSpeed) return false

        return true
    }

    override fun hashCode(): Int {
        return maxSpeed.hashCode()
    }

    override fun toString(): String {
        return "Car(name=$name, color=$color, maxSpeed=$maxSpeed)"
    }
}

@Serializable
data class TestSnippet(
    @SerialName("experiments") val experiments: List<String>
) : Snippet("test", "aaa")

@Serializable
data class ScreenSnippet(
    @SerialName("name") val name: String,
    @SerialName("uuid") val uuid: String? = null,
    @SerialName("source") val source: String? = null
) : Snippet("screen", "aaa")

@Serializable
class NotInConstructorTest : NotInConstructorBase() {
    val c = "val c"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NotInConstructorTest) return false

        if (a != other.a) return false
        if (b != other.b) return false
        if (c != other.c) return false

        return true
    }

    override fun hashCode(): Int {
        return a.hashCode() * 31 + b.hashCode() * 31 + c.hashCode()
    }
}
