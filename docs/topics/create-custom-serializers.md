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

However, if you need to customize how your data is encoded and decoded, such as converting an RGB value into a hexadecimal string and back,
you can create a custom serializer.
Custom serializers implement the [`KSerializer`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-k-serializer/) interface and
allowing you to control how your data is transformed between its Kotlin object form and its serialized form.

To create a custom serializer:

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

### Composite serializer via surrogate

Now our challenge is to get `Color` serialized so that it is represented in JSON as if it is a class
with three properties&mdash;`r`, `g`, and `b`&mdash;so that JSON encodes it as an object.
The easiest way to achieve this is to define a _surrogate_ class mimicking the serialized form of `Color` that
we are going to use for its serialization. We also set the [SerialName] of this surrogate class to `Color`. Then if
any format uses this name the surrogate looks like it is a `Color` class.
The surrogate class can be `private`, and can enforce all the constraints on the serial representation
of the class in its `init` block.

```kotlin
@Serializable
@SerialName("Color")
private class ColorSurrogate(val r: Int, val g: Int, val b: Int) {
    init {     
        require(r in 0..255 && g in 0..255 && b in 0..255)
    }
}
```

> An example of where the class name is used is shown in
> the [Custom subclass serial name](polymorphism.md#custom-subclass-serial-name) section in the chapter on polymorphism.

Now we can use the `ColorSurrogate.serializer()` function to retrieve a plugin-generated serializer for the
surrogate class.

We can use the same approach as in [delegating serializer](#delegating-serializers), but this time,
we are fully reusing an automatically
generated [SerialDescriptor] for the surrogate because it should be indistinguishable from the original.

```kotlin
object ColorSerializer : KSerializer<Color> {
    override val descriptor: SerialDescriptor = ColorSurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Color) {
        val surrogate = ColorSurrogate((value.rgb shr 16) and 0xff, (value.rgb shr 8) and 0xff, value.rgb and 0xff)
        encoder.encodeSerializableValue(ColorSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): Color {
        val surrogate = decoder.decodeSerializableValue(ColorSurrogate.serializer())
        return Color((surrogate.r shl 16) or (surrogate.g shl 8) or surrogate.b)
    }
}
```

We bind the `ColorSerializer` serializer to the `Color` class.

```kotlin
@Serializable(with = ColorSerializer::class)
class Color(val rgb: Int)
```  

Now we can enjoy the result of serialization for the `Color` class.

<!--- INCLUDE
fun main() {
    val green = Color(0x00ff00)
    println(Json.encodeToString(green))
}
-->

<!--- > You can get the full code [here](../../guide/example/example-serializer-4.kt). -->

<!---
```text
{"r":0,"g":255,"b":0}
```
-->

<!--- TEST -->

### Hand-written composite serializer

There are some cases where a surrogate solution does not fit. Perhaps we want to avoid the performance
implications of additional allocation, or we want a configurable/dynamic set of properties for the
resulting serial representation. In these cases we need to manually write a class
serializer which mimics the behaviour of a generated serializer.

```kotlin 
object ColorAsObjectSerializer : KSerializer<Color> {
```

Let's introduce it piece by piece. First, a descriptor is defined using the [buildClassSerialDescriptor] builder.
The [element][ClassSerialDescriptorBuilder.element] function in the builder DSL automatically fetches serializers
for the corresponding fields by their type. The order of elements is important. They are indexed starting from zero.

```kotlin
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("Color") {
            element<Int>("r")
            element<Int>("g")
            element<Int>("b")
        }
```                                                                        

> The "element" is a generic term here. What is an element of a descriptor depends on its [SerialKind].
> Elements of a class descriptor are its properties, elements of a enum descriptor are its cases, etc.

Then we write the `serialize` function using the [encodeStructure] DSL that provides access to
the [CompositeEncoder] in its block. The difference between [Encoder] and [CompositeEncoder] is the latter
has `encodeXxxElement` functions that correspond to the `encodeXxx` functions of the former. They must be called
in the same order as in the descriptor.

```kotlin
    override fun serialize(encoder: Encoder, value: Color) =
        encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, (value.rgb shr 16) and 0xff)
            encodeIntElement(descriptor, 1, (value.rgb shr 8) and 0xff)
            encodeIntElement(descriptor, 2, value.rgb and 0xff)
        }
```                                     

The most complex piece of code is the `deserialize` function. It must support formats, like JSON, that
can decode properties in an arbitrary order. It starts with the call to [decodeStructure] to
get access to a [CompositeDecoder]. Inside it we write a loop that repeatedly calls
[decodeElementIndex][CompositeDecoder.decodeElementIndex] to decode the index of the next element, then we decode the corresponding
element using [decodeIntElement][CompositeDecoder.decodeIntElement] in our example, and finally we terminate the loop when
`CompositeDecoder.DECODE_DONE` is encountered.

```kotlin
    override fun deserialize(decoder: Decoder): Color =
        decoder.decodeStructure(descriptor) {
            var r = -1
            var g = -1
            var b = -1
            while (true) {
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
```

<!--- INCLUDE
}
-->

Now we bind the resulting serializer to the `Color` class and test its serialization/deserialization.

```kotlin   
@Serializable(with = ColorAsObjectSerializer::class)
data class Color(val rgb: Int)

fun main() {
    val color = Color(0x00ff00)
    val string = Json.encodeToString(color) 
    println(string)
    require(Json.decodeFromString<Color>(string) == color)
}  
```              

> You can get the full code [here](../../guide/example/example-serializer-12.kt).

As before, we got the `Color` class represented as a JSON object with three keys:

```text
{"r":0,"g":255,"b":0}
```                        

<!--- TEST -->    

### Sequential decoding protocol (experimental)

The implementation of the `deserialize` function from the previous section works with any format. However,
some formats either always store all the complex data in order, or only do so sometimes (JSON always stores
collections in order). With these formats the complex protocol of calling `decodeElementIndex` in the loop is
not needed, and a faster implementation can be used if the [CompositeDecoder.decodeSequentially] function returns `true`.
The plugin-generated serializers are actually conceptually similar to the below code.

<!--- INCLUDE
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
-->

```kotlin
    override fun deserialize(decoder: Decoder): Color =
        decoder.decodeStructure(descriptor) {
            var r = -1
            var g = -1
            var b = -1     
            if (decodeSequentially()) { // sequential decoding protocol
                r = decodeIntElement(descriptor, 0)           
                g = decodeIntElement(descriptor, 1)  
                b = decodeIntElement(descriptor, 2)
            } else while (true) {
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
```

<!--- INCLUDE
}        

