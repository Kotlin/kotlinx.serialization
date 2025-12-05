[//]: # (title: JSON serialization overview)

The Kotlin serialization library allows you to easily convert Kotlin objects to JSON and back.
The [`Json`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json/) class is the primary tool for this, offering flexibility in how JSON is generated and parsed.
You can configure `Json` instances to handle specific JSON behaviors or use its default instance for basic tasks.

With the `Json` class, you can:

* Serialize Kotlin objects to JSON strings using the [`encodeToString()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json/encode-to-string.html) function.
* Deserialize JSON strings back to Kotlin objects with the [`decodeFromString()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json/decode-from-string.html) function.
* [Work directly with the `JsonElement`](serialization-json-elements.md) when handling complex JSON structures using the [`encodeToJsonElement()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/encode-to-json-element.html) and the [`decodeFromJsonElement()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/decode-from-json-element.html) functions.
* Use [Experimental](components-stability.md#stability-levels-explained) extension functions to [serialize and deserialize I/O sources](serialization-json-io-sources.md) without creating intermediate strings, including JVM streams as well as [`kotlinx-io`](https://github.com/Kotlin/kotlinx-io) and [Okio](https://square.github.io/okio/) types.

Before you start, import the following declarations from the serialization library:

```kotlin
import kotlinx.serialization.*
import kotlinx.serialization.json.*
```

Here's a simple example that uses the default `Json` instance to show how JSON serialization works in Kotlin:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
data class User(val name: String, val age: Int)

fun main() {
    // Uses the default Json instance
    val json = Json

    // Creates a User object
    val user = User("Alice", 30)

    // Converts the User object to a JSON string
    val jsonString = json.encodeToString(user)
    println(jsonString)
    // {"name":"Alice","age":30}

    // Converts the JSON string back to a User object
    val deserializedUser = json.decodeFromString<User>(jsonString)
    println(deserializedUser)
    // User(name=Alice, age=30)
//sampleEnd
}
```
{kotlin-runnable="true"}

In addition to using the default configuration, you can [customize the `Json` instance](serialization-json-configuration.md) for specific different use cases,
such as ignoring unknown keys:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
data class Project(val name: String)

// Configures a Json instance to ignore unknown keys
val customJson = Json {
    ignoreUnknownKeys = true
}

fun main() {
    val data = customJson.decodeFromString<Project>("""
        {"name":"kotlinx.serialization","language":"Kotlin"}
    """)
    println(data)
    // Project(name=kotlinx.serialization)
}
//sampleEnd
```
{kotlin-runnable="true"}

## What's next

* Learn how to [customize `Json` instances](serialization-json-configuration.md) to address different use cases for serialization and deserialization.
* Explore [advanced JSON element handling](serialization-json-elements.md) to manipulate and work with JSON data before it is parsed or serialized.
* Discover how to [transform JSON during serialization and deserialization](serialization-transform-json.md) for more control over your data.
