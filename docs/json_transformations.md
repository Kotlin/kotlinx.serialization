# Json-specific serializers

To affect the shape and contents of JSON output after serialization, or adapt input to deserialization,
it is possible to write a [custom serializer](custom_serializers.md). However, it may not be convenient to
carefully follow complex Encoder and Decoder calling conventions, especially for relatively small and easy tasks.
For that purpose, `kotlinx.serialization` library gives you API that
can boil down the burden of implementing a custom serializer to a problem of manipulating a Json elements tree.

* [Prerequisites](#prerequisites)
* [Json transformations](#json-transformations)
    + [Samples](#samples)
* [Json parametric polymorphic deserialization](#json-parametric-polymorphic-deserialization)
* [Under the hood](#under-the-hood)

## Prerequisites

It is still strongly recommended to be acquainted with [custom serializers guide](custom_serializers.md)
in its [using serializers](custom_serializers.md#using-custom-serializers) part,
since the instantiation process and annotations to enable custom serializers remain the same.
It is also important to remember that since the discussed in this article serializers are manipulating Json tree,
they work only with Json format.

## Json transformations

Transformation capabilities are provided by the abstract `JsonTransformingSerializer` class that implements `KSerializer`. 

Instead of direct interaction with Encoder or Decoder, this class asks you to supply transformations for JSON tree represented by the `JsonElement` class
using `transformSerialize(element: JsonElement): JsonElement` and `transformDeserialize(element: JsonElement): JsonElement` methods in order
to transform the input or the putput JSON.


### Transformation examples. List mainupulation

The first example is our own implementation of list wrapping. Consider the API that returns list 
of objects, or, if there is only one element in the result, then it is a single object, not wrapped in the list.
 
To simplify object model, it is possible to implement transforming serializer that always returns list of objects,
automatically wrapping a single object in a list as well: 

```kotlin
@Serializable // Sample class with data
data class StringData(val data: String)


object WrappingJsonListSerializer : JsonTransformingSerializer<List<StringData>>(
    StringData.serializer().list
) {
    // If response is not an array, then it is a single object that should be wrapped in the array
    override fun transformDeserialize(element: JsonElement): JsonElement =
        if (element !is JsonArray) JsonArray(listOf(element)) else element
}

// This transformation does the opposite and can unwrap an element, if it is returned in an array.
object UnwrappingJsonListSerializer :
    JsonTransformingSerializer<StringData>(StringData.serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        if (element !is JsonArray) return element
        require(element.size == 1) { "Array size must be equal to 1 to unwrap it automatically" }
        return element.first()
    }
}
```

Now these serializers can be used as regular custom serializers and all transformations will be applied automatically:
```
// We can use these transformations as regular serializers:
@Serializable
data class Example(
    val name: String,
    @Serializable(UnwrappingJsonListSerializer::class) val data: StringData,
    @Serializable(WrappingJsonListSerializer::class) val moreData: List<StringData>
)

// And for input 
{"name":"test","data":{"data":"str1"},"moreData": {"data":"str2"} }

the corresponding Example("test", Data("str1"), listOf(Data("str2"))) will be deserialized.
```

### Transformation examples. Default values mainupulation.

Another example of a transformation is omitting specific values from the output JSON, e.g. because it 
is treated as default when missing or for any other domain-specific reasons.
 

```kotlin
// Serializer that removes "name: Second" key-value pair from resultuing JSON
object DroppingNameSerializer : JsonTransformingSerializer<Example>(Example.serializer()) {
    override fun transformSerialize(element: JsonElement): JsonElement =
        // Filter top-level key value pair with key "name" with value equal to "Second"
        JsonObject(element.jsonObject.filterNot {
            (k, v) -> k == "name" && v.jsonPrimitive.content == "Second"
        })
}

// Result of such transformation is:
json.stringify(DroppingNameSerializer, Example("First", StringData("str1")))
// =>  """{"name":"First","data":{"data":"str1"}}"""

json.stringify(DroppingNameSerializer, Example("Second", StringData("str1")))
// =>  """{"data":{"data":"str1"}}"""
```

## Json parametric polymorphic deserialization

Typically, [polymorphic serialization](polymorphism.md) requires a dedicated `"type"` property
(also known as class discriminator) in the incoming JSON to determine actual serializer
which can be used to deserialize Kotlin class.

However, sometimes (e.g. when interacting with external API) type property may not be present in the input,
and it is expected to guess the actual type by the shape of JSON, for example by the presence of specific key.

`JsonContentPolymorphicSerializer` provides a skeleton implementation for such strategy.
As with `JsonTransformingSerializer`, `JsonContentPolymorphicSerializer` is a base class for custom serializers.
It does not allow to override `serialize` and `deserialize` methods; instead, one should
implement `selectDeserializer(content: JsonElement): DeserializationStrategy<out T>` method.
The idea can be demonstrated by the following example:

```kotlin
interface Payment {
    val amount: String
}

@Serializable
data class SuccessfulPayment(override val amount: String, val date: String) : Payment

@Serializable
data class RefundedPayment(override val amount: String, val date: String, val reason: String) : Payment

object PaymentSerializer : JsonContentPolymorphicSerializer<Payment>(Payment::class) {
    override fun selectDeserializer(content: JsonElement) = when {
        "reason" in element.jsonObject -> RefundedPayment.serializer()
        else -> SuccessfulPayment.serializer()
    }
}

// Both statements yield different subclasses of Payment:
Json.parse(PaymentSerializer, """{"amount":"1.0","date":"03.02.2020"}""")
Json.parse(PaymentSerializer, """{"amount":"2.0","date":"03.02.2020","reason":"complaint"}""")
```

You also can serialize data with such serializer. In that case, either [registered](polymorphism.md#basic-case) or
default serializer would be selected for the actual property type in runtime. No class discriminator would be added.

## Under the hood

Although abstract serializers mentioned above can cover most of the cases, it is possible to implement similar machinery
by ourselves, using only the `KSerializer`.
If one is unsatisfied with abstract methods `transformSerialize`/`transformDeserialize`/`selectDeserializer`,
altering `serialize`/`deserialize` is a way to go.

There are several hints on reading, working with and writing of `JsonElement`:

* `Encoder` could be cast to `JsonInput`, and `Decoder` to `JsonOutput`, if the current format is `Json`.
* `JsonInput` has method `decodeJson(): JsonElement`, and `JsonOutput` has method `encodeJson(element: JsonElement)`
which basically retrieve/insert an element from/to a current position in the stream.
* Both `JsonInput` and `JsonOutput` have `json` property which returns `Json` instance with all settings that are currently in use.
* `Json` has methods `fromJson(deserializer: DeserializationStrategy<T>, json: JsonElement): T`
and `toJson(serializer: SerializationStrategy<T>, value: T): JsonElement`.

Given all that, it is possible to implement two-stage conversion `Decoder -> JsonElement -> T` or `T -> JsonElement -> Encoder`.

Typical usage would look like this:

```kotlin
// Class representing Either<Left|Right>
sealed class Either {
    data class Left(val errorMsg: String) : Either()
    data class Right(val data: Payload) : Either()
}

// Serializer injects custom behavior by inspecting object content and writing
object EitherSerializer : KSerializer<Either> {
    override val descriptor: SerialDescriptor = SerialDescriptor("mypackage.Either", UnionKind.SEALED)

    override fun deserialize(decoder: Decoder): Either {
        // Decoder -> JsonInput
        val input = decoder as? JsonInput
            ?: throw SerializationException("This class can be loaded only by Json")
        // JsonInput => JsonElement (JsonObject in this case)
        val tree = input.decodeJson() as? JsonObject
            ?: throw SerializationException("Expected JsonObject")
        if ("error" in tree) return Either.Left(tree.getPrimitive("error").content)
        // JsonElement -> object
        return Either.Right(input.json.decodeJson(tree, Payload.serializer()))
    }

    override fun serialize(encoder: Encoder, obj: Either) {
        // Encoder -> JsonOutput
        val output = encoder as? JsonOutput
            ?: throw SerializationException("This class can be saved only by Json")
        // object -> JsonElement
        val tree = when (obj) {
            is Either.Left -> JsonObject(mapOf("error" to JsonLiteral(obj.errorMsg)))
            is Either.Right -> output.json.toJson(obj.data, Payload.serializer())
        }
        // JsonElement => JsonOutput
        output.encodeJson(tree)
    }
}
```
