[//]: # (title: JSON serialization overview)

JSON serialization in Kotlin allows you to easily convert Kotlin objects to JSON and back.
The [`Json`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json/) class is the primary tool for this, offering flexibility in how JSON is generated and parsed.
You can configure `Json` instances to handle specific JSON behaviors or use it as is for basic tasks.

The key features include:

* Serialization of Kotlin objects to JSON strings using `Json.encodeToString`. 
* Deserialization of JSON strings back into Kotlin objects with `Json.decodeFromString`. 
* Working directly with `JsonElement` for more complex JSON structures using `Json.encodeToJsonElement` and `Json.decodeFromJsonElement`.

Import the necessary libraries to use the `Json` class for JSON serialization and deserialization:

```kotlin
import kotlinx.serialization.*
import kotlinx.serialization.json.*
```

Let's look at a simple example:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.Json

@Serializable
data class User(val name: String, val age: Int)

fun main() {
    // Creates a Json instance with default settings
    val json = Json {}

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
}
```

Additionally, you can [customize the `Json` instance](serialization-json-configuration.md) to handle specific needs, such as ignoring unknown keys:

```kotlin
// Configures a Json instance to ignore unknown keys
val customJson = Json {
    ignoreUnknownKeys = true
}
```

## What's next?

* Learn how to [customize JSON serialization settings](serialization-json-configuration.md) to fit your specific needs.
* Explore [advanced JSON element handling](serialization-json-elements.md) to manipulate and work with JSON data before it is parsed or serialized.
* Discover how to [transform JSON during serialization and deserialization](serialization-transform-json.md) for more control over your data.