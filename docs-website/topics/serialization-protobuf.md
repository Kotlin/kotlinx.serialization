[//]: # (title: ProtoBuf format)
<primary-label ref="experimental-general"/>

[Protocol Buffers](https://developers.google.com/protocol-buffers), or ProtoBuf, is a language-neutral binary format that normally
relies on a separate `.proto` file to define the protocol schema.
It's more compact than CBOR, because it assigns integer numbers to fields instead of names.

Kotlin serialization uses [proto2 semantics](https://protobuf.dev/programming-guides/proto2/), where all fields are explicitly required or optional.

## Add dependencies for ProtoBuf

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

## Use ProtoBuf for binary serialization

To serialize objects with ProtoBuf, use the
[`ProtoBuf`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-protobuf/kotlinx.serialization.protobuf/-proto-buf/) class with the [`.encodeToByteArray()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-protobuf/kotlinx.serialization.protobuf/-proto-buf/encode-to-byte-array.html) and the [`.decodeFromByteArray()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-protobuf/kotlinx.serialization.protobuf/-proto-buf/decode-from-byte-array.html) functions:

```kotlin
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

## Assign field numbers for ProtoBuf serialization

By default, ProtoBuf assigns field numbers automatically.

To keep your schema stable over time, use the [`@ProtoNumber`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-protobuf/kotlinx.serialization.protobuf/-proto-number/) annotation to assign field numbers explicitly, without needing a separate `.proto` file.
For example, `@ProtoNumber(1)` assigns field number 1 to a property, so the Kotlin serialization's ProtoBuf format uses that number during encoding and decoding instead of assigning one automatically.

This is useful if you plan to reorder properties, and it aligns with Protobuf's compatibility rules for evolving schemas.

Here's an example:

```kotlin
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

## Specify integer encoding in ProtoBuf

ProtoBuf encodes integer properties using varint encoding by default.

To use a different integer encoding for a property, apply the [`@ProtoType`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-protobuf/kotlinx.serialization.protobuf/-proto-type/) annotation with a [`ProtoIntegerType`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-protobuf/kotlinx.serialization.protobuf/-proto-integer-type/) value.
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

## Encode numeric collections as packed fields

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
    // {08}{01}{08}{02}{08}{03}

    println(ProtoBuf.decodeFromByteArray(DataRepeated.serializer(), packed))
    // DataRepeated(values=[1, 2, 3])

    println(ProtoBuf.decodeFromByteArray(DataPacked.serializer(), repeated))
    // DataPacked(values=[1, 2, 3])
}
```

## Represent `oneof` fields

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

1. Create a `sealed interface` or `abstract class` to represent the fields inside the `oneof` declaration:

    ```kotlin
    @Serializable
    sealed interface IPhoneType
    ```

2. Define a class for the entire message. Add a `name` property and annotate it with `@ProtoNumber(1)`.
Add a `phone` property of the polymorphic `IPhoneType` and annotate it with [`@ProtoOneOf`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-protobuf/kotlinx.serialization.protobuf/-proto-one-of/):

    ```kotlin
    @Serializable
    data class Data(
        @ProtoNumber(1)
        val name: String,
    
        @ProtoOneOf
        val phone: IPhoneType?,
    )
    ```

3. Create a subclass for each field in the `oneof` declaration. Each subclass can be a value class or a data class and must have a single property for that field:

    ```kotlin
    @Serializable
    @JvmInline
    value class HomePhone(
        val number: String
    ) : IPhoneType
    
    @Serializable
    data class WorkPhone(
        val number: String
    ) : IPhoneType
    ```
   
4. Annotate each subclass property with `@ProtoNumber` using the field number from the `oneof` declaration:

    ```kotlin
    @Serializable
    @JvmInline
    value class HomePhone(
        @ProtoNumber(2) val number: String
    ) : IPhoneType
    
    @Serializable
    data class WorkPhone(
        @ProtoNumber(3) val number: String
    ) : IPhoneType
    ```

Here's a more detailed example where `oneof` is used to store either a home phone or a work phone:

```kotlin
import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*

// Represents the oneof group
@Serializable
sealed interface IPhoneType

// Represents the home_phone field
@OptIn(ExperimentalSerializationApi::class)
@Serializable @JvmInline value class HomePhone(@ProtoNumber(2) val number: String): IPhoneType

// Represents the work_phone field as a data class
@OptIn(ExperimentalSerializationApi::class)
@Serializable data class WorkPhone(@ProtoNumber(3) val number: String): IPhoneType

// Defines the data class with a oneof property
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Data(
    // Assigns field number 1 to the name property
    @ProtoNumber(1) val name: String,

    // Maps the polymorphic property to the oneof declaration
    @ProtoOneOf val phone: IPhoneType?,
)

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

## Generate a ProtoBuf schema

Typically, working with ProtoBuf involves using a `.proto` file and a code generator to create code for serialization and deserialization.
However, with Kotlin serialization, you can use Kotlin classes annotated with `@Serializable` as the source for the schema, making `.proto` files optional.

This approach simplifies the process when all the code involved is written in Kotlin,
but interoperability with other languages often still requires a `.proto` schema.

To generate this schema, use the [`ProtoBufSchemaGenerator`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-protobuf/kotlinx.serialization.protobuf.schema/-proto-buf-schema-generator/).
It generates a Proto2-compatible schema from one or more `SerialDescriptor` instances.

This gives you a `.proto` schema that you can use with other ProtoBuf tools.

Here's an example that generates a `.proto` schema from a Kotlin data class:

```kotlin
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
