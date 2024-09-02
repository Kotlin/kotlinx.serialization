// This file was automatically generated from serialization-customization-options.md by Knit tool. Do not edit.
package example.exampleClasses01

@Serializable
class Project(
    // name is a property with backing field -- serialized
    var name: String
) {
    // stars is property with a backing field -- serialized
    var stars: Int = 0

    // path is getter only, no backing field -- not serialized
    val path: String
        get() = "kotlin/$name"

    // id is a delegated property -- not serialized
    var id by ::name
}

fun main() {
    val data = Project("kotlinx.serialization").apply { stars = 9000 }
    // Only the name and the stars properties are present in the JSON output.
    println(Json.encodeToString(data))
    // {"name":"kotlinx.serialization","stars":9000}
}

@Serializable
// The 'renamedTo' property is nullable and defaults to null, and it's not encoded
class Project(val name: String, val renamedTo: String? = null)

fun main() {
    val data = Project("kotlinx.serialization")
    println(Json.encodeToString(data))
    // {"name":"kotlinx.serialization"}
}

@Serializable
data class Project(val name: String, val language: String = "Kotlin")

fun main() {
    val data = Json.decodeFromString<Project>("""
        {"name":"kotlinx.serialization","language":null}
    """)
    println(data)
    // JsonDecodingException
}

fun computeLanguage(): String {
    println("Computing")
    return "Kotlin"
}

@Serializable
// Initializer is skipped if `language` is in input
data class Project(val name: String, val language: String = computeLanguage())

fun main() {
    val data = Json.decodeFromString<Project>("""
        {"name":"kotlinx.serialization","language":"Java"}
    """)
    println(data)
    // Project(name=kotlinx.serialization, language=Java)
}

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

@Serializable
class Project(val name: String, val owner: User, val maintainer: User)

@Serializable
class User(val name: String)

fun main() {
    val owner = User("kotlin")
    // 'owner' is referenced twice
    val data = Project("kotlinx.serialization", owner, owner)
    println(Json.encodeToString(data))
    // {"name":"kotlinx.serialization","owner":{"name":"kotlin"},"maintainer":{"name":"kotlin"}}
}

@Serializable
// The `Box<T>` class can be used with built-in types like `Int`, or with user-defined types like `Project`.
class Box<T>(val contents: T)
@Serializable
data class Project(val name: String, val language: String)

@Serializable
class Data(
    val a: Box<Int>,
    val b: Box<Project>
)

fun main() {
    val data = Data(Box(42), Box(Project("kotlinx.serialization", "Kotlin")))
    println(Json.encodeToString(data))
    // {"a":{"contents":42},"b":{"contents":{"name":"kotlinx.serialization","language":"Kotlin"}}}
}

@Serializable
// The language property is abbreviated to lang using @SerialName
class Project(val name: String, @SerialName("lang") val language: String)

fun main() {
    val data = Project("kotlinx.serialization", "Kotlin")
    // In the JSON output, the abbreviated name lang is used instead of the full property name
    println(Json.encodeToString(data))
    // {"name":"kotlinx.serialization","lang":"Kotlin"}
}

@Serializable
class Project(val name: String, @SerialName("lang") val language: String)

fun main() {
    val data = Project("kotlinx.serialization", "Kotlin")
    println(Json.encodeToString(data))
}
