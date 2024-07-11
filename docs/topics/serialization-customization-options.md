[//]: # (title: Serialization customization options)

By default, the `@Serializable` annotation in Kotlin enables the serialization of all properties in the primary constructor.
You can customize this behavior to fit your specific needs.
This section covers how to adjust serialization using various techniques to control which properties are serialized and
how the serialization process is managed.

To get started, make sure you have the necessary libraries installed. 
For setup instructions, see the [Get started with Kotlin serialization guide](serialization-get-started.md).

## Backing fields are serialized

Only a class's properties with backing fields are serialized, so properties with a getter/setter that don't
have a backing field and delegated properties are not serialized, as the following example shows.

```kotlin
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
```

## Constructor properties requirement

If we want to define the `Project` class so that it takes a path string, and then
deconstructs it into the corresponding properties, we might be tempted to write something like the code below.

```kotlin
@Serializable
class Project(path: String) {
    val owner: String = path.substringBefore('/')
    val name: String = path.substringAfter('/')
}
```

This class does not compile because the `@Serializable` annotation requires that all parameters of the class's primary
constructor be properties. A simple workaround is to define a private primary constructor with the class's
properties, and turn the constructor we wanted into the secondary one.

```kotlin
@Serializable
class Project private constructor(val owner: String, val name: String) {
    constructor(path: String) : this(
        owner = path.substringBefore('/'),
        name = path.substringAfter('/')
    )

    val path: String
        get() = "$owner/$name"
}
```

Serialization works with a private primary constructor, and still serializes only backing fields.

```kotlin
fun main() {
    println(Json.encodeToString(Project("kotlin/kotlinx.serialization")))
}
```

> You can get the full code [here](../../guide/example/example-classes-02.kt).

This example produces the expected output.

```text
{"owner":"kotlin","name":"kotlinx.serialization"}
```

<!--- TEST -->

## Data validation

Another case where you might want to introduce a primary constructor parameter without a property is when you
want to validate its value before storing it to a property. To make it serializable you shall replace it
with a property in the primary constructor, and move the validation to an `init { ... }` block.

```kotlin
@Serializable
class Project(val name: String) {
    init {
        require(name.isNotEmpty()) { "name cannot be empty" }
    }
}
```

A deserialization process works like a regular constructor in Kotlin and calls all `init` blocks, ensuring that you
cannot get an invalid class as a result of deserialization. Let's try it.

```kotlin
fun main() {
    val data = Json.decodeFromString<Project>("""
        {"name":""}
    """)
    println(data)
}
```

> You can get the full code [here](../../guide/example/example-classes-03.kt).

Running this code produces the exception:

```text
Exception in thread "main" java.lang.IllegalArgumentException: name cannot be empty
```

<!--- TEST LINES_START -->

## Optional properties

An object can be deserialized only when all its properties are present in the input.
For example, run the following code.

```kotlin
@Serializable
data class Project(val name: String, val language: String)

fun main() {
    val data = Json.decodeFromString<Project>("""
        {"name":"kotlinx.serialization"}
    """)
    println(data)
}
```

> You can get the full code [here](../../guide/example/example-classes-04.kt).

It produces the exception:

```text
Exception in thread "main" kotlinx.serialization.MissingFieldException: Field 'language' is required for type with serial name 'example.exampleClasses04.Project', but it was missing at path: $
```

<!--- TEST LINES_START -->

This problem can be fixed by adding a default value to the property, which automatically makes it optional
for serialization.

```kotlin
@Serializable
data class Project(val name: String, val language: String = "Kotlin")

fun main() {
    val data = Json.decodeFromString<Project>("""
        {"name":"kotlinx.serialization"}
    """)
    println(data)
}
```

> You can get the full code [here](../../guide/example/example-classes-05.kt).

It produces the following output with the default value for the `language` property.

```text
Project(name=kotlinx.serialization, language=Kotlin)
```

<!--- TEST -->

## Optional property initializer call

When an optional property is present in the input, the corresponding initializer for this
property is not even called. This is a feature designed for performance, so be careful not
to rely on side effects in initializers. Consider the example below.

```kotlin
fun computeLanguage(): String {
    println("Computing")
    return "Kotlin"
}

@Serializable
data class Project(val name: String, val language: String = computeLanguage())

fun main() {
    val data = Json.decodeFromString<Project>("""
        {"name":"kotlinx.serialization","language":"Kotlin"}
    """)
    println(data)
}
```

> You can get the full code [here](../../guide/example/example-classes-06.kt).

Since the `language` property was specified in the input, we don't see the "Computing" string printed
in the output.

```text
Project(name=kotlinx.serialization, language=Kotlin)
```

<!--- TEST -->

## Required properties

A property with a default value can be required in a serial format with the [`@Required`][Required] annotation.
Let us change the previous example by marking the `language` property as `@Required`.

```kotlin
@Serializable
data class Project(val name: String, @Required val language: String = "Kotlin")

fun main() {
    val data = Json.decodeFromString<Project>("""
        {"name":"kotlinx.serialization"}
    """)
    println(data)
}
```

> You can get the full code [here](../../guide/example/example-classes-07.kt).

We get the following exception.

```text
Exception in thread "main" kotlinx.serialization.MissingFieldException: Field 'language' is required for type with serial name 'example.exampleClasses07.Project', but it was missing at path: $
```

<!--- TEST LINES_START -->

## Transient properties

A property can be excluded from serialization by marking it with the [`@Transient`][Transient] annotation
(don't confuse it with [kotlin.jvm.Transient]). Transient properties must have a default value.

```kotlin
@Serializable
data class Project(val name: String, @Transient val language: String = "Kotlin")

fun main() {
    val data = Json.decodeFromString<Project>("""
        {"name":"kotlinx.serialization","language":"Kotlin"}
    """)
    println(data)
}
```

> You can get the full code [here](../../guide/example/example-classes-08.kt).

Attempts to explicitly specify its value in the serial format, even if the specified
value is equal to the default one, produces the following exception.

```text
Exception in thread "main" kotlinx.serialization.json.internal.JsonDecodingException: Unexpected JSON token at offset 42: Encountered an unknown key 'language' at path: $.name
Use 'ignoreUnknownKeys = true' in 'Json {}' builder to ignore unknown keys.
```

<!--- TEST LINES_START -->

> The 'ignoreUnknownKeys' feature is explained in the [Ignoring Unknown Keys section](json.md#ignoring-unknown-keys) section.

## Defaults are not encoded by default

Default values are not encoded by default in JSON. This behavior is motivated by the fact that in most real-life scenarios
such configuration reduces visual clutter, and saves the amount of data being serialized.

```kotlin
@Serializable
data class Project(val name: String, val language: String = "Kotlin")

fun main() {
    val data = Project("kotlinx.serialization")
    println(Json.encodeToString(data))
}
```

> You can get the full code [here](../../guide/example/example-classes-09.kt).

It produces the following output, which does not have the `language` property because its value is equal to the default one.

```text
{"name":"kotlinx.serialization"}
```

<!--- TEST -->

See JSON's [Encoding defaults](json.md#encoding-defaults) section on how this behavior can be configured for JSON.
Additionally, this behavior can be controlled without taking format settings into account.
For that purposes, [EncodeDefault] annotation can be used:

```kotlin
@Serializable
data class Project(
    val name: String,
    @EncodeDefault val language: String = "Kotlin"
)
```

This annotation instructs the framework to always serialize property, regardless of its value or format settings.
It's also possible to tweak it into the opposite behavior using [EncodeDefault.Mode] parameter:

```kotlin

@Serializable
data class User(
    val name: String,
    @EncodeDefault(EncodeDefault.Mode.NEVER) val projects: List<Project> = emptyList()
)

fun main() {
    val userA = User("Alice", listOf(Project("kotlinx.serialization")))
    val userB = User("Bob")
    println(Json.encodeToString(userA))
    println(Json.encodeToString(userB))
}
```

> You can get the full code [here](../../guide/example/example-classes-10.kt).

As you can see, `language` property is preserved and `projects` is omitted:

```text
{"name":"Alice","projects":[{"name":"kotlinx.serialization","language":"Kotlin"}]}
{"name":"Bob"}
```

<!--- TEST -->

## Nullable properties

Nullable properties are natively supported by Kotlin Serialization.

```kotlin
@Serializable
class Project(val name: String, val renamedTo: String? = null)

fun main() {
    val data = Project("kotlinx.serialization")
    println(Json.encodeToString(data))
}
```

> You can get the full code [here](../../guide/example/example-classes-11.kt).

This example does not encode `null` in JSON because [Defaults are not encoded](#defaults-are-not-encoded).

```text
{"name":"kotlinx.serialization"}
```

<!--- TEST -->

## Type safety is enforced

Kotlin Serialization strongly enforces the type safety of the Kotlin programming language.
In particular, let us try to decode a `null` value from a JSON object into a non-nullable Kotlin property `language`.

```kotlin
@Serializable
data class Project(val name: String, val language: String = "Kotlin")

fun main() {
    val data = Json.decodeFromString<Project>("""
        {"name":"kotlinx.serialization","language":null}
    """)
    println(data)
}
```

> You can get the full code [here](../../guide/example/example-classes-12.kt).

Even though the `language` property has a default value, it is still an error to attempt to assign
the `null` value to it.

```text
Exception in thread "main" kotlinx.serialization.json.internal.JsonDecodingException: Unexpected JSON token at offset 52: Expected string literal but 'null' literal was found at path: $.language
Use 'coerceInputValues = true' in 'Json {}' builder to coerce nulls if property has a default value.
```

<!--- TEST LINES_START -->

> It might be desired, when decoding 3rd-party JSONs, to coerce `null` to a default value.
> The corresponding feature is explained in the [Coercing input values](json.md#coercing-input-values) section.

## Referenced objects

Serializable classes can reference other classes in their serializable properties.
The referenced classes must be also marked as `@Serializable`.

```kotlin
@Serializable
class Project(val name: String, val owner: User)

@Serializable
class User(val name: String)

fun main() {
    val owner = User("kotlin")
    val data = Project("kotlinx.serialization", owner)
    println(Json.encodeToString(data))
}
```

> You can get the full code [here](../../guide/example/example-classes-13.kt).

When encoded to JSON it results in a nested JSON object.

```text
{"name":"kotlinx.serialization","owner":{"name":"kotlin"}}
```

> References to non-serializable classes can be marked as [Transient properties](#transient-properties), or a
> custom serializer can be provided for them as shown in the [Serializers](serializers.md) chapter.

<!--- TEST -->

## No compression of repeated references

Kotlin Serialization is designed for encoding and decoding of plain data. It does not support reconstruction
of arbitrary object graphs with repeated object references. For example, let us try to serialize an object
that references the same `owner` instance twice.

```kotlin
@Serializable
class Project(val name: String, val owner: User, val maintainer: User)

@Serializable
class User(val name: String)

fun main() {
    val owner = User("kotlin")
    val data = Project("kotlinx.serialization", owner, owner)
    println(Json.encodeToString(data))
}
```

> You can get the full code [here](../../guide/example/example-classes-14.kt).

We simply get the `owner` value encoded twice.

```text
{"name":"kotlinx.serialization","owner":{"name":"kotlin"},"maintainer":{"name":"kotlin"}}
```

> Attempt to serialize a circular structure will result in stack overflow.
> You can use the [Transient properties](#transient-properties) to exclude some references from serialization.

<!--- TEST -->

## Generic classes

Generic classes in Kotlin provide type-polymorphic behavior, which is enforced by Kotlin Serialization at
compile-time. For example, consider a generic serializable class `Box<T>`.

```kotlin
@Serializable
class Box<T>(val contents: T)
```

The `Box<T>` class can be used with builtin types like `Int`, as well as with user-defined types like `Project`.

<!--- INCLUDE

@Serializable
data class Project(val name: String, val language: String)
-->

```kotlin
@Serializable
class Data(
    val a: Box<Int>,
    val b: Box<Project>
)

fun main() {
    val data = Data(Box(42), Box(Project("kotlinx.serialization", "Kotlin")))
    println(Json.encodeToString(data))
}
```

> You can get the full code [here](../../guide/example/example-classes-15.kt).

The actual type that we get in JSON depends on the actual compile-time type parameter that was specified for `Box`.

```text
{"a":{"contents":42},"b":{"contents":{"name":"kotlinx.serialization","language":"Kotlin"}}}
```

<!--- TEST -->

If the actual generic type is not serializable a compile-time error will be produced.

## Serial field names

The names of the properties used in encoded representation, JSON in our examples, are the same as
their names in the source code by default. The name that is used for serialization is called a _serial name_, and
can be changed using the [`@SerialName`][SerialName] annotation. For example, we can have a `language` property in
the source with an abbreviated serial name.

```kotlin
@Serializable
class Project(val name: String, @SerialName("lang") val language: String)

fun main() {
    val data = Project("kotlinx.serialization", "Kotlin")
    println(Json.encodeToString(data))
}
```

> You can get the full code [here](../../guide/example/example-classes-16.kt).

Now we see that an abbreviated name `lang` is used in the JSON output.

```text
{"name":"kotlinx.serialization","lang":"Kotlin"}
```