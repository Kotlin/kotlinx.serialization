[//]: # (title: Alternative and custom serialization formats)
<primary-label ref="experimental-general"/>

JSON is currently the only stable format in Kotlin serialization.

Experimental support is available for several alternative formats, including binary formats, such as [CBOR](#cbor) and [ProtoBuf](#protobuf), as well as [custom formats](#create-custom-formats).
You can use these experimental formats when JSON isn't the right fit for your use case, for example when you need a binary representation or a custom encoding.

## CBOR

Concise Binary Object Representation ([CBOR](https://datatracker.ietf.org/doc/html/rfc7049)) is a compact binary format based on JSON.
It supports a subset of [JSON features](configure-json-serialization.md) and produces binary output instead of text.

### Add dependencies for CBOR

To use CBOR in your project, add the CBOR serialization library dependency to your build file:

<tabs>
<tab id="dependency-gradle" title="Gradle">

```kotlin
// build.gradle(.kts)

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:%serializationVersion%")
}
```

</tab>

<tab id="dependency-maven" title="Maven">

```xml
<!-- pom.xml -->

<dependencies>
    <dependency>
        <groupId>org.jetbrains.kotlinx</groupId>
        <artifactId>kotlinx-serialization-cbor</artifactId>
        <version>%serializationVersion%</version>
    </dependency>
</dependencies>
```

</tab>

</tabs>

### Use CBOR for binary serialization

The [`Cbor`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-cbor/kotlinx.serialization.cbor/-cbor/) class provides two main functions:

* [`encodeToByteArray()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/encode-to-byte-array.html) serializes objects to a binary array.
* [`decodeFromByteArray()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/decode-from-byte-array.html) deserializes objects from a binary array.

Let's look at an example where a `Project` object is serialized into a binary array and then deserialized back to its original form:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.cbor.*

fun ByteArray.toAsciiHexString() = joinToString("") {
    // Shows printable ASCII bytes as characters and other bytes as hex values
    if (it in 32..127) it.toInt().toChar().toString() else
        "{${it.toUByte().toString(16).padStart(2, '0').uppercase()}}"
}

@Serializable
data class Project(val name: String, val language: String)

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val data = Project("kotlinx.serialization", "Kotlin")
    
    // Serializes the object to a CBOR binary array
    val bytes = Cbor.encodeToByteArray(data)

    // Converts the binary array to a human-readable hex string
    println(bytes.toAsciiHexString())
    // {BF}dnameukotlinx.serializationhlanguagefKotlin{FF}
    
    // Deserializes the binary array back to an object
    val obj = Cbor.decodeFromByteArray<Project>(bytes)
    println(obj)
    // Project(name=kotlinx.serialization, language=Kotlin)
}
```

<!-- REMOVE AFTER REVIEW: no runnable examples here, because playground doesn't support these formats (yet) -->

This example prints the encoded bytes in a readable mixed form. It represents printable ASCII bytes as characters and non-printable bytes as hexadecimal values.

The following table shows the same output in full [CBOR hex notation](http://cbor.me/):

| Hex Code                                     | CBOR Type    | Description                        |
|----------------------------------------------|--------------|------------------------------------|
| BF                                           | map(*)       | Start of a CBOR map                |
| 64                                           | text(4)      | Length of the string               |
| 6E616D65                                     | string       | The string "name"                  |
| 75                                           | text(21)     | Length of the string               |
| 6B6F746C696E782E73657269616C697A6174696F6E   | string       | The string "kotlinx.serialization" |
| 68                                           | text(8)      | Length of the string               |
| 6C616E6775616765                             | string       | The string "language"              |
| 66                                           | text(6)      | Length of the string               |
| 4B6F746C696E                                 | string       | The string "Kotlin"                |
| FF                                           | primitive(*) | End of the CBOR map                |

<!-- REMOVE AFTER REVIEW: moving the note from the original further down where we first talk about map formats in depth. while this is a map it doesn't have non-trivial keys, and I feel like it's better to introduce this below -->

### Ignore unknown keys in CBOR

CBOR is commonly used in communication with [IoT](https://en.wikipedia.org/wiki/Internet_of_things) devices where new properties may be added as part of API evolution.
By default, unknown keys encountered during deserialization result in an error.

Just like in [JSON](serialization-json-configuration.md#ignore-unknown-keys), you set the [`ignoreUnknownKeys`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-cbor/kotlinx.serialization.cbor/-cbor-builder/ignore-unknown-keys.html) property to `true` to ignore them during deserialization:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.cbor.*

@Serializable
data class Project(val name: String)

@OptIn(ExperimentalSerializationApi::class)
fun main() {

    // Creates a Cbor instance that ignores unknown keys during deserialization
    val format = Cbor { ignoreUnknownKeys = true }

    // Decodes the CBOR input and ignores the unknown "language" key
    val data = format.decodeFromHexString<Project>(
        // CBOR hex notation input with an extra, unknown "language" key
        "bf646e616d65756b6f746c696e782e73657269616c697a6174696f6e686c616e6775616765664b6f746c696eff"
    )
    println(data)
    // Project(name=kotlinx.serialization)
}
```

In this CBOR input, the following bytes represent the unknown `"language"` key:

* `68`: Length of the key `"language"`
* `6c616e6775616765`: The key `"language"`
* `66`: Length of the value `"Kotlin"`
* `4b6f746c696e`: The value `"Kotlin"`

### Customize how CBOR encodes data

According to the [RFC 8949 Major Types](https://datatracker.ietf.org/doc/html/rfc8949#section-3.1) specification, CBOR supports the following data types:

* Major type 0: an unsigned integer
* Major type 1: a negative integer
* Major type 2: a byte string
* Major type 3: a text string
* Major type 4: an array of data items
* Major type 5: a map of pairs of data items
* Major type 6: optional semantic tagging of other major types
* Major type 7: floating-point numbers, simple data types with no content, and the "break" stop code

By default, Kotlin `ByteArray` instances are encoded as major type 4, which represents an array of data items.

To encode `ByteArray` instances as major type 2, a byte string, use the [`@ByteString`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-cbor/kotlinx.serialization.cbor/-byte-string/) annotation:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.cbor.*

fun ByteArray.toAsciiHexString() = joinToString("") {
    if (it in 32..127) it.toInt().toChar().toString() else
        "{${it.toUByte().toString(16).padStart(2, '0').uppercase()}}"
}

@Serializable
data class Data(
    // Encodes the byte array as CBOR major type 2 as a byte string
    @ByteString
    val type2: ByteArray,
    // Encodes the byte array as CBOR major type 4 as an array of individual data items
    val type4: ByteArray
)

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    // Creates a Data object with two ByteArray fields
    val data = Data(byteArrayOf(1, 2, 3, 4), byteArrayOf(5, 6, 7, 8))

    // Serializes the Data object into a CBOR byte array
    val bytes = Cbor.encodeToByteArray(data)

    println(bytes.toAsciiHexString())
    // {BF}etype2D{01}{02}{03}{04}etype4{9F}{05}{06}{07}{08}{FF}{FF}
    
    val obj = Cbor.decodeFromByteArray<Data>(bytes)
    println(obj)
    // Data(type2=[1, 2, 3, 4], type4=[5, 6, 7, 8])
}
```

<!-- REMOVE AFTER REVIEW: I felt like we have shown enough times how it looks in CBOR hex notation so I removed that part to keep it a bit more concise. -->

In this example, the bytes before each `ByteArray` value differ because the properties use different CBOR major types.

> Instead of annotating each property with `@ByteString`, you can also encode all `ByteArray` values as major type 2 by setting the [`alwaysUseByteString`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-cbor/kotlinx.serialization.cbor/-cbor-builder/always-use-byte-string.html) property to `true`.
> 
{style="note"}

<!-- REMOVE AFTER REVIEW: I decided to move information from the original Arrays part here, to me it fits logically together with this part -->

You can also customize how CBOR encodes entire classes.

By default, classes are serialized as a CBOR map, which corresponds to major type 5.
This means that each property of the class is stored as a key-value pair.

You can serialize a class as a CBOR array, major type 4, with the [`@CborArray`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-cbor/kotlinx.serialization.cbor/-cbor-array/) annotation.
This can be useful for encoding COSE message structures, which [RFC 9052](https://www.rfc-editor.org/rfc/rfc9052#section-2) defines as CBOR arrays.

Here's an example:

```kotlin
@Serializable
@CborArray
data class DataClass(
val alg: Int,
val kid: String?
)
 
Cbor.encodeToByteArray(DataClass(alg = -7, kid = null))
```

With the `@CborArray` annotation, this example is encoded as a CBOR array: `0x8226f6`.
Without it, the same class is encoded as a CBOR map: `0xa263616c6726636b6964f6`.

Unlike JSON, CBOR supports maps with non-trivial keys.
Some parsers, such as [`jackson-dataformat-cbor`](https://github.com/FasterXML/jackson-dataformats-binary), don't support this feature.

> For a JSON workaround, see [Allow structured map keys](serialization-json-configuration.md#allow-structured-map-keys).
>
{style="tip"}

By using annotations like `@ByteString` and `@CborArray`, you can customize how CBOR encodes data to better match existing specifications and, in some cases, reduce binary size.

### Definite and indefinite length encoding in CBOR

CBOR supports two encodings for maps and arrays: *definite length encoding* and *indefinite length encoding*.

By default, Kotlin serialization uses indefinite length encoding.
This means that the number of elements in a map or array isn't encoded explicitly, and a terminating byte is appended after the last element.

Definite length encoding omits the terminating byte and encodes the number of elements at the start of the map or array.

To switch between these two modes, use the [`useDefiniteLengthEncoding`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-cbor/kotlinx.serialization.cbor/-cbor-builder/use-definite-length-encoding.html) property.

### Tags and labels in CBOR

CBOR allows you to define *tags* that encode additional information for properties and values.
You can specify these tags with the [`@KeyTags`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-cbor/kotlinx.serialization.cbor/-key-tags/) and [`@ValueTags`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-cbor/kotlinx.serialization.cbor/-value-tags/) annotations.
The `encodeKeyTags`, `encodeValueTags`, `verifyKeyTags`, and `verifyValueTags` properties control
the encoding and verification of these tags.

> For more information on tagging in CBOR, see [RFC 8949 Tagging of Items](https://datatracker.ietf.org/doc/html/rfc8949#name-tagging-of-items).
>
{style="tip"}

You can also tag classes using the [`@ObjectTags`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-cbor/kotlinx.serialization.cbor/-object-tags/) annotation, which applies tags to all instances of a class.

When serializing, `@ObjectTags` are encoded directly before the data of the tagged object.
If a property has value tags and its type has object tags, the value tags are encoded before the object tags.
The `encodeObjectTags` and `verifyObjectTags` properties control whether object tags are encoded and verified.
If you verify only value tags and don't verify object tags, the decoder can still deserialize data with additional object tags.

> For a list of well-known tags, see [`CborTag`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-cbor/kotlinx.serialization.cbor/-cbor-tag/).
>
{style="tip"}

CBOR supports map keys of any type.
In COSE (CBOR Object Signing and Encryption), these keys are restricted to strings and numbers and are called *labels*.

You can assign string labels with the [`@SerialName`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-serial-name/) annotation and numeric labels with the [`@CborLabel`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-cbor/kotlinx.serialization.cbor/-cbor-label/) annotation.
The [`preferCborLabelsOverNames`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-cbor/kotlinx.serialization.cbor/-cbor-configuration/prefer-cbor-labels-over-names.html) property allows prioritizing numeric labels over serial names when both are present.
You can use it to keep compact labels for CBOR while still keeping readable names when serializing to JSON.

Kotlin serialization also provides a predefined [`Cbor.CoseCompliant`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-cbor/kotlinx.serialization.cbor/-cbor/-default/-cose-compliant.html) instance that follows COSE encoding requirements.
It uses definite length encoding, encodes and verifies all tags, and prefers numeric labels over serial names.

### Custom CBOR-specific serializers

CBOR encoders and decoders implement the [`CborEncoder`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-cbor/kotlinx.serialization.cbor/-cbor-encoder/) and [`CborDecoder`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-cbor/kotlinx.serialization.cbor/-cbor-decoder/) interfaces.

These interfaces extend the general `Encoder` and `Decoder` APIs, providing access to CBOR-specific configurations through the [`cbor`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-cbor/kotlinx.serialization.cbor/-cbor-encoder/cbor.html) property.
Custom serializers can use this property to access the current [`Cbor`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-cbor/kotlinx.serialization.cbor/-cbor/) instance, produce embedded byte arrays, and read the current settings, such as `preferCborLabelsOverNames` and `useDefiniteLengthEncoding`.

For more information about creating custom serializers, see [Create custom serializers](create-custom-serializers.md).

## ProtoBuf

[Protocol Buffers](https://developers.google.com/protocol-buffers), or ProtoBuf, is a language-neutral binary format that normally
relies on a separate `.proto` file to define the protocol schema.
It's more compact than CBOR, because it assigns integer numbers to fields instead of names.

Kotlin serialization uses [proto2 semantics](https://protobuf.dev/programming-guides/proto2/), where all fields are explicitly required or optional.

### Add dependencies for ProtoBuf

To use ProtoBuf in your project, add the ProtoBuf serialization library dependency to your build file:

<tabs>

<tab id="gradle-proto" title="Gradle">

```kotlin
// build.gradle(.kts)

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:%serializationVersion%")
}
```

</tab>

<tab id="maven-proto" title="Maven">

```xml
<!-- pom.xml -->

<dependencies>
    <dependency>
        <groupId>org.jetbrains.kotlinx</groupId>
        <artifactId>kotlinx-serialization-protobuf</artifactId>
        <version>%serializationVersion%</version>
    </dependency>
</dependencies>
```

</tab>
</tabs>

### Use ProtoBuf for binary serialization

To serialize objects with ProtoBuf, use the
[`ProtoBuf`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-protobuf/kotlinx.serialization.protobuf/-proto-buf/) class with the [`.encodeToByteArray()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-protobuf/kotlinx.serialization.protobuf/-proto-buf/encode-to-byte-array.html) and the [`.decodeFromByteArray()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-protobuf/kotlinx.serialization.protobuf/-proto-buf/decode-from-byte-array.html) functions:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*

fun ByteArray.toAsciiHexString() = joinToString("") {
    if (it in 32..127) it.toInt().toChar().toString() else
        "{${it.toUByte().toString(16).padStart(2, '0').uppercase()}}"
}

@Serializable
data class Project(val name: String, val language: String)

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val data = Project("kotlinx.serialization", "Kotlin")
    // Serializes the Project instance into a ProtoBuf byte array
    val bytes = ProtoBuf.encodeToByteArray(data)

    // Prints the byte array in a readable mixed form
    println(bytes.toAsciiHexString())
    // {0A}{15}kotlinx.serialization{12}{06}Kotlin

    // Deserializes the ProtoBuf byte array back to an object
    val obj = ProtoBuf.decodeFromByteArray<Project>(bytes)
    println(obj)
    // Project(name=kotlinx.serialization, language=Kotlin)
}
```

In this example, the output prints string values as readable text and the remaining bytes in hexadecimal form.
The same bytes correspond to the following values in [ProtoBuf hex notation](https://protogen.marcgravell.com/decode):

```text
Field #1: 0A String Length = 21, Hex = 15, UTF8 = "kotlinx.serialization"
Field #2: 12 String Length = 6, Hex = 06, UTF8 = "Kotlin"
```

### Assign field numbers for ProtoBuf serialization

By default, ProtoBuf assigns field numbers automatically.

To keep your schema stable over time, use the [`@ProtoNumber`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-protobuf/kotlinx.serialization.protobuf/-proto-number/) annotation to assign field numbers explicitly, without needing a separate `.proto` file.
For example, `@ProtoNumber(1)` assigns field number 1 to a property, so the Kotlin serialization's ProtoBuf format uses that number during encoding and decoding instead of assigning one automatically.

This is useful if you plan to reorder properties, and it aligns with Protobuf's compatibility rules for evolving schemas.

Here's an example:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*

fun ByteArray.toAsciiHexString() = joinToString("") {
    if (it in 32..127) it.toInt().toChar().toString() else
        "{${it.toUByte().toString(16).padStart(2, '0').uppercase()}}"
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Project(
    // Assigns field number 1 to the name property
    @ProtoNumber(1)
    val name: String,

    // Assigns field number 3 to the language property
    @ProtoNumber(3)
    val language: String
)

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val data = Project("kotlinx.serialization", "Kotlin") 
    val bytes = ProtoBuf.encodeToByteArray(data)   

    println(bytes.toAsciiHexString())
    // {0A}{15}kotlinx.serialization{1A}{06}Kotlin

    val obj = ProtoBuf.decodeFromByteArray<Project>(bytes)

    println(obj)
    // Project(name=kotlinx.serialization, language=Kotlin)
}
```

In this example, the `name` property uses field number 1, and its encoded tag is `0A`.
The `language` property uses field number 3, and its encoded tag is `1A`.

In ProtoBuf hex notation, the output is equivalent to the following:

```text
Field #1: 0A String Length = 21, Hex = 15, UTF8 = "kotlinx.serialization" (total 21 chars)
Field #3: 1A String Length = 6, Hex = 06, UTF8 = "Kotlin"
```

> For more information about Protobuf field numbers, see the [Official Protobuf Language Guide](https://protobuf.dev/programming-guides/proto2/#assigning).
>
{style="tip"}

### Specify integer encoding in ProtoBuf

ProtoBuf encodes integer properties using varint encoding by default.

To use a different integer encoding for a property, apply the the [`@ProtoType`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-protobuf/kotlinx.serialization.protobuf/-proto-type/) annotation with a [`ProtoIntegerType`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-protobuf/kotlinx.serialization.protobuf/-proto-integer-type/) value.
This annotation affects `Byte`, `Short`, `Int`, `Long`, and `Char` properties.

The `ProtoIntegerType` enum supports three options:

* The [`DEFAULT`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-protobuf/kotlinx.serialization.protobuf/-proto-integer-type/-d-e-f-a-u-l-t/) option uses varint encoding (`intXX`), which is optimized for
  small non-negative numbers. For example, the value of `1` is encoded in one byte as `01`.
* The [`SIGNED`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-protobuf/kotlinx.serialization.protobuf/-proto-integer-type/-s-i-g-n-e-d/) option uses signed ZigZag encoding (`sintXX`), making it suitable
  for small signed integers. For example, it encodes the value of `-2` in one byte as `03`.
* The [`FIXED`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-protobuf/kotlinx.serialization.protobuf/-proto-integer-type/-f-i-x-e-d/) option uses fixed-width encoding (`fixedXX`), which always uses a fixed number of bytes.
  For example, it encodes the value of `3` as four bytes `03 00 00 00`.

> `uintXX` and `sfixedXX` Protocol Buffer types are not supported.
>
{style="note"}

The following example shows all three supported options:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*

fun ByteArray.toAsciiHexString() = joinToString("") {
    if (it in 32..127) it.toInt().toChar().toString() else
        "{${it.toUByte().toString(16).padStart(2, '0').uppercase()}}"
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
class Data(
    // Uses DEFAULT encoding, optimized for small non-negative numbers
    @ProtoType(ProtoIntegerType.DEFAULT)
    val a: Int,

    // Uses SIGNED encoding, optimized for small signed integers
    @ProtoType(ProtoIntegerType.SIGNED)
    val b: Int,

    // Uses FIXED encoding, which always uses a fixed number of bytes
    @ProtoType(ProtoIntegerType.FIXED)
    val c: Int
)

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val data = Data(1, -2, 3) 
    println(ProtoBuf.encodeToByteArray(data).toAsciiHexString())
    // {08}{01}{10}{03}{1D}{03}{00}{00}{00}
}
```

<!-- REMOVE AFTER REVIEW: Like with CBOR examples, I felt like we have shown enough times how it looks in ProtoBuf hex notation so I removed that part to keep it a bit more concise. -->

### Encode empty lists in ProtoBuf

In ProtoBuf, lists and other collections are encoded as *repeated fields*, where each element is written as a separate entry for the same field number.
When Kotlin serialization encodes an empty list with ProtoBuf, it writes no entries for that field.
This makes an empty collection indistinguishable from a missing field during deserialization.

To deserialize an empty collection correctly, specify `emptyList()` as the default value for collection and map properties.

Here's an example:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*

fun ByteArray.toAsciiHexString() = joinToString("") {
    if (it in 32..127) it.toInt().toChar().toString() else
        "{${it.toUByte().toString(16).padStart(2, '0').uppercase()}}"
}

@Serializable
data class Data(
    // Sets an empty list as the default value for lists a and b
    val a: List<Int> = emptyList(),
    val b: List<Int> = emptyList()
)

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val data = Data(listOf(1, 2, 3), listOf())
    val bytes = ProtoBuf.encodeToByteArray(data)

    println(bytes.toAsciiHexString())
    // {08}{01}{08}{02}{08}{03}

    println(ProtoBuf.decodeFromByteArray<Data>(bytes))
    // Data(a=[1, 2, 3], b=[])
}
```

In this example, the list `a` contains three elements, which are encoded in the output, while the list `b` is empty.
The `emptyList()` default lets Kotlin serialization decode `b` as an empty list instead of treating it as a missing field.

### Encode numeric collections as packed fields

In ProtoBuf, packed fields store repeated primitive numeric values more efficiently by writing the list
as a single length-delimited entry instead of repeating the field tag for every element.

You can use the [`@ProtoPacked`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-protobuf/kotlinx.serialization.protobuf/-proto-packed/) annotation
to serialize collection types except maps in this form.
Packed encoding applies only to repeated primitive numeric fields, and the annotation is ignored for other element types.

According to the [Protobuf encoding specification](https://developers.google.com/protocol-buffers/docs/encoding#packed),
parsers accept both packed and unpacked repeated numeric fields, so decoding doesn't depend on whether `@ProtoPacked` is present.

Here's an example:

```kotlin
import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*

fun ByteArray.toAsciiHexString() = joinToString("") {
    "{${it.toUByte().toString(16).padStart(2, '0').uppercase()}}"
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class DataPacked(
    // Uses packed encoding for the values list
    @ProtoPacked
    val values: List<Int>
)

@Serializable
data class DataRepeated(
    val values: List<Int>
)

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val packed = ProtoBuf.encodeToByteArray(
        DataPacked.serializer(),
        DataPacked(listOf(1, 2, 3))
    )
    println(packed.toAsciiHexString())
    // {0A}{03}{01}{02}{03}

    val repeated = ProtoBuf.encodeToByteArray(
        DataRepeated.serializer(),
        DataRepeated(listOf(1, 2, 3))
    )
    println(repeated.toAsciiHexString())
    // {08}{01}{08}{02}{08}{03}}

    println(ProtoBuf.decodeFromByteArray(DataRepeated.serializer(), packed))
    // DataRepeated(values=[1, 2, 3])

    println(ProtoBuf.decodeFromByteArray(DataPacked.serializer(), repeated))
    // DataPacked(values=[1, 2, 3])
}
```

<!-- REMOVE AFTER REVIEW: I wanted to add an example here as well, please let me know if this works -->

### Represent `oneof` fields

A [`oneof`](https://protobuf.dev/programming-guides/proto2/#oneof) field defines a group of fields where only one value can be set at a time.

In Kotlin serialization, you can represent this structure with a [polymorphic](serialization-polymorphism.md) type.

Consider this ProtoBuf message definition:

```text
message Data {
    required string name = 1;
    oneof phone {
        string home_phone = 2;
        string work_phone = 3;
    }
}
```

To represent this message in Kotlin:

1. Define a class for the entire message. 
2. Add a property of the polymorphic type to the class. Annotate this property with [`@ProtoOneOf`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-protobuf/kotlinx.serialization.protobuf/-proto-one-of/).
3. Create a `sealed interface` or `abstract class` to represent the fields inside the `oneof` declaration.
4. Create a subclass for each field in the `oneof` declaration. Each subclass must have a single property for that field.
5. Annotate these properties with `@ProtoNumber` using the field numbers from the `oneof` declaration.

Here's a more detailed example where `oneof` is used to store either a home phone or a work phone:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*

// Defines the data class with a oneof property
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Data(
    // Assigns field number 1 to the name property
    @ProtoNumber(1) val name: String,

    // Maps the polymorphic property to the oneof declaration
    @ProtoOneOf val phone: IPhoneType?,
)

// Represents the oneof group
@Serializable sealed interface IPhoneType

// Represents the home_phone field
@OptIn(ExperimentalSerializationApi::class)
@Serializable @JvmInline value class HomePhone(@ProtoNumber(2) val number: String): IPhoneType

// Represents the work_phone field
@OptIn(ExperimentalSerializationApi::class)
@Serializable data class WorkPhone(@ProtoNumber(3) val number: String): IPhoneType

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val dataTom = Data("Tom", HomePhone("123"))
    val stringTom = ProtoBuf.encodeToHexString(dataTom)
    val dataJerry = Data("Jerry", WorkPhone("789"))
    val stringJerry = ProtoBuf.encodeToHexString(dataJerry)

    println(stringTom)
    // 0a03546f6d1203313233

    println(stringJerry)
    // 0a054a657272791a03373839
  
    println(ProtoBuf.decodeFromHexString<Data>(stringTom))
    // Data(name=Tom, phone=HomePhone(number=123))

    println(ProtoBuf.decodeFromHexString<Data>(stringJerry))
    // Data(name=Jerry, phone=WorkPhone(number=789))
}
```

The output shows that ProtoBuf encodes only one field from the `oneof` declaration in each message:

* `0a03546f6d1203313233` represents `"Tom"` with `home_phone`.
* `0a054a657272791a03373839` represents `"Jerry"` with `work_phone`.

> To prevent potential field number conflicts or an `IllegalArgumentException` at runtime, map each `oneof` type hierarchy to only one data class.
>
{style="note"}

You can also define a class without the `@ProtoOneOf` annotation if you only need to deserialize data.

For example:

```kotlin
@Serializable  
data class Data(  
    @ProtoNumber(1) val name: String,
    @ProtoNumber(2) val homeNumber: String? = null,  
    @ProtoNumber(3) val workNumber: String? = null,  
)  
```

This way, you can deserialize `oneof` fields without using a sealed hierarchy.

However, it doesn't enforce exclusivity between `homeNumber` and `workNumber` during serialization.
If both fields have values, the serialized output may not match the original `oneof` schema,
and another ProtoBuf parser may keep only the last field it reads.

<!-- REMOVE AFTER REVIEW: I'm not sure I understand the original meaning, so I slightly rewritten this one based on my understanding. "....if an instance of Data2 has both (or none).. " If I understand correctly, if both are null simply nothing is set, if one is null other is set value it works, and if both are set there is a mismatched schema (if not true we can simply add if both are null to the above explanation) --> 

### Generate a ProtoBuf schema

Typically, working with ProtoBuf involves using a `.proto` file and a code generator to create code for serialization and deserialization.
However, with Kotlin serialization, you can use Kotlin classes annotated with `@Serializable` as the source for the schema, making `.proto` files optional.

This approach simplifies the process when all the code involved is written in Kotlin,
interoperability with other languages often still requires a `.proto` schema.

To generate this schema, use the [`ProtoBufSchemaGenerator`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-protobuf/kotlinx.serialization.protobuf.schema/-proto-buf-schema-generator/).
It generates a Proto2-compatible schema from one or more `SerialDescriptor` instances.

This gives you a `.proto` schema that you can use with other ProtoBuf tools.

Here's an example that generates a `.proto` schema from a Kotlin data class:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*
import kotlinx.serialization.protobuf.schema.ProtoBufSchemaGenerator

@Serializable
data class SampleData(
    val amount: Long,
    val description: String?,
    val department: String = "QA"
)

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val descriptors = listOf(SampleData.serializer().descriptor)
    val schemas = ProtoBufSchemaGenerator.generateSchemaText(descriptors)
    println(schemas)
}
```

This code generates the following `.proto` schema:

```text
syntax = "proto2";


// serial name 'example.exampleFormats09.SampleData'
message SampleData {
  required int64 amount = 1;
  optional string description = 2;
  // WARNING: a default value decoded when value is missing
  optional string department = 3;
}
```

> Default Kotlin values aren't represented in `.proto` files, so the generated schema adds a warning comment for properties with default values.
>
{style="note"}

## Properties

Kotlin serialization can serialize a class into a flat map with `String` keys using
the [`Properties`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-properties/kotlinx.serialization.properties/-properties/) format implementation.

To use the `Properties` format in your project, add the properties serialization library dependency to your build file:

<tabs> 

<tab id="gradle-properties" title="Gradle">

```kotlin
// build.gradle(.kts)

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-properties:%serializationVersion%")
}
```

</tab> 

<tab id="maven-properties" title="Maven">

```xml
<!-- pom.xml -->

