[//]: # (title: Serialize built-in types)

<!--- TEST_NAME BuiltinClassesTest -->

The `kotlinx.serialization` library supports various built-in types, including primitives, composite types, and some standard library classes.
The following sections describe these types in more detail and provide examples of how to serialize them.

## Primitive types

Kotlin serialization supports the following primitive types: `Boolean`, `Byte`, `Short`, `Int`, `Long`, `Float`, `Double`, `Char`, `String`, and `enums`.

For example, here’s how you can serialize a `Long` type:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*
import kotlin.math.*

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

<!--- > You can get the full code [here](../../guide/example/example-builtin-01.kt). -->

<!---
```text
{"signature":2067120338512882656}
```
-->

<!--- TEST -->

### Numbers

You can serialize all Kotlin number types, including integers and floating-point numbers, using their natural JSON representations.

For example:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*
import kotlin.math.*

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

<!--- > You can get the full code [here](../../guide/example/example-builtin-02.kt). -->

<!---
```text
{"answer":42,"pi":3.141592653589793}
```
-->

<!--- TEST -->

### Long numbers as strings

When you serialize Kotlin `Long` values to JSON, JavaScript's native number type cannot represent the full range of a Kotlin `Long` type,
leading to precision loss.

Kotlin/JS handles these large `Long` numbers correctly, but JavaScript's native methods don't.
A common workaround is to represent long numbers with full precision using the JSON string type.
Kotlin Serialization supports this approach with [`LongAsStringSerializer`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.builtins/-long-as-string-serializer/),
which you can apply to a `Long` property using the `@Serializable` annotation:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*
import kotlin.math.*

//sampleStart
@Serializable
class Data(
    @Serializable(with=LongAsStringSerializer::class)
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

<!--- > You can get the full code [here](../../guide/example/example-builtin-03.kt). -->

<!---
```text
{"signature":"2067120338512882656"}
```
-->

> You can also specify a serializer like `LongAsStringSerializer` for all properties in a file.
> For more information, see the [Specify serializers for a file](third-party-classes.md#specify-serializers-for-a-file) section for more details.
> 
{type="tip"}

<!--- TEST -->

### Enum classes

All enum classes are serializable by default requiring the `@Serializable` annotation.
When serialized in JSON, an `enum` is encoded as a string:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*

//sampleStart
// The @Serializable annotation is not needed for enum classes
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

<!--- > You can get the full code [here](../../guide/example/example-builtin-04.kt). -->

<!---
```text
{"name":"kotlinx.serialization","status":"SUPPORTED"}
```
-->

> On Kotlin/JS and Kotlin/Native, you must use the `@Serializable` annotation for an enum class if you want to use as a root object,
> such as in `encodeToString<Status>(Status.SUPPORTED)`.
> 
{type="note"}

<!--- TEST -->

#### Customize serial names of enum entries

> For more information on customizing serial names, see the [Customize serial names](serialization-customization-options.md#customize-serial-names) section.
> 
{type="tip"}

To customize the serial names of enum entries, apply the `@SerialName` annotation to the entries and annotate the entire enum class with `@Serializable`:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
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

<!--- > You can get the full code [here](../../guide/example/example-builtin-05.kt). -->

<!---
```text
{"name":"kotlinx.serialization","status":"maintained"}
```
-->

<!--- TEST -->

## Composite types

Kotlin Serialization supports several composite types from the standard library, but not all classes are serializable.
For example, ranges and the [`Regex`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.text/-regex/) class are currently not supported.

### Pair and triple

You can serialize the [`Pair`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-pair/) and [`Triple`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-triple/) classes from the Kotlin standard library:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
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

<!--- > You can get the full code [here](../../guide/example/example-builtin-06.kt). -->

<!---
```text
{"first":1,"second":{"name":"kotlinx.serialization"}}
```
-->

<!--- TEST -->

### Collections

Kotlin Serialization supports various collection types, including [`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/), [`Set`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/), and [`Map`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/).
Lists and sets are serialized as JSON arrays, while maps are represented as JSON objects.

#### Serialize lists

You can serialize a [`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/) of serializable classes, which is represented as an array in JSON:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
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

<!--- > You can get the full code [here](../../guide/example/example-builtin-07.kt). -->

<!---
```text
[{"name":"kotlinx.serialization"},{"name":"kotlinx.coroutines"}]
```
-->

<!--- TEST -->

#### Serialize sets

[`Set`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/) is also represented as a JSON array:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
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

<!--- > You can get the full code [here](../../guide/example/example-builtin-08.kt). -->

<!---
```text
[{"name":"kotlinx.serialization"},{"name":"kotlinx.coroutines"}]
```
-->

<!--- TEST -->

#### Serialize maps

You can serialize a  [`Map`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/) with primitive or enum keys and any serializable values:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
class Project(val name: String)

fun main() {
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

<!--- > You can get the full code [here](../../guide/example/example-builtin-09.kt). -->

In JSON, Kotlin maps are represented as objects. Since JSON object keys are always strings, keys are encoded as strings, even if they are numbers in Kotlin.

> JSON doesn't natively support complex or composite keys.
> To work around this and use structured objects as map keys, see the [Encode structured map keys](serialization-json-configuration.md#encode-structured-map-keys) section.
> 
{type="note"}

<!---
```text
{"1":{"name":"kotlinx.serialization"},"2":{"name":"kotlinx.coroutines"}}
```
-->

<!--- TEST -->

#### Deserialization behavior of collections

When deserializing collections, the type specified in the code determines how the JSON data is interpreted.
For example, `List` allows duplicates, while `Set` automatically removes them:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
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
    // No duplicate values in data.b property, because it is a Set
    println(data)
    // Data(a=[42, 42], b=[42])
}
//sampleEnd
```
{kotlin-runnable="true"}

<!--- > You can get the full code [here](../../guide/example/example-builtin-10.kt). -->

<!---
```text
Data(a=[42, 42], b=[42])
```
-->

<!--- TEST -->

### Unit and singleton objects

The Kotlin [`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/) type, along with other singleton objects, is serializable.
A [singleton]((object-declarations.md)) is a class with only one instance, meaning its state is defined by the object itself, not by external properties.
In JSON, singleton objects are serialized as empty structures:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
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

<!--- > You can get the full code [here](../../guide/example/example-builtin-11.kt). -->

While serializing singleton objects might seem unnecessary,
it is useful for closed polymorphic class serialization, as described in the [Serialize objects in sealed hierarchies](serialization-polymorphism.md#serialize-objects-in-sealed-hierarchies) section.

<!---
```text
{}
{}
```
-->

<!--- TEST -->

### Duration

Kotlin's [`Duration`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.time/-duration/) class is serialized as a string in the ISO-8601-2 format.
Since Kotlin `1.7.20`, you can serialize `Duration` the following way:

```kotlin
// Imports the necessary libraries
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

<!--- > You can get the full code [here](../../guide/example/example-builtin-12.kt). -->

<!---
```text
"PT16M40S"
```
-->

<!--- TEST -->


## Nothing

The  [`Nothing`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-nothing.html) class is serializable by default,
but since it has no instances, encoding or decoding its values causes an exception.
It is used when a type is required syntactically but isn't involved in serialization, such as in parameterized polymorphic base classes:

```kotlin
// Imports the necessary libraries
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

<!--- > You can get the full code [here](../../guide/example/example-builtin-13.kt). -->

<!---
```text
{"value":42}
```
-->

<!--- TEST -->
