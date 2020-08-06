<!--- TEST_NAME JsonTest -->

# JSON features

This is the fifth chapter of the [Kotlin Serialization Guide](serialization-guide.md).
In this chapter we'll walk through various [Json] features.

**Table of contents**

<!--- TOC -->

* [Json configuration](#json-configuration)
  * [Pretty printing](#pretty-printing)
  * [Lenient parsing](#lenient-parsing)
  * [Ignoring unknown keys](#ignoring-unknown-keys)
  * [Coercing input values](#coercing-input-values)
  * [Encoding defaults](#encoding-defaults)
  * [Allowing structured map keys](#allowing-structured-map-keys)
  * [Allowing special floating-point values](#allowing-special-floating-point-values)
  * [Class discriminator](#class-discriminator)
* [Json elements](#json-elements)
  * [Parsing to Json element](#parsing-to-json-element)
  * [Subtypes of Json elements](#subtypes-of-json-elements)
  * [Json element builders](#json-element-builders)
  * [Decoding Json element](#decoding-json-element)
* [Json transformations](#json-transformations)
  * [Array wrapping](#array-wrapping)
  * [Array unwrapping](#array-unwrapping)
  * [Manipulating default values](#manipulating-default-values)
  * [Content-based polymorphic deserialization](#content-based-polymorphic-deserialization)
  * [Under the hood (experimental)](#under-the-hood-experimental)

<!--- END -->

## Json configuration

By default, [Json] implementation is quite strict with respect to invalid inputs, enforces Kotlin type safety, and
restricts Kotlin values that can be serialized so that the resulting JSON representations are standard.
Many non-standard JSON features are supported by creating a custom instance of a JSON _format_.    

JSON format configuration can be specified by creating your own [Json] class instance using an existing 
instance, such as a default `Json` object, and a [Json()] builder function. Additional parameters
are specified in a block via [JsonBuilder] DSL. The resulting `Json` format instance is immutable and thread-safe; 
it can be simply stored in a top-level property. 

> It is recommended to store and reuse custom instances of formats for performance reasons as format implementations
> may cache format-specific additional information about the classes they serialize. 

This chapter shows various configuration features that [Json] supports.

<!--- INCLUDE .*-json-.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
-->

### Pretty printing

JSON can be configured to pretty print the output by setting the [prettyPrint][JsonBuilder.prettyPrint] property.

```kotlin
val format = Json { prettyPrint = true }

@Serializable 
data class Project(val name: String, val language: String)

fun main() {                                      
    val data = Project("kotlinx.serialization", "Kotlin")
    println(format.encodeToString(data))
}
```

> You can get the full code [here](../guide/example/example-json-01.kt).

It gives the following nice result.

```text 
{
    "name": "kotlinx.serialization",
    "language": "Kotlin"
}
``` 

<!--- TEST -->


### Lenient parsing

By default, [Json] parser enforces various JSON restrictions to be as specification-compliant as possible 
(see [RFC-4627]). Keys must be quoted, literals shall be unquoted. Those restrictions can be relaxed with
the [isLenient][JsonBuilder.isLenient] property. With `isLenient = true` we can parse quite freely-formatted data.

```kotlin
val format = Json { isLenient = true }

enum class Status { SUPPORTED }                                                     

@Serializable 
data class Project(val name: String, val status: Status, val votes: Int)
    
fun main() {             
    val data = format.decodeFromString<Project>("""
        { 
            name   : kotlinx.serialization,
            status : SUPPORTED,
            votes  : "9000"
        }
    """)
    println(data)
}
```                                  

> You can get the full code [here](../guide/example/example-json-02.kt).

We get the object, even though all keys, string and enum values are unquoted, while an integer was quoted. 
 
```text
Project(name=kotlinx.serialization, status=SUPPORTED, votes=9000)
``` 

<!--- TEST -->

### Ignoring unknown keys

JSON format is often used to read the output of 3rd-party services or in otherwise highly-dynamic environment where
new properties could be added as a part of API evolution. By default, unknown keys encountered during deserialization produces an error. 
This behavior can be configured with 
the [ignoreUnknownKeys][JsonBuilder.ignoreUnknownKeys] property.

```kotlin
val format = Json { ignoreUnknownKeys = true }

@Serializable 
data class Project(val name: String)
    
fun main() {             
    val data = format.decodeFromString<Project>("""
        {"name":"kotlinx.serialization","language":"Kotlin"}
    """)
    println(data)
}
```                                  

> You can get the full code [here](../guide/example/example-json-03.kt).

It decodes the object, despite the fact that it is missing the `language` property.
 
```text
Project(name=kotlinx.serialization)
``` 

<!--- TEST -->

### Coercing input values

JSON formats that are encountered in the wild can be flexible in terms of types and evolve quickly.
This can lead to exceptions during decoding when the actual values do not match the expected values. 
By default [Json] implementation is strict with respect to input types as was demonstrated in
the [Type safety is enforced](basic-serialization.md#type-safety-is-enforced) section. It can be somewhat relaxed using
the [coerceInputValues][JsonBuilder.coerceInputValues] property. 

This property only affects decoding. It treats a limited subset of invalid input values as if the
corresponding property was missing and uses a default value of the corresponding property instead.
The current list of supported invalid values is:

* `null` inputs for non-nullable types.
* Unknown values for enums.

> This list may be expanded in the future, so that [Json] instance configured with this property becomes even more
> permissive to invalid value in the input, replacing them with defaults.    

Let us take the example from the [Type safety is enforced](basic-serialization.md#type-safety-is-enforced) section.

```kotlin
val format = Json { coerceInputValues = true }

@Serializable 
data class Project(val name: String, val language: String = "Kotlin")

fun main() {
    val data = format.decodeFromString<Project>("""
        {"name":"kotlinx.serialization","language":null}
    """)
    println(data)
}
```                                  

> You can get the full code [here](../guide/example/example-json-04.kt).

We see that invalid `null` value for the `language` property was coerced into the default value.

```text
Project(name=kotlinx.serialization, language=Kotlin)
```    

<!--- TEST -->


### Encoding defaults 

Default values of properties don't have to be encoded, because they will be reconstructed during encoding anyway.
It can be configured by the [encodeDefaults][JsonBuilder.encodeDefaults] property.
This is especially useful for nullable properties with null defaults to avoid writing the corresponding 
null values.

```kotlin
val format = Json { encodeDefaults = false }

@Serializable 
class Project(
    val name: String, 
    val language: String = "Kotlin",
    val website: String? = null
)           

fun main() {
    val data = Project("kotlinx.serialization")
    println(format.encodeToString(data))
}
```                                  

> You can get the full code [here](../guide/example/example-json-05.kt).

Produces the following output which has only the `name` property:

```text
{"name":"kotlinx.serialization"}
```                 

<!--- TEST -->

### Allowing structured map keys

JSON format does not natively support the concept of a map with structured keys. Keys in JSON objects
are strings and can be used to represent only primitives or enums by default.
Non-standard support for structured keys can be enabled with 
the [allowStructuredMapKeys][JsonBuilder.allowStructuredMapKeys] property.

```kotlin
val format = Json { allowStructuredMapKeys = true }

@Serializable 
data class Project(val name: String)
    
fun main() {             
    val map = mapOf(
        Project("kotlinx.serialization") to "Serialization",
        Project("kotlinx.coroutines") to "Coroutines"
    )
    println(format.encodeToString(map))
}
```                                  

> You can get the full code [here](../guide/example/example-json-06.kt).

The map with structured keys gets represented as `[key1, value1, key2, value2,...]` JSON array.
 
```text
[{"name":"kotlinx.serialization"},"Serialization",{"name":"kotlinx.coroutines"},"Coroutines"]
``` 

<!--- TEST -->           

### Allowing special floating-point values

By default, special floating-point values like [Double.NaN] and infinities are not supported in JSON, because
the JSON specification prohibits it.
But they can be enabled using the [allowSpecialFloatingPointValues][JsonBuilder.allowSpecialFloatingPointValues]
property.

```kotlin       
val format = Json { allowSpecialFloatingPointValues = true }

@Serializable
class Data(
    val value: Double
)                     

fun main() {
    val data = Data(Double.NaN)
    println(format.encodeToString(data))
}
```                                   

> You can get the full code [here](../guide/example/example-json-07.kt).

This example produces the following non-stardard JSON output, yet it is a widely used encoding for
special values in JVM world.

```text
{"value":NaN}
```   

<!--- TEST -->

### Class discriminator

A key name that specifies a type when you have a polymorphic data can be specified 
with the [classDiscriminator][JsonBuilder.classDiscriminator] property. 

```kotlin        
val format = Json { classDiscriminator = "#class" }

@Serializable
sealed class Project {
    abstract val name: String
}
            
@Serializable         
@SerialName("owned")
class OwnedProject(override val name: String, val owner: String) : Project()

fun main() {
    val data: Project = OwnedProject("kotlinx.coroutines", "kotlin")
    println(format.encodeToString(data))
}  
```

> You can get the full code [here](../guide/example/example-json-08.kt).

In combination with an explicitly specified [SerialName] of the class it provides full
control on the resulting JSON object. 

```text 
{"#class":"owned","name":"kotlinx.coroutines","owner":"kotlin"}
```                   

<!--- TEST -->

## Json elements

So far, we've been working with JSON format by converting objects to strings and back. However, JSON is often so 
flexible in practice that you might need to tweak the data before it can parse or otherwise work with such an 
unstructured data that it does not readily fit into the typesafe world of Kotlin serialization.

### Parsing to Json element

A string can _parsed_ into an instance of [JsonElement] with the [Json.parseToJsonElement] function.
It is called neither decoding nor deserialization, because none of that happens in the process. 
Only JSON parser is being used here.  

```kotlin
fun main() {
    val element = Json.parseToJsonElement("""
        {"name":"kotlinx.serialization","language":"Kotlin"}
    """)
    println(element)
}
```

> You can get the full code [here](../guide/example/example-json-09.kt).

A `JsonElement` prints itself as a valid JSON.

```text 
{"name":"kotlinx.serialization","language":"Kotlin"}
```
    
<!--- TEST -->
    
### Subtypes of Json elements

A [JsonElement] class has three direct subtypes, closely following JSON grammar.

* [JsonPrimitive] represents all primitive JSON elements, such as string, number, boolean, and null.
  Each primitive has a simple string [content][JsonPrimitive.content]. There is also a 
  [JsonPrimitive()] constructor function overloaded to accept various primitive Kotlin types and
  to convert them to `JsonPrimitive`.
  
* [JsonArray] represents a JSON `[...]` array. It is a Kotlin [List] of `JsonElement`.
  
* [JsonObject] represents a JSON `{...}` object. It is a Kotlin [Map] from `String` key to `JsonElement` value.

The `JsonElement` class has `jsonXxx` extensions that cast it to its corresponding subtypes
([jsonPrimitive], [jsonArray], [jsonObject]). The `JsonPrimitive` class, in turn,
has convenient converters to Kotlin primitive types ([int], [intOrNull], [long], [longOrNull], etc)
that allow fluent code to work with JSON for which you know the structure of.

```kotlin
fun main() {
    val element = Json.parseToJsonElement("""
        {
            "name": "kotlinx.serialization",
            "forks": [{"votes": 42}, {"votes": 9000}, {}]
        }
    """)
    val sum = element
        .jsonObject["forks"]!!
        .jsonArray.sumOf { it.jsonObject["votes"]?.jsonPrimitive?.int ?: 0 }
    println(sum)
}
```

> You can get the full code [here](../guide/example/example-json-10.kt).

The above example sums `votes` in all objects in the `forks` array, ignoring the objects that have no `votes`, but 
failing if the structure of the data is otherwise different.

```text 
9042
```                 

<!--- TEST -->

### Json element builders

We can construct instances of specific [JsonElement] subtypes using the respective builder functions
[buildJsonArray] and [buildJsonObject]. They provide a DSL to define the resulting structure that
is similar to Kotlin standard library collection builders, but with some added JSON-specific convenience
of more type-specific overloads and inner builder functions. The following example shows
all the key features.  

```kotlin
fun main() {
    val element = buildJsonObject {
        put("name", "kotlinx.serialization")
        putJsonObject("owner") {
            put("name", "kotlin")
        }
        putJsonArray("forks") {
            addJsonObject {
                put("votes", 42)
            }
            addJsonObject {
                put("votes", 9000)
            }
        }
    }
    println(element)
}
```

> You can get the full code [here](../guide/example/example-json-11.kt).

At the end, we get a proper JSON string.
 
```text 
{"name":"kotlinx.serialization","owner":{"name":"kotlin"},"forks":[{"votes":42},{"votes":9000}]}
```    

<!--- TEST -->

### Decoding Json element

An instance of the [JsonElement] class can be decoded into a serializable object using 
the [Json.decodeFromJsonElement] function.

```kotlin
@Serializable 
data class Project(val name: String, val language: String)

fun main() {
    val element = buildJsonObject {
        put("name", "kotlinx.serialization")
        put("language", "Kotlin")
    }
    val data = Json.decodeFromJsonElement<Project>(element)
    println(data)
}
```

> You can get the full code [here](../guide/example/example-json-12.kt).

The result is exactly what we would expect.

```text 
Project(name=kotlinx.serialization, language=Kotlin)
``` 

<!--- TEST -->

## Json transformations

To affect the shape and contents of JSON output after serialization, or adapt input to deserialization,
it is possible to write a [custom serializer](serializers.md). However, it may not be convenient to
carefully follow [Encoder] and [Decoder] calling conventions, especially for relatively small and easy tasks.
For that purpose, Kotlin serialization provides an API that can reduce the burden of implementing a custom 
serializer to a problem of manipulating a Json elements tree.

You are still strongly advised to become familiar with the [Serializers](serializers.md) chapter, as
it explains, among other things, how custom serializers are bound to classes.

Transformation capabilities are provided by the abstract [JsonTransformingSerializer] class which implements [KSerializer]. 
Instead of direct interaction with `Encoder` or `Decoder`, this class asks you to supply transformations for JSON tree 
represented by the [JsonElement] class using the 
[transformSerialize][JsonTransformingSerializer.transformSerialize] and 
[transformDeserialize][JsonTransformingSerializer.transformDeserialize] methods. Let us take a look at the examples.

### Array wrapping

The first example is our own implementation of JSON array wrapping for lists. Consider a REST API that returns a 
JSON array of `User` objects, or, if there is only one element in the result, then it is a single object, not wrapped 
into an array. In our data model, we use [`@Serializable`][Serializable] annotation to specify a custom serializer for a
`users: List<User>` property.  

<!--- INCLUDE
import kotlinx.serialization.builtins.*
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

For now, we are only concerned with deserialization, so we implement `UserListSerializer` and override only the 
`transformDeserialize` function. The `JsonTransformingSerializer` constructor takes an original serializer 
as parameter and here we use the approach from 
the [Constructing collection serializers](serializers.md#constructing-collection-serializers) section 
to create one.  

```kotlin
object UserListSerializer : JsonTransformingSerializer<List<User>>(ListSerializer(User.serializer())) {
    // If response is not an array, then it is a single object that should be wrapped into the array
    override fun transformDeserialize(element: JsonElement): JsonElement =
        if (element !is JsonArray) JsonArray(listOf(element)) else element
}
```

Now we can test our code with a JSON array or a single JSON object as inputs.

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

> You can get the full code [here](../guide/example/example-json-13.kt).

The output shows that both cases are correctly deserialized into a Kotlin [List].

```text 
Project(name=kotlinx.serialization, users=[User(name=kotlin)])
Project(name=kotlinx.serialization, users=[User(name=kotlin), User(name=jetbrains)])
```  

<!--- TEST -->

### Array unwrapping

We can also implement the `transformSerialize` function to unwrap a single-element list into a single JSON object 
during serialization. 

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
        require(element is JsonArray) // we are using this serializer with lists only
        return element.singleOrNull() ?: element
    }
```

<!--- INCLUDE
}
-->

Now, when we start with a single-element list of objects in Kotlin.

```kotlin 
fun main() {     
    val data = Project("kotlinx.serialization", listOf(User("kotlin")))
    println(Json.encodeToString(data))
}
```

> You can get the full code [here](../guide/example/example-json-14.kt).

We end up with a single JSON object. 

```text 
{"name":"kotlinx.serialization","users":{"name":"kotlin"}}
```  

<!--- TEST -->

### Manipulating default values

Another kind of useful transformation is omitting specific values from the output JSON, e.g. because it 
is treated as default when missing or for any other domain-specific reasons.

Suppose that our `Project` data model cannot specify a default value for the `language` property, 
but it has to omitted from the JSON when it is equal to `Kotlin` (we can all agree that Kotlin should be default anyway).
We'll fix it by writing the special `ProjectSerializer` based on 
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
the [Passing a serializer manually](serializers.md#passing-a-serializer-manually) section.   

```kotlin
fun main() {
    val data = Project("kotlinx.serialization", "Kotlin")
    println(Json.encodeToString(data)) // using plugin-generated serializer
    println(Json.encodeToString(ProjectSerializer, data)) // using custom serializer
}
```

> You can get the full code [here](../guide/example/example-json-15.kt).

We can clearly see the effect of the custom serializer.

```text 
{"name":"kotlinx.serialization","language":"Kotlin"}
{"name":"kotlinx.serialization"}
```

<!--- TEST -->

### Content-based polymorphic deserialization

Typically, [polymorphic serialization](polymorphism.md) requires a dedicated `"type"` key 
(also known as class discriminator) in the incoming JSON object to determine the actual serializer
which should be used to deserialize Kotlin class.

However, sometimes type property may not be present in the input, and it is expected to guess the actual type by the 
shape of JSON, for example by the presence of a specific key.

[JsonContentPolymorphicSerializer] provides a skeleton implementation for such a strategy.
To use it, we override its [selectDeserializer][JsonContentPolymorphicSerializer.selectDeserializer] method.
Let us start with the following class hierarchy. 

> Note, that is does not have to be `sealed` as recommended in the [Sealed classes](polymorphism.md#sealed-classes) section,
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

We want to distinguish between the `BasicProject` and `OwnedProject` subclasses by the presence of 
the `owner` key in the JSON object.

```kotlin
object ProjectSerializer : JsonContentPolymorphicSerializer<Project>(Project::class) {
    override fun selectDeserializer(element: JsonElement) = when {
        "owner" in element.jsonObject -> OwnedProject.serializer()
        else -> BasicProject.serializer()
    }
}
```

We can serialize data with such serializer. In that case, either [registered](polymorphism.md#registered-subclasses) or
the default serializer is selected for the actual type at runtime. 

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

> You can get the full code [here](../guide/example/example-json-16.kt).

No class discriminator is added in the JSON output.

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

There are several tidbits on custom serializers with [Json].

* [Encoder] can be cast to [JsonEncoder], and [Decoder] to [JsonDecoder], if the current format is [Json].
* `JsonDecoder` has the [decodeJsonElement][JsonDecoder.decodeJsonElement] method and `JsonEncoder` 
  has the [encodeJsonElement][JsonEncoder.encodeJsonElement] method.
  which basically retrieve/insert an element from/to a current position in the stream.
* Both [`JsonDecoder`][JsonDecoder.json] and [`JsonEncoder`][JsonEncoder.json] have the `json` property 
  which returns [Json] instance with all settings that are currently in use.
* [Json] has the [encodeToJsonElement][Json.encodeToJsonElement] and [decodeFromJsonElement][Json.decodeFromJsonElement] methods.

Given all that, it is possible to implement two-stage conversion `Decoder -> JsonElement -> value` or  
`value -> JsonElement -> Encoder`.
For example, we can implement a fully custom serializer for the following `Response` class so that its 
`Ok` subclass is represented directly, but `Error` subclass by an object with the error message.

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
        element("Ok", buildClassSerialDescriptor("Ok") {
            element<String>("message")
        })
        element("Error", dataSerializer.descriptor)
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

Armed with this serializable `Response` implementation we can take any serializable payload for its data 
and serialize/deserialize the corresponding responses.

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

> You can get the full code [here](../guide/example/example-json-17.kt).

This gives us fine-grained control on the representation of the `Response` class in our JSON output.

```text       
[{"name":"kotlinx.serialization"},{"error":"Not found"}]
[Ok(data=Project(name=kotlinx.serialization)), Error(message=Not found)]
```     

<!--- TEST -->

---

The next chapter covers [Alternative and custom formats (experimental)](formats.md).


<!-- references -->
[RFC-4627]: https://www.ietf.org/rfc/rfc4627.txt

<!-- stdlib references -->
[Double.NaN]: https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-double/-na-n.html
[List]: https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/ 
[Map]: https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/ 

<!--- MODULE /kotlinx-serialization -->
<!--- INDEX kotlinx.serialization -->
[SerialName]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization/-serial-name/index.html
[KSerializer]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization/-k-serializer/index.html
[Serializable]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization/-serializable/index.html
<!--- INDEX kotlinx.serialization.encoding -->
[Encoder]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.encoding/-encoder/index.html
[Decoder]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.encoding/-decoder/index.html
<!--- INDEX kotlinx.serialization.json -->
[Json]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.json/-json/index.html
[Json()]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.json/-json.html
[JsonBuilder]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.json/-json-builder/index.html
[JsonBuilder.prettyPrint]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.json/-json-builder/pretty-print.html
[JsonBuilder.isLenient]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.json/-json-builder/is-lenient.html
[JsonBuilder.ignoreUnknownKeys]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.json/-json-builder/ignore-unknown-keys.html
[JsonBuilder.coerceInputValues]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.json/-json-builder/coerce-input-values.html
[JsonBuilder.encodeDefaults]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.json/-json-builder/encode-defaults.html
[JsonBuilder.allowStructuredMapKeys]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.json/-json-builder/allow-structured-map-keys.html
[JsonBuilder.allowSpecialFloatingPointValues]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.json/-json-builder/allow-special-floating-point-values.html
[JsonBuilder.classDiscriminator]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.json/-json-builder/class-discriminator.html
[JsonElement]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.json/-json-element.html
[Json.parseToJsonElement]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.json/-json/parse-to-json-element.html
[JsonPrimitive]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.json/-json-primitive/index.html
[JsonPrimitive.content]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.json/-json-primitive/content.html
[JsonPrimitive()]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.json/-json-primitive.html
[JsonArray]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.json/-json-array/index.html
[JsonObject]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.json/-json-object/index.html
[jsonPrimitive]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.json/json-primitive.html
[jsonArray]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.json/json-array.html
[jsonObject]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.json/json-object.html
[int]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.json/int.html
[intOrNull]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.json/int-or-null.html
[long]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.json/long.html
[longOrNull]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.json/long-or-null.html
[buildJsonArray]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.json/build-json-array.html
[buildJsonObject]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.json/build-json-object.html
[Json.decodeFromJsonElement]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.json/-json/decode-from-json-element.html
[JsonTransformingSerializer]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.json/-json-transforming-serializer/index.html
[JsonTransformingSerializer.transformSerialize]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.json/-json-transforming-serializer/transform-serialize.html
[JsonTransformingSerializer.transformDeserialize]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.json/-json-transforming-serializer/transform-deserialize.html
[Json.encodeToString]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.json/-json/encode-to-string.html
[JsonContentPolymorphicSerializer]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.json/-json-content-polymorphic-serializer/index.html
[JsonContentPolymorphicSerializer.selectDeserializer]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.json/-json-content-polymorphic-serializer/select-deserializer.html
[JsonEncoder]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.json/-json-encoder/index.html
[JsonDecoder]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.json/-json-decoder/index.html
[JsonDecoder.decodeJsonElement]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.json/-json-decoder/decode-json-element.html
[JsonEncoder.encodeJsonElement]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.json/-json-encoder/encode-json-element.html
[JsonDecoder.json]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.json/-json-decoder/json.html
[JsonEncoder.json]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.json/-json-encoder/json.html
[Json.encodeToJsonElement]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.json/-json/encode-to-json-element.html
<!--- END -->

