[//]: # (title: Transform JSON structure)
<primary-label ref="advanced"/>

> This section builds on concepts from [Serialize polymorphic classes](serialization-polymorphism.md) and [Create custom serializers](create-custom-serializers.md).
> If you're not familiar with these concepts, we recommend reviewing them first.
>
{style="note"}

To control the structure and content of the JSON you generate during serialization, you can create [custom serializers](create-custom-serializers.md).
For smaller adjustments, the [`JsonTransformingSerializer`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-transforming-serializer/) class offers a simpler way to modify JSON by working directly with the JSON element tree instead of interacting with [`Encoder`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-encoder/)
or [`Decoder`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx.serialization-core/kotlinx.serialization.encoding/-decoder/) manually.

The `JsonTransformingSerializer` implements the [`KSerializer`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-k-serializer/) interface, which
lets you override the [`transformSerialize()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-transforming-serializer/transform-serialize.html) and [`transformDeserialize()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-transforming-serializer/transform-deserialize.html) functions to adjust the JSON element tree either before serialization or before deserialization.

> In addition to transforming JSON structures, you can also use `JsonContentPolymorphicSerializer` to [select the appropriate polymorphic class based on the JSON content](#select-the-appropriate-polymorphic-class-based-on-the-json-content).
> 
{style="tip"} 

## Modify JSON structure

You can make small structural adjustments to JSON by transforming the JSON element tree.
The following examples demonstrate common use cases, such as wrapping or unwrapping arrays and omitting specific properties.

### Wrap a single object in an array during deserialization

Some APIs return a single JSON object when there is one item and an array when there are multiple.
If you want both cases to deserialize into a [`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/), use
`JsonTransformingSerializer` to modify the JSON element tree during deserialization.
To implement the wrapping logic, override the `transformDeserialize()` function.

> You must specify a serializer when calling the `JsonTransformingSerializer` constructor.
> To use the standard conversion logic for the type, specify a default serializer.
> For example, you can use the default list serializer with the [`ListSerializer()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.builtins/-list-serializer.html) function.
>
{style="note"}

Here's an example:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*

//sampleStart
// Uses UserListSerializer to handle the serialization of the users property
@Serializable
data class Project(
    val name: String,
    // Specifies a custom serializer for the users property
    @Serializable(with = UserListSerializer::class)
    val users: List<User>
)

@Serializable
data class User(val name: String)

// Specifies the default List serializer
object UserListSerializer : JsonTransformingSerializer<List<User>>(ListSerializer(User.serializer())) {
    // If not an array, wrap the element as a single-item array
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
{kotlin-runnable="true"}

### Unwrap a single-element array during serialization

To unwrap a single-element list into a single JSON object during serialization, override the `transformSerialize()` function:

```kotlin
// Imports declarations from the serialization library
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
{kotlin-runnable="true"}

### Omit specific properties during serialization

You can omit properties from the JSON output. This can be useful when they have default values, match specific values, or when the values are missing.
This helps streamline the data, reducing unnecessary information while ensuring that only relevant properties are serialized.

To omit properties during serialization, create a custom serializer and override the `transformSerialize()` function to remove the fields you don't want in the JSON output:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
class Project(val name: String, val language: String)

// Creates a custom serializer that omits the language property if it's equal to "Kotlin"
object ProjectSerializer : JsonTransformingSerializer<Project>(Project.serializer()) {
    override fun transformSerialize(element: JsonElement): JsonElement =
        // Omits the language property if its value is "Kotlin"
        JsonObject(element.jsonObject.filterNot {
            (k, v) -> k == "language" && v.jsonPrimitive.content == "Kotlin"
        })
}

fun main() {
    val data = Project("kotlinx.serialization", "Kotlin")

    // Uses the default serializer
    println(Json.encodeToString(data))
    // {"name":"kotlinx.serialization","language":"Kotlin"}

    // Applies the custom serializer to omit the language property 
    println(Json.encodeToString(ProjectSerializer, data))
    // {"name":"kotlinx.serialization"}
}
//sampleEnd
```
{kotlin-runnable="true"}

In this example, the `Project` class has a `language` property, which is omitted when its value is `"Kotlin"`.
The custom serializer is passed to `encodeToString()` to ensure the custom transformation is applied.

> When serializing an object directly, you need to explicitly pass the custom serializer to the `encodeToString()`
> function to ensure that the custom serialization logic is applied. For more information, see the [Pass serializers manually](third-party-classes.md#pass-serializers-manually) section.
>
{style="note"}

## Select the appropriate polymorphic class based on the JSON content

In [polymorphic serialization](serialization-polymorphism.md), JSON often includes a dedicated _class discriminator_ property that identifies the concrete subtype during deserialization.

When no class discriminator is present in the JSON input, you can use [`JsonContentPolymorphicSerializer`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-content-polymorphic-serializer/) to infer the type from the structure of the JSON.
This serializer allows you to override the `selectDeserializer()` function to choose the correct deserializer based on the JSON content.

> When you use this serializer, the appropriate deserializer is chosen at runtime.
> It may be one you [specified in a `SerializersModule`](serialization-polymorphism.md#serialize-closed-polymorphic-classes) or the default serializer.
>
{style="tip"}

Here's an example:

```kotlin
// Imports declarations from the serialization library
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

// Creates a custom serializer that selects deserializer based on the presence of "owner"
object ProjectSerializer : JsonContentPolymorphicSerializer<Project>(Project::class) {
    override fun selectDeserializer(element: JsonElement) = when {
        // Selects the OwnedProject serializer if the JSON object contains an "owner" key
        "owner" in element.jsonObject -> OwnedProject.serializer()
        else -> BasicProject.serializer()
    }
}

fun main() {
    val data = listOf(
        OwnedProject("kotlinx.serialization", "kotlin"),
        BasicProject("example")
    )
    // No class discriminator in the JSON output
    val string = Json.encodeToString(ListSerializer(ProjectSerializer), data)

    println(string)
    // [{"name":"kotlinx.serialization","owner":"kotlin"},{"name":"example"}]

    println(Json.decodeFromString(ListSerializer(ProjectSerializer), string))
    // [OwnedProject(name=kotlinx.serialization, owner=kotlin), BasicProject(name=example)]
}
//sampleEnd
```
{kotlin-runnable="true"}

In this example, the serializer decides which subtype to use based on the JSON content, so you don't need a `sealed` class for the class hierarchy.

## Add custom behavior to the default serializer

You can add custom behavior to the default serializer that Kotlin serialization generates.

To do so, annotate a serializable class with the [Experimental](components-stability.md#stability-levels-explained) [`@KeepGeneratedSerializer`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-keep-generated-serializer/) and use the automatically created `generatedSerializer()` as the base serializer in your custom implementation:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*

//sampleStart
// Defines a sealed hierarchy where each subtype has a name property
@Serializable
sealed class Project {
    abstract val name: String
}

@OptIn(ExperimentalSerializationApi::class)
@KeepGeneratedSerializer
@Serializable(with = BasicProjectSerializer::class)
@SerialName("basic")
data class BasicProject(override val name: String): Project()

// Adds custom logic to the default serializer to rename a field during deserialization
object BasicProjectSerializer : JsonTransformingSerializer<BasicProject>(BasicProject.generatedSerializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        val jsonObject = element.jsonObject
        // Renames "basic-name" to "name" if it exists in the JSON input
        return if ("basic-name" in jsonObject) {
            val nameElement = jsonObject["basic-name"] ?: throw IllegalStateException()
            JsonObject(mapOf("name" to nameElement))
        } else {
            jsonObject
        }
    }
}


fun main() {

    // Deserializes JSON where the field name differs from the expected structure
    val project = Json.decodeFromString<Project>("""{"type":"basic","basic-name":"example"}""")

    println(project)
    // BasicProject(name=example)
}
//sampleEnd
```
{kotlin-runnable="true"}

In this example, the custom serializer updates the JSON structure during deserialization by renaming a field so it matches the `name` property defined in the base class.

<!-- Would another example like this work? to focus more on the custom behavior not the polymorphism:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
data class Project(val name: String)

// Adds custom logic to rename "projectName" to "name" during deserialization
object ProjectSerializer : JsonTransformingSerializer<Project>(Project.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        val jsonObject = element.jsonObject

        // If "projectName" is present, map it to "name"
        return if ("projectName" in jsonObject) {
            val nameElement = jsonObject.getValue("projectName")
            JsonObject(mapOf("name" to nameElement))
        } else {
            jsonObject
        }
    }
}

fun main() {
    // Deserializes JSON where the key needs renaming
    val fromExternal = Json.decodeFromString(
        ProjectSerializer,
        """{"projectName":"example"}"""
    )
    println(fromExternal)
    // Project(name=example)

    // Deserializes JSON where the key already matches the property name
    val fromInternal = Json.decodeFromString(
        ProjectSerializer,
        """{"name":"example"}"""
    )
    println(fromInternal)
    // Project(name=example)
}
//sampleEnd

```

-->

## Implement custom serialization logic in JSON
<primary-label ref="experimental-general"/>

If the transformation functions provided by `JsonTransformingSerializer` or
`JsonContentPolymorphicSerializer` aren't enough, you can implement custom
serialization logic by defining your own [`KSerializer`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-k-serializer/) class.

This gives you full control over how values are serialized and deserialized by overriding the
`serialize()` and `deserialize()` functions directly.

When you implement custom serialization logic for JSON, you can cast `Encoder` to
`JsonEncoder` and `Decoder` to `JsonDecoder` to call JSON-specific functions such as
[`decodeJsonElement()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-decoder/decode-json-element.html) and [`encodeToJsonElement()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/encode-to-json-element.html).
These functions allow you to retrieve and insert JSON elements at specific points in the serialization process.

Both `JsonDecoder` and `JsonEncoder` expose a `json` property that gives access to the active `Json` instance, which controls how values are encoded and decoded.
Through that instance, you can use [`encodeToJsonElement()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/encode-to-json-element.html) and [`decodeFromJsonElement()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/decode-from-json-element.html) to convert
between `JsonElement` instances and Kotlin objects.

Using these APIs, you can implement two-stage conversions. For example, you can:

* decode the input into a `JsonElement` first and then convert that element into a Kotlin value.
* convert a Kotlin value into a `JsonElement` first and then encode that element with the encoder.

Here's an example that shows how to implement a custom `KSerializer` to fully control how a type is encoded and decoded in JSON:

```kotlin
// Imports declarations from the serialization library
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

// Implements custom serialization logic for Response
@OptIn(InternalSerializationApi::class)
class ResponseSerializer<T>(
    private val dataSerializer: KSerializer<T>
) : KSerializer<Response<T>> {

    override val descriptor: SerialDescriptor =
        buildSerialDescriptor("Response", PolymorphicKind.SEALED) {
            element("Ok", dataSerializer.descriptor)
            element("Error", buildClassSerialDescriptor("Error") {
                element<String>("message")
            })
        }

    // Deserializes a Response value from JSON
    override fun deserialize(decoder: Decoder): Response<T> {
        // Ensures that the decoder is a JsonDecoder
        require(decoder is JsonDecoder)

        // Decodes the input into a JsonElement
        val element = decoder.decodeJsonElement()

        // Converts the JsonElement into the corresponding Response value
        return if (element is JsonObject && "error" in element) {
            Response.Error(element["error"]!!.jsonPrimitive.content)
        } else {
            Response.Ok(
                decoder.json.decodeFromJsonElement(dataSerializer, element)
            )
        }
    }

    // Serializes a Response value to JSON
    override fun serialize(encoder: Encoder, value: Response<T>) {
        // Ensures that the encoder is a JsonEncoder
        require(encoder is JsonEncoder)

        // Converts the Response value into a JsonElement
        val element = when (value) {
            is Response.Ok ->
                encoder.json.encodeToJsonElement(dataSerializer, value.data)
            is Response.Error ->
                buildJsonObject { put("error", value.message) }
        }

        // Encodes the JsonElement using the encoder
        encoder.encodeJsonElement(element)
    }
}

@Serializable
data class Project(val name: String)

fun main() {
    val responses = listOf(
        Response.Ok(Project("kotlinx.serialization")),
        Response.Error("Not found")
    )

    val json = Json.encodeToString(responses)
    println(json)
    // [{"name":"kotlinx.serialization"},{"error":"Not found"}]

    println(Json.decodeFromString<List<Response<Project>>>(json))
    // [Ok(data=Project(name=kotlinx.serialization)), Error(message=Not found)]
}
```
{kotlin-runnable="true"}

In this example, the `Response` class has a custom serializer that handles `Ok` values directly as JSON values, but handles `Error` values as JSON objects containing the error message.

### Preserve unknown JSON attributes

A common use case for a custom JSON-specific serializer is preserving JSON properties from the input that your serializable class doesn't define.
By default, these properties are ignored during deserialization.

To preserve these JSON properties, implement a custom JSON-specific serializer that collects all properties not defined in the target class into a dedicated [`JsonObject`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-object/) field during deserialization.
This allows you to preserve these properties in the serializable class without modifying the original JSON structure.

Here's an example:

```kotlin
// Imports declarations from the serialization library
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

        // Extracts and removes the name property
        val name = json.getValue("name").jsonPrimitive.content

        // Collects all remaining JSON properties into the details field
        val details = json.toMutableMap()
        details.remove("name")
        return UnknownProject(name, JsonObject(details))
    }

    override fun serialize(encoder: Encoder, value: UnknownProject) {
        error("Serialization is not supported")
    }
}

fun main() {
    // Deserializes JSON with properties not defined in the serializable class into UnknownProject
    println(Json.decodeFromString(UnknownProjectSerializer, """{"type":"unknown","name":"example","maintainer":"Unknown","license":"Apache 2.0"}"""))
    // UnknownProject(name=example, details={"type":"Unknown","maintainer":"Unknown","license":"Apache 2.0"})

}
//sampleEnd
```
{kotlin-runnable="true"}

In this example, the preserved JSON properties remain at the same level within the input JSON object as the properties defined in the serializable class.


## What's next

* Learn how to [serialize polymorphic classes](serialization-polymorphism.md) and handle objects of various types within a shared hierarchy.
* Discover [other serialization formats](alternative-serialization-formats.md), such as CBOR and ProtoBuf.