<dependencies>
    <dependency>
        <groupId>org.jetbrains.kotlinx</groupId>
        <artifactId>kotlinx-serialization-properties</artifactId>
        <version>%serializationVersion%</version>
    </dependency>
</dependencies>
```

</tab>
</tabs>

Here's an example that encodes a class into a flat map with dot-separated keys for nested properties:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.properties.*

@Serializable
class Project(val name: String, val owner: User)

@Serializable
class User(val name: String)

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val data = Project("kotlinx.serialization",  User("kotlin"))
    // Encodes the object into a flat map
    val map = Properties.encodeToMap(data)

    // Iterates through the map and prints the key-value pairs
    map.forEach { (k, v) -> println("$k = $v") }
    // name = kotlinx.serialization
    // owner.name = kotlin
}
```

## Create custom formats

To implement a custom format in Kotlin serialization,
implement the [`Encoder`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-encoder/) and [`Decoder`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-decoder/) interfaces.

These interfaces are extensive, but the [`AbstractEncoder`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-abstract-encoder/) and [`AbstractDecoder`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-abstract-decoder/) classes can simplify the process.

The `AbstractEncoder` class provides default implementations for most of the encode functions, such as [`encodeString()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-abstract-encoder/encode-string.html),
which delegate to the [`encodeValue(value: Any)`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-abstract-encoder/encode-value.html) function.
This means that by overriding the `encodeValue()` function, you can create a basic, functional custom format with minimal effort.

### Create a basic encoder

To build a custom `Encoder` in Kotlin serialization, you can extend the `AbstractEncoder` class.
This allows you to control how each value is serialized during the encoding process.

To create a basic encoder:

1. Create a class that extends `AbstractEncoder` to define custom serialization logic:

    ```kotlin
    @ExperimentalSerializationApi
    class ListEncoder : AbstractEncoder() {
        val list = mutableListOf<Any>()
    
        override val serializersModule: SerializersModule = EmptySerializersModule()
   }
    ```

2. Override `encodeValue()` to define how the encoder handles each value.

    ```kotlin
        override fun encodeValue(value: Any) {
            list.add(value)
        }
    ```

3. Create a function that uses the encoder to serialize a value:

    ```kotlin
    @ExperimentalSerializationApi
    fun <T> encodeToList(serializer: SerializationStrategy<T>, value: T): List<Any> {
        val encoder = ListEncoder()
        encoder.encodeSerializableValue(serializer, value)
        return encoder.list
    }
    ```

4. Add an inline overload to make serialization easier to call:

    ```kotlin
    @ExperimentalSerializationApi
    inline fun <reified T> encodeToList(value: T) = encodeToList(serializer(), value)
    ```

> Since encoders are usually used by other parts of an application,
> propagate the `@ExperimentalSerializationApi` annotation instead of opting in only inside individual functions.
>
{style="note"}

Here's a complete example that encodes the primitive values from an object graph into a flat list in serialization order:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*

// Creates a custom encoder that stores serialized values in a list
@ExperimentalSerializationApi
class ListEncoder : AbstractEncoder() {
    val list = mutableListOf<Any>()

    override val serializersModule: SerializersModule = EmptySerializersModule()

    // Stores each encoded value
    override fun encodeValue(value: Any) {
        list.add(value)
    }
}

// Encodes a value and returns the collected values as a list
@ExperimentalSerializationApi
fun <T> encodeToList(serializer: SerializationStrategy<T>, value: T): List<Any> {
    val encoder = ListEncoder()
    encoder.encodeSerializableValue(serializer, value)
    return encoder.list
}

// Provides a type-safe inline overload for convenience
@ExperimentalSerializationApi
inline fun <reified T> encodeToList(value: T) = encodeToList(serializer(), value)

@Serializable
data class Project(val name: String, val owner: User, val votes: Int)

@Serializable
data class User(val name: String)

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val data = Project("kotlinx.serialization",  User("kotlin"), 9000)
    println(encodeToList(data))
    // [kotlinx.serialization, kotlin, 9000]
}
```

The output shows that the encoder collects all serialized values into a flat list in serialization order.
This can be useful when you need to process those values uniformly, for example to compute a hash or digest.

### Create a basic decoder

To build a custom `Decoder` in Kotlin serialization, you can extend the `AbstractDecoder` class.
This allows you to control how each value is deserialized during the decoding process.

To create a basic decoder:

1. Create a class that extends `AbstractDecoder` to define custom deserialization logic:

    ```kotlin
    @ExperimentalSerializationApi
    class ListDecoder(val list: ArrayDeque<Any>) : AbstractDecoder() {
        private var elementIndex = 0
    
        override val serializersModule: SerializersModule = EmptySerializersModule()
    }
    ```

2. Override the [`decodeValue()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-abstract-decoder/decode-value.html) function to define how the decoder deserializes values:

    ```kotlin
        override fun decodeValue(): Any = list.removeFirst()
    ```

3. Override the [`decodeElementIndex()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-composite-decoder/decode-element-index.html) function to report which element is decoded next and return `DECODE_DONE` when the structure is complete:

    ```kotlin
        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            if (elementIndex == descriptor.elementsCount) return CompositeDecoder.DECODE_DONE
            return elementIndex++
        }
    ```

4. Override the [`beginStructure()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-abstract-decoder/begin-structure.html) function to create a new decoder for each nested structure:

    ```kotlin
        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
            ListDecoder(list)
    ```

5. Create a function that uses the decoder to deserialize a value:

    ```kotlin
    @ExperimentalSerializationApi
    fun <T> decodeFromList(list: List<Any>, deserializer: DeserializationStrategy<T>): T {
        val decoder = ListDecoder(ArrayDeque(list))
        return decoder.decodeSerializableValue(deserializer)
    }
    ```

6. Add an inline overload to make deserialization easier to call:

    ```kotlin
    @ExperimentalSerializationApi
    inline fun <reified T> decodeFromList(list: List<Any>): T = decodeFromList(list, serializer())
    ```

Here's a complete example that decodes a list of primitive values back into an object:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*

// Creates a custom encoder that stores serialized values in a list
@ExperimentalSerializationApi
class ListEncoder : AbstractEncoder() {
    val list = mutableListOf<Any>()

    override val serializersModule: SerializersModule = EmptySerializersModule()

    // Stores each encoded value
    override fun encodeValue(value: Any) {
       list.add(value)
    }
}

// Encodes a value and returns the collected values as a list
@ExperimentalSerializationApi
fun <T> encodeToList(serializer: SerializationStrategy<T>, value: T): List<Any> {
    val encoder = ListEncoder()
    encoder.encodeSerializableValue(serializer, value)
    return encoder.list
}

// Provides a type-safe, inline version of encodeToList for convenience
@ExperimentalSerializationApi
inline fun <reified T> encodeToList(value: T) = encodeToList(serializer(), value)

// Creates a custom decoder that reads serialized values from a list
@ExperimentalSerializationApi
class ListDecoder(val list: ArrayDeque<Any>) : AbstractDecoder() {
    private var elementIndex = 0

    override val serializersModule: SerializersModule = EmptySerializersModule()

    // Returns the next value from the list
    override fun decodeValue(): Any = list.removeFirst()
    
    // Reports the next element index or signals that decoding is complete
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (elementIndex == descriptor.elementsCount) return CompositeDecoder.DECODE_DONE
        return elementIndex++
    }

    // Creates a new decoder for each nested structure
    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
        ListDecoder(list)
}

// Deserializes an object from a list using the custom decoder
@ExperimentalSerializationApi
fun <T> decodeFromList(list: List<Any>, deserializer: DeserializationStrategy<T>): T {
    val decoder = ListDecoder(ArrayDeque(list))
    return decoder.decodeSerializableValue(deserializer)
}

// Provides a type-safe, inline version of decodeFromList for convenience
@ExperimentalSerializationApi
inline fun <reified T> decodeFromList(list: List<Any>): T = decodeFromList(list, serializer())

@Serializable
data class Project(val name: String, val owner: User, val votes: Int)

@Serializable
data class User(val name: String)

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val data = Project("kotlinx.serialization",  User("kotlin"), 9000)
    val list = encodeToList(data)
    println(list)
    // [kotlinx.serialization, kotlin, 9000]

    val obj = decodeFromList<Project>(list)
    println(obj)
    // Project(name=kotlinx.serialization, owner=User(name=kotlin), votes=9000)
}
```

The output shows that the decoder reads the list back in serialization order and reconstructs the original object.

The `decodeElementIndex()` function reports which property comes next, and the `beginStructure()` function creates a new decoder state for nested objects.

### Optimize custom encoders and decoders

The following sections build on the `ListEncoder` and `ListDecoder` implementations introduced in the [Create a basic encoder](#create-a-basic-encoder) and [Create a basic decoder](#create-a-basic-decoder) sections.

These improvements make the examples more robust and better suited for more complex data structures.

#### Optimize with sequential decoding

If your format always stores values in declaration order, you can optimize decoding with the [`decodeSequentially()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-composite-decoder/decode-sequentially.html) function.

When this function returns `true`, serializers that support sequential decoding can read values in order without repeatedly requesting the next element index.
This can improve performance when deserializing data stored in sequential formats.

Here's how to apply this optimization to the custom `ListDecoder`:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*

@ExperimentalSerializationApi
class ListEncoder : AbstractEncoder() {
    val list = mutableListOf<Any>()

    override val serializersModule: SerializersModule = EmptySerializersModule()

    override fun encodeValue(value: Any) {
        list.add(value)
    }
}

@ExperimentalSerializationApi
fun <T> encodeToList(serializer: SerializationStrategy<T>, value: T): List<Any> {
    val encoder = ListEncoder()
    encoder.encodeSerializableValue(serializer, value)
    return encoder.list
}

@ExperimentalSerializationApi
inline fun <reified T> encodeToList(value: T) = encodeToList(serializer(), value)

@ExperimentalSerializationApi
class ListDecoder(val list: ArrayDeque<Any>) : AbstractDecoder() {
    private var elementIndex = 0

    override val serializersModule: SerializersModule = EmptySerializersModule()

    override fun decodeValue(): Any = list.removeFirst()
  
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (elementIndex == descriptor.elementsCount) return CompositeDecoder.DECODE_DONE
        return elementIndex++
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
        ListDecoder(list) 

    // Enables sequential decoding for serializers that support it
    override fun decodeSequentially(): Boolean = true
}


@ExperimentalSerializationApi
fun <T> decodeFromList(list: List<Any>, deserializer: DeserializationStrategy<T>): T {
    val decoder = ListDecoder(ArrayDeque(list))
    return decoder.decodeSerializableValue(deserializer)
}

@ExperimentalSerializationApi
inline fun <reified T> decodeFromList(list: List<Any>): T = decodeFromList(list, serializer())

@Serializable
data class Project(val name: String, val owner: User, val votes: Int)

@Serializable
data class User(val name: String)

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val data = Project("kotlinx.serialization",  User("kotlin"), 9000)
    val list = encodeToList(data)
    println(list)
    // [kotlinx.serialization, kotlin, 9000]

    val obj = decodeFromList<Project>(list)
    println(obj)
    // Project(name=kotlinx.serialization, owner=User(name=kotlin), votes=9000)
}
```

<!-- REMOVE AFTER REVIEW: A bit awkward that we don't have //sampleStart and //sampleEnd for non-runnable code snippets, but I still think it's better to show the entire code here (hopefully it'll get introduced soon) - please let me know what you think -->

In this example, `ListDecoder` overrides the `decodeSequentially()` function to return `true`.
This lets supported serializers read values directly in declaration order instead of querying each element index one by one.

#### Add collection support

A basic custom format can encode collection elements one by one, but decoding also needs to know how many elements belong to the collection.

To support collections in a custom format, encode the collection size along with its elements so the decoder knows where the collection ends:

1. Implement the [`beginCollection()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-encoder/begin-collection.html) function in the encoder to handle the collection size.
2. Return the encoder instance from the `beginCollection()` function if the encoder doesn't need extra collection-specific state.
3. Implement the [`decodeCollectionSize()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-composite-decoder/decode-collection-size.html) function in the decoder to decode and store the collection size during deserialization.
4. Return `true` from the `decodeSequentially()` function returns `true` if the format stores collection size in advance and values are read in order.

Here's a complete example that adds collection support to `ListEncoder`:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*

@ExperimentalSerializationApi
class ListEncoder : AbstractEncoder() {
    val list = mutableListOf<Any>()

    override val serializersModule: SerializersModule = EmptySerializersModule()

    override fun encodeValue(value: Any) {
        list.add(value)
    }

    // Stores the collection size before its elements
    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        encodeInt(collectionSize)
        return this
    }
}

@ExperimentalSerializationApi
fun <T> encodeToList(serializer: SerializationStrategy<T>, value: T): List<Any> {
    val encoder = ListEncoder()
    encoder.encodeSerializableValue(serializer, value)
    return encoder.list
}

@ExperimentalSerializationApi
inline fun <reified T> encodeToList(value: T) = encodeToList(serializer(), value)

@ExperimentalSerializationApi
class ListDecoder(val list: ArrayDeque<Any>, var elementsCount: Int = 0) : AbstractDecoder() {
    private var elementIndex = 0

    override val serializersModule: SerializersModule = EmptySerializersModule()

    override fun decodeValue(): Any = list.removeFirst()

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (elementIndex == elementsCount) return CompositeDecoder.DECODE_DONE
        return elementIndex++
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
        ListDecoder(list, descriptor.elementsCount)

    override fun decodeSequentially(): Boolean = true

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int =
        decodeInt().also { elementsCount = it }
}

@ExperimentalSerializationApi
fun <T> decodeFromList(list: List<Any>, deserializer: DeserializationStrategy<T>): T {
    val decoder = ListDecoder(ArrayDeque(list))
    return decoder.decodeSerializableValue(deserializer)
}

@ExperimentalSerializationApi
inline fun <reified T> decodeFromList(list: List<Any>): T = decodeFromList(list, serializer())

@Serializable
data class Project(val name: String, val owners: List<User>, val votes: Int)

@Serializable
data class User(val name: String)

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val data = Project("kotlinx.serialization",  listOf(User("kotlin"), User("jetbrains")), 9000)
    val list = encodeToList(data)
    println(list)
    // [kotlinx.serialization, 2, kotlin, jetbrains, 9000]

    val obj = decodeFromList<Project>(list)
    println(obj)
    // Project(name=kotlinx.serialization, owners=[User(name=kotlin), User(name=jetbrains)], votes=9000)
}
```

In this example, the encoded list includes the collection size before the collection elements, so the decoder can correctly decode collections.

#### Add null support

To support null values in a custom format, you need a way to distinguish `null` from a regular value.

This typically involves adding a "null indicator" before each nullable value that distinguishes between `null` values and actual data.

To add support for null values in a custom format:

1. Override [`encodeNull()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-abstract-encoder/encode-null.html) in the encoder to specify how `null` values are marked.
2. Override [`encodeNotNullMark()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-encoder/encode-not-null-mark.html) in the encoder to mark non-null values.
3. Override [`decodeNotNullMark()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-decoder/decode-not-null-mark.html) in the decoder to check that marker before decoding the value.

Here's an example that adds `null` support to the `ListEncoder` and `ListDecoder` implementations:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*

@ExperimentalSerializationApi
class ListEncoder : AbstractEncoder() {
    val list = mutableListOf<Any>()

    override val serializersModule: SerializersModule = EmptySerializersModule()

    override fun encodeValue(value: Any) {
        list.add(value)
    }

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        encodeInt(collectionSize)
        return this
    }

    // Represents null values as NULL
    override fun encodeNull() = encodeValue("NULL")

    // Represents non-null values with "!!"
    override fun encodeNotNullMark() = encodeValue("!!")
}

@ExperimentalSerializationApi
fun <T> encodeToList(serializer: SerializationStrategy<T>, value: T): List<Any> {
    val encoder = ListEncoder()
    encoder.encodeSerializableValue(serializer, value)
    return encoder.list
}

@ExperimentalSerializationApi
inline fun <reified T> encodeToList(value: T) = encodeToList(serializer(), value)

@ExperimentalSerializationApi
class ListDecoder(val list: ArrayDeque<Any>, var elementsCount: Int = 0) : AbstractDecoder() {
    private var elementIndex = 0

    override val serializersModule: SerializersModule = EmptySerializersModule()

    override fun decodeValue(): Any = list.removeFirst()

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (elementIndex == elementsCount) return CompositeDecoder.DECODE_DONE
        return elementIndex++
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
        ListDecoder(list, descriptor.elementsCount)

    override fun decodeSequentially(): Boolean = true

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int =
        decodeInt().also { elementsCount = it }

    // Checks if the next value is "NULL" to determine whether the next nullable value is null
    override fun decodeNotNullMark(): Boolean = decodeString() != "NULL"
}

@ExperimentalSerializationApi
fun <T> decodeFromList(list: List<Any>, deserializer: DeserializationStrategy<T>): T {
    val decoder = ListDecoder(ArrayDeque(list))
    return decoder.decodeSerializableValue(deserializer)
}

@ExperimentalSerializationApi
inline fun <reified T> decodeFromList(list: List<Any>): T = decodeFromList(list, serializer())

@Serializable
data class Project(val name: String, val owner: User?, val votes: Int?)

@Serializable
data class User(val name: String)

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val data = Project("kotlinx.serialization", User("kotlin"), null)
    val list = encodeToList(data)
    println(list)
    // [kotlinx.serialization, !!, kotlin, NULL]

    val obj = decodeFromList<Project>(list)
    println(obj)
    // Project(name=kotlinx.serialization, owner=User(name=kotlin), votes=null)
}
```

In this example, the encoder writes `!!` before a non-null value and writes `NULL` for a `null` value.
The decoder checks these markers to decide whether to decode a value or return `null`.

### Create a compact binary format

Binary formats are often used for their compact representation of data, making them ideal for scenarios where minimizing storage size or transmission bandwidth is important.

Custom binary formats allow you to control how data is serialized and deserialized at a low level,
providing flexibility to optimize performance and compatibility with other systems.

You can implement a custom binary format with Kotlin serialization by using the
[`java.io.DataOutput`](https://docs.oracle.com/javase/8/docs/api/java/io/DataOutput.html) and the [`java.io.DataInput`](https://docs.oracle.com/javase/8/docs/api/java/io/DataInput.html) interfaces.
These interfaces let you control how values of [primitive types](serialization-serialize-builtin-types.md#basic-types) are encoded and decoded in binary form.

Let's look at an example of how to turn the `ListEncoder` and `ListDecoder` implementations into a compact binary format using `DataOutput` and `DataInput`:

1. Override the encode functions for each primitive type, such as `encodeInt()` for integers or `encodeString()` for strings.
This lets the encoder encode values directly to the `DataOutput` stream:
    
    ```kotlin
    // Imports declarations from the serialization library
    import kotlinx.serialization.*
    import kotlinx.serialization.Serializable
    import kotlinx.serialization.descriptors.*
    import kotlinx.serialization.encoding.*
    import kotlinx.serialization.modules.*
    import java.io.*
    
    @ExperimentalSerializationApi
    class DataOutputEncoder(val output: DataOutput) : AbstractEncoder() {
        override val serializersModule: SerializersModule = EmptySerializersModule()
    
        // Encodes primitive values directly in binary form
        override fun encodeBoolean(value: Boolean) = output.writeByte(if (value) 1 else 0)
        override fun encodeByte(value: Byte) = output.writeByte(value.toInt())
        override fun encodeShort(value: Short) = output.writeShort(value.toInt())
        override fun encodeInt(value: Int) = output.writeInt(value)
        override fun encodeLong(value: Long) = output.writeLong(value)
        override fun encodeFloat(value: Float) = output.writeFloat(value)
        override fun encodeDouble(value: Double) = output.writeDouble(value)
        override fun encodeChar(value: Char) = output.writeChar(value.code)
        override fun encodeString(value: String) = output.writeUTF(value)
        override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) = output.writeInt(index)
    
        override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
            encodeInt(collectionSize)
            return this
        }
    
        override fun encodeNull() = encodeBoolean(false)
        override fun encodeNotNullMark() = encodeBoolean(true)
    }
    
    @ExperimentalSerializationApi
    fun <T> encodeTo(output: DataOutput, serializer: SerializationStrategy<T>, value: T) {
        val encoder = DataOutputEncoder(output)
        encoder.encodeSerializableValue(serializer, value)
    }
    
    @ExperimentalSerializationApi
    inline fun <reified T> encodeTo(output: DataOutput, value: T) = encodeTo(output, serializer(), value)
    ```

2. Implement the decode functions for each primitive type, such as `decodeInt()` or `decodeString()`.
This lets the decoder decode values directly from the `DataInput` stream and reconstruct the original data structure:
    
    ```kotlin 
    @ExperimentalSerializationApi
    class DataInputDecoder(val input: DataInput, var elementsCount: Int = 0) : AbstractDecoder() {
        private var elementIndex = 0
        override val serializersModule: SerializersModule = EmptySerializersModule()
        
        // Decodes primitive values directly from binary form
        override fun decodeBoolean(): Boolean = input.readByte().toInt() != 0
        override fun decodeByte(): Byte = input.readByte()
        override fun decodeShort(): Short = input.readShort()
        override fun decodeInt(): Int = input.readInt()
        override fun decodeLong(): Long = input.readLong()
        override fun decodeFloat(): Float = input.readFloat()
        override fun decodeDouble(): Double = input.readDouble()
        override fun decodeChar(): Char = input.readChar()
        override fun decodeString(): String = input.readUTF()
        override fun decodeEnum(enumDescriptor: SerialDescriptor): Int = input.readInt()
    
        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            if (elementIndex == elementsCount) return CompositeDecoder.DECODE_DONE
            return elementIndex++
        }
    
        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
            DataInputDecoder(input, descriptor.elementsCount)
    
        override fun decodeSequentially(): Boolean = true
    
        override fun decodeCollectionSize(descriptor: SerialDescriptor): Int =
            decodeInt().also { elementsCount = it }
    
        override fun decodeNotNullMark(): Boolean = decodeBoolean()
    }
    
    
    @ExperimentalSerializationApi
    fun <T> decodeFrom(input: DataInput, deserializer: DeserializationStrategy<T>): T {
        val decoder = DataInputDecoder(input)
        return decoder.decodeSerializableValue(deserializer)
    }
    
    @ExperimentalSerializationApi
    inline fun <reified T> decodeFrom(input: DataInput): T = decodeFrom(input, serializer())
    
    fun ByteArray.toAsciiHexString() = joinToString("") {
        if (it in 32..127) it.toInt().toChar().toString() else
            "{${it.toUByte().toString(16).padStart(2, '0').uppercase()}}"
    }
    ```

3. Use these classes to serialize and deserialize Kotlin objects in a binary format:

    ```kotlin    
    @Serializable
    data class Project(val name: String, val language: String)
    
    @OptIn(ExperimentalSerializationApi::class)
    fun main() {
        val data = Project("kotlinx.serialization", "Kotlin")
        // Encodes the object to the custom binary format
        val output = ByteArrayOutputStream()
        encodeTo(DataOutputStream(output), data)
        
        val bytes = output.toByteArray()
        println(bytes.toAsciiHexString())
        // {00}{15}kotlinx.serialization{00}{06}Kotlin

        // Decodes the object from the custom binary format
        val input = ByteArrayInputStream(bytes)
        val obj = decodeFrom<Project>(DataInputStream(input))
        println(obj)
        // Project(name=kotlinx.serialization, language=Kotlin)
    }
    ```

In this example, the custom format encodes only the serialized values in binary form and decodes them back into a `Project` object.
This makes it easier to adapt the format for cases where you need a compact representation and precise control over the binary encoding.

#### Add support for format-specific types

A custom format can provide support for types that don't map directly to the standard primitive encoding functions.

To do add this support, override the `encodeSerializableValue()` function in the encoder and the `decodeSerializableValue()` in the decoder.
This lets you [define a custom serialization](create-custom-serializers.md) logic for format-specific types,
while maintaining efficient handling and flexibility for non-standard data representations.

To detect a type correctly, compare the `serializer.descriptor` property with the descriptor of the serializer for that type instead of checking the runtime type of the value.
This preserves the declared serialized form, even when a type uses a custom serializer or shares the same underlying representation as another type.

Let's look at an example of how to extend the [compact binary format example](#create-a-compact-binary-format) with specialized support for the type `ByteArray`:

1. Define a serializer for the format-specific type, so the encoder can detect it by descriptor: 

    ```kotlin
    private val byteArraySerializer = serializer<ByteArray>()
    ```   

    > You can also use the built-in [`ByteArraySerializer()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.builtins/-byte-array-serializer.html) function for similar result.
    >
    {style="tip"}

