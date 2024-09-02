// This file was automatically generated from serialization-customization-options.md by Knit tool. Do not edit.
package example.exampleClasses05

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
// The 'owner' property references another serializable class `User`
class Project(val name: String, val owner: User)

// The referenced class must also be annotated with `@Serializable`
@Serializable
class User(val name: String)

fun main() {
    val owner = User("kotlin")
    val data = Project("kotlinx.serialization", owner)
    println(Json.encodeToString(data))
    // {"name":"kotlinx.serialization","owner":{"name":"kotlin"}}
}