@Serializable(with = ColorAsObjectSerializer::class)
data class Color(val rgb: Int)

fun main() {
    val color = Color(0x00ff00)
    val string = Json.encodeToString(color) 
    println(string)
    require(Json.decodeFromString<Color>(string) == color)
}  
-->

> You can get the full code [here](../../guide/example/example-serializer-13.kt).

<!--- TEST
{"r":0,"g":255,"b":0}
--> 

### Serializing 3rd party classes

Sometimes an application has to work with an external type that is not serializable.
Let us use [java.util.Date] as an example. As before, we start by writing an implementation of [KSerializer]
for the class. Our goal is to get a `Date` serialized as a long number of milliseconds following the
approach from the [Primitive serializer](#primitive-serializer) section.

> In the following sections any kind of `Date` serializer would work. For example, if we want `Date` to be serialized
> as an object, we would use an approach from
> the [Composite serializer via surrogate](#composite-serializer-via-surrogate) section.     
> See also [Deriving external serializer for another Kotlin class (experimental)](#deriving-external-serializer-for-another-kotlin-class-experimental)
> when you need to serialize a 3rd-party Kotlin class that could have been serializable, but is not.

<!--- INCLUDE 
import java.util.Date
import java.text.SimpleDateFormat
-->

```kotlin
object DateAsLongSerializer : KSerializer<Date> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: Date) = encoder.encodeLong(value.time)
    override fun deserialize(decoder: Decoder): Date = Date(decoder.decodeLong())
}
```

We cannot bind the `DateAsLongSerializer` serializer to the `Date` class with the [`@Serializable`][Serializable] annotation
because we don't control the `Date` source code. There are several ways to work around that.

### Passing a serializer manually

All `encodeToXxx` and `decodeFromXxx` functions have an overload with the first serializer parameter.
When a non-serializable class, like `Date`, is the top-level class being serialized, we can use those.

```kotlin
fun main() {                                              
    val kotlin10ReleaseDate = SimpleDateFormat("yyyy-MM-ddX").parse("2016-02-15+00") 
    println(Json.encodeToString(DateAsLongSerializer, kotlin10ReleaseDate))    
}
``` 

> You can get the full code [here](../../guide/example/example-serializer-14.kt).

```text
1455494400000
```     

<!--- TEST -->

### Specifying serializer on a property

When a property of a non-serializable class, like `Date`, is serialized as part of a serializable class we must supply
its serializer or the code will not compile. This is accomplished using the [`@Serializable`][Serializable] annotation on the property.

<!--- INCLUDE 
import java.util.Date
import java.text.SimpleDateFormat
  
object DateAsLongSerializer : KSerializer<Date> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: Date) = encoder.encodeLong(value.time)
    override fun deserialize(decoder: Decoder): Date = Date(decoder.decodeLong())
}
-->

```kotlin
@Serializable          
class ProgrammingLanguage(
    val name: String,
    @Serializable(with = DateAsLongSerializer::class)
    val stableReleaseDate: Date
)

fun main() {
    val data = ProgrammingLanguage("Kotlin", SimpleDateFormat("yyyy-MM-ddX").parse("2016-02-15+00"))
    println(Json.encodeToString(data))
}
``` 

> You can get the full code [here](../../guide/example/example-serializer-15.kt).

The `stableReleaseDate` property is serialized with the serialization strategy that we specified for it:

```text
{"name":"Kotlin","stableReleaseDate":1455494400000}
```    

<!--- TEST -->

### Specifying serializer for a particular type

[`@Serializable`][Serializable] annotation can also be applied directly to the types.
This is handy when a class that requires a custom serializer, such as `Date`, happens to be a generic type argument.
The most common use case for that is when you have a list of dates:

<!--- INCLUDE 
import java.util.Date
import java.text.SimpleDateFormat
  
object DateAsLongSerializer : KSerializer<Date> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: Date) = encoder.encodeLong(value.time)
    override fun deserialize(decoder: Decoder): Date = Date(decoder.decodeLong())
}
-->

