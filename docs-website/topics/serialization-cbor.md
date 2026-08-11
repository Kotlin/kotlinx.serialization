[//]: # (title: CBOR format)
<primary-label ref="experimental-general"/>

Concise Binary Object Representation ([CBOR](https://datatracker.ietf.org/doc/html/rfc8949)) is a compact binary format that extends the JSON data model.

## Add dependencies for CBOR

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

## Use CBOR for binary serialization

The [`Cbor`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-cbor/kotlinx.serialization.cbor/-cbor/) class provides two main functions:

* [`encodeToByteArray()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/encode-to-byte-array.html) serializes objects to a byte array.
* [`decodeFromByteArray()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/decode-from-byte-array.html) deserializes objects from a byte array.

Let's look at an example where a `Project` object is serialized into a byte array and then deserialized back to its original form:

```kotlin
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
    
    // Serializes the object to a CBOR byte array
    val bytes = Cbor.encodeToByteArray(data)

    // Converts the byte array to a human-readable hex string
    println(bytes.toAsciiHexString())
    // {BF}dnameukotlinx.serializationhlanguagefKotlin{FF}
    
    // Deserializes the byte array back to an object
    val obj = Cbor.decodeFromByteArray<Project>(bytes)
    println(obj)
    // Project(name=kotlinx.serialization, language=Kotlin)
}
```

This example prints the encoded bytes in a readable mixed form. It represents printable ASCII bytes as characters and non-printable bytes as hexadecimal values.

Here's the same output in full [CBOR hex notation](http://cbor.me/):


```none
Hex code                                             | CBOR type    | Description
-----------------------------------------------------|--------------|------------------------------------
BF                                                   | map(*)       | Start of a CBOR map
├── 64                                               | text(4)      | Length of the string
│   └── 6E616D65                                     | string       | The string "name"
├── 75                                               | text(21)     | Length of the string
│   └── 6B6F746C696E782E73657269616C697A6174696F6E   | string       | The string "kotlinx.serialization"
├── 68                                               | text(8)      | Length of the string
│   └── 6C616E6775616765                             | string       | The string "language"
├── 66                                               | text(6)      | Length of the string
│   └── 4B6F746C696E                                 | string       | The string "Kotlin"
└── FF                                               | primitive(*) | End of the CBOR map
```

## Ignore unknown keys in CBOR

CBOR is commonly used in communication with [IoT](https://en.wikipedia.org/wiki/Internet_of_things) devices where new properties may be added as part of API evolution.
By default, unknown keys encountered during deserialization result in an error.

Just like in [JSON](serialization-json-configuration.md#ignore-unknown-keys), you set the [`ignoreUnknownKeys`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-cbor/kotlinx.serialization.cbor/-cbor-builder/ignore-unknown-keys.html) property to `true` to ignore them during deserialization:

```kotlin
import kotlinx.serialization.*
import kotlinx.serialization.cbor.*

@Serializable
data class Project(val name: String)

@OptIn(ExperimentalSerializationApi::class)
fun main() {

    // Creates a Cbor instance that ignores unknown keys during deserialization
    val format = Cbor { ignoreUnknownKeys = true }

    // Decodes the CBOR input with an extra, unknown "language" key
    val data = format.decodeFromHexString<Project>(
        "bf646e616d65756b6f746c696e782e73657269616c697a6174696f6e686c616e6775616765664b6f746c696eff"
    )
    println(data)
    // Project(name=kotlinx.serialization)
}
```

In this CBOR input, the following bytes represent the unknown `"language"` key and its value:

* The unknown key `"language"`:
    * `68`: Length of the key
        * `6c616e6775616765`: The key
* The value `"Kotlin"`:
    * `66`: Length of the value
        * `4b6f746c696e`: The value

```none
Hex code                                             | CBOR type    | Description
-----------------------------------------------------|--------------|------------------------------------
BF                                                   | map(*)       | Start of a CBOR map
├── 64                                               | text(4)      | Length of the string
│   └── 6E616D65                                     | string       | The string "name"
├── 75                                               | text(21)     | Length of the string
│   └── 6B6F746C696E782E73657269616C697A6174696F6E   | string       | The string "kotlinx.serialization"
├── 68                                               | text(8)      | Length of the string
│   └── 6C616E6775616765                             | string       | The string "language"
├── 66                                               | text(6)      | Length of the string
│   └── 4B6F746C696E                                 | string       | The string "Kotlin"
└── FF                                               | primitive(*) | End of the CBOR map
```
{collapsible="true" collapsed-title="The full CBOR hex notation"}

## Customize CBOR data encoding

According to the [RFC 8949 Major Types](https://datatracker.ietf.org/doc/html/rfc8949#section-3.1) specification, CBOR supports the following data types:

* Major type 0: an unsigned integer
* Major type 1: a negative integer
* Major type 2: a byte string
* Major type 3: a text string
* Major type 4: an array of data items
* Major type 5: a map of pairs of data items
* Major type 6: optional semantic tagging of other major types
* Major type 7: floating-point numbers, simple data types with no content, and the "break" stop code

Unlike JSON, CBOR supports maps with non-trivial keys.
Some parsers, such as [`jackson-dataformat-cbor`](https://github.com/FasterXML/jackson-dataformats-binary), don't support this feature.

> For a JSON workaround, see [Allow structured map keys](serialization-json-configuration.md#allow-structured-map-keys).
>
{style="tip"}

### Encode `ByteArray` properties as byte strings

You can customize how CBOR encodes data to better match existing specifications and, in some cases, reduce binary size.

By default, Kotlin `ByteArray` values are encoded as major type 4, which represents an array of data items.
To encode `ByteArray` properties as major type 2, a byte string, use the [`@ByteString`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-cbor/kotlinx.serialization.cbor/-byte-string/) annotation:

```kotlin
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
    // Encodes the byte array as CBOR major type 4:
    // an array of individual data items
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

In this example, the bytes before each `ByteArray` value differ because the properties use different CBOR major types.

Here's the encoded byte array in full CBOR hex notation:

```none
Hex code               | CBOR type    | Description
-----------------------|--------------|-----------------------------------------
BF                     | map(*)       | Start of a CBOR map
├── 65                 | text(5)      | Length of the string
│   └── 7479706532     | string       | The property name "type2"
├── 44                 | bytes(4)     | Length of the byte string
│   └── 01020304       | byte string  | The byte string "\x01\x02\x03\x04"
├── 65                 | text(5)      | Length of the string
│   └── 7479706534     | string       | The property name "type4"
├── 9F                 | array(*)     | Start of a CBOR array
│   ├── 05             | unsigned(5)  | The unsigned integer 5
│   ├── 06             | unsigned(6)  | The unsigned integer 6
│   ├── 07             | unsigned(7)  | The unsigned integer 7
│   ├── 08             | unsigned(8)  | The unsigned integer 8
│   └── FF             | primitive(*) | End of the CBOR array
└── FF                 | primitive(*) | End of the CBOR map
```

To encode all `ByteArray` values as major type 2 without annotating each property with `@ByteString`, set the [`alwaysUseByteString`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-cbor/kotlinx.serialization.cbor/-cbor-builder/always-use-byte-string.html) property to `true`:

```kotlin
val format = Cbor {
    alwaysUseByteString = true
}
```

### Encode classes as CBOR arrays

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

## Definite and indefinite length encoding in CBOR

CBOR supports [two encodings](https://datatracker.ietf.org/doc/html/rfc8949#section-3.2.2) for maps and arrays: *definite length encoding* and *indefinite length encoding*.

By default, Kotlin serialization uses indefinite length encoding.
This means that the number of elements in a map or array isn't encoded explicitly, and a terminating byte is appended after the last element.

Definite length encoding omits the terminating byte and encodes the number of elements at the start of the map or array.

To switch between these two modes, use the [`useDefiniteLengthEncoding`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-cbor/kotlinx.serialization.cbor/-cbor-builder/use-definite-length-encoding.html) property.

## Tags and labels in CBOR

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

## Custom CBOR-specific serializers

CBOR encoders and decoders implement the [`CborEncoder`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-cbor/kotlinx.serialization.cbor/-cbor-encoder/) and [`CborDecoder`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-cbor/kotlinx.serialization.cbor/-cbor-decoder/) interfaces.

These interfaces extend the general `Encoder` and `Decoder` interfaces, providing access to CBOR-specific configurations through the [`cbor`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-cbor/kotlinx.serialization.cbor/-cbor-encoder/cbor.html) property.
Custom serializers can use this property to access the current [`Cbor`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-cbor/kotlinx.serialization.cbor/-cbor/) instance, produce embedded byte arrays, and read the current settings, such as `preferCborLabelsOverNames` and `useDefiniteLengthEncoding`.

For more information about creating custom serializers, see [Create custom serializers](serialization-custom-formats.md).
