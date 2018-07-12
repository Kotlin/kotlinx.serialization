import kotlinx.serialization.*
import kotlinx.serialization.json.*

fun main(args: Array<String>) {
    val myJson = JsonObject(mapOf("a" to JsonNull, "b" to JsonLiteral(42), "c" to JsonLiteral("my string")))
    println("Hello world from $myJson!")
}
