# Json-specific serializers

To affect the shape and contents of JSON output after serialization, or adapt input to deserialization,
one may write a [custom serializer](custom_serializers.md). However, it may not be convenient to
carefully follow complex Encoder and Decoder calling conventions, especially for relatively small and easy tasks.
For that purpose, kotlinx.serialization library gives you some abstract classes, which
can boil down the burden of implementing a custom serializer to a problem of manipulating a Json abstract syntax tree.

* [Prerequisites](#prerequisites)
* [Json transformations](#json-transformations)
    + [Samples](#samples)
* [Json parametric polymorphic deserialization](#json-parametric-polymorphic-deserialization)
* [Under the hood](#under-the-hood)

## Prerequisites

It is still strongly recommended to be acquainted with [custom serializers guide](custom_serializers.md)
in its [using serializers](custom_serializers.md#using-custom-serializers) part,
since the instantiation process and annotations to enable custom serializers remain the same.
It is also important to remember that since the discussed in this article serializers are manipulating Json AST,
they have the following limitations:

1. These serializers work only with `Json` format.
2. One may expect slightly degraded performance or increased memory traffic since
standard Json encoders do not usually create a full syntax tree.

However, these limitations are not really the issue for all typical use-cases.

## Json transformations

Such functionality is provided by the class `JsonTransformingSerializer`. `JsonTransformingSerializer` is an abstract
class that implements `KSerializer`. Instead of direct interaction with Encoder or Decoder, this class
asks you to supply transformations for JSON AST represented by the `JsonElement` class.
You may use it as a base class for your serializers. In that case, you won't be able to override
`serialize` and `deserialize` methods — instead,
you should define corresponding `writeTransform` or/and `readTransform` methods.
These methods have extremely simple signature: `(JsonElement) -> JsonElement`, which does not need explanations.
See `JsonElement`, `JsonObject`, `JsonArray`, and `JsonPrimitive` documentation for details.

### Samples

Here are the samples of several popular (judged by open issues) transformations. You can copy-paste them to your project.

```kotlin
// We're going to use this simple class as a target for our transformations
@Serializable
data class StringData(val data: String)

// This transformation is suitable for APIs which can return either list or a single object
// under the same key.
// It always returns JsonArray, therefore serializer type is List<StringData>.
object WrappingJsonListSerializer :
    JsonTransformingSerializer<List<StringData>>(StringData.serializer().list, "WrappingList") {
    override fun readTransform(element: JsonElement): JsonElement =
        if (element !is JsonArray) JsonArray(listOf(element)) else element
    }

// This transformation does the opposite and can unwrap an element,
// if it is returned in an array.
object UnwrappingJsonListSerializer :
    JsonTransformingSerializer<StringData>(StringData.serializer(), "UnwrappingList") {
    override fun readTransform(element: JsonElement): JsonElement {
        if (element !is JsonArray) return element
        require(element.size == 1) { "Array size must be equal to 1 to unwrap it" }
        return element.first()
    }
}

// We can use these transformations as regular serializers:
@Serializable
data class Example(
    val name: String,
    @Serializable(UnwrappingJsonListSerializer::class) val data: StringData,
    @Serializable(WrappingJsonListSerializer::class) val moreData: List<StringData>
)

// And be able to parse different shapes of the same data, such as:
```

```json
{"name":"test","data":{"data":"str1"},"moreData":[{"data":"str2"}]}
{"name":"test","data":{"data":"str1"},"moreData":{"data":"str2"}}
{"name":"test","data":[{"data":"str1"}],"moreData":[{"data":"str2"}]}
{"name":"test","data":[{"data":"str1"}],"moreData":{"data":"str2"}}
```

```kotlin
// Here's the sample of complex write transformation
// which elide values based on a predicate
object DroppingNameSerializer : JsonTransformingSerializer<Example>(Example.serializer(), "DropName") {
    override fun writeTransform(element: JsonElement): JsonElement =
        JsonObject(element.jsonObject.filterNot {
            (k, v) -> k == "name" && v.primitive.content == "Second"
        })
}

// Result of such transformation would be:
json.stringify(DroppingNameSerializer, Example("First", StringData("str1")))
    // =>  """{"name":"First","data":{"data":"str1"}}"""

json.stringify(DroppingNameSerializer, Example("Second", StringData("str1")))
    // =>  """{"data":{"data":"str1"}}"""
```

## Json parametric polymorphic deserialization

Usually, [polymorphic serialization](polymorphism.md) requires a dedicated `"type"` property
(a so-called 'class discriminator') in JSON stream to determine actual serializer
which can be used to deserialize Kotlin class.
However, sometimes (often when interacting with external API) such property is not present in input,
and one should guess type using other evidence, e.g., presence of some keys in object.
`JsonParametricSerializer` offers a skeleton implementation of such a guessing strategy.
As with `JsonTransformingSerializer`, `JsonParametricSerializer` is a base class for custom serializers.
It does not allow to override `serialize` and `deserialize` methods; instead, one should
implement `selectSerializer(element: JsonElement): KSerializer<out T>` method.
The idea is understandable from the example:

```kotlin
interface Payment {
    val amount: String
}

@Serializable
data class SuccessfulPayment(override val amount: String, val date: String) : Payment

@Serializable
data class RefundedPayment(override val amount: String, val date: String, val reason: String) : Payment

object PaymentSerializer : JsonParametricSerializer<Payment>(Payment::class) {
    override fun selectSerializer(element: JsonElement): KSerializer<out Payment> = when {
        "reason" in element -> RefundedPayment.serializer()
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

Although abstract serializers mentioned above can cover most of the cases, it is not a hard task to implement such machinery
by yourself, given only the `KSerializer`.
If one is not satisfied with abstract methods `writeTransform`/`readTransform`/`selectSerializer`,
altering `serialize`/`deserialize` is a way to go.
There are several hints on reading, working with and writing of `JsonElement`:

* `Encoder` could be cast to `JsonInput`, and `Decoder` to `JsonOutput`, if the current format is `Json`.
* `JsonInput` has method `decodeJson(): JsonElement`, and `JsonOutput` has method `encodeJson(element: JsonElement)`
which basically retrieve/insert an element from/to a current position in the stream.
* Both `JsonInput` and `JsonOutput` have `json` property which returns `Json` instance with all settings that are currently in use.
* `Json` has methods `fromJson(deserializer: DeserializationStrategy<T>, json: JsonElement): T`
and `toJson(serializer: SerializationStrategy<T>, value: T): JsonElement`.

Given all that, it is pretty straightforward to implement two-stage conversion `Decoder -> JsonElement -> T` or `T -> JsonElement -> Encoder`.

Typical usage would look like this:

```kotlin
// Class representing Either<Left|Right>
sealed class Either {
    data class Left(val errorMsg: String) : Either()
    data class Right(val data: Payload) : Either()
}

// Serializer injects custom behavior by inspecting object content and writing
object EitherSerializer : KSerializer<Either> {
    override val descriptor: SerialDescriptor = SerialClassDescImpl("Either")

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
