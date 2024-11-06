<!--- TEST_NAME JsonTestTransform -->
[//]: # (title: Transform JSON output)

> This guide builds upon concepts introduced in the [Serialize polymorphic classes](serialization-polymorphism.md) and [Create custom serializers](create-custom-serializers.md) guides.
>
{style="note"}

To modify the structure and content of JSON after serialization, or adapt input for deserialization, you can create a [custom serializer](create-custom-serializers.md).
While the [`Encoder`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-encoder/)
and [`Decoder`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-decoder/) offer precise control,
Kotlin serialization also provides an API that makes it easy to manipulate a JSON elements tree.
This can be ideal for smaller tasks or quick transformations.

The transformation features are available through the abstract [`JsonTransformingSerializer`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-transforming-serializer/) class, which implements [`KSerializer`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-k-serializer/).
Instead of interacting directly with `Encoder` or `Decoder`, this class allows you to define transformations using the
`transformSerialize()` and `transformDeserialize()` functions for the JSON tree
represented by the [`JsonElement`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-element/) class.

Before exploring the specific features in the following sections, ensure that the following libraries are imported:

```kotlin
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*
```

## Wrap a single object in an array during deserialization

When working with JSON data, you might encounter the same property having different structures, such as being either a single object or an array.
To ensure consistent handling of these variations during deserialization, a custom serializer can be used to wrap single objects in an array,
allowing seamless deserialization into [`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/) types.

Let's look at an example, where the `Project` class has a `users` property that expects a list of objects.
Although the input can either be a single object or an array, the goal is to ensure both are consistently deserialized into a `List`.
You can use the [`@Serializable`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-serializable/) annotation in the data model to specify a custom serializer for the
`users: List<User>` property:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*

//sampleStart
// Uses UserListSerializer to handle the serialization of the users property
@Serializable
data class Project(
    val name: String,
    @Serializable(with = UserListSerializer::class)
    val users: List<User>
)

// Defines the User data class
@Serializable
data class User(val name: String)

// Implements a custom serializer that wraps single objects into arrays during deserialization
object UserListSerializer : JsonTransformingSerializer<List<User>>(ListSerializer(User.serializer())) {
    // If response is not an array, then it is a single object that should be wrapped in an array
    override fun transformDeserialize(element: JsonElement): JsonElement =
        if (element !is JsonArray) JsonArray(listOf(element)) else element
}

fun main() {
    // Deserializes a single JSON object wrapped as an array
    println(Json.decodeFromString<Project>("""
        {"name":"kotlinx.serialization","users":{"name":"kotlin"}}
    """))
    // Project(name=kotlinx.serialization, users=[User(name=kotlin)])

    // Deserializes a JSON array of objects
    println(Json.decodeFromString<Project>("""
        {"name":"kotlinx.serialization","users":[{"name":"kotlin"},{"name":"jetbrains"}]}
    """))
    // Project(name=kotlinx.serialization, users=[User(name=kotlin), User(name=jetbrains)])
}
//sampleEnd
```
{kotlin-runnable="true}

Since this example focuses on deserialization, the `UserListSerializer` only overrides the
`transformDeserialize()` function. The `JsonTransformingSerializer` constructor takes the original serializer
as parameter.

<!--- > You can get the full code [here](../../guide/example/example-json-transform-01.kt). -->

<!---
```text
Project(name=kotlinx.serialization, users=[User(name=kotlin)])
Project(name=kotlinx.serialization, users=[User(name=kotlin), User(name=jetbrains)])
```
-->

<!--- TEST -->

## Unwrap a single-element array during serialization

You can use the `transformSerialize()` function to unwrap a single-element list into a single JSON object
during serialization:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
data class Project(
  val name: String,
  @Serializable(with = UserListSerializer::class)
  val users: List<User>
)

@Serializable
data class User(val name: String)

// Unwraps single-element lists into a single object during serialization
object UserListSerializer : JsonTransformingSerializer<List<User>>(ListSerializer(User.serializer())) {

    override fun transformSerialize(element: JsonElement): JsonElement {
        // Ensures that the input is a list
        require(element is JsonArray)
        // Unwraps single-element lists into a single JSON object
        return element.singleOrNull() ?: element
    }
}
  
fun main() {
    val data = Project("kotlinx.serialization", listOf(User("kotlin")))
    println(Json.encodeToString(data))
    // {"name":"kotlinx.serialization","users":{"name":"kotlin"}}
}
//sampleEnd
```
{kotlin-runnable="true}

<!--- > You can get the full code [here](../../guide/example/example-json-transform-02.kt). -->

<!---
```text
{"name":"kotlinx.serialization","users":{"name":"kotlin"}}
```
-->

<!--- TEST -->

## Omit specific properties during serialization

You can omit properties from the JSON output when they have default values, match specific values, or when they are missing.
This helps streamline the data, reducing unnecessary information while ensuring that only relevant properties are serialized.

Let's look at an example where the `Project` class has a `language` property that should be omitted from the JSON output when its value is `"Kotlin"`.
To do this, you can write a custom serializer for the `Project` class:

In the example below, we are serializing the `Project` class at the top-level, so we explicitly
pass the above `ProjectSerializer` to `encodeToString()` function:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
class Project(val name: String, val language: String)

// Custom serializer that omits the "language" property if it is equal to "Kotlin"
object ProjectSerializer : JsonTransformingSerializer<Project>(Project.serializer()) {
    override fun transformSerialize(element: JsonElement): JsonElement =
        // Omits the "language" property if its value is "Kotlin"
        JsonObject(element.jsonObject.filterNot {
            (k, v) -> k == "language" && v.jsonPrimitive.content == "Kotlin"
        })
}

fun main() {
    val data = Project("kotlinx.serialization", "Kotlin")
    // Uses the plugin-generated serializer
    println(Json.encodeToString(data))
    // {"name":"kotlinx.serialization","language":"Kotlin"}
    println(Json.encodeToString(ProjectSerializer, data)) // using custom serializer
    // {"name":"kotlinx.serialization"}
}
//sampleEnd
```
{kotlin-runnable="true}


> When serializing an object directly, you need to explicitly pass the custom serializer to the `encodeToString()`
> function to ensure that the custom serialization logic is applied. For more information, see the [Pass serializers manually](third-party-classes.md#pass-serializers-manually) section.
> 
{style="note"}

<!--- > You can get the full code [here](../../guide/example/example-json-transform-03.kt). -->

<!---
```text
{"name":"kotlinx.serialization","language":"Kotlin"}
{"name":"kotlinx.serialization"}
```
-->

<!--- TEST -->

## Content-based polymorphic deserialization

In [polymorphic serialization](serialization-polymorphism.md), a _class discriminator_, a dedicated `"type"` property in the JSON,
is usually included to determine which serializer should be used to deserialize the Kotlin class.

When no class discriminator is present in the JSON input, you can use [`JsonContentPolymorphicSerializer`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-content-polymorphic-serializer/) to infer the type from the structure of the JSON.
This serializer allows you to override the `selectDeserializer()` function to choose the correct deserializer based on the JSON content.

> When you use this serializer, the appropriate deserializer is chosen at runtime. 
> It can either come from the [registered](serialization-polymorphism.md#serialize-closed-polymorphic-classes) or the default serializer.
>
{style="tip"}

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
abstract class Project {
    abstract val name: String
}

@Serializable
data class BasicProject(override val name: String): Project()


@Serializable
data class OwnedProject(override val name: String, val owner: String) : Project()

// Custom serializer that selects deserializer based on the presence of "owner"
object ProjectSerializer : JsonContentPolymorphicSerializer<Project>(Project::class) {
    override fun selectDeserializer(element: JsonElement) = when {
        // Distinguishes the BasicProject and OwnedProject subclasses by the presence of "owner" key
        "owner" in element.jsonObject -> OwnedProject.serializer()
        else -> BasicProject.serializer()
    }
}

fun main() {
    val data = listOf(
        OwnedProject("kotlinx.serialization", "kotlin"),
        BasicProject("example")
    )
    val string = Json.encodeToString(ListSerializer(ProjectSerializer), data)
    // No class discriminator is added in the JSON output
    println(string)
    // [{"name":"kotlinx.serialization","owner":"kotlin"},{"name":"example"}]
    println(Json.decodeFromString(ListSerializer(ProjectSerializer), string))
    // [OwnedProject(name=kotlinx.serialization, owner=kotlin), BasicProject(name=example)]
}
//sampleEnd
```
{kotlin-runnable="true"}

This example manually selects the appropriate subclass without using plugin-generated code,
which is why the class doesn't need to be `sealed`.

<!--- > You can get the full code [here](../../guide/example/example-json-transform-04.kt). -->

<!---
```text
[{"name":"kotlinx.serialization","owner":"kotlin"},{"name":"example"}]
[OwnedProject(name=kotlinx.serialization, owner=kotlin), BasicProject(name=example)]
```
-->

<!--- TEST -->

## Implement custom serialization logic in JSON (experimental)

Although abstract serializers can handle most cases, you can manually implement custom serialization logic in JSON using the [`KSerializer`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-k-serializer/) class.
If modifying the `transformSerialize()`, `transformDeserialize()`, or `selectDeserializer()` functions is insufficient,
you can adjust the `serialize()` and `deserialize()` functions directly.

When working with `Json`, consider the following:

* You can cast `Encoder` to `JsonEncoder` and `Decoder` to `JsonDecoder` when the format is `Json`.
* `JsonDecoder` provides the [`decodeJsonElement()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-decoder/decode-json-element.html) function, and `JsonEncoder` offers the [`encodeJsonElement()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-encoder/encode-json-element.html) function.
These functions allow retrieving and inserting JSON elements at specific points in the stream.
* Both `JsonDecoder` and `JsonEncoder` have a `json` property, which gives access to the current `Json` instance with its active settings.
* `Json` provides the [`encodeToJsonElement()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/encode-to-json-element.html) and [`decodeFromJsonElement()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/decode-from-json-element.html) functions.

Using these tools, you can implement two-stage conversion processes such as `Decoder -> JsonElement -> value` or
`value -> JsonElement -> Encoder`.
For example, you can implement a custom serializer for the following `Response` class so that its
`Ok` subclass is represented directly, but the `Error` subclass is represented by an object with the error message:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

// Defines a sealed class for API responses
@Serializable(with = ResponseSerializer::class)
sealed class Response<out T> {
    data class Ok<out T>(val data: T) : Response<T>()
    data class Error(val message: String) : Response<Nothing>()
}

// Implements custom serialization logic for Response class
class ResponseSerializer<T>(private val dataSerializer: KSerializer<T>) : KSerializer<Response<T>> {
    override val descriptor: SerialDescriptor = buildSerialDescriptor("Response", PolymorphicKind.SEALED) {
        element("Ok", dataSerializer.descriptor)
        element("Error", buildClassSerialDescriptor("Error") {
          element<String>("message")
        })
    }

    // Deserializes Response from JSON
    override fun deserialize(decoder: Decoder): Response<T> {
        // Decoder -> JsonDecoder
        // Ensures the decoder is a JsonDecoder
        require(decoder is JsonDecoder)
        // JsonDecoder -> JsonElement
        val element = decoder.decodeJsonElement()
        // JsonElement -> value
        if (element is JsonObject && "error" in element)
            return Response.Error(element["error"]!!.jsonPrimitive.content)
        return Response.Ok(decoder.json.decodeFromJsonElement(dataSerializer, element))
    }

    // Serializes Response to JSON
    override fun serialize(encoder: Encoder, value: Response<T>) {
        // Encoder -> JsonEncoder
        // Ensures the encoder is a JsonEncoder
        require(encoder is JsonEncoder)
        // value -> JsonElement
        val element = when (value) {
            is Response.Ok -> encoder.json.encodeToJsonElement(dataSerializer, value.data)
            is Response.Error -> buildJsonObject { put("error", value.message) }
        }
        // JsonElement -> JsonEncoder
        encoder.encodeJsonElement(element)
    }
}
```

This `Response` class works with any serializable data type, giving you precise control over how the class is represented in JSON output:

```kotlin
@Serializable
data class Project(val name: String)

fun main() {
    val responses = listOf(
        Response.Ok(Project("kotlinx.serialization")),
        Response.Error("Not found")
    )
    val string = Json.encodeToString(responses)
    println(string)
    // [{"name":"kotlinx.serialization"},{"error":"Not found"}]
    println(Json.decodeFromString<List<Response<Project>>>(string))
    // [Ok(data=Project(name=kotlinx.serialization)), Error(message=Not found)]
}
```

<!--- > You can get the full code [here](../../guide/example/example-json-transform-05.kt). -->

<!---
```text
[{"name":"kotlinx.serialization"},{"error":"Not found"}]
[Ok(data=Project(name=kotlinx.serialization)), Error(message=Not found)]
```
-->

<!--- TEST -->

## Maintain custom JSON attributes

When JSON input contains properties that aren't predefined in your class, you can use a custom deserializer to capture these unknown properties.
By storing them in a `JsonObject`, you ensure that they are preserved during deserialization.

If your class defines a set of known fields, but you need to capture any additional unknown fields, you can create a custom serializer.
This serializer can store the unknown fields in a separate field, which ensures that they are preserved during deserialization.
The unknown fields are flattened, meaning they are kept in the same object as the known fields, without being nested inside a separate structure:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

//sampleStart
data class UnknownProject(val name: String, val details: JsonObject)

object UnknownProjectSerializer : KSerializer<UnknownProject> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("UnknownProject") {
        element<String>("name")
        element<JsonElement>("details")
    }

    override fun deserialize(decoder: Decoder): UnknownProject {
        // Ensures the decoder is JSON-specific
        val jsonInput = decoder as? JsonDecoder ?: error("Can be deserialized only by JSON")

        // Reads the entire content as JSON
        val json = jsonInput.decodeJsonElement().jsonObject

        // Extracts and removes the "name" property
        val name = json.getValue("name").jsonPrimitive.content

        // Flattens the remaining properties into the 'details' field
        val details = json.toMutableMap()
        details.remove("name")
        return UnknownProject(name, JsonObject(details))
    }

    override fun serialize(encoder: Encoder, value: UnknownProject) {
        error("Serialization is not supported")
    }
}

fun main() {
    // Deserializes JSON with unknown fields into 'UnknownProject'
    println(Json.decodeFromString(UnknownProjectSerializer, """{"type":"unknown","name":"example","maintainer":"Unknown","license":"Apache 2.0"}"""))
    // UnknownProject(name=example, details={"type":"unknown","maintainer":"Unknown","license":"Apache 2.0"})

}
//sampleEnd
```
{kotlin-runnable="true}

<!--- > You can get the full code [here](../../guide/example/example-json-transform-06.kt). -->

<!---
```text
UnknownProject(name=example, details={"type":"unknown","maintainer":"Unknown","license":"Apache 2.0"})
```
-->

<!--- TEST -->

## What's next

* Learn how to [serialize polymorphic classes](serialization-polymorphism.md) and handle objects of various types within a shared hierarchy.
* Discover [alternative experimental serialization formats](alternative-serialization-formats.md), such as CBOR and ProtoBuf, to improve performance and flexibility for your applications.
