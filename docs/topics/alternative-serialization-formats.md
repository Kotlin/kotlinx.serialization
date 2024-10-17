[//]: # (title: Alternative and custom serialization formats (experimental))

<!--- TEST_NAME FormatsTest -->

While JSON is the only stable format in Kotlin Serialization,
experimental support is available for several alternative formats,
including binary formats like CBOR and ProtoBuf, as well as custom formats.
These experimental formats provide additional flexibility and efficiency for performance-critical applications,
though they may be subject to change in future releases.

## CBOR (experimental)

Concise Binary Object Representation ([CBOR](https://datatracker.ietf.org/doc/html/rfc7049)) is a compact binary format based on JSON.
It supports a subset of [JSON features](configure-json-serialization.md) and provides a compact representation of data.
While CBOR behaves similarly to JSON, it produces binary output instead of text.

To use CBOR in your project, add the CBOR serialization library dependency to your `build.gradle(.kts)` file:

<tabs>

<tab id="kotlin" title="Kotlin DSL">

```kotlin
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-cbor:%serializationVersion%")
}
```

<!--- CLEAR -->

</tab>

<tab id="groovy" title="Groovy DSL">

```groovy
dependencies {
    implementation 'org.jetbrains.kotlinx:kotlinx-serialization-cbor:%serializationVersion%'
}
```

</tab>

</tabs>

### Use CBOR for binary serialization

The [`Cbor`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-cbor/kotlinx.serialization.cbor/-cbor/) class provides two main functions:

* [`encodeToByteArray()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/encode-to-byte-array.html) serializes objects to a binary array.
* [`decodeFromByteArray()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/decode-from-byte-array.html) deserializes objects from a binary array.

Let's look at an example where a `Project` object is serialized into a binary array and then deserialized back to its original form:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.cbor.*

fun ByteArray.toAsciiHexString() = joinToString("") {
    // // Converts bytes to ASCII characters if printable, otherwise shows their hexadecimal value
    if (it in 32..127) it.toInt().toChar().toString() else
        "{${it.toUByte().toString(16).padStart(2, '0').uppercase()}}"
}

@Serializable
data class Project(val name: String, val language: String)

fun main() {
    val data = Project("kotlinx.serialization", "Kotlin")
    
    // Serializes the object into a CBOR binary array
    val bytes = Cbor.encodeToByteArray(data)

    // Converts the binary array to a human-readable hex string
    println(bytes.toAsciiHexString())
    // {BF}dnameukotlinx.serializationhlanguagefKotlin{FF}
    
    // Deserializes the binary array back into a Project object
    val obj = Cbor.decodeFromByteArray<Project>(bytes)
    println(obj)
    // Project(name=kotlinx.serialization, language=Kotlin)
}
```

<!--- > You can get the full code [here](../../guide/example/example-formats-01.kt). -->

<!---
```text 
{BF}dnameukotlinx.serializationhlanguagefKotlin{FF}
Project(name=kotlinx.serialization, language=Kotlin)
```
-->

<!--- TEST -->

In [CBOR hex notation](http://cbor.me/), the output of the above example corresponds to the following values:

| Hex Code | CBOR Type   | Description                          |
|----------|-------------|--------------------------------------|
| BF       | map(*)      | Start of a CBOR map                  |
| 64       | text(4)     | Length of the string (4 characters)  |
| 6E616D65 | string      | The string "name"                    |
| 75       | text(21)    | Length of the string (21 characters) |
| 6B6F746C696E782E73657269616C697A6174696F6E | string | The string "kotlinx.serialization"   |
| 68       | text(8)     | Length of the string (8 characters)  |
| 6C616E6775616765 | string | The string "language"                |
| 66       | text(6)     | Length of the string (6 characters)  |
| 4B6F746C696E | string   | The string "Kotlin"                  |
| FF       | primitive(*)| End of the CBOR map                  |


> Unlike JSON, CBOR supports maps with non-trivial keys. However, some parsers, like `jackson-dataformat-cbor`, don't support this feature.
> For JSON workaround, see the [Encode structured map keys](serialization-json-configuration.md#encode-structured-map-keys) section.
> 
{style="note"}

### Ignore unknown keys in CBOR

CBOR is commonly used in communication with [IoT](https://en.wikipedia.org/wiki/Internet_of_things) devices where new properties could be added as a part of a device's
API evolution. By default, unknown keys encountered during deserialization cause an error.
You can configure the deserializer to ignore unknown keys by setting the [`ignoreUnknownKeys`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-cbor/kotlinx.serialization.cbor/-cbor-builder/ignore-unknown-keys.html) property to `true`:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.cbor.*

// Sets ignoreUnknownKeys to true to allow unknown keys during deserialization
val format = Cbor { ignoreUnknownKeys = true }

@Serializable
data class Project(val name: String)

fun main() {
    val data = format.decodeFromHexString<Project>(
        // CBOR hex notation input with an extra, unknown "language" key
        "bf646e616d65756b6f746c696e782e73657269616c697a6174696f6e686c616e6775616765664b6f746c696eff"
    )
    // Prints the deserialized Project object, ignoring the language property
    println(data)
    // Project(name=kotlinx.serialization)
}
```

> In the [CBOR hex string](http://cbor.me/), the part representing the unknown "language" property is:
>
> * 68: Length of the key "language" (8 characters).
> * 6c616e6775616765: The key "language".
> * 66: Length of the value "Kotlin" (6 characters).
> * 4b6f746c696e: The value "Kotlin".
> 
{style="note"}

<!--- > You can get the full code [here](../../guide/example/example-formats-02.kt). -->

<!---
```text
Project(name=kotlinx.serialization)
```
-->

<!--- TEST -->

### Customize CBOR encoding

According to the [RFC 7049 Major Types](https://datatracker.ietf.org/doc/html/rfc7049#section-2.1) specification, CBOR supports the following data types:

* Major type 0: an unsigned integer
* Major type 1: a negative integer
* Major type 2: a byte string
* Major type 3: a text string
* Major type 4: an array of data items
* Major type 5: a map of pairs of data items
* Major type 6: optional semantic tagging of other major types
* Major type 7: floating-point numbers, simple data types with no content, and the "break" stop code

By default, Kotlin `ByteArray` instances are encoded as **major type 4**, which represents an array of data items.
You can encode `ByteArray` instances as major type 2, a byte string, by using the [`@ByteString`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-cbor/kotlinx.serialization.cbor/-byte-string/) annotation:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.cbor.*

fun ByteArray.toAsciiHexString() = joinToString("") {
    if (it in 32..127) it.toInt().toChar().toString() else
        "{${it.toUByte().toString(16).padStart(2, '0').uppercase()}}"
}

@Serializable
data class Data(
    // Encodes the byte array as CBOR major type 2 as a continuous byte string
    @ByteString
    val type2: ByteArray,
    // Encodes the byte array as CBOR major type 4 as an array of individual data items
    val type4: ByteArray
)        

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

<!--- > You can get the full code [here](../../guide/example/example-formats-03.kt). -->

<!---
```text
{BF}etype2D{01}{02}{03}{04}etype4{9F}{05}{06}{07}{08}{FF}{FF}
Data(type2=[1, 2, 3, 4], type4=[5, 6, 7, 8])
```
-->

<!--- TEST -->

In addition to encoding `ByteArray` fields, you can customize CBOR serialization for entire classes.
By default, classes are serialized as a CBOR Map, which corresponds to major type 5.
This means that each property of the class is stored as a key-value pair.

You can serialize a class as a CBOR Array, major type 4, by using the [`@CborArray`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-cbor/kotlinx.serialization.cbor/-cbor-array/) annotation.
This can be useful for encoding structures like those in [RFC 9052: Basic COSE Structure](https://www.rfc-editor.org/rfc/rfc9052#section-2):

```kotlin
@Serializable
@CborArray
data class DataClass(
val alg: Int,
val kid: String?
)
 
Cbor.encodeToByteArray(DataClass(alg = -7, kid = null))
```

By using annotations like `@ByteString` and `@CborArray`, you can fine-tune how data is encoded, optimizing both binary size and compatibility with existing specifications.

### Definite and indefinite length encoding in CBOR

CBOR supports two encodings for maps and arrays: *definite length encoding* and *indefinite length encoding*.
By default, Kotlin Serialization uses indefinite length encoding.
This means that the number of elements in a map or array is not explicitly encoded, and a terminating byte is appended after the last element.

Definite length encoding works differently by omitting the terminating byte and instead encoding the number of elements at the start of the map or array.
You can switch between these two modes
by using the [`useDefiniteLengthEncoding`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-cbor/kotlinx.serialization.cbor/-cbor-builder/use-definite-length-encoding.html) property.

### Tags and labels in CBOR

CBOR allows defining *tags* to encode additional information for properties and values. You can specify these tags using the
[`@KeyTags`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-cbor/kotlinx.serialization.cbor/-key-tags/) and [`@ValueTags`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-cbor/kotlinx.serialization.cbor/-value-tags/) annotations. 
The `encodeKeyTags`, `encodeValueTags`, `verifyKeyTags`, and `verifyValueTags` properties control
the encoding and verification of these tags.

> For more information on tagging in CBOR, see [RFC 8949 Tagging of Items](https://datatracker.ietf.org/doc/html/rfc8949#name-tagging-of-items).
> 
{style="tip"}

You can also tag entire classes using the [`@ObjectTags`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-cbor/kotlinx.serialization.cbor/-object-tags/) annotation, which applies tags to all instances of the class.
When serializing, `@ObjectTags` are always encoded directly before the data of the tagged object.
If a property is both value-tagged and part of an object-tagged type, the value tags precede the object tags.
The `encodeObjectTags` and `verifyObjectTags` properties manage the encoding and verification of these object tags.

CBOR supports keys of all types, similar to the [`@SerialName`](serialization-customization-options.md#customize-serial-names) annotation.
In the context of COSE (CBOR Object Signing and Encryption), keys are restricted to strings and numbers and are called *labels*.  String labels can be
You can assign string labels using the `@SerialName` annotation and number labels using the [`@CborLabel`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-cbor/kotlinx.serialization.cbor/-cbor-label/) annotation.
The `preferCborLabelsOverNames` property allows prioritizing number labels over serial names when both are present,
optimizing the representation for CBOR while keeping readable names for JSON.

Kotlin Serialization provides a predefined instance, [`Cbor.CoseCompliant`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-cbor/kotlinx.serialization.cbor/-cbor/-default/-cose-compliant.html), which follows COSE encoding requirements.
This instance uses definite length encoding, ensures that all tags are encoded and verified,
and prioritizes numeric labels over serial names for more compact representations.

### Custom CBOR-specific Serializers

CBOR encoders and decoders implement the [`CborEncoder`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-cbor/kotlinx.serialization.cbor/-cbor-encoder/) and [`CborDecoder`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-cbor/kotlinx.serialization.cbor/-cbor-decoder/) interfaces.
These interfaces are similar to the general `Encoder` and `Decoder` interfaces, providing access to CBOR-specific configurations through the `cbor` property.
Custom serializers can use this property to access the current `Cbor` instance, embed byte arrays, and adjust settings like `preferCborLabelsOverNames` and `useDefiniteLengthEncoding`.

For more details on creating custom serializers, see the [Create custom serializers](create-custom-serializers.md) section.

## ProtoBuf (experimental)

[Protocol Buffers](https://developers.google.com/protocol-buffers) is a language-neutral binary format that normally
relies on a separate `.proto` file to define the protocol schema. 
It is more compact than CBOR, as it assigns integer numbers to fields instead of names.

To use ProtoBuf in your project, add the ProtoBuf serialization library dependency to your `build.gradle(.kts)` file:

<tabs>

<tab id="kotlin-proto" title="Kotlin DSL">

```kotlin
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:%serializationVersion%")
}
```
<!--- CLEAR -->
</tab>

<tab id="groovy-proto" title="Groovy DSL">

```groovy
dependencies {
    implementation 'org.jetbrains.kotlinx:kotlinx-serialization-protobuf:%serializationVersion%'
}
```

</tab>
</tabs>

Kotlin Serialization uses proto2 semantics, where all fields are explicitly required or optional. 

For a basic example, use the
[`ProtoBuf`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-protobuf/kotlinx.serialization.protobuf/-proto-buf/) class with the [`encodeToByteArray()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-protobuf/kotlinx.serialization.protobuf/-proto-buf/encode-to-byte-array.html) and the [`decodeFromByteArray()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-protobuf/kotlinx.serialization.protobuf/-proto-buf/decode-from-byte-array.html) functions:

```kotlin
// Imports the necessary libraries
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
    
    // Converts the byte array into a readable hex string for demonstration
    println(bytes.toAsciiHexString())
    // {0A}{15}kotlinx.serialization{12}{06}Kotlin
    
    val obj = ProtoBuf.decodeFromByteArray<Project>(bytes)
    println(obj)
    // Project(name=kotlinx.serialization, language=Kotlin)
}
```

<!--- > You can get the full code [here](../guide/example/example-formats-04.kt). -->

<!---
```text 
{0A}{15}kotlinx.serialization{12}{06}Kotlin
Project(name=kotlinx.serialization, language=Kotlin)
```
-->

<!--- TEST -->

In [ProtoBuf hex notation](https://protogen.marcgravell.com/decode), the output is equivalent to the following:

```protobuf
Field #1: 0A String Length = 21, Hex = 15, UTF8 = "kotlinx.serialization"
Field #2: 12 String Length = 6, Hex = 06, UTF8 = "Kotlin"
```

### Assign field numbers

`ProtoBuf` serialization in Kotlin assigns field numbers automatically by default.
To maintain a consistent schema as your data changes,
you can use the [`@ProtoNumber`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-protobuf/kotlinx.serialization.protobuf/-proto-number/) annotation.
This allows you to assign field numbers directly in your data classes, without needing a separate `.proto` file.

Let's look at an example:

```kotlin
// Imports the necessary libraries
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

In the output, the `name` property uses field number 1 (`0A`), as specified. The `language` property uses field number 3 (`1A`), skipping the default sequence.

> For more information about Protobuf field numbers, see the [Official Protobuf Language Guide](https://protobuf.dev/programming-guides/proto2/#assigning).
> 
{style="tip"}

<!--- > You can get the full code [here](../guide/example/example-formats-05.kt). -->

<!---
```text 
{0A}{15}kotlinx.serialization{1A}{06}Kotlin
Project(name=kotlinx.serialization, language=Kotlin)
```
-->

<!--- TEST -->

### Specify integer encodings in ProtoBuf

ProtoBuf supports various integer encodings optimized for different ranges.
You can specify these using the [`@ProtoType`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-protobuf/kotlinx.serialization.protobuf/-proto-type/) annotation and the [`ProtoIntegerType`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-protobuf/kotlinx.serialization.protobuf/-proto-integer-type/) enum.

The following example shows all three supported options:

```kotlin
// Imports the necessary libraries
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

<!--- > You can get the full code [here](../guide/example/example-formats-06.kt). -->

<!---
```text 
{08}{01}{10}{03}{1D}{03}{00}{00}{00}
```
-->

<!--- TEST -->

### Encode empty lists in ProtoBuf

ProtoBuf serialization in Kotlin encodes lists and other collections as _repeated fields_.
A repeated field is a way to represent a list or array in ProtoBuf,
where each element of the list is encoded as an individual entry in the serialized data.
This allows ProtoBuf to efficiently handle collections by encoding each item as if it were a separate field with the same tag.

In ProtoBuf, when a list is empty, it does not produce any elements in the encoded stream for that field.
To ensure that empty lists can be correctly deserialized, you must explicitly specify `emptyList()` as the default
value for properties of collection or map types.
Without this default, an empty list is indistinguishable from a missing field during deserialization, which can cause issues.

Here's an example:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*

fun ByteArray.toAsciiHexString() = joinToString("") {
    if (it in 32..127) it.toInt().toChar().toString() else
        "{${it.toUByte().toString(16).padStart(2, '0').uppercase()}}"
}

@Serializable
data class Data(
    // Sets default values for the lists to ensure empty lists can be deserialized
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
The default value of `emptyList()` ensures that `b` is properly recognized as an empty list during deserialization,
rather than being mistaken for a missing field.

<!--- > You can get the full code [here](../guide/example/example-formats-07.kt). -->

<!---
```text 
{08}{01}{08}{02}{08}{03}
Data(a=[1, 2, 3], b=[])
```
-->

<!--- TEST -->

### Packed fields

In ProtoBuf, _packed fields_ are a way to serialize repeated fields, such as lists of numbers, more efficiently.
Instead of encoding each element with its own tag, a packed field groups all elements into a single entry,
which reduces the overall size of the serialized data.

You can use the [`@ProtoPacked`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-protobuf/kotlinx.serialization.protobuf/-proto-packed/) annotation
to serialize collection types, excluding maps, as packed fields.
According to the ProtoBuf standard, packed fields are supported only for primitive numeric types, and the annotation is ignored for other types.

As described in the [format specification](https://developers.google.com/protocol-buffers/docs/encoding#packed),
the ProtoBuf parser automatically handles lists in either packed or repeated format, regardless of the annotation.

### The oneof field

ProtoBuf serialization in Kotlin supports [oneof](https://protobuf.dev/programming-guides/proto2/#oneof) fields
using Kotlin's [polymorphism](serialization-polymorphism.md) functionality.
This feature enables a field to hold one of several possible types, but only one at a time.

Consider this ProtoBuf message definition:

```protobuf
message Data {
    required string name = 1;
    oneof phone {
        string home_phone = 2;
        string work_phone = 3;
    }
}
```

To represent this message in Kotlin:


1. Define a class for the entire message, and add a property for the `oneof` interface. Annotate this property with [`@ProtoOneOf`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-protobuf/kotlinx.serialization.protobuf/-proto-one-of/).
2. Create a `sealed interface` or `abstract class` to represent the `oneof` group.
3. Create subclasses for each `oneof` group element, with each subclass having a single property corresponding to its field.
4. Annotate these properties with `@ProtoNumber` according to their field numbers in the `oneof` definition.

Let's look at an example:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*

// Defines the data class with a oneof property
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Data(
    // Represents the name field with field number 1
    @ProtoNumber(1) val name: String,
    // Uses the IPhoneType interface for the oneof phone group
    @ProtoOneOf val phone: IPhoneType?,
)

// The sealed interface representing the 'oneof' group
@Serializable sealed interface IPhoneType

// Represents the home_phone field from the oneof group
@OptIn(ExperimentalSerializationApi::class)
@Serializable @JvmInline value class HomePhone(@ProtoNumber(2) val number: String): IPhoneType

// Represents the work_phone field from the oneof group
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

<!--- > You can get the full code [here](../guide/example/example-formats-08.kt). -->

<!---
```text
0a03546f6d1203313233
0a054a657272791a03373839
Data(name=Tom, phone=HomePhone(number=123))
Data(name=Jerry, phone=WorkPhone(number=789))
```
-->

<!--- TEST -->

The output shows how each oneof type is encoded:

* `0a03546f6d1203313233` represents "Tom" with a home phone.
* `0a054a657272791a03373839` represents "Jerry" with a work phone.

> Each `oneof` group must be tied to a single data class to prevent ID conflicts or runtime exceptions.
> 
{style="note"}

You can also define a class without the `@ProtoOneOf` annotation if you plan to use it only for deserialization.

For example:

```protobuf
@Serializable  
data class Data2(  
    @ProtoNumber(1) val name: String,  
    @ProtoNumber(2) val homeNumber: String? = null,  
    @ProtoNumber(3) val workNumber: String? = null,  
)  
```

This approach allows deserializing the same input as `Data`.
However, it doesn't enforce exclusivity between `homeNumber` and `workNumber`.
If both fields have values or are both `null`, the serialized output may not comply with the original schema,
which could cause compatibility issues when parsed by other ProtoBuf tools.

### Generate a ProtoBuf schema

Typically, working with ProtoBuf involves using a `.proto` file and a code generator to create code for serialization and deserialization.
However, with Kotlin Serialization, you can use Kotlin classes annotated with `@Serializable` as the source for the schema, making `.proto` files optional.

This approach simplifies the process when all the code involved is written in Kotlin,
but it can complicate interoperability with other languages.
To address this, you can use the [`ProtoBufSchemaGenerator`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-protobuf/kotlinx.serialization.protobuf.schema/-proto-buf-schema-generator/) to generate a `.proto` file from your Kotlin classes.
This allows you to maintain Kotlin classes as the primary source while using traditional ProtoBuf tools for other languages.

Here’s an example that generates a `.proto` schema from a Kotlin data class:

```kotlin
// Imports the necessary libraries
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

<!--- > You can get the full code [here](../guide/example/example-formats-09.kt). -->

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

<!--- TEST -->

> Default values aren't represented in `.proto` files, so the schema includes a warning when they are present.
> 
{style="note"}

## Properties (experimental)

Kotlin Serialization can serialize a class into a flat map with `String` keys using
the [`Properties`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-properties/kotlinx.serialization.properties/-properties/) format implementation.

To use the `Properties` format in your project, add the properties serialization library dependency to your `build.gradle(.kts)` file:

<tabs> 

<tab id="kotlin-properties" title="Kotlin DSL">

```kotlin
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-properties:%serializationVersion%")
}
```

</tab> 

<tab id="groovy-properties" title="Groovy DSL">

```groovy
dependencies {
    implementation 'org.jetbrains.kotlinx:kotlinx-serialization-properties:%serializationVersion%'
}
```

</tab> 

</tabs>

```kotlin
// Imports the necessary libraries
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
    // Encodes the data object into a map with dot-separated keys for nested properties
    val map = Properties.encodeToMap(data)
    // Iterates through the map and prints the key-value pairs
    map.forEach { (k, v) -> println("$k = $v") }
    // name = kotlinx.serialization
    // owner.name = kotlin
}
```

<!--- > You can get the full code [here](../guide/example/example-formats-10.kt). -->

<!---
```text 
name = kotlinx.serialization
owner.name = kotlin
```
-->

<!--- TEST -->

## Encode and decode Base64 formats (experimental)

[Base64](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.io.encoding/-base64/) encoding transforms binary data into a text-based format,
which can be easily transmitted or stored in formats like JSON or XML.
You can apply a custom Base64 serializer to any format that supports string-based encoding,
allowing for flexibility in how binary data is handled across various formats.

To encode and decode Base64 formats, create a serializer that controls how data is transformed into and from Base64:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.descriptors.*
import kotlin.io.encoding.*

@OptIn(ExperimentalEncodingApi::class)
// Custom serializer for converting ByteArray to Base64 format and back
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
    
    // Encodes the data class to a Base64 string
    val encoded = Json.encodeToString(value)
    println(encoded)
    // {"base64Input":"Zm9vIHN0cmluZw=="}

    // Decodes the Base64 string back to its original form
    val decoded = Json.decodeFromString<Value>(encoded)
    println(decoded.base64Input.decodeToString())
    // foo string
}
```
{kotlin-runnable="true"}

<!--- > You can get the full code [here](../../guide/example/example-formats-04.kt) -->

<!---
```text
{"base64Input":"Zm9vIHN0cmluZw=="}
foo string
```
-->

<!--- TEST -->

> For projects frequently using a Base64 serializer, you can [apply it globally](third-party-classes.md#specify-custom-serializers-globally-using-typealias) using the `typealias` keyword:
> 
> ```kotlin
> typealias Base64ByteArray = @Serializable(ByteArrayAsBase64Serializer::class) ByteArray
> ```
>
{style="tip"}

## Create custom formats (experimental)

To implement a custom format in Kotlin Serialization,
you need to provide custom implementations for the [`Encoder`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-encoder/) and [`Decoder`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-decoder/) interfaces.
These interfaces are extensive, but the [`AbstractEncoder`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-abstract-encoder/) and [`AbstractDecoder`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-abstract-decoder/) classes can simplify the process.

The `AbstractEncoder` class provides default implementations for most of the encode functions, such as [`encodeString()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-abstract-encoder/encode-string.html),
which delegate to the `encodeValue(value: Any)` function.
This means that by overriding the `encodeValue()` function, you can create a basic, functional custom format with minimal effort.

### Create a basic encoder

To implement custom serialization behavior in Kotlin, you can create a custom `Encoder` by extending the `AbstractEncoder` class.
This allows you to control how each value is serialized during the encoding process.

To create a basic encoder:

1. Create a class that extends `AbstractEncoder` to define custom serialization logic.
2. Override the `encodeValue()` function to specify how each value should be handled.
3. Write a function that uses your custom encoder to serialize an object.
4. Add an inline version of this function to make it easier to use with different types.

> Since encoders are usually used by other parts of an application,
> it is recommended to propagate the `@ExperimentalSerializationApi` annotation rather than opting in only within specific functions.
> 
{style="note"}

Here's an example that encodes data into a list:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*

// Creates a custom encoder that stores serialized values in a list
@ExperimentalSerializationApi
class ListEncoder : AbstractEncoder() {
    val list = mutableListOf<Any>()

    override val serializersModule: SerializersModule = EmptySerializersModule()

    // Adds the value to the list
    override fun encodeValue(value: Any) {
        list.add(value)
    }
}

// Serializes an object into a list of its values using the custom encoder
@ExperimentalSerializationApi
fun <T> encodeToList(serializer: SerializationStrategy<T>, value: T): List<Any> {
    val encoder = ListEncoder()
    encoder.encodeSerializableValue(serializer, value)
    return encoder.list
}

// Provides a type-safe, inline version of encodeToList for convenience
// Uses a reified type parameter to automatically retrieve the correct serializer
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

This example demonstrates how to create a custom `ListEncoder` to transform a Kotlin object into a list of its individual values,
maintaining the order they are defined in the class.

<!--- > You can get the full code [here](../guide/example/example-formats-12.kt). -->

<!---
```text
[kotlinx.serialization, kotlin, 9000]
```
-->

<!--- TEST -->

### Create a basic decoder

To decode custom serialized data in Kotlin, you can create a custom `Decoder` by extending the `AbstractDecoder` class.
This allows you to control how each value is deserialized during the decoding process.

To create a basic decoder:

1. Create a class that extends `AbstractDecoder` to define custom deserialization logic.
2. Override the [`decodeValue()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-abstract-decoder/decode-value.html) function to provide how values should be read during deserialization.
3. Override the [`decodeElementIndex()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-composite-decoder/decode-element-index.html) function to manage the order of deserialized values, returning the next index with each call.
4. Override the [`beginStructure()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-abstract-decoder/begin-structure.html) function to manage nested structures, ensuring each maintains its own state.
5. Write a function that uses your custom decoder to deserialize an object.
6. Optionally, add an inline version of this function to make it easier to use with different types.

Let's look at an example where the list is decoded back into an object:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*

// Creates a custom encoder that stores serialized values in a list
@ExperimentalSerializationApi
class ListEncoder : AbstractEncoder() {
    val list = mutableListOf<Any>()

    override val serializersModule: SerializersModule = EmptySerializersModule()

    override fun encodeValue(value: Any) {
       list.add(value)
    }
}

// Serializes an object into a list of its values using the custom encoder
@ExperimentalSerializationApi
fun <T> encodeToList(serializer: SerializationStrategy<T>, value: T): List<Any> {
    val encoder = ListEncoder()
    encoder.encodeSerializableValue(serializer, value)
    return encoder.list
}

// Provides a type-safe, inline version of encodeToList for convenience
@ExperimentalSerializationApi
inline fun <reified T> encodeToList(value: T) = encodeToList(serializer(), value)

// Creates a custom decoder that deserializes values from a list
@ExperimentalSerializationApi
class ListDecoder(val list: ArrayDeque<Any>) : AbstractDecoder() {
    private var elementIndex = 0

    override val serializersModule: SerializersModule = EmptySerializersModule()

    // Retrieves the next value from the list
    override fun decodeValue(): Any = list.removeFirst()
    
    // Manages the decoding order of values, returning the next index
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (elementIndex == descriptor.elementsCount) return CompositeDecoder.DECODE_DONE
        return elementIndex++
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
        ListDecoder(list)
}

// Deserializes an object from a list using the custom decoder
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

In the example above, a custom `ListDecoder` is created by extending `AbstractDecoder`.
The decoder reads values from a list, using the `decodeElementIndex()` function to track the current index and ensure values are deserialized in order.
The `beginStructure()` function manages nested objects during deserialization.

<!--- > You can get the full code [here](../guide/example/example-formats-13.kt). -->

<!---
```text
[kotlinx.serialization, kotlin, 9000]
Project(name=kotlinx.serialization, owner=User(name=kotlin), votes=9000)
```
-->

<!--- TEST -->

### Custom encoder and decoder optimization

The following sections focus on improving the `ListEncoder` and `ListDecoder` implementations introduced in the [Create a basic encoder](#create-a-basic-encoder) and [Create a basic decoder](#create-a-basic-decoder) sections.
By building on the foundations of the `ListEncoder` and `ListDecoder` examples, these optimizations
provide a more robust and versatile custom serialization setup, suitable for a wider range of data structures and scenarios.

#### Optimize with sequential decoding

To optimize a custom decoder for formats that store elements in order, use the [`decodeSequentially()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-composite-decoder/decode-sequentially.html) function.
When this function returns `true`, it signals that the decoder can handle elements in sequence without needing manual index tracking.
This can improve performance when deserializing data stored in sequential formats:

```kotlin
// Imports the necessary libraries
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


// Creates a custom decoder that supports sequential decoding
@ExperimentalSerializationApi
class ListDecoder(val list: ArrayDeque<Any>) : AbstractDecoder() {
    private var elementIndex = 0

    override val serializersModule: SerializersModule = EmptySerializersModule()

    override fun decodeValue(): Any = list.removeFirst()
    
    // Manages the decoding order of values, returning the next index
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (elementIndex == descriptor.elementsCount) return CompositeDecoder.DECODE_DONE
        return elementIndex++
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
        ListDecoder(list) 

    // Indicates support for sequential decoding for improved performance
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

<!--- > You can get a full runnable code example [here](../guide/example/example-formats-14.kt). -->

<!--- TEST 
[kotlinx.serialization, kotlin, 9000]
Project(name=kotlinx.serialization, owner=User(name=kotlin), votes=9000)
-->

#### Add collection support

To add collection support to a custom format, you need to manage how collection sizes are encoded and decoded.
While a basic format might handle individual elements,
it must also track the size of collections to ensure accurate deserialization of collections and maps.

To add support for collections in a custom format:

1. Implement the [`beginCollection()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-encoder/begin-collection.html) function in the encoder to handle the collection size.
2. Return the encoder instance from the `beginCollection()` function if no additional state is needed.
3. Implement the [`decodeCollectionSize()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-composite-decoder/decode-collection-size.html) function in the decoder to retrieve and store the collection size during deserialization.
4. Ensure that the `decodeSequentially()` function returns `true` if the format stores collection size in advance.

Here's how you can add collection support to the `ListEncoder` example:

```kotlin
// Imports the necessary libraries
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

This example demonstrates adding collection support to the ListEncoder and ListDecoder:

* `beginCollection()` in `ListEncoder` encodes the collection size, ensuring that the size is recorded alongside its elements.
* `decodeCollectionSize()` in `ListDecoder` reads this encoded size during deserialization, allowing the decoder to reconstruct collections and maps accurately.

This setup enables precise encoding and decoding of collections in a custom format.

<!--- > You can get the full code [here](../guide/example/example-formats-15.kt). -->

<!---
```text
[kotlinx.serialization, 2, kotlin, jetbrains, 9000]
Project(name=kotlinx.serialization, owners=[User(name=kotlin), User(name=jetbrains)], votes=9000)
```
-->

<!--- TEST -->

#### Add null support

To handle `null` values in a custom format, you need to introduce a way to indicate when a value is `null` or not.
This typically involves adding a "null indicator" that distinguishes between `null` values and actual data.

To add support for null values in a custom format:

1. Override [`encodeNull()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-abstract-encoder/encode-null.html) in the encoder to specify how `null` should be represented during serialization.
2. Override [`encodeNotNullMark()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-encoder/encode-not-null-mark.html) in the encoder to indicate non-null values.
3. Override [`decodeNotNullMark()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-decoder/decode-not-null-mark.html) in the decoder to differentiate between null and non-null values during deserialization.

Here's an example that adds `null` support to the `ListEncoder` and `ListDecoder` implementations:

```kotlin
// Imports the necessary libraries
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

// Creates a custom decoder that handles null values
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

    // Checks if the next value is "NULL" to determine if it's actually null
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

<!--- > You can get the full code [here](../guide/example/example-formats-16.kt). -->

<!---
```text
[kotlinx.serialization, !!, kotlin, NULL]
Project(name=kotlinx.serialization, owner=User(name=kotlin), votes=null)
```
-->

<!--- TEST -->

### Create a compact binary format

Binary formats are often used for their compact representation of data, making them ideal for scenarios where minimizing storage size or transmission bandwidth is important.
Custom binary formats allow you to control how data is serialized and deserialized at a low level,
providing flexibility to optimize performance and compatibility with other systems.

In the following example, a custom encoder and decoder are implemented using
[`java.io.DataOutput`](https://docs.oracle.com/javase/8/docs/api/java/io/DataOutput.html) and [`java.io.DataInput`](https://docs.oracle.com/javase/8/docs/api/java/io/DataInput.html) interfaces.
This approach enables detailed control over the serialization of each [primitive type](serialization.md#supported-formats).

First, override the encode functions for each primitive type, such as `encodeInt()` for integers or `encodeString()` for strings.
This allows you to write the binary data directly to the `DataOutput` stream:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*
import java.io.*

@ExperimentalSerializationApi
class DataOutputEncoder(val output: DataOutput) : AbstractEncoder() {
    override val serializersModule: SerializersModule = EmptySerializersModule()
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

Next, implement the decode functions for each primitive type, such as `decodeInt()` or `decodeString()`.
This allows reading binary data from the `DataInput` stream and reconstructing the original data structure:

```kotlin 
@ExperimentalSerializationApi
class DataInputDecoder(val input: DataInput, var elementsCount: Int = 0) : AbstractDecoder() {
    private var elementIndex = 0
    override val serializersModule: SerializersModule = EmptySerializersModule()
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

Finally, use these custom encoder and decoder classes to serialize and deserialize Kotlin objects to a binary format.
You can now serialize and deserialize arbitrary data. Here is an example using the `Project` data class:

```kotlin    
@Serializable
data class Project(val name: String, val language: String)

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val data = Project("kotlinx.serialization", "Kotlin")
    val output = ByteArrayOutputStream()
    encodeTo(DataOutputStream(output), data)
    val bytes = output.toByteArray()
    println(bytes.toAsciiHexString())
    // {00}{15}kotlinx.serialization{00}{06}Kotlin
    val input = ByteArrayInputStream(bytes)
    val obj = decodeFrom<Project>(DataInputStream(input))
    println(obj)
    // Project(name=kotlinx.serialization, language=Kotlin)
}
```

The result is a compact binary representation of the `Project` class that only includes the essential data.
This makes it easy to customize for specific needs where small size and efficiency are important.

<!--- > You can get the full code [here](../guide/example/example-formats-17.kt). -->

<!---
```text
{00}{15}kotlinx.serialization{00}{06}Kotlin
Project(name=kotlinx.serialization, language=Kotlin)
```
-->

<!--- TEST -->

### Add support for format-specific types

Custom binary formats may need to handle data types that are not part of the standard primitives in Kotlin Serialization.
You can achieve this by overriding the [`encodeSerializableValue(serializer, value)`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-encoder/encode-serializable-value.html) function in the encoder.
This approach allows you to [define custom serialization](create-custom-serializers.md) logic for format-specific types,
while maintaining efficient handling and flexibility for non-standard data representations.

In the following example, a specialized approach is used for `ByteArray` types,
utilizing Java's [`DataOutput`](https://docs.oracle.com/javase/8/docs/api/java/io/DataOutput.html) methods for efficient serialization.
By referencing the `serializer.descriptor` property, the encoder can determine when a `ByteArray` is
serialized and apply the appropriate logic for more efficient processing:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.modules.*
import kotlinx.serialization.encoding.*
import java.io.*

private val byteArraySerializer = serializer<ByteArray>()
```   

> Alternatively, you can use the built-in [`ByteArraySerializer()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.builtins/-byte-array-serializer.html) function for similar functionality.
> 
{style="note"}

The following code builds upon the `ListEncoder` example from the [Create a compact binary format](#create-a-compact-binary-format) section.
You can extend the encoder to support collections and implement a specialized path for handling `ByteArray` data.

To handle collection sizes more efficiently, you can create a function that represents sizes up to 254 bytes with just one byte.

To efficiently handle `ByteArray` data in the decoder, override the `decodeSerializableValue()` function:

```kotlin
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

    // Checks if the value is a ByteArray and uses a specific encoding path for it
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

    // Uses one byte for sizes up to 254, otherwise writes the size as an integer
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
```

Similarly, override the `Decoder` implementation's [`decodeSerializableValue()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-decoder/decode-serializable-value.html) function to handle the efficient reading of `ByteArray` data:

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

Now, you can serialize and deserialize objects with embedded `ByteArray` data:

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

<!--- > You can get the full code [here](../guide/example/example-formats-18.kt). -->

<!---
```text
{00}{15}kotlinx.serialization{04}{0A}{0B}{0C}{0D}
Project(name=kotlinx.serialization, attachment=[10, 11, 12, 13])
```
-->

<!--- TEST -->