2. Override the `encodeSerializableValue()` function in the encoder to detect `ByteArray` values by descriptor and use a specialized encoding path.
   Then define helper functions to encode the byte array and represent sizes up to 254 bytes in a single byte:

   ```kotlin
   @ExperimentalSerializationApi
   class DataOutputEncoder(val output: DataOutput) : AbstractEncoder() {
       override val serializersModule: SerializersModule = EmptySerializersModule()
   
       override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
           if (serializer.descriptor == byteArraySerializer.descriptor)
               encodeByteArray(value as ByteArray)
           else
               super.encodeSerializableValue(serializer, value)
       }
   
       private fun encodeByteArray(bytes: ByteArray) {
           encodeCompactSize(bytes.size)
           output.write(bytes)
       }
   
       private fun encodeCompactSize(value: Int) {
           if (value < 0xff) {
               output.writeByte(value)
           } else {
               output.writeByte(0xff)
               output.writeInt(value)
           }
       }
   }
   ```

3. Override the [`decodeSerializableValue()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-decoder/decode-serializable-value.html) function in the decoder to detect `ByteArray` values by descriptor and deserialize them with the matching compact size format:

```kotlin
// Handles ByteArray decoding by checking if the descriptor matches ByteArray
    @Suppress("UNCHECKED_CAST")
    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>, previousValue: T?): T =
        if (deserializer.descriptor == byteArraySerializer.descriptor)
            decodeByteArray() as T
        else
            super.decodeSerializableValue(deserializer, previousValue)

    // Decodes ByteArray data
    private fun decodeByteArray(): ByteArray {
        val bytes = ByteArray(decodeCompactSize())
        input.readFully(bytes)
        return bytes
    }

    // Decodes size efficiently using a compact format
    private fun decodeCompactSize(): Int {
        val byte = input.readByte().toInt() and 0xff
        if (byte < 0xff) return byte
        return input.readInt()
    }
}
```

4. Serialize and deserialize objects with the embedded `ByteArray` data:

   ```kotlin
   @Serializable
   data class Project(val name: String, val attachment: ByteArray)
   
   @OptIn(ExperimentalSerializationApi::class)
   fun main() {
       val data = Project("kotlinx.serialization", byteArrayOf(0x0A, 0x0B, 0x0C, 0x0D))
       val output = ByteArrayOutputStream()
       encodeTo(DataOutputStream(output), data)
       val bytes = output.toByteArray()
       println(bytes.toAsciiHexString())
       // {00}{15}kotlinx.serialization{04}{0A}{0B}{0C}{0D}

       val input = ByteArrayInputStream(bytes)
       val obj = decodeFrom<Project>(DataInputStream(input))
       println(obj)
       // Project(name=kotlinx.serialization, attachment=[10, 11, 12, 13])
   }
   ```

Here's the complete example that serializes and deserializes a class with a `ByteArray` property using the specialized encoding and decoding path:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.modules.*
import kotlinx.serialization.encoding.*
import java.io.*

// Declares the serializer used to detect ByteArray by descriptor
private val byteArraySerializer = serializer<ByteArray>()

@ExperimentalSerializationApi
class DataOutputEncoder(val output: DataOutput) : AbstractEncoder() {
    override val serializersModule: SerializersModule = EmptySerializersModule()

    // Encodes the primitive types
    override fun encodeBoolean(value: Boolean) = output.writeByte(if (value) 1 else 0)
    override fun encodeByte(value: Byte) = output.writeByte(value.toInt())
    override fun encodeShort(value: Short) = output.writeShort(value.toInt())
    override fun encodeInt(value: Int) = output.writeInt(value)
    override fun encodeLong(value: Long) = output.writeLong(value)
    override fun encodeFloat(value: Float) = output.writeFloat(value)
    override fun encodeDouble(value: Double) = output.writeDouble(value)
    override fun encodeChar(value: Char) = output.writeChar(value.code)
    override fun encodeString(value: String) = output.writeUTF(value)
    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) = output.writeInt(index)

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        encodeInt(collectionSize)
        return this
    }

    override fun encodeNull() = encodeBoolean(false)
    override fun encodeNotNullMark() = encodeBoolean(true)

    // Detects ByteArray by descriptor and uses a specialized encoding path
    override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
        if (serializer.descriptor == byteArraySerializer.descriptor)
            encodeByteArray(value as ByteArray)
        else
            super.encodeSerializableValue(serializer, value)
    }

    // Encodes a ByteArray with a compact size representation
    private fun encodeByteArray(bytes: ByteArray) {
        encodeCompactSize(bytes.size)
        output.write(bytes)
    }

    // Encodes sizes up to 254 in a single byte
    private fun encodeCompactSize(value: Int) {
        if (value < 0xff) {
            output.writeByte(value)
        } else {
            output.writeByte(0xff)
            output.writeInt(value)
        }
    }
}

@ExperimentalSerializationApi
fun <T> encodeTo(output: DataOutput, serializer: SerializationStrategy<T>, value: T) {
    val encoder = DataOutputEncoder(output)
    encoder.encodeSerializableValue(serializer, value)
}

@ExperimentalSerializationApi
inline fun <reified T> encodeTo(output: DataOutput, value: T) = encodeTo(output, serializer(), value)

@ExperimentalSerializationApi
class DataInputDecoder(val input: DataInput, var elementsCount: Int = 0) : AbstractDecoder() {
    private var elementIndex = 0
    override val serializersModule: SerializersModule = EmptySerializersModule()
    // Decodes the primitive types
    override fun decodeBoolean(): Boolean = input.readByte().toInt() != 0
    override fun decodeByte(): Byte = input.readByte()
    override fun decodeShort(): Short = input.readShort()
    override fun decodeInt(): Int = input.readInt()
    override fun decodeLong(): Long = input.readLong()
    override fun decodeFloat(): Float = input.readFloat()
    override fun decodeDouble(): Double = input.readDouble()
    override fun decodeChar(): Char = input.readChar()
    override fun decodeString(): String = input.readUTF()
    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int = input.readInt()

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (elementIndex == elementsCount) return CompositeDecoder.DECODE_DONE
        return elementIndex++
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
        DataInputDecoder(input, descriptor.elementsCount)

    override fun decodeSequentially(): Boolean = true

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int =
        decodeInt().also { elementsCount = it }

    override fun decodeNotNullMark(): Boolean = decodeBoolean()

    // Detects ByteArray by descriptor and uses a specialized decoding path
    @Suppress("UNCHECKED_CAST")
    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>, previousValue: T?): T =
        if (deserializer.descriptor == byteArraySerializer.descriptor)
            decodeByteArray() as T
        else
            super.decodeSerializableValue(deserializer, previousValue)

    // Decodes the array size and then the byte array contents
    private fun decodeByteArray(): ByteArray {
        val bytes = ByteArray(decodeCompactSize())
        input.readFully(bytes)
        return bytes
    }

    // Decodes sizes up to 254 from a single byte
    private fun decodeCompactSize(): Int {
        val byte = input.readByte().toInt() and 0xff
        if (byte < 0xff) return byte
        return input.readInt()
    }
}

@ExperimentalSerializationApi
fun <T> decodeFrom(input: DataInput, deserializer: DeserializationStrategy<T>): T {
    val decoder = DataInputDecoder(input)
    return decoder.decodeSerializableValue(deserializer)
}

@ExperimentalSerializationApi
inline fun <reified T> decodeFrom(input: DataInput): T = decodeFrom(input, serializer())

fun ByteArray.toAsciiHexString() = joinToString("") {
    if (it in 32..127) it.toInt().toChar().toString() else
        "{${it.toUByte().toString(16).padStart(2, '0').uppercase()}}"
}

@Serializable
data class Project(val name: String, val attachment: ByteArray)

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val data = Project("kotlinx.serialization", byteArrayOf(0x0A, 0x0B, 0x0C, 0x0D))
    val output = ByteArrayOutputStream()
    encodeTo(DataOutputStream(output), data)
    val bytes = output.toByteArray()
    println(bytes.toAsciiHexString())
    // {00}{15}kotlinx.serialization{04}{0A}{0B}{0C}{0D}

    val input = ByteArrayInputStream(bytes)
    val obj = decodeFrom<Project>(DataInputStream(input))
    println(obj)
    // Project(name=kotlinx.serialization, attachment=[10, 11, 12, 13])
}
```
{initial-collapse-state="collapsed" collapsible="true"  collapsed-title="Complete code example for format-specific ByteArray support"}
