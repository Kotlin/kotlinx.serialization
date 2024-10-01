[//]: # (title: Serialize third-party classes)

<!--- TEST_NAME SerializersThirdParty -->

Third-party types, such as [java.util.Date](https://docs.oracle.com/javase/8/docs/api/java/util/Date.html), cannot be directly annotated with [`@Serializable`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-serializable/) because you do not have control over their source code.
To serialize these non-serializable classes, you can implement a custom [`KSerializer`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-k-serializer/), as described in the [Create a custom primitive serializer](create-custom-serializers.md#create-a-custom-primitive-serializer) section.

In this section, you can explore several approaches to working around this limitation, using `java.util.Date` as an example.
The goal is to serialize a `Date` object as a long value representing the number of milliseconds since the Unix epoch.

## Pass serializers manually

To serialize a non-serializable class like `Date`, you can manually pass a custom serializer using the overloads of
`Encoder` and `Decoder` functions, such as [`encodeLong()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-encoder/encode-long.html) or [`decodeLong()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-decoder/decode-long.html): 

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.*
import java.util.Date
import java.text.SimpleDateFormat

//sampleStart
// Cannot use @Serializable on Date as without control over its source code
object DateAsLongSerializer : KSerializer<Date> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: Date) = encoder.encodeLong(value.time)
    override fun deserialize(decoder: Decoder): Date = Date(decoder.decodeLong())
}

fun main() {                                              
    val kotlin10ReleaseDate = SimpleDateFormat("yyyy-MM-ddX").parse("2016-02-15+00") 
    // Serializes Date as a Long in milliseconds
    println(Json.encodeToString(DateAsLongSerializer, kotlin10ReleaseDate))    
    // 1455494400000
}
//sampleEnd
```
{kotlin-runnable="true"}

<!--- > You can get the full code [here](../../guide/example/example-thirdparty-1.kt). -->

<!---
```text
1455494400000
```
-->

<!--- TEST -->

## Specify serializer on a property

When a non-serializable class, like `Date`, is used as a property in a serializable class, you must specify its serializer to ensure the code compiles.
To do so, use the `@Serializable` annotation on the property:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.*
import java.util.Date
import java.text.SimpleDateFormat

object DateAsLongSerializer : KSerializer<Date> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: Date) = encoder.encodeLong(value.time)
    override fun deserialize(decoder: Decoder): Date = Date(decoder.decodeLong())
}

//sampleStart
@Serializable          
class ProgrammingLanguage(
    val name: String,
    // Specifies the custom serializer for the Date property
    @Serializable(with = DateAsLongSerializer::class)
    val stableReleaseDate: Date
)

fun main() {
    val data = ProgrammingLanguage("Kotlin", SimpleDateFormat("yyyy-MM-ddX").parse("2016-02-15+00"))
    println(Json.encodeToString(data))
    // {"name":"Kotlin","stableReleaseDate":1455494400000}
}
//sampleEnd
```
{kotlin-runnable="true"}

<!--- > You can get the full code [here](../../guide/example/example-thirdparty-2.kt). -->

<!---
```text
{"name":"Kotlin","stableReleaseDate":1455494400000}
```
-->

<!--- TEST -->

## Specify a custom serializer for generic types

When a class, such as `Date` requires a custom serializer, you can apply the `@Serializable` annotation directly to specific types.
This approach is useful when that class is used as a generic type argument, such as in a list of dates:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.*
import java.util.Date
import java.text.SimpleDateFormat

object DateAsLongSerializer : KSerializer<Date> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: Date) = encoder.encodeLong(value.time)
    override fun deserialize(decoder: Decoder): Date = Date(decoder.decodeLong())
}

//sampleStart
@Serializable          
class ProgrammingLanguage(
    val name: String,
    // Applies the custom serializer to a List<Date> generic type
    val releaseDates: List<@Serializable(DateAsLongSerializer::class) Date>
)

