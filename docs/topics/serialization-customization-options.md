<!--- TEST_NAME BasicSerializationTest -->
[//]: # (title: Serialize classes)

The [`@Serializable`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-serializable/) annotation in Kotlin enables the serialization of all properties in classes defined by the primary constructor.
You can further customize this behavior to fit your specific needs.
This section covers how to adjust serialization using various techniques to control which properties are serialized and
how the serialization process is managed.

To get started, make sure you have the necessary libraries imported. 
For setup instructions, see the [Get started with Kotlin serialization guide](serialization-get-started.md).

## The @Serializable annotation

The `@Serializable` annotation in Kotlin triggers the automatic serialization of class properties,
allowing classes to be easily converted to and from formats like JSON.

In Kotlin, only properties with backing fields are serialized.
This means that properties defined solely by getter/setter methods or delegated properties without backing fields are excluded from serialization:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
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
//sampleEnd
```
{kotlin-runnable="true"}

<!--- > You can get the full code [here](../../guide/example/example-classes-01.kt). -->

<!---
```text
{"name":"kotlinx.serialization","stars":9000}
```
-->

<!--- TEST LINES_START -->

Kotlin Serialization natively supports nullable properties.
Like [other defaults](#set-default-values-for-optional-properties), `null` values are not encoded in JSON:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
// The renamedTo property is nullable and defaults to null, and it's not encoded
class Project(val name: String, val renamedTo: String? = null)

fun main() {
    val data = Project("kotlinx.serialization")
    println(Json.encodeToString(data))
    // {"name":"kotlinx.serialization"}
}
//sampleEnd
```
{kotlin-runnable="true"}

<!--- > You can get the full code [here](../../guide/example/example-classes-02.kt). -->

<!---
```text
{"name":"kotlinx.serialization"}
```
-->

<!--- TEST -->

Additionally, the type safety of Kotlin is strongly enforced.
If a `null` value is encountered in a JSON object for a non-nullable Kotlin property,
even if the property has a default value, an exception is raised:

```kotlin
// Imports the necessary libraries
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
{kotlin-runnable="true"}

<!--- > You can get the full code [here](../../guide/example/example-classes-03.kt). -->

<!---
```text
Exception in thread "main" kotlinx.serialization.json.internal.JsonDecodingException: Unexpected JSON token at offset 52: Expected string literal but 'null' literal was found at path: $.language
Use 'coerceInputValues = true' in 'Json {}' builder to coerce nulls if property has a default value.
```
-->

<!--- TEST LINES_START -->

> If you need to handle `null` values from third-party JSON, you can [coerce them to a default value](json.md#coercing-input-values).
>
{type="tip"}

When an optional property is present in the input, the initializer for that property is not called.
In the following example, since the `language` property is specified in the input, the `Computing` string is not printed
in the output:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
fun computeLanguage(): String {
    println("Computing")
    return "Kotlin"
}

@Serializable
// Initializer is skipped if language is in input
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

<!--- > You can get the full code [here](../../guide/example/example-classes-04.kt). -->

<!---

```text
Project(name=kotlinx.serialization, language=Java)
```
-->

<!--- TEST -->

> This behavior is intended to improve performance.
> Avoid relying on any side effects in the initializer, as they will be bypassed if the initializer is not called.
>
{type="note"}

### Serialization of class references

Serializable classes can contain references to other classes within their properties.
To ensure proper serialization, the referenced classes must also be annotated with `@Serializable`.
When encoded to JSON, this results in a nested JSON object:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
// The owner property references another serializable class `User`
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

<!--- > You can get the full code [here](../../guide/example/example-classes-05.kt). -->

<!---
```text
{"name":"kotlinx.serialization","owner":{"name":"kotlin"}}
```
-->

<!--- TEST -->

> If you need to reference non-serializable classes, you can mark them as [transient properties](#exclude-properties-with-the-transient-annotation), or
> provide a [custom serializer](serializers.md) for them.
>
{type="tip"}

### Serialization of repeated object references

Kotlin Serialization is designed for encoding and decoding of plain data. It does not support reconstruction
of arbitrary object graphs with repeated object references.
For example, when serializing an object that references the same instance twice, it is simply encoded twice:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
class Project(val name: String, val owner: User, val maintainer: User)

@Serializable
class User(val name: String)

fun main() {
    val owner = User("kotlin")
    // owner is referenced twice
    val data = Project("kotlinx.serialization", owner, owner)
    println(Json.encodeToString(data))
    // {"name":"kotlinx.serialization","owner":{"name":"kotlin"},"maintainer":{"name":"kotlin"}}
}
//sampleEnd
```
{kotlin-runnable="true"}

<!--- > You can get the full code [here](../../guide/example/example-classes-06.kt). -->

<!---
```text
{"name":"kotlinx.serialization","owner":{"name":"kotlin"},"maintainer":{"name":"kotlin"}}
```
-->

<!--- TEST -->

> If you attempt to serialize a circular structure, it will result in stack overflow.
> You can use the [Transient properties](#exclude-properties-with-the-transient-annotation) to exclude some references from serialization.
>
{type="tip"}

### Generic class serialization

Generic classes in Kotlin provide type-polymorphic behavior, which is enforced by Kotlin Serialization at
compile-time. For example, consider a generic serializable class `Box<T>`:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
// The Box<T> class can be used with built-in types like Int, or with user-defined types like Project.
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

<!--- > You can get the full code [here](../../guide/example/example-classes-07.kt). -->

<!---
```text
{"a":{"contents":42},"b":{"contents":{"name":"kotlinx.serialization","language":"Kotlin"}}}
```
-->

<!--- TEST -->

The type that is serialized to JSON depends on the actual compile-time type parameter specified for `Box`.

If the generic type is not serializable, a compile-time error will occur, preventing the code from compiling.

## Customize serialization behavior

Kotlin Serialization offers various ways to modify how your classes are serialized, allowing you to tailor the process to your specific needs.
This section covers techniques for customizing property names, managing default values, and more.

### Customize serial names

By default, the property names in the serialization output, such as JSON, match their names in the source code.
These names, known as _serial names_, can be customized
using the [`@SerialName`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-serial-name/) annotation.
For example, you can customize a property’s serial name to be shorter or more descriptive in the serialized output:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
// The language property is abbreviated to lang using @SerialName
class Project(val name: String, @SerialName("lang") val language: String)

fun main() {
    val data = Project("kotlinx.serialization", "Kotlin")
    // In the JSON output, the abbreviated name lang is used instead of the full property name
    println(Json.encodeToString(data))
    // {"name":"kotlinx.serialization","lang":"Kotlin"}
}
//sampleEnd
```
{kotlin-runnable="true"}

<!--- > You can get the full code [here](../../guide/example/example-classes-08.kt). -->

<!---
```text
{"name":"kotlinx.serialization","lang":"Kotlin"}
```
-->

<!--- TEST -->

### Define constructor properties for serialization

The `@Serializable` annotation requires all parameters of the class's primary constructor to be properties.

As a workaround, you can define a private primary constructor with the class's properties and create a
secondary constructor to handle the path string.
Serialization works with a private primary constructor and still serializes only the backing fields:

```kotlin
// Imports the necessary libraries
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

<!--- > You can get the full code [here](../../guide/example/example-classes-09.kt). -->

<!---
```text
{"owner":"kotlin","name":"kotlinx.serialization"}
```
-->

<!--- TEST -->

### Validate data in primary constructor

When you need to validate a constructor parameter before storing it in a property,
replace the parameter with a property in the primary constructor and perform validation in an `init { ... }` block.
This ensures the class is serializable and that invalid data cannot be deserialized:

```kotlin
// Imports the necessary libraries
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

<!--- > You can get the full code [here](../../guide/example/example-classes-10.kt). -->

<!---
```text
Exception in thread "main" java.lang.IllegalArgumentException: name cannot be empty
```
-->

<!--- TEST LINES_START -->

### Set default values for optional properties

In Kotlin, an object can only be deserialized when all its properties are present in the input.
If a property is missing, deserialization fails.

To resolve this issue, you can add a default value to the property, which automatically makes it optional for
serialization:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
// Sets a default value for the optional `language` property
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

<!--- > You can get the full code [here](../../guide/example/example-classes-11.kt). -->

<!---
```text
Project(name=kotlinx.serialization, language=Kotlin)
```
-->

<!--- TEST -->

### Make properties required with the @Required annotation

A property with a default value can be made required in a serialized format with the [`@Required`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-required/) annotation.
This ensures that the property must be present in the input, even if it has a default value:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.Required

//sampleStart
@Serializable
// Marks the `language` property as required
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

<!--- > You can get the full code [here](../../guide/example/example-classes-12.kt). -->

<!---
```text
Exception in thread "main" kotlinx.serialization.MissingFieldException: Field 'language' is required for type with serial name 'example.exampleClasses07.Project', but it was missing at path: $
```
-->

<!--- TEST LINES_START -->

### Exclude properties with the @Transient annotation

A property can be excluded from serialization by marking it with the [`@Transient`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-transient/) annotation
(not to be confused with [kotlin.jvm.Transient](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.jvm/-transient/)).
Transient properties must have a default value.

Attempts to explicitly specify its value in the serial format, even if the specified
value is equal to the default one, produces a `JsonDecodingException` exception:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json

//sampleStart
@Serializable
// Excludes the `language` property from serialization
data class Project(val name: String, @Transient val language: String = "Kotlin")

fun main() {
    val data = Json.decodeFromString<Project>("""
        {"name":"kotlinx.serialization","language":"Kotlin"}
    """)
    println(data)
    // JsonDecodingException
}
//sampleEnd
```
{kotlin-runnable="true"}

<!--- > You can get the full code [here](../../guide/example/example-classes-13.kt). -->

<!---
```text
Exception in thread "main" kotlinx.serialization.json.internal.JsonDecodingException: Unexpected JSON token at offset 42: Encountered an unknown key 'language' at path: $.name
Use 'ignoreUnknownKeys = true' in 'Json {}' builder to ignore unknown keys.
```
-->

<!--- TEST LINES_START -->

> You can avoid exceptions from unknown keys in JSON, including those marked with the @Transient annotation, with the [`ignoreUnknownKeys`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/ignore-unknown-keys.html) setting. 
> For more information, see the [Ignoring Unknown Keys](json.md#ignoring-unknown-keys) section.
> 
{type="tip"}

### Manage the serialization of default properties with @EncodedDefault

In JSON, default values are not encoded by default.
This behavior improves efficiency by reducing visual clutter and minimizing the amount of serialized data.
In the example below, the `language` property is omitted from the output because its value is equal to the default one:

```kotlin
// Imports the necessary libraries
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

<!--- > You can get the full code [here](../../guide/example/example-classes-14.kt). -->

<!---
```text
{"name":"kotlinx.serialization"}
```
-->

<!--- TEST -->

> You can learn more about how this behavior can be configured in the JSON format in the [Encoding defaults](json.md#encoding-defaults) section.
> 
{type="tip"}

To ensure that a property is always serialized, regardless of its value or format settings, use the [`@EncodeDefault`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-encode-default/) annotation.
It's also possible to do the opposite by configuring the [`EncodeDefault.Mode`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-encode-default/-mode/) parameter.
Let's look at an example, where the `language` property is included in the serialized output regardless of its value,
while the `projects` property is not serialized when it is an empty list:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
data class Project(
    val name: String,
    // The 'language' property will always be included in the serialized output, even if it has the default value "Kotlin"
    @EncodeDefault val language: String = "Kotlin"
)

@Serializable
data class User(
    val name: String,
    // The 'projects' property will never be included in the serialized output, even if it has a value
    // Since the default value is an empty list, 'projects' will be omitted unless it contains elements
    @EncodeDefault(EncodeDefault.Mode.NEVER) val projects: List<Project> = emptyList()
)

fun main() {
    val userA = User("Alice", listOf(Project("kotlinx.serialization")))
    val userB = User("Bob")
    // 'projects' is serialized because it contains a value, and 'language' is always serialized
    println(Json.encodeToString(userA))
    // {"name":"Alice","projects":[{"name":"kotlinx.serialization","language":"Kotlin"}]}

    // 'projects' is omitted because it's an empty list and EncodeDefault.Mode is set to NEVER, so it's not serialized
    println(Json.encodeToString(userB))
    // {"name":"Bob"}
}
//sampleEnd
```
{kotlin-runnable="true"}

<!--- > You can get the full code [here](../../guide/example/example-classes-15.kt). -->

<!---
```text
{"name":"Alice","projects":[{"name":"kotlinx.serialization","language":"Kotlin"}]}
{"name":"Bob"}
```
-->

<!--- TEST -->
