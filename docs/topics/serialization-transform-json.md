<!--- TEST_NAME JsonTestTransform -->
[//]: # (title: Transform JSON during serialization and deserialization)

To affect the shape and contents of JSON output after serialization, or adapt input to deserialization,
it is possible to write a [custom serializer](serializers.md). However, it may be inconvenient to
carefully follow [Encoder] and [Decoder] calling conventions, especially for relatively small and easy tasks.
For that purpose, Kotlin serialization provides an API that can reduce the burden of implementing a custom
serializer to a problem of manipulating a Json elements tree.

We recommend that you get familiar with the [Serializers](serializers.md) chapter: among other things, it
explains how custom serializers are bound to classes.

Transformation capabilities are provided by the abstract [JsonTransformingSerializer] class which implements [KSerializer].
Instead of direct interaction with `Encoder` or `Decoder`, this class asks you to supply transformations for JSON tree
represented by the [JsonElement] class using the`transformSerialize` and
`transformDeserialize` methods. Let's take a look at the examples.

### Array wrapping

The first example is an implementation of JSON array wrapping for lists.

Consider a REST API that returns a JSON array of `User` objects, or a single object (not wrapped into an array) if there
is only one element in the result.

In the data model, use the [`@Serializable`][Serializable] annotation to specify a custom serializer for a
`users: List<User>` property.

<!--- INCLUDE .*-json-.*
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*
-->

```kotlin
@Serializable
data class Project(
    val name: String,
    @Serializable(with = UserListSerializer::class)
    val users: List<User>
)

@Serializable
data class User(val name: String)
```

Since this example covers only the deserialization case, you can implement `UserListSerializer` and override only the
`transformDeserialize` function. The `JsonTransformingSerializer` constructor takes an original serializer
as parameter (this approach is shown in the section [Constructing collection serializers](serializers.md#constructing-collection-serializers)):

```kotlin
object UserListSerializer : JsonTransformingSerializer<List<User>>(ListSerializer(User.serializer())) {
    // If response is not an array, then it is a single object that should be wrapped into the array
    override fun transformDeserialize(element: JsonElement): JsonElement =
        if (element !is JsonArray) JsonArray(listOf(element)) else element
}
```

Now you can test the code with a JSON array or a single JSON object as inputs.

```kotlin
fun main() {
    println(Json.decodeFromString<Project>("""
        {"name":"kotlinx.serialization","users":{"name":"kotlin"}}
    """))
    println(Json.decodeFromString<Project>("""
        {"name":"kotlinx.serialization","users":[{"name":"kotlin"},{"name":"jetbrains"}]}
    """))
}
```

> You can get the full code [here](../../guide/example/example-json-transform-01.kt).

The output shows that both cases are correctly deserialized into a Kotlin [List].

```text
Project(name=kotlinx.serialization, users=[User(name=kotlin)])
Project(name=kotlinx.serialization, users=[User(name=kotlin), User(name=jetbrains)])
```

<!--- TEST -->

### Array unwrapping

You can also implement the `transformSerialize` function to unwrap a single-element list into a single JSON object
during serialization:

<!--- INCLUDE
import kotlinx.serialization.builtins.*

@Serializable
data class Project(
    val name: String,
    @Serializable(with = UserListSerializer::class)
    val users: List<User>
)

@Serializable
data class User(val name: String)

object UserListSerializer : JsonTransformingSerializer<List<User>>(ListSerializer(User.serializer())) {
-->

```kotlin
    override fun transformSerialize(element: JsonElement): JsonElement {
        require(element is JsonArray) // this serializer is used only with lists
        return element.singleOrNull() ?: element
    }
```

<!--- INCLUDE
}
-->

Now, if you serialize a single-element list of objects from Kotlin:

```kotlin
fun main() {
    val data = Project("kotlinx.serialization", listOf(User("kotlin")))
    println(Json.encodeToString(data))
}
```

> You can get the full code [here](../../guide/example/example-json-transform-02.kt).

You end up with a single JSON object, not an array with one element:

```text
{"name":"kotlinx.serialization","users":{"name":"kotlin"}}
```

<!--- TEST -->

### Manipulating default values

Another kind of useful transformation is omitting specific values from the output JSON, for example, if it
is used as default when missing or for other reasons.

Imagine that you cannot specify a default value for the `language` property in the `Project` data model for some reason,
but you need it omitted from the JSON when it is equal to `Kotlin` (we can all agree that Kotlin should be default anyway).
You can fix it by writing the special `ProjectSerializer` based on
the [Plugin-generated serializer](serializers.md#plugin-generated-serializer) for the `Project` class.

```kotlin
@Serializable
class Project(val name: String, val language: String)

object ProjectSerializer : JsonTransformingSerializer<Project>(Project.serializer()) {
    override fun transformSerialize(element: JsonElement): JsonElement =
        // Filter out top-level key value pair with the key "language" and the value "Kotlin"
        JsonObject(element.jsonObject.filterNot {
            (k, v) -> k == "language" && v.jsonPrimitive.content == "Kotlin"
        })
}
```

In the example below, we are serializing the `Project` class at the top-level, so we explicitly
pass the above `ProjectSerializer` to [Json.encodeToString] function as was shown in
the [Passing a serializer manually](serializers.md#passing-a-serializer-manually) section:

```kotlin
fun main() {
    val data = Project("kotlinx.serialization", "Kotlin")
    println(Json.encodeToString(data)) // using plugin-generated serializer
    println(Json.encodeToString(ProjectSerializer, data)) // using custom serializer
}
```

> You can get the full code [here](../../guide/example/example-json-transform-03.kt).

See the effect of the custom serializer:

```text
{"name":"kotlinx.serialization","language":"Kotlin"}
{"name":"kotlinx.serialization"}
```

<!--- TEST -->

### Content-based polymorphic deserialization

Typically, [polymorphic serialization](polymorphism.md) requires a dedicated `"type"` key
(also known as _class discriminator_) in the incoming JSON object to determine the actual serializer
which should be used to deserialize Kotlin class.

However, sometimes the `type` property may not be present in the input. In this case, you need to guess
the actual type by the shape of JSON, for example by the presence of a specific key.

[JsonContentPolymorphicSerializer] provides a skeleton implementation for such a strategy.
To use it, override its `selectDeserializer` method.
Let's start with the following class hierarchy.

> Note that is does not have to be `sealed` as recommended in the [Sealed classes](polymorphism.md#sealed-classes) section,
> because we are not going to take advantage of the plugin-generated code that automatically selects the
> appropriate subclass, but are going to implement this code manually.

<!--- INCLUDE
import kotlinx.serialization.builtins.*
-->

```kotlin
@Serializable
abstract class Project {
    abstract val name: String
}

@Serializable
data class BasicProject(override val name: String): Project()


@Serializable
data class OwnedProject(override val name: String, val owner: String) : Project()
```

You can distinguish the `BasicProject` and `OwnedProject` subclasses by the presence of
the `owner` key in the JSON object.

```kotlin
object ProjectSerializer : JsonContentPolymorphicSerializer<Project>(Project::class) {
    override fun selectDeserializer(element: JsonElement) = when {
        "owner" in element.jsonObject -> OwnedProject.serializer()
        else -> BasicProject.serializer()
    }
}
```

When you use this serializer to serialize data, either [registered](polymorphism.md#registered-subclasses) or
the default serializer is selected for the actual type at runtime:

```kotlin
fun main() {
    val data = listOf(
        OwnedProject("kotlinx.serialization", "kotlin"),
        BasicProject("example")
    )
    val string = Json.encodeToString(ListSerializer(ProjectSerializer), data)
    println(string)
    println(Json.decodeFromString(ListSerializer(ProjectSerializer), string))
}
```

> You can get the full code [here](../../guide/example/example-json-transform-04.kt).

No class discriminator is added in the JSON output:

```text
[{"name":"kotlinx.serialization","owner":"kotlin"},{"name":"example"}]
[OwnedProject(name=kotlinx.serialization, owner=kotlin), BasicProject(name=example)]
```

<!--- TEST -->

### Under the hood (experimental)

Although abstract serializers mentioned above can cover most of the cases, it is possible to implement similar machinery
manually, using only the [KSerializer] class.
If tweaking the abstract methods `transformSerialize`/`transformDeserialize`/`selectDeserializer` is not enough,
then altering `serialize`/`deserialize` is a way to go.

Here are some useful things about custom serializers with [Json]:

* [Encoder] can be cast to [JsonEncoder], and [Decoder] to [JsonDecoder], if the current format is [Json].
* `JsonDecoder` has the [decodeJsonElement][JsonDecoder.decodeJsonElement] method and `JsonEncoder`
  has the [encodeJsonElement][JsonEncoder.encodeJsonElement] method,
  which basically retrieve an element from and insert an element to a current position in the stream.
* Both [`JsonDecoder`][JsonDecoder.json] and [`JsonEncoder`][JsonEncoder.json] have the `json` property,
  which returns [Json] instance with all settings that are currently in use.
* [Json] has the [encodeToJsonElement][Json.encodeToJsonElement] and [decodeFromJsonElement][Json.decodeFromJsonElement] methods.

Given all that, it is possible to implement two-stage conversion `Decoder -> JsonElement -> value` or
`value -> JsonElement -> Encoder`.
For example, you can implement a fully custom serializer for the following `Response` class so that its
`Ok` subclass is represented directly, but the `Error` subclass is represented by an object with the error message:

<!--- INCLUDE
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
-->

```kotlin
@Serializable(with = ResponseSerializer::class)
sealed class Response<out T> {
    data class Ok<out T>(val data: T) : Response<T>()
    data class Error(val message: String) : Response<Nothing>()
}

class ResponseSerializer<T>(private val dataSerializer: KSerializer<T>) : KSerializer<Response<T>> {
    override val descriptor: SerialDescriptor = buildSerialDescriptor("Response", PolymorphicKind.SEALED) {
        element("Ok", dataSerializer.descriptor)
        element("Error", buildClassSerialDescriptor("Error") {
          element<String>("message")
        })
    }

    override fun deserialize(decoder: Decoder): Response<T> {
        // Decoder -> JsonDecoder
        require(decoder is JsonDecoder) // this class can be decoded only by Json
        // JsonDecoder -> JsonElement
        val element = decoder.decodeJsonElement()
        // JsonElement -> value
        if (element is JsonObject && "error" in element)
            return Response.Error(element["error"]!!.jsonPrimitive.content)
        return Response.Ok(decoder.json.decodeFromJsonElement(dataSerializer, element))
    }

    override fun serialize(encoder: Encoder, value: Response<T>) {
        // Encoder -> JsonEncoder
        require(encoder is JsonEncoder) // This class can be encoded only by Json
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

Having this serializable `Response` implementation, you can take any serializable payload for its data
and serialize or deserialize the corresponding responses:

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
    println(Json.decodeFromString<List<Response<Project>>>(string))
}
```

> You can get the full code [here](../../guide/example/example-json-transform-05.kt).

This gives you fine-grained control on the representation of the `Response` class in the JSON output:

```text
[{"name":"kotlinx.serialization"},{"error":"Not found"}]
[Ok(data=Project(name=kotlinx.serialization)), Error(message=Not found)]
```

<!--- TEST -->

### Maintaining custom JSON attributes

A good example of custom JSON-specific serializer would be a deserializer
that packs all unknown JSON properties into a dedicated field of `JsonObject` type.

Let's add `UnknownProject` &ndash; a class with the `name` property and arbitrary details flattened into the same object:

<!--- INCLUDE
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
-->

```kotlin
data class UnknownProject(val name: String, val details: JsonObject)
```

However, the default plugin-generated serializer requires details
to be a separate JSON object and that's not what we want.

To mitigate that, write an own serializer that uses the fact that it works only with the `Json` format:

```kotlin
object UnknownProjectSerializer : KSerializer<UnknownProject> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("UnknownProject") {
        element<String>("name")
        element<JsonElement>("details")
    }

    override fun deserialize(decoder: Decoder): UnknownProject {
        // Cast to JSON-specific interface
        val jsonInput = decoder as? JsonDecoder ?: error("Can be deserialized only by JSON")
        // Read the whole content as JSON
        val json = jsonInput.decodeJsonElement().jsonObject
        // Extract and remove name property
        val name = json.getValue("name").jsonPrimitive.content
        val details = json.toMutableMap()
        details.remove("name")
        return UnknownProject(name, JsonObject(details))
    }

    override fun serialize(encoder: Encoder, value: UnknownProject) {
        error("Serialization is not supported")
    }
}
```

Now it can be used to read flattened JSON details as `UnknownProject`:

```kotlin
fun main() {
    println(Json.decodeFromString(UnknownProjectSerializer, """{"type":"unknown","name":"example","maintainer":"Unknown","license":"Apache 2.0"}"""))
}
```

> You can get the full code [here](../../guide/example/example-json-transform-06.kt).

```text
UnknownProject(name=example, details={"type":"unknown","maintainer":"Unknown","license":"Apache 2.0"})
```

<!--- TEST -->
