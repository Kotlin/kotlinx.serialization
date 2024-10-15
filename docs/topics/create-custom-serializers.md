[//]: # (title: Create custom serializers)

<!--- TEST_NAME SerializersTest -->

Formats like JSON control how an object is encoded into bytes, but a serializer determines how the object’s properties are represented.
Automatically generated serializers with the `@Serializable` annotation can handle most common use cases, making it easy to serialize Kotlin objects without additional configuration.
The Kotlin Serialization plugin automatically generates a `KSerializer` implementation for each class annotated with `@Serializable`,
which you can retrieve using the `.serializer()` function:

```kotlin
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*

//sampleStart
@Serializable
data class Color(val rgb: Int)

fun main() {
    val colorSerializer: KSerializer<Color> = Color.serializer()
    println(colorSerializer.descriptor)
    // Color(rgb: kotlin.Int)
}
//sampleEnd
```
{kotlin-runnable="true"}

<!--- > You can get the full code [here](../../guide/example/example-serializer-01.kt). -->

<!---
```text
Color(rgb: kotlin.Int)
```
-->

<!--- TEST -->

## Create a custom primitive serializer

If you need more control over how your data is encoded and decoded, you can create a custom serializer.
This allows you to customize how your data is represented, such as by converting an RGB integer value into a hexadecimal string.

Custom serializers implement the [`KSerializer`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-k-serializer/) interface,
allowing you to control how your data is transformed between its Kotlin object form and its serialized form.

To create a custom primitive serializer:

1. Use the [`@Serializable`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-serializable/) annotation with the [`with`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-serializable/with.html) property to bind a custom serializer to a class. The `with` property specifies the serializer used for the bound class.
2. Create an `object` that implements the `KSerializer<YourClass>` interface. This allows you to define how instances of your class are serialized and deserialized.
3. Override the [`descriptor`]((https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-k-serializer/descriptor.html)) property to define the schema for the serialized data. Use the [`PrimitiveSerialDescriptor()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.descriptors/-primitive-serial-descriptor.html) function to specify the name and type of the data, such as string, integer, or custom structure.
4. Implement the [`serialize()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-serialization-strategy/serialize.html) function to define how to convert an instance of your class into its serialized form. The [`Encoder`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-encoder/) provides functions to write different data types, such as [`encodeString()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-encoder/encode-string.html).
5. Implement the [`deserialize()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-deserialization-strategy/deserialize.html) function to define how to convert the serialized data back into an instance of your class. The [`Decoder`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-decoder/) provides functions to read the data, such as [`decodeString()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-decoder/decode-string.html).

Let's look at an example:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.*

//sampleStart
// Binds the Color class with the custom ColorAsStringSerializer using the with property
@Serializable(with = ColorAsStringSerializer::class)
data class Color(val rgb: Int)

// Creates the custom serializer for the Color class
object ColorAsStringSerializer : KSerializer<Color> {
    // Defines the schema for the serialized data as a single string
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Color", PrimitiveKind.STRING)

    // Defines how the Color object is converted to a string during serialization
    override fun serialize(encoder: Encoder, value: Color) {
        // Converts the RGB value to a hex string
        val hexValue = value.rgb.toString(16).padStart(6, '0')
        // Encodes the hex string using the encodeString() function
        encoder.encodeString(hexValue)
    }

    // Defines how the string is converted back to a Color object during deserialization
    override fun deserialize(decoder: Decoder): Color {
        // Decodes the string using the decodeString() function
        val hexValue = decoder.decodeString()
        // Converts the hex value back into a Color object
        return Color(hexValue.toInt(16))
    }
}

fun main() {
    val color = Color(0x00FF00)
    // Serializes a color to JSON
    val jsonString = Json.encodeToString(color)
    println(jsonString)
    // "00ff00"

    // Deserializes the color back
    val deserializedColor = Json.decodeFromString<Color>(jsonString)
    println(deserializedColor.rgb)
    // 65280
}
//sampleEnd
```
{kotlin-runnable="true"}

<!--- > You can get the full code [here](../../guide/example/example-serializer-02.kt). -->

<!---
```text
"00ff00"
65280
```
-->

<!--- TEST -->


## Delegate serialization to another serializer

To serialize a class as a different type, you can delegate the serialization logic to another serializer.
This approach allows you to transform the data of your class, like breaking down an `Int` into an `IntArray` of its individual bytes,
and then delegate the actual encoding and decoding to the appropriate serializer.

To delegate the serialization logic, create a custom serializer that uses another serializer as a delegate.
Within the `serialize()` and `deserialize()` functions, you can transform your class’s data and pass it to the delegate
serializer with the [`encodeSerializableValue()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-encoder/encode-serializable-value.html) and [`decodeSerializableValue()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-decoder/decode-serializable-value.html) functions, respectively:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.builtins.IntArraySerializer
import kotlinx.serialization.json.*

//sampleStart
// Creates a custom serializer that delegates to IntArraySerializer
class ColorIntArraySerializer : KSerializer<Color> {
    private val delegateSerializer = IntArraySerializer()
    override val descriptor = SerialDescriptor("Color", delegateSerializer.descriptor)

    // Delegates serialization logic to IntArraySerializer
    override fun serialize(encoder: Encoder, value: Color) {
        val data = intArrayOf(
            (value.rgb shr 16) and 0xFF,
            (value.rgb shr 8) and 0xFF,
            value.rgb and 0xFF
        )
        encoder.encodeSerializableValue(delegateSerializer, data)
    }

    // Delegates deserialization logic to IntArraySerializer
    override fun deserialize(decoder: Decoder): Color {
        val array = decoder.decodeSerializableValue(delegateSerializer)
        return Color((array[0] shl 16) or (array[1] shl 8) or array[2])
    }
}

@Serializable(with = ColorIntArraySerializer::class)
class Color(val rgb: Int)

fun main() {
    val green = Color(0x00ff00)
    println(Json.encodeToString(green))
    // [0,255,0]
}
//sampleEnd
```
{kotlin-runnable="true"}

This array representation may not be ideal for JSON, but it becomes highly efficient when used with a `ByteArray` and binary formats, where it can significantly reduce data size.
For more information on how format can treat arrays, see the [Format specific types](formats.md#format-specific-types) section.

> When using delegated serialization, you cannot rely on the default class serializer’s `descriptor`.
> Formats that depend on schemas may incorrectly assume you are encoding a primitive type, like `Int`,
> and expect `encodeInt()` to be called instead of the [`encodeSerializableValue()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-encoder/encode-serializable-value.html) function.
> Similarly, using a specific serializer’s `descriptor` directly can lead to confusion in formats that treat certain types specially, like arrays.
>
{type="note"}

<!--- > You can get the full code [here](../../guide/example/example-serializer-3.kt). -->

<!---
```text
[0,255,0]
```
-->

<!--- TEST -->

### Serialize with a surrogate class

To serialize a class in a specific structure, you can create a _surrogate class_.
A surrogate class is a separate class that mirrors the desired serialized form of your original class, allowing you to customize how the data is structured during serialization.
This allows you to enforce specific constraints, like ensuring that property values remain within a valid range.
The surrogate class can also have a [custom `@SerialName`](serialization-customization-options.md#customize-serial-names) annotation,
making it indistinguishable from the original class when formats rely on class names.

Just like [delegated serialization](#delegate-serialization-to-another-serializer), surrogate classes also rely on reusing existing serialization logic.
However, instead of transforming or restructuring individual fields within the same class,
you create a new surrogate class to represent the serialized form.

The surrogate class can be [`private`](visibility-modifiers.md#class-members), and you use the `init` block to enforce constraints on the serialized data.
After defining the surrogate class, you can retrieve the plugin-generated serializer for it by calling its `serializer()` function.
The function reuses the automatically generated [`SerialDescriptor`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.descriptors/-serial-descriptor/) of
the surrogate class to describe the structure of the serialized data while maintaining compatibility with the original class.

To serialize with the surrogate class, delegate the serialization process to its serializer using the `encodeSerializableValue()` and `decodeSerializableValue()` functions:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.builtins.IntArraySerializer
import kotlinx.serialization.json.*

//sampleStart
// Defines a private surrogate class with custom properties
@Serializable
@SerialName("Color")
private class ColorSurrogate(val r: Int, val g: Int, val b: Int) {
    init {
        // Ensures values are within a valid range
        require(r in 0..255 && g in 0..255 && b in 0..255)
    }
}

// Custom serializer that delegates to the surrogate class
object ColorSerializer : KSerializer<Color> {
    override val descriptor: SerialDescriptor = ColorSurrogate.serializer().descriptor

    // Serializes the original class as a surrogate
    override fun serialize(encoder: Encoder, value: Color) {
        val surrogate = ColorSurrogate((value.rgb shr 16) and 0xff, (value.rgb shr 8) and 0xff, value.rgb and 0xff)
        encoder.encodeSerializableValue(ColorSurrogate.serializer(), surrogate)
    }

    // Deserializes the surrogate back into the original class
    override fun deserialize(decoder: Decoder): Color {
        val surrogate = decoder.decodeSerializableValue(ColorSurrogate.serializer())
        return Color((surrogate.r shl 16) or (surrogate.g shl 8) or surrogate.b)
    }
}

// Binds the ColorSerializer serializer to the original class
@Serializable(with = ColorSerializer::class)
class Color(val rgb: Int)
//sampleEnd

fun main() {
    val green = Color(0x00ff00)
    println(Json.encodeToString(green))
    // {"r":0,"g":255,"b":0}
}
```
{kotlin-runnable="true"}

<!--- > You can get the full code [here](../../guide/example/example-serializer-4.kt). -->

<!---
```text
{"r":0,"g":255,"b":0}
```
-->

<!--- TEST -->

## Create a custom composite serializer

Unlike custom primitive serializers, which handle a single value like a string or integer, composite serializers
can represent complex data structures, such as classes with multiple properties.

To create a custom composite serializer:

1. Define the serialization schema by overriding the `descriptor` property. Use the [`buildClassSerialDescriptor()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.descriptors/build-class-serial-descriptor.html) function to define the class’s schema.
The descriptor outlines the structure of the serialized data, specifying its fields and their order.
2. Specify the elements of the class in the `descriptor` by calling the [`element()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.descriptors/element.html) function for each property.
The order of the elements is important and must match the order in which they are serialized and deserialized.

    > The term "element" refers to different parts of the serialized data depending on its [`SerialKind`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.descriptors/-serial-kind/).
    > For class descriptors, elements represent properties, while for enum descriptors, elements represent constants such as RED or GREEN in an enum class.
    >
    {type="note"}

3. Implement the `serialize()` function using the [`encodeStructure()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/encode-structure.html) DSL.
Inside the block, call the appropriate [`CompositeEncoder`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-composite-encoder/) functions like [`encodeIntElement()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-composite-encoder/encode-int-element.html) for each field, following the order defined in the descriptor.
4. Implement the `deserialize()` function using the [`decodeStructure()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/decode-structure.html) DSL.
Within this block, use the `CompositeDecoder` to decode each property by calling functions like [`decodeIntElement()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-composite-decoder/decode-int-element.html).
You can use [`decodeElementIndex()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-composite-decoder/decode-element-index.html) to identify which element to decode. The decoding order may vary depending on the format, such as JSON.
5. Bind the custom serializer to the class by specifying it with the `@Serializable(with = YourSerializer::class)` annotation.
This ensures that the class uses the custom composite serializer during serialization and deserialization.

    > If you only need to transform or restructure data without manually handling each element,
    > consider using [delegated serialization](#delegate-serialization-to-another-serializer) or [surrogate serialization](#serialize-with-a-surrogate-class) for a simpler approach.
    >
    {type="tip"}

Let's look at an example of how to serialize a class with multiple properties:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.*

//sampleStart
// Creates a custom serializer for the Color class with multiple properties
object ColorAsObjectSerializer : KSerializer<Color> {
    // Defines the schema for the Color class
    // specifying the properties with the buildClassSerialDescriptor() function
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("Color") {
            // Specifies each property with its type and name with the element() function
            element<Int>("r")
            element<Int>("g")
            element<Int>("b")
        }

    // Serializes the Color object into a structured format
    override fun serialize(encoder: Encoder, value: Color) =
        encoder.encodeStructure(descriptor) {
            // Encodes the red, green, and blue values in the specified order
            encodeIntElement(descriptor, 0, (value.rgb shr 16) and 0xff)
            encodeIntElement(descriptor, 1, (value.rgb shr 8) and 0xff)
            encodeIntElement(descriptor, 2, value.rgb and 0xff)
        }

    // Deserializes the data back into a Color object
    override fun deserialize(decoder: Decoder): Color =
        decoder.decodeStructure(descriptor) {
            // Temporary variables to hold the decoded values
            var r = -1
            var g = -1
            var b = -1
            // Loops to decode each property by its index
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> r = decodeIntElement(descriptor, 0)
                    1 -> g = decodeIntElement(descriptor, 1)
                    2 -> b = decodeIntElement(descriptor, 2)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
            // Ensures the values are valid and returns a new Color object
            require(r in 0..255 && g in 0..255 && b in 0..255)
            Color((r shl 16) or (g shl 8) or b)
        }
}

// Binds the custom serializer to the Color class
@Serializable(with = ColorAsObjectSerializer::class)
data class Color(val rgb: Int)
//sampleEnd

fun main() {
    val color = Color(0x00ff00)
    val string = Json.encodeToString(color)
    println(string)
    // {"r":0,"g":255,"b":0}
    require(Json.decodeFromString<Color>(string) == color)
}
```
{kotlin-runnable="true"}

<!--- > You can get the full code [here](../../guide/example/example-serializer-5.kt). -->

<!---
```text
{"r":0,"g":255,"b":0}
```
-->

<!--- TEST -->

### Optimize deserialization with sequential decoding (experimental)

For formats that store complex data in sequential order, like JSON, you can create a more optimized deserialization process by calling the [`decodeSequentially()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-composite-decoder/decode-sequentially.html) function.
This function checks whether the data is stored sequentially, allowing you to skip the more complex logic of decoding individual elements out of order.

If `decodeSequentially()` returns true, you can directly decode the elements in order, eliminating the need for `decodeElementIndex()` in a loop to determine the order of elements.
If it returns `false`, you revert to the standard deserialization method, which handles unordered formats:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.*


object ColorAsObjectSerializer : KSerializer<Color> {

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("Color") {
            element<Int>("r")
            element<Int>("g")
            element<Int>("b")
        }

    override fun serialize(encoder: Encoder, value: Color) =
        encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, (value.rgb shr 16) and 0xff)
            encodeIntElement(descriptor, 1, (value.rgb shr 8) and 0xff)
            encodeIntElement(descriptor, 2, value.rgb and 0xff)
        }

//sampleStart
    override fun deserialize(decoder: Decoder): Color =
        decoder.decodeStructure(descriptor) {
            var r = -1
            var g = -1
            var b = -1
            // Decodes values directly in order if the format stores data sequentially
            if (decodeSequentially()) {
                r = decodeIntElement(descriptor, 0)           
                g = decodeIntElement(descriptor, 1)  
                b = decodeIntElement(descriptor, 2)
            } else while (true) {
                // Decodes elements using a loop to handle formats where data may be unordered
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> r = decodeIntElement(descriptor, 0)
                    1 -> g = decodeIntElement(descriptor, 1)
                    2 -> b = decodeIntElement(descriptor, 2)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
            require(r in 0..255 && g in 0..255 && b in 0..255)
            Color((r shl 16) or (g shl 8) or b)
        }
}
//sampleEnd

@Serializable(with = ColorAsObjectSerializer::class)
data class Color(val rgb: Int)

fun main() {
    val color = Color(0x00ff00)
    val string = Json.encodeToString(color)
    println(string)
    // {"r":0,"g":255,"b":0}
    require(Json.decodeFromString<Color>(string) == color)
}
```
{kotlin-runnable="true"}

<!--- > You can get the full code [here](../../guide/example/example-serializer-13.kt). -->

<!--- TEST
{"r":0,"g":255,"b":0}
-->

## Create a custom serializer for generic types

When creating a custom serializer for a generic class, you must provide the appropriate `KSerializer` for each generic parameter.
This ensures that the correct serialization strategy is applied to each type used within the generic class.

To define a custom serializer for a generic class:

1. Mark the class with `@Serializable(with = YourSerializer::class)`. 
2. Create a custom serializer as a `class`, which accepts one or more `KSerializer` instances for its generic parameters. 
3. Delegate the serialization logic to the provided `KSerializer` for each type parameter during serialization and deserialization.

Let's look at an example using a generic `Box<T>` class:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.*

//sampleStart
// Marks the Box<T> class with @Serializable and specifies a custom serializer
@Serializable(with = BoxSerializer::class)
data class Box<T>(val contents: T)

// Creates a custom serializer as a class for Box<T>
class BoxSerializer<T>(private val dataSerializer: KSerializer<T>) : KSerializer<Box<T>> {
    // Uses the descriptor from the provided KSerializer to define the structure of Box<T>
    override val descriptor: SerialDescriptor = dataSerializer.descriptor
    // Delegates serialization and deserialization
    override fun serialize(encoder: Encoder, value: Box<T>) = dataSerializer.serialize(encoder, value.contents)
    override fun deserialize(decoder: Decoder) = Box(dataSerializer.deserialize(decoder))
}

@Serializable
data class Project(val name: String)

fun main() {
    val box = Box(Project("kotlinx.serialization"))
    val string = Json.encodeToString(box)
    println(string)
    // {"name":"kotlinx.serialization"}
    println(Json.decodeFromString<Box<Project>>(string))
    // Box(contents=Project(name=kotlinx.serialization))
}
//sampleEnd
```
{kotlin-runnable="true"}

<!--- > You can get the full code [here](../../guide/example/example-serializer-19.kt). -->

<!---
```text
{"name":"kotlinx.serialization"}
Box(contents=Project(name=kotlinx.serialization))
```
-->

<!--- TEST -->

## Implement contextual serialization

_Contextual serialization_ allows you to adjust the serialization strategy for specific types at runtime, based on the context in which they are used.
Unlike static serialization,
which is fully defined at compile-time, contextual serialization lets you modify how objects are serialized deep within an object tree.
For example, you could serialize `java.util.Date` in JSON format either as an ISO 8601 `String` or as a `Long`, depending on the protocol version being used with contextual serialization.
This approach is supported by the built-in [`ContextualSerializer`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-contextual-serializer/) class.

To implement contextual serialization:

1. Define a custom `KSerializer` for the type you want to serialize dynamically. This controls how the type is serialized and deserialized at runtime.
2. Mark the property in your serializable class with the [`@Contextual`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-contextual/) annotation .

    > The [`@Contextual`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-contextual/) annotation is a convenient shortcut for using the [`ContextualSerializer`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-contextual-serializer/) class, which enables contextual serialization.
    > It is equivalent to using @Serializable(with = ContextualSerializer::class).
    > Alternatively, you can also apply the [`@UseContextualSerialization`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-use-contextual-serialization/) annotation at the file level to apply contextual serialization across multiple properties in the same file, similar to [how @UseSerializers works]((third-party-classes.md#specify-serializers-for-a-file)).
    >
    {type="note"}

3. Create a [`SerializersModule`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.modules/-serializers-module/) using the [`SerializersModule {}`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.modules/-serializers-module.html) builder function.
Use the [`contextual()`]((https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.modules/contextual.html)) function to apply the custom serializer for contextual serialization.

    > Without the `SerializersModule`, a `SerializationException` is thrown during the serialization or deserialization of contextually annotated types.
    >
    {type="note"}

4. Create a `Json` instance and pass the `SerializersModule` to the [`serializersModule`]((https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/serializers-module.html)) property. This ensures that the correct contextual serializer is used for serialization.

    > For more information about configuring a Json instance, see the [JSON configuration](serialization-json-configuration.md) section.
    >
    {type="tip"}

Let's look at an example:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.*
import java.util.Date
import java.text.SimpleDateFormat

//sampleStart
// Defines a custom serializer for Date
object DateAsLongSerializer : KSerializer<Date> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: Date) = encoder.encodeLong(value.time)
    override fun deserialize(decoder: Decoder): Date = Date(decoder.decodeLong())
}

@Serializable
class ProgrammingLanguage(
    val name: String,
    // Applies @Contextual to dynamically serialize the Date property
    @Contextual
    val stableReleaseDate: Date
)

// Defines the SerializersModule and registers DateAsLongSerializer using the contextual() function
private val module = SerializersModule { 
    contextual(DateAsLongSerializer)
}

// Creates an instance of Json with the custom SerializersModule
val format = Json { serializersModule = module }

fun main() {
    val data = ProgrammingLanguage("Kotlin", SimpleDateFormat("yyyy-MM-ddX").parse("2016-02-15+00"))
    println(format.encodeToString(data))
    // {"name":"Kotlin","stableReleaseDate":1455494400000}
}
//sampleEnd
```
{kotlin-runnable="true"}

<!--- > You can get the full code [here](../../guide/example/example-serializer-21.kt). -->

<!---
```text
{"name":"Kotlin","stableReleaseDate":1455494400000}
```
-->

<!--- TEST -->

### Serialize generic classes contextually

To serialize generic classes contextually, you can register a function in the `SerializersModule`
that dynamically provides the appropriate serializer based on the generic type used at runtime.

You cannot use a single serializer instance for a generic class because [different type arguments require different serializers](third-party-classes.md#specify-a-custom-serializer-for-generic-types).
For example, the following approach would only work for `Box<Int>`, but not for other types like `Box<String>`:

```kotlin
val incorrectModule = SerializersModule {
    // This only works for Box<Int>, but not for Box<String> or other types
    contextual(BoxSerializer(Int.serializer()))
}
```

Instead, you can use a function that provides the correct serializer for any type argument.
The following example demonstrates how to register a function that dynamically supplies the appropriate serializer for generic types like `Box<T>`:

```kotlin
val correctModule = SerializersModule {
    // args[0] dynamically provides the appropriate serializer for the type argument
    // For example, Int.serializer() or String.serializer()
    contextual(Box::class) { args -> BoxSerializer(args[0]) }
}
```

<!--- CLEAR -->

> If you have multiple `SerializersModule` instances, such as those handling both generic and non-generic classes,
> you can combine them using the `plus` operator.
> For more details on merging modules, see the [Merging library serializers modules](serialization-polymorphism.md#merge-multiple-serializermodule-instances) section.
> 
{type="tip"}

## What's next

* The [Json transformations](json.md#json-transformations) section of the [Json](json.md) chapter provides examples
  of serializers that utilize JSON-specific features.

* A format implementation can have a format-specific representation for a type as explained
  in the [Format-specific types](formats.md#format-specific-types) section of
  the [Alternative and custom formats (experimental)](formats.md) chapter.