fun main() {
    val df = SimpleDateFormat("yyyy-MM-ddX")
    val data = ProgrammingLanguage("Kotlin", listOf(df.parse("2023-07-06+00"), df.parse("2023-04-25+00"), df.parse("2022-12-28+00")))
    println(Json.encodeToString(data))
    // {"name":"Kotlin","releaseDates":[1688601600000,1682380800000,1672185600000]}
}
//sampleEnd
```
{kotlin-runnable="true"}

<!--- > You can get the full code [here](../../guide/example/example-thirdparty-3.kt). -->

<!---
```text
{"name":"Kotlin","releaseDates":[1688601600000,1682380800000,1672185600000]}
```
-->

<!--- TEST -->

## Specify serializers for a file

To specify a serializer for an entire source code file, use the [`@UseSerializers`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-use-serializers/) annotation at the beginning of the file:

```kotlin
@file:UseSerializers(DateAsLongSerializer::class)
```

This applies the custom serializer, such as `DateAsLongSerializer`, to all instances of that type within the file,
so you don't need to annotate each property separately:

<!--- PREFIX -->

```kotlin
// Specifies a serializer for the file
@file:UseSerializers(DateAsLongSerializer::class)
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.*
import java.util.Date
import java.text.SimpleDateFormat

object DateAsLongSerializer : KSerializer<Date> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: Date) = encoder.encodeLong(value.time)
    override fun deserialize(decoder: Decoder): Date = Date(decoder.decodeLong())
}

//sampleStart
// No need to specify the custom serializer on the property because it’s applied to the file
@Serializable
class ProgrammingLanguage(val name: String, val stableReleaseDate: Date)

fun main() {
    val data = ProgrammingLanguage("Kotlin", SimpleDateFormat("yyyy-MM-ddX").parse("2016-02-15+00"))
    println(Json.encodeToString(data))
    // {"name":"Kotlin","stableReleaseDate":1455494400000}
}
```
{kotlin-runnable="true"}

<!--- > You can get the full code [here](../../guide/example/example-thirdparty-4.kt). -->

<!---
```text
{"name":"Kotlin","stableReleaseDate":1455494400000}
```
-->

<!--- TEST -->

## Specify custom serializers globally using typealias

`kotlinx.serialization` requires explicit specification of serialization strategies, typically through the `@Serializable` annotation.
There is no global serializer configuration, except for [contextual serialization](create-custom-serializers.md#contextual-serialization).
However, in larger projects, repeatedly specifying the same serializers, like `Date` across many files can become tedious
when used across many files.

In such cases, you can use [`typealias`](type-alias.md) to apply custom serializers globally for specific types,
eliminating the need to annotate each occurrence:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.*
import java.util.Date
import java.text.SimpleDateFormat
import java.util.TimeZone

object DateAsLongSerializer : KSerializer<Date> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("DateAsLong", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: Date) = encoder.encodeLong(value.time)
    override fun deserialize(decoder: Decoder): Date = Date(decoder.decodeLong())
}
//sampleStart
// Defines a serializer that encodes Date as a formatted string (yyyy-MM-dd)
object DateAsSimpleTextSerializer: KSerializer<Date> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("DateAsSimpleText", PrimitiveKind.LONG)
    private val format = SimpleDateFormat("yyyy-MM-dd").apply {
        // Sets the time zone to UTC for consistent output
        setTimeZone(TimeZone.getTimeZone("UTC"))
    }
    override fun serialize(encoder: Encoder, value: Date) = encoder.encodeString(format.format(value))
    override fun deserialize(decoder: Decoder): Date = format.parse(decoder.decodeString())
}

// Applies global serializers using typealias to avoid annotating each occurrence
typealias DateAsLong = @Serializable(DateAsLongSerializer::class) Date

typealias DateAsText = @Serializable(DateAsSimpleTextSerializer::class) Date

// Uses typealiases to apply custom serializers for Date properties
@Serializable          
class ProgrammingLanguage(val stableReleaseDate: DateAsText, val lastReleaseTimestamp: DateAsLong)

fun main() {
    val format = SimpleDateFormat("yyyy-MM-ddX")
    val data = ProgrammingLanguage(format.parse("2016-02-15+00"), format.parse("2022-07-07+00"))
    println(Json.encodeToString(data))
    // {"stableReleaseDate":"2016-02-15","lastReleaseTimestamp":1657152000000}
}
//sampleEnd
```
{kotlin-runnable="true"}

<!--- > You can get the full code [here](../../guide/example/example-thirdparty-5.kt). -->

<!---
```text
{"stableReleaseDate":"2016-02-15","lastReleaseTimestamp":1657152000000}
```
-->

<!--- TEST -->