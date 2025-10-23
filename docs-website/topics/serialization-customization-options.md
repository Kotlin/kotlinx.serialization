[//]: # (title: Serialize classes)

The [`@Serializable`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-serializable/) annotation in Kotlin enables the serialization of all properties in classes defined by the primary constructor.
You can further customize this behavior to fit your specific needs.
This page covers various serialization techniques, showing you how to specify which properties are serialized and how the serialization process is managed.

Before starting, make sure you have the [necessary library dependencies imported](serialization-get-started.md).

## The @Serializable annotation

The `@Serializable` annotation enables the automatic serialization of class properties,
allowing a class to be converted to and from formats such as JSON.

In Kotlin, only properties with backing fields are serialized.
Properties defined with [accessors](properties.md) or delegated properties without backing fields are excluded from serialization:

```kotlin
// Imports declarations from the serialization and JSON handling libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
class Project(
    // Declares a property with a backing field - serialized
    var name: String
) {
    // Declares a property with a backing field - serialized
    var stars: Int = 0

    // Defines a getter-only property without a backing field - not serialized
    val path: String
        get() = "kotlin/$name"

    // Declares a delegated property - not serialized
    var id by ::name
}

fun main() {
    val data = Project("kotlinx.serialization").apply { stars = 9000 }
    // Prints only the name and the stars properties
    println(Json.encodeToString(data))
    // {"name":"kotlinx.serialization","stars":9000}
}
//sampleEnd
```
{kotlin-runnable="true"}

Kotlin Serialization natively supports nullable properties.
Like [other _default values_](#set-default-values-for-optional-properties), `null` values aren't encoded in the JSON output:

```kotlin
// Imports declarations from the serialization and JSON handling libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
// Declares a class with the renamedTo nullable property that has a null default value
class Project(val name: String, val renamedTo: String? = null)

fun main() {
    val data = Project("kotlinx.serialization")
    // The renamedTo property isn't encoded because its value is null
    println(Json.encodeToString(data))
    // {"name":"kotlinx.serialization"}
}
//sampleEnd
```
{kotlin-runnable="true"}

Additionally, Kotlin's [null safety](null-safety.md) is strongly enforced during deserialization.
If a JSON object contains a `null` value for a non-nullable property, an exception is thrown
even when the property has a default value:

```kotlin
// Imports declarations from the serialization and JSON handling libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
data class Project(val name: String, val language: String = "Kotlin")

fun main() {
    val data = Json.decodeFromString<Project>("""
        {"name":"kotlinx.serialization","language":null}
    """)
    println(data)
    // JsonDecodingException
}
//sampleEnd
```
{kotlin-runnable="true" validate="false"}

> If you need to handle `null` values from third-party JSON, you can [coerce them to a default value](serialization-json-configuration.md#coerce-input-values).
>
{style="tip"}

Similarly, when the input contains an [optional property](#set-default-values-for-optional-properties) (a property with a default value), its initializer isn't called.
For this reason, avoid using code with side effects in property initializers.

Here's an example:

```kotlin
// Imports declarations from the serialization and JSON handling libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
fun computeLanguage(): String {
    println("Computing")
    return "Kotlin"
}

@Serializable
// Skips the initializer if language is in the input
data class Project(val name: String, val language: String = computeLanguage())

fun main() {
    val data = Json.decodeFromString<Project>("""
        {"name":"kotlinx.serialization","language":"Java"}
    """)
    println(data)
    // Project(name=kotlinx.serialization, language=Java)
}
//sampleEnd
```
{kotlin-runnable="true"}

In this example, since the `language` property is specified in the input, the `Computing` string isn't printed
in the output.

### Serialization of class references

Serializable classes can contain properties that reference other classes.
The referenced classes must also be annotated with `@Serializable`.
When encoded to JSON, this results in a nested JSON object:

```kotlin
// Imports declarations from the serialization and JSON handling libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
// The owner property references another serializable class User
class Project(val name: String, val owner: User)

// The referenced class must also be annotated with @Serializable
@Serializable
class User(val name: String)

fun main() {
    val owner = User("kotlin")
    val data = Project("kotlinx.serialization", owner)
    println(Json.encodeToString(data))
    // {"name":"kotlinx.serialization","owner":{"name":"kotlin"}}
}
//sampleEnd
```
{kotlin-runnable="true"}

> To reference non-serializable classes, annotate the corresponding properties with [`@Transient`](#exclude-properties-with-the-transient-annotation), or
> provide a [custom serializer](create-custom-serializers.md) for them.
>
{style="tip"}

### Serialization of repeated object references

Kotlin Serialization is designed to encode and decode plain data. It doesn't support reconstruction
of arbitrary object graphs with repeated object references.
For example, when serializing an object that references the same instance twice, it's simply encoded twice:

```kotlin
// Imports declarations from the serialization and JSON handling libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
class Project(val name: String, val owner: User, val maintainer: User)

@Serializable
class User(val name: String)

fun main() {
    val owner = User("kotlin")
    // References owner twice
    val data = Project("kotlinx.serialization", owner, owner)
    println(Json.encodeToString(data))
    // {"name":"kotlinx.serialization","owner":{"name":"kotlin"},"maintainer":{"name":"kotlin"}}
}
//sampleEnd
```
{kotlin-runnable="true"}

> If you attempt to serialize a circular structure, it results in stack overflow.
> To exclude references from serialization, use [the @Transient annotation](#exclude-properties-with-the-transient-annotation).
>
{style="tip"}

### Generic class serialization

Generic classes in Kotlin support type-polymorphism, which is enforced by Kotlin Serialization at
compile-time. For example, consider a generic serializable class `Box<T>`:

```kotlin
// Imports declarations from the serialization and JSON handling libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
// The Box<T> class can be used with built-in types like Int
// Alternatively, with user-defined types like Project
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
//sampleEnd
```
{kotlin-runnable="true"}

When you serialize a generic class like `Box<T>`, the JSON output depends on the actual type you specify for `T` at compile time.
If that type isn't serializable, you get a compile-time error.

## Customize serialization behavior

Kotlin Serialization provides several ways to modify how classes are serialized.
This section covers techniques for customizing property names, managing default values, and more.

### Customize serial names

By default, property names in the serialization output, such as JSON, match their names in the source code.

You can customize these names, called _serial names_,
with the [`@SerialName`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-serial-name/) annotation.
Use it to make a property name shorter or more descriptive in the serialized output:

```kotlin
// Imports declarations from the serialization and JSON handling libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
// Abbreviates the language property to lang using @SerialName
class Project(val name: String, @SerialName("lang") val language: String)

fun main() {
    val data = Project("kotlinx.serialization", "Kotlin")
    // Prints the abbreviated name in the JSON output
    println(Json.encodeToString(data))
    // {"name":"kotlinx.serialization","lang":"Kotlin"}
}
//sampleEnd
```
{kotlin-runnable="true"}

### Define constructor properties for serialization

A class annotated with `@Serializable` must declare all parameters in its primary constructor as properties.

If you need to perform additional initialization logic before assigning values to properties, use a secondary constructor.
The primary constructor can remain private and handle property initialization.

Here's an example where the secondary constructor parses a string into two values, which are then passed to the primary constructor for serialization:

```kotlin
// Imports declarations from the serialization and JSON handling libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
class Project private constructor(val owner: String, val name: String) {
    // Creates a Project object using a path string
    constructor(path: String) : this(
        owner = path.substringBefore('/'),
        name = path.substringAfter('/')
    )

    val path: String
        get() = "$owner/$name"
}
fun main() {
    println(Json.encodeToString(Project("kotlin/kotlinx.serialization")))
    // {"owner":"kotlin","name":"kotlinx.serialization"}
}
//sampleEnd
```
{kotlin-runnable="true"}

### Validate data in the primary constructor

You can validate constructor parameters before storing it in properties to ensure that a class is serializable and invalid data is rejected during deserialization.

Use an `init` block to perform this validation:

```kotlin
// Imports declarations from the serialization and JSON handling libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
class Project(val name: String) {
    // Validates that the name is not empty
    init {
        require(name.isNotEmpty()) { "name cannot be empty" }
    }
}

fun main() {
    val data = Json.decodeFromString<Project>("""
        {"name":""}
    """)
    println(data)
    // Exception in thread "main" java.lang.IllegalArgumentException: name cannot be empty
}
//sampleEnd
```
{kotlin-runnable="true"}

### Set default values for optional properties

In Kotlin, an object can be deserialized only when all its properties are present in the input.

You can set a _default value_ to a property to make it optional for serialization. 
The default value is used when no value is provided in the input.

Here's an example:

```kotlin
// Imports declarations from the serialization and JSON handling libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
// Sets a default value for the optional language property
data class Project(val name: String, val language: String = "Kotlin")

fun main() {
    val data = Json.decodeFromString<Project>("""
        {"name":"kotlinx.serialization"}
    """)
    println(data)
    // Project(name=kotlinx.serialization, language=Kotlin)
}
//sampleEnd
```
{kotlin-runnable="true"}

### Make properties required with the `@Required` annotation

You can annotate a property with [`@Required`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-required/) to make it required in the input.
This enforces that the input contains the property, even if it has a default value:

```kotlin
// Imports declarations from the serialization and JSON handling libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.Required

//sampleStart
@Serializable
// Marks the language property as required
data class Project(val name: String, @Required val language: String = "Kotlin")

fun main() {
    val data = Json.decodeFromString<Project>("""
        {"name":"kotlinx.serialization"}
    """)
    println(data)
    // MissingFieldException
}
//sampleEnd
```
{kotlin-runnable="true"}

### Exclude properties with the `@Transient` annotation

You can exclude a property from serialization with the [`@Transient`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-transient/) annotation.
Transient properties must have a default value.

If you explicitly specify a value for a transient property in the input, even if it matches the default value, a `JsonDecodingException` is thrown:

```kotlin
// Imports declarations from the serialization and JSON handling libraries
import kotlinx.serialization.*
import kotlinx.serialization.Transient
import kotlinx.serialization.json.*

//sampleStart
@Serializable
// Excludes the language property from serialization
data class Project(val name: String, @Transient val language: String = "Kotlin")

fun main() {
    // Throws an exception even though input matches the default value
    val data = Json.decodeFromString<Project>("""
        {"name":"kotlinx.serialization","language":"Kotlin"}
    """)
    println(data)
    // JsonDecodingException
}
//sampleEnd
```
{kotlin-runnable="true" validate="false"}

> To avoid exceptions from unknown keys in JSON, including those marked with the `@Transient` annotation, enable the [`ignoreUnknownKeys`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/ignore-unknown-keys.html) configuration property.
> For more information, see the [Ignore unknown keys](serialization-json-configuration.md#ignore-unknown-keys) section.
>
{style="tip"}

### Manage the serialization of default properties with `@EncodedDefault`

By default, JSON serialization excludes properties that have default values.
This reduces the size of the serialized data and avoids unnecessary visual clutter.

Here's an example where the `language` property is excluded from the output because its value equals the default:

```kotlin
// Imports declarations from the serialization and JSON handling libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
data class Project(val name: String, val language: String = "Kotlin")

fun main() {
    val data = Project("kotlinx.serialization")
    println(Json.encodeToString(data))
    // {"name":"kotlinx.serialization"}
}
//sampleEnd
```
{kotlin-runnable="true"}

> Learn more about configuring this JSON behavior in the [Encode default values](serialization-json-configuration.md#encode-default-values) section.
>
{style="tip"}

To always serialize a property, regardless of its value or format settings, use the [`@EncodeDefault`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-encode-default/) annotation.
You can also change this behavior by setting the [`EncodeDefault.Mode`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-encode-default/-mode/) parameter.

Let's look at an example, where the `language` property is always included in the serialized output,
while the `projects` property is excluded when it's an empty list:

```kotlin
// Imports declarations from the serialization and JSON handling libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
data class Project(
    val name: String,
    // Always includes the language property in the serialized output
    // even if it has the default value "Kotlin"
    @EncodeDefault val language: String = "Kotlin"
)

@Serializable
data class User(
    val name: String,
    // Excludes projects when it’s an empty list, even if it has a default value
    @EncodeDefault(EncodeDefault.Mode.NEVER) val projects: List<Project> = emptyList()
)

fun main() {
    val userA = User("Alice", listOf(Project("kotlinx.serialization")))
    val userB = User("Bob")
    // Serializes projects because it contains a value
    // language is always serialized
    println(Json.encodeToString(userA))
    // {"name":"Alice","projects":[{"name":"kotlinx.serialization","language":"Kotlin"}]}

    // Excludes projects because it's an empty list
    // and EncodeDefault.Mode is set to NEVER, so it's not serialized
    println(Json.encodeToString(userB))
    // {"name":"Bob"}
}
//sampleEnd
```
{kotlin-runnable="true"}

## What's next

* Explore more complex JSON serialization scenarios in the [JSON serialization overview](configure-json-serialization.md).
* Learn how to define and customize your own serializers in [Create custom serializers](serialization-custom-serializers.md).
