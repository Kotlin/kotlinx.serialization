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
This allows you to customize how your data is represented, such as converting an RGB integer value into a hexadecimal string.

Custom serializers implement the [`KSerializer`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-k-serializer/) interface and
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

Like with [delegated serialization]((#delegate-serialization-to-another-serializer)), surrogate classes also rely on reusing existing serialization logic.
However, instead of transforming or restructuring individual fields within the same class,
you create a new surrogate class to represent the serialized form.

The surrogate class can be [`private`](visibility-modifiers.md#class-members), and you use the `init` block to enforce constraints on the serialized data.
After defining the surrogate class, you can retrieve the plugin-generated serializer for it by calling its `serializer()` function.
The function reuses the automatically generated [`SerialDescriptor`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.descriptors/-serial-descriptor/) of
the surrogate class to describe the structure of the serialized data while maintaining compatibility with the original class.

To serialize with the surrogate class, delegate the serialization process to its serializer by using the `encodeSerializableValue()` and `decodeSerializableValue()` functions:

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

## Create a custom serializer for a generic type

Let us take a look at the following example of the generic `Box<T>` class.
It is marked with `@Serializable(with = BoxSerializer::class)` as we plan to have a custom serialization
strategy for it.

```kotlin       
@Serializable(with = BoxSerializer::class)
data class Box<T>(val contents: T) 
```

An implementation of [KSerializer] for a regular type is written as an `object`, as we saw in this chapter's
examples for the `Color` type. A generic class serializer is instantiated with serializers
for its generic parameters. We saw this in the [Plugin-generated generic serializer](#plugin-generated-generic-serializer) section.
A custom serializer for a generic class must be a `class` with a constructor that accepts as many [KSerializer]
parameters as the type has generic parameters. Let us write a `Box<T>` serializer that erases itself during
serialization, delegating everything to the underlying serializer of its `data` property.

```kotlin
class BoxSerializer<T>(private val dataSerializer: KSerializer<T>) : KSerializer<Box<T>> {
    override val descriptor: SerialDescriptor = dataSerializer.descriptor
    override fun serialize(encoder: Encoder, value: Box<T>) = dataSerializer.serialize(encoder, value.contents)
    override fun deserialize(decoder: Decoder) = Box(dataSerializer.deserialize(decoder))
}
```

Now we can serialize and deserialize `Box<Project>`.

```kotlin
@Serializable
data class Project(val name: String)

fun main() {
    val box = Box(Project("kotlinx.serialization"))
    val string = Json.encodeToString(box)
    println(string)
    println(Json.decodeFromString<Box<Project>>(string))
}
```

> You can get the full code [here](../../guide/example/example-serializer-19.kt).

The resulting JSON looks like the `Project` class was serialized directly.

```text
{"name":"kotlinx.serialization"}
Box(contents=Project(name=kotlinx.serialization))
```

<!--- TEST -->

## Format-specific serializers

The above custom serializers worked in the same way for every format. However, there might be format-specific
features that a serializer implementation would like to take advantage of.

* The [Json transformations](json.md#json-transformations) section of the [Json](json.md) chapter provides examples
  of serializers that utilize JSON-specific features.

* A format implementation can have a format-specific representation for a type as explained
  in the [Format-specific types](formats.md#format-specific-types) section of
  the [Alternative and custom formats (experimental)](formats.md) chapter.

This chapter proceeds with a generic approach to tweaking the serialization strategy based on the context.

## Contextual serialization

All the previous approaches to specifying custom serialization strategies were _static_, that is
fully defined at compile-time. The exception was the [Passing a serializer manually](#passing-a-serializer-manually)
approach, but it worked only on a top-level object. You might need to change the serialization
strategy for objects deep in the serialized object tree at run-time, with the strategy being selected in a context-dependent way.
For example, you might want to represent `java.util.Date` in JSON format as an ISO 8601 string or as a long integer
depending on a version of a protocol you are serializing data for. This is called _contextual_ serialization, and it
is supported by a built-in [ContextualSerializer] class. Usually we don't have to use this serializer class explicitly&mdash;there
is the [Contextual] annotation providing a shortcut to
the `@Serializable(with = ContextualSerializer::class)` annotation,
or the [UseContextualSerialization] annotation can be used at the file-level just like
the [UseSerializers] annotation. Let's see an example utilizing the former.

<!--- INCLUDE
import java.util.Date
import java.text.SimpleDateFormat
-->

```kotlin
@Serializable          
class ProgrammingLanguage(
    val name: String,
    @Contextual 
    val stableReleaseDate: Date
)
```

<!--- INCLUDE

fun main() {
    val data = ProgrammingLanguage("Kotlin", SimpleDateFormat("yyyy-MM-ddX").parse("2016-02-15+00"))
    println(Json.encodeToString(data))
}
-->    

To actually serialize this class we must provide the corresponding context when calling the `encodeToXxx`/`decodeFromXxx`
functions. Without it we'll get a "Serializer for class 'Date' is not found" exception.

> See [here](../../guide/example/example-serializer-20.kt) for an example that produces that exception.

<!--- TEST LINES_START 
Exception in thread "main" kotlinx.serialization.SerializationException: Serializer for class 'Date' is not found.
Please ensure that class is marked as '@Serializable' and that the serialization compiler plugin is applied.
-->

<!--- INCLUDE
import kotlinx.serialization.modules.*
import java.util.Date
import java.text.SimpleDateFormat
  
object DateAsLongSerializer : KSerializer<Date> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: Date) = encoder.encodeLong(value.time)
    override fun deserialize(decoder: Decoder): Date = Date(decoder.decodeLong())
}

@Serializable          
class ProgrammingLanguage(
    val name: String,
    @Contextual 
    val stableReleaseDate: Date
)
-->

### Serializers module

To provide a context, we define a [SerializersModule] instance that describes which serializers shall be used
at run-time to serialize which contextually-serializable classes. This is done using the
[SerializersModule {}][SerializersModule()] builder function, which provides the [SerializersModuleBuilder] DSL to
register serializers. In the below example we use the [contextual][_contextual] function with the serializer. The corresponding
class this serializer is defined for is fetched automatically via the `reified` type parameter.

```kotlin
private val module = SerializersModule { 
    contextual(DateAsLongSerializer)
}
```

Next we create an instance of the [Json] format with this module using the
[Json {}][Json()] builder function and the [serializersModule][JsonBuilder.serializersModule] property.

> Details on custom JSON configurations can be found in
> the [JSON configuration](json.md#json-configuration) section.

```kotlin           
val format = Json { serializersModule = module }
```

Now we can serialize our data with this `format`.

```kotlin
fun main() {
    val data = ProgrammingLanguage("Kotlin", SimpleDateFormat("yyyy-MM-ddX").parse("2016-02-15+00"))
    println(format.encodeToString(data))
}
```

> You can get the full code [here](../../guide/example/example-serializer-21.kt).
```text
{"name":"Kotlin","stableReleaseDate":1455494400000}
```

<!--- TEST -->

### Contextual serialization and generic classes

In the previous section we saw that we can register serializer instance in the module for a class we want to serialize contextually.
We also know that [serializers for generic classes have constructor parameters](#custom-serializers-for-a-generic-type) — type arguments serializers.
It means that we can't use one serializer instance for a class if this class is generic:

```kotlin
val incorrectModule = SerializersModule {
    // Can serialize only Box<Int>, but not Box<String> or others
    contextual(BoxSerializer(Int.serializer()))
}
```

For cases when one want to serialize contextually a generic class, it is possible to register provider in the module:

```kotlin
val correctModule = SerializersModule {
    // args[0] contains Int.serializer() or String.serializer(), depending on the usage
    contextual(Box::class) { args -> BoxSerializer(args[0]) } 
}
```

<!--- CLEAR -->

> Additional details on serialization modules are given in
> the [Merging library serializers modules](polymorphism.md#merging-library-serializers-modules) section of
> the [Polymorphism](polymorphism.md) chapter.

## Deriving external serializer for another Kotlin class (experimental)

If a 3rd-party class to be serialized is a Kotlin class with a properties-only primary constructor, a kind of
class which could have been made `@Serializable`, then you can generate an _external_ serializer for it
using the [Serializer] annotation on an object with the [`forClass`][Serializer.forClass] property.

```kotlin         
// NOT @Serializable
class Project(val name: String, val language: String)
                           
@Serializer(forClass = Project::class)
object ProjectSerializer
```

You must bind this serializer to a class using one of the approaches explained in this chapter. We'll
follow the [Passing a serializer manually](#passing-a-serializer-manually) approach for this example.

```kotlin 
fun main() {
    val data = Project("kotlinx.serialization", "Kotlin")
    println(Json.encodeToString(ProjectSerializer, data))    
}
```          

> You can get the full code [here](../../guide/example/example-serializer-22.kt).

This gets all the `Project` properties serialized:

```text
{"name":"kotlinx.serialization","language":"Kotlin"}
```     

<!--- TEST -->

### External serialization uses properties

As we saw earlier, the regular `@Serializable` annotation creates a serializer so that
[Backing fields are serialized](basic-serialization.md#backing-fields-are-serialized). _External_ serialization using
`Serializer(forClass = ...)` has no access to backing fields and works differently.
It serializes only _accessible_ properties that have setters or are part of the primary constructor.
The following example shows this.

```kotlin        
// NOT @Serializable, will use external serializer
class Project(
    // val in a primary constructor -- serialized
    val name: String
) {
    var stars: Int = 0 // property with getter & setter -- serialized
 
    val path: String // getter only -- not serialized
        get() = "kotlin/$name"                                         

    private var locked: Boolean = false // private, not accessible -- not serialized 
}              

@Serializer(forClass = Project::class)
object ProjectSerializer

fun main() {
    val data = Project("kotlinx.serialization").apply { stars = 9000 }
    println(Json.encodeToString(ProjectSerializer, data))
}
```             

> You can get the full code [here](../../guide/example/example-serializer-23.kt).

The output is shown below.

```text
{"name":"kotlinx.serialization","stars":9000}
```     

<!--- TEST -->


### Base64

To encode and decode Base64 formats, we will need to manually write a serializer. Here, we will use a default
implementation of Kotlin's Base64 encoder. Note that some serializers use different RFCs for Base64 encoding by default.
For example, Jackson uses a variant of [Base64 Mime](https://datatracker.ietf.org/doc/html/rfc2045). The same result in
kotlinx.serialization can be achieved with Base64.Mime encoder.
[Kotlin's documentation for Base64](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.io.encoding/-base64/) lists
other available encoders.

```kotlin
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.descriptors.*
import kotlin.io.encoding.*

@OptIn(ExperimentalEncodingApi::class)
object ByteArrayAsBase64Serializer : KSerializer<ByteArray> {
    private val base64 = Base64.Default

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor(
            "ByteArrayAsBase64Serializer",
            PrimitiveKind.STRING
        )

    override fun serialize(encoder: Encoder, value: ByteArray) {
        val base64Encoded = base64.encode(value)
        encoder.encodeString(base64Encoded)
    }

    override fun deserialize(decoder: Decoder): ByteArray {
        val base64Decoded = decoder.decodeString()
        return base64.decode(base64Decoded)
    }
}
```

For more details on how to create your own custom serializer, you can
see [custom serializers](serializers.md#custom-serializers).

Then we can use it like this:

```kotlin
@Serializable
data class Value(
    @Serializable(with = ByteArrayAsBase64Serializer::class)
    val base64Input: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Value
        return base64Input.contentEquals(other.base64Input)
    }

    override fun hashCode(): Int {
        return base64Input.contentHashCode()
    }
}

fun main() {
    val string = "foo string"
    val value = Value(string.toByteArray())
    val encoded = Json.encodeToString(value)
    println(encoded)
    val decoded = Json.decodeFromString<Value>(encoded)
    println(decoded.base64Input.decodeToString())
}
```

> You can get the full code [here](../../guide/example/example-json-16.kt)

```text
{"base64Input":"Zm9vIHN0cmluZw=="}
foo string
```

Notice the serializer we wrote is not dependent on `Json` format, therefore, it can be used in any format.

For projects that use this serializer in many places, to avoid specifying the serializer every time, it is possible
to [specify a serializer globally using typealias](serializers.md#specifying-serializer-globally-using-typealias).
For example:
````kotlin
typealias Base64ByteArray = @Serializable(ByteArrayAsBase64Serializer::class) ByteArray
````

<!--- TEST -->