```kotlin
@Serializable          
class ProgrammingLanguage(
    val name: String,
    val releaseDates: List<@Serializable(DateAsLongSerializer::class) Date>
)

fun main() {
    val df = SimpleDateFormat("yyyy-MM-ddX")
    val data = ProgrammingLanguage("Kotlin", listOf(df.parse("2023-07-06+00"), df.parse("2023-04-25+00"), df.parse("2022-12-28+00")))
    println(Json.encodeToString(data))
}
```

> You can get the full code [here](../../guide/example/example-serializer-16.kt).

```text
{"name":"Kotlin","releaseDates":[1688601600000,1682380800000,1672185600000]}
```   

<!--- TEST -->

### Specifying serializers for a file

A serializer for a specific type, like `Date`, can be specified for a whole source code file with the file-level
[UseSerializers] annotation at the beginning of the file.

```kotlin
@file:UseSerializers(DateAsLongSerializer::class)
```      

<!--- PREFIX -->

<!--- INCLUDE
import java.util.Date
import java.text.SimpleDateFormat
  
object DateAsLongSerializer : KSerializer<Date> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: Date) = encoder.encodeLong(value.time)
    override fun deserialize(decoder: Decoder): Date = Date(decoder.decodeLong())
}
-->

Now a `Date` property can be used in a serializable class without additional annotations.

```kotlin
@Serializable          
class ProgrammingLanguage(val name: String, val stableReleaseDate: Date)

fun main() {
    val data = ProgrammingLanguage("Kotlin", SimpleDateFormat("yyyy-MM-ddX").parse("2016-02-15+00"))
    println(Json.encodeToString(data))
}
```   
> You can get the full code [here](../../guide/example/example-serializer-17.kt).

```text
{"name":"Kotlin","stableReleaseDate":1455494400000}
```

<!--- TEST --> 

### Specifying serializer globally using typealias

kotlinx.serialization tends to be the always-explicit framework when it comes to serialization strategies: normally,
they should be explicitly mentioned in `@Serializable` annotation. Therefore, we do not provide any kind of global serializer
configuration (except for [context serializer](#contextual-serialization) mentioned later).

However, in projects with a large number of files and classes, it may be too cumbersome to specify `@file:UseSerializers`
every time, especially for classes like `Date` or `Instant` that have a fixed strategy of serialization across the project.
For such cases, it is possible to specify serializers using `typealias`es, as they preserve annotations, including serialization-related ones:
<!--- INCLUDE
import java.util.Date
import java.util.TimeZone
import java.text.SimpleDateFormat
  
object DateAsLongSerializer : KSerializer<Date> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("DateAsLong", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: Date) = encoder.encodeLong(value.time)
    override fun deserialize(decoder: Decoder): Date = Date(decoder.decodeLong())
}

object DateAsSimpleTextSerializer: KSerializer<Date> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("DateAsSimpleText", PrimitiveKind.LONG)
    private val format = SimpleDateFormat("yyyy-MM-dd").apply {
        // Here we explicitly set time zone to UTC so output for this sample remains locale-independent.
        // Depending on your needs, you may have to adjust or remove this line.
        setTimeZone(TimeZone.getTimeZone("UTC"))
    }
    override fun serialize(encoder: Encoder, value: Date) = encoder.encodeString(format.format(value))
    override fun deserialize(decoder: Decoder): Date = format.parse(decoder.decodeString())
}
-->

```kotlin
typealias DateAsLong = @Serializable(DateAsLongSerializer::class) Date

typealias DateAsText = @Serializable(DateAsSimpleTextSerializer::class) Date
```

Using these new different types, it is possible to serialize a Date differently without additional annotations:

```kotlin
@Serializable          
class ProgrammingLanguage(val stableReleaseDate: DateAsText, val lastReleaseTimestamp: DateAsLong)

fun main() {
    val format = SimpleDateFormat("yyyy-MM-ddX")
    val data = ProgrammingLanguage(format.parse("2016-02-15+00"), format.parse("2022-07-07+00"))
    println(Json.encodeToString(data))
}
```

> You can get the full code [here](../../guide/example/example-serializer-18.kt).

```text
{"stableReleaseDate":"2016-02-15","lastReleaseTimestamp":1657152000000}
```

<!--- TEST -->

### Custom serializers for a generic type

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

### Format-specific serializers

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