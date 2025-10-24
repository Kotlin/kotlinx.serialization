[//]: # (title: Serialize built-in types)

The Kotlin serialization library supports various built-in types, including basic types such as primitives and strings, as well as certain standard library classes.
The following sections describe these types in detail and provide examples of how to serialize them.

## Basic types

Kotlin serialization provides built-in serializers for types that are represented as a single value in serialized data.
This includes primitives, strings, and enums.

For example, here’s how you can serialize a `Long` type:

```kotlin
// Imports the necessary library declarations
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
class Data(val signature: Long)

fun main() {
    val data = Data(0x1CAFE2FEED0BABE0)
    println(Json.encodeToString(data))
    // {"signature":2067120338512882656}
}
//sampleEnd
```
{kotlin-runnable="true"}

### Numbers

You can serialize all Kotlin number types, including integers and floating-point numbers, using their natural JSON representations:

```kotlin
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.math.PI

//sampleStart
@Serializable
class Data(
    val answer: Int,
    val pi: Double
)                     

fun main() {
    val data = Data(42, PI)
    println(Json.encodeToString(data))
    // {"answer":42,"pi":3.141592653589793}
}
//sampleEnd
```
{kotlin-runnable="true"}

### Unsigned numbers

Kotlin serialization supports Kotlin's [unsigned integer types](unsigned-integer-types.md) like `UByte` and `UInt`.
In JSON, these values are serialized as regular JSON numbers and preserve their full unsigned range:

```kotlin
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
class Counter(val counted: UByte, val description: String)

fun main() {
    val counted = 239.toUByte()
    println(Json.encodeToString(Counter(counted, "tries")))
    // {"counted":239,"description":"tries"}
}
//sampleEnd
```
{kotlin-runnable="true"}

> Although JSON preserves the full range of unsigned numbers, other serialization formats may handle them differently.
> For example, ProtoBuf and CBOR serialize these types using their signed counterparts.
>
{style="note"} 

### `Long` numbers as strings

You can represent `Long` numbers as strings in JSON.
This is useful in JavaScript environments, where JavaScript's `Number` type can't precisely represent all Kotlin `Long` values, which may lead to precision loss.

Use [`LongAsStringSerializer`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.builtins/-long-as-string-serializer/) with the `@Serializable` annotation to encode `Long` values as strings in JSON:

```kotlin
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
class Data(
    @Serializable(LongAsStringSerializer::class)
    val signature: Long
)

fun main() {
    val data = Data(0x1CAFE2FEED0BABE0)
    println(Json.encodeToString(data))
    // {"signature":"2067120338512882656"}
}
//sampleEnd
```
{kotlin-runnable="true"}

> You can also specify serializers like `LongAsStringSerializer` for all properties in a file.
> For more information, see [Specify serializers for a file](third-party-classes.md#specify-serializers-for-a-file).
>
{style="tip"}

### Enum classes

All `enum` classes are serializable by default without the `@Serializable` annotation.
When serialized in JSON, an `enum` is encoded as a string:

```kotlin
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
// The @Serializable annotation isn't required for enum classes
enum class Status { SUPPORTED }

@Serializable
class Project(val name: String, val status: Status) 

fun main() {
    val data = Project("kotlinx.serialization", Status.SUPPORTED)
    println(Json.encodeToString(data))
    // {"name":"kotlinx.serialization","status":"SUPPORTED"}
}
//sampleEnd
```
{kotlin-runnable="true"}

> On Kotlin/JS and Kotlin/Native, you must use the `@Serializable` annotation for an `enum` class to use as a root object,
> such as in `encodeToString<Status>(Status.SUPPORTED)`.
>
{style="note"}

#### Customize serial names of enum entries

> For more information on customizing serial names, see [Customize serial names](serialization-customization-options.md#customize-serial-names).
>
{style="tip"}

To customize the serial names of enum entries, use the `@SerialName` annotation and mark the enum class with `@Serializable`:

```kotlin
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
// Requires the @Serializable annotation because of @SerialName
@Serializable
enum class Status { @SerialName("maintained") SUPPORTED }

@Serializable
class Project(val name: String, val status: Status) 

fun main() {
    val data = Project("kotlinx.serialization", Status.SUPPORTED)
    println(Json.encodeToString(data))
    // {"name":"kotlinx.serialization","status":"maintained"}
}
//sampleEnd
```
{kotlin-runnable="true"}

## Standard library types

Kotlin serialization supports several types from the standard library, but some classes,
such as ranges and the [`Regex`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.text/-regex/) class, aren't supported.

### Pair and triple

You can serialize the [`Pair`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-pair/) and [`Triple`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-triple/) classes from the Kotlin standard library:

```kotlin
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
class Project(val name: String)

fun main() {
    val pair = 1 to Project("kotlinx.serialization")
    println(Json.encodeToString(pair))
    // {"first":1,"second":{"name":"kotlinx.serialization"}}
}
//sampleEnd
```
{kotlin-runnable="true"}

### Collections

Kotlin serialization supports collection types, including both read-only and mutable variants of [`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/), [`Set`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/), and [`Map`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/).
It also supports their concrete implementations such as [`ArrayList`](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-array-list/) and [`LinkedHashSet`](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.collections/-linked-hash-set/), as well as generic and primitive array types.
The way these collections are represented depends on the serialization format.

In JSON, lists and sets are serialized as JSON arrays, and maps are represented as JSON objects.

Kotlin uses the declared type to deserialize JSON.
During deserialization, the type of the resulting object is determined by the static type specified in the source code.
This type can be either the type of the property or the type parameter of the decoding function.

#### Serialize lists

Kotlin serialization serializes [`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/) types as JSON arrays.
Here’s an example with a list of classes:

```kotlin
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
class Project(val name: String)

fun main() {
    val list = listOf(
        Project("kotlinx.serialization"),
        Project("kotlinx.coroutines")    
    )
    println(Json.encodeToString(list))
    // [{"name":"kotlinx.serialization"},{"name":"kotlinx.coroutines"}]
}
//sampleEnd
```
{kotlin-runnable="true"}

#### Serialize sets

[`Set`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/) types are serialized as JSON arrays, just like [`List` types](#serialize-lists):

```kotlin
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
class Project(val name: String)

fun main() {
    val set = setOf(
        Project("kotlinx.serialization"),
        Project("kotlinx.coroutines")    
    )
    println(Json.encodeToString(set))
    // [{"name":"kotlinx.serialization"},{"name":"kotlinx.coroutines"}]
}
//sampleEnd
```
{kotlin-runnable="true"}

> By default, you can deserialize sets with duplicate entries. The behavior for handling duplicates is implementation-defined.
> 
{style="tip"}

#### Serialize maps

Kotlin serialization supports [`Map`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/) types with primitive or enum keys:

```kotlin
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
class Project(val name: String)

fun main() {
    // Creates a map with Int keys
    val map = mapOf(
        1 to Project("kotlinx.serialization"),
        2 to Project("kotlinx.coroutines")    
    )
    println(Json.encodeToString(map))
    // {"1":{"name":"kotlinx.serialization"},"2":{"name":"kotlinx.coroutines"}}
}
//sampleEnd
```
{kotlin-runnable="true"}

Map serialization depends on the format.
In JSON, maps are represented as objects. Since JSON object keys are always strings, keys are encoded as strings even if they are numbers in Kotlin.
Other formats, such as CBOR, support maps with non-primitive keys and preserve them as such.

> JSON doesn't natively support complex or composite keys.
> To encode structured objects as map keys, see [Encode structured map keys](serialization-json-configuration.md#encode-structured-map-keys).
>
{style="note"}

#### Deserialization behavior of collections



```kotlin
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
data class Data(
    val a: List<Int>,
    val b: Set<Int>
)

fun main() {
    val data = Json.decodeFromString<Data>("""
        {
            "a": [42, 42],
            "b": [42, 42]
        }
    """)
    // Duplicates are removed from data.b because Set enforces unique elements
    println(data)
    // Data(a=[42, 42], b=[42])
}
//sampleEnd
```
{kotlin-runnable="true"}

> For more information about collections in Kotlin, see [Collections overview](collections-overview.md).
>
{style="tip"}

### Unit and singleton objects

Kotlin's [`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/) type and other singleton objects are serializable.
A [singleton](object-declarations.md) is a class with only one instance, where the state is defined by the object itself rather than by external properties.
In JSON, singleton objects are serialized as empty structures:

```kotlin
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
object SerializationVersion {
    val libraryVersion: String = "1.0.0"
}

fun main() {
    println(Json.encodeToString(SerializationVersion))
    // {}
    println(Json.encodeToString(Unit))
    // {}
}
//sampleEnd
```
{kotlin-runnable="true"}

> You can use serialized singleton objects in [closed polymorphic hierarchies](serialization-polymorphism.md#serialize-objects-in-sealed-hierarchies)
> to represent cases without additional fields.
>
{style="tip"}

### Duration and Instant

Kotlin's [`Duration`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.time/-duration/) type is serialized to a string using the ISO-8601-2 format:

```kotlin
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.time.*

//sampleStart
fun main() {
    val duration = 1000.toDuration(DurationUnit.SECONDS)
    println(Json.encodeToString(duration))
    // "PT16M40S"
}
//sampleEnd
```
{kotlin-runnable="true"}

Starting with Kotlin 2.2.0, you can serialize Kotlin's [`Instant`](https://kotlinlang.org/api/core/kotlin-stdlib/kotlin.time/-instant/) type
as a string representing a point in time using the ISO-8601-1 format:

```kotlin
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.time.*

//sampleStart
fun main() {
    val instant = Instant.fromEpochMilliseconds(1607505416124)
    println(Json.encodeToString(instant))
    // "2020-12-09T09:16:56.124Z"
}
//sampleEnd
```
{kotlin-runnable="true" kotlin-min-compiler-version="2.2"}

### Nothing

The [`Nothing`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-nothing.html) type is serializable by default.
It has no instances, so encoding or decoding it throws an exception.
Use `Nothing` when a type is syntactically required, but not involved in serialization, like in [polymorphic classes with generic base types](serialization-polymorphism.md#serialize-polymorphic-types-with-generic-base-types):

```kotlin
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
sealed class ParametrizedParent<out R> {
    @Serializable
    data class ChildWithoutParameter(val value: Int) : ParametrizedParent<Nothing>()
}

fun main() {
    println(Json.encodeToString(ParametrizedParent.ChildWithoutParameter(42)))
    // {"value":42}
}
//sampleEnd
```
{kotlin-runnable="true"}

## What's next

* Dive into the [Serialize classes](serialization-customization-options.md) section to learn how to serialize classes and how to modify the default behavior of the `@Serializable` annotation.
* To explore more complex JSON serialization scenarios, see [JSON serialization overview](configure-json-serialization.md).
* Learn more about polymorphism and serializing different types through a shared base in [Serialize polymorphic classes](serialization-polymorphism.md).
