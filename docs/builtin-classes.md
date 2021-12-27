<!--- TEST_NAME BuiltinClassesTest -->

# Builtin classes

This is the second chapter of the [Kotlin Serialization Guide](serialization-guide.md).
In addition to all the primitive types and strings, serialization for some classes from the Kotlin standard library, 
including the standard collections, is built into Kotlin Serialization. This chapter explains the details.

**Table of contents**

<!--- TOC -->

* [Primitives](#primitives)
  * [Numbers](#numbers)
  * [Long numbers](#long-numbers)
  * [Long numbers as strings](#long-numbers-as-strings)
  * [Enum classes](#enum-classes)
  * [Serial names of enum entries](#serial-names-of-enum-entries)
* [Composites](#composites)
  * [Pair and triple](#pair-and-triple)
  * [Lists](#lists)
  * [Sets and other collections](#sets-and-other-collections)
  * [Deserializing collections](#deserializing-collections)
  * [Maps](#maps)
  * [Unit and singleton objects](#unit-and-singleton-objects)

<!--- END -->

<!--- INCLUDE .*-builtin-.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
-->

## Primitives

Kotlin Serialization has the following ten primitives: 
`Boolean`, `Byte`, `Short`, `Int`, `Long`, `Float`, `Double`, `Char`, `String`, and enums.
The other types in Kotlin Serialization are _composite_&mdash;composed of those primitive values.

### Numbers

All types of integer and floating-point Kotlin numbers can be serialized. 

<!--- INCLUDE
import kotlin.math.*
-->

```kotlin
@Serializable
class Data(
    val answer: Int,
    val pi: Double
)                     

fun main() {
    val data = Data(42, PI)
    println(Json.encodeToString(data))
}
```                                   

> You can get the full code [here](../guide/example/example-builtin-01.kt).

Their natural representation in JSON is used.

```text
{"answer":42,"pi":3.141592653589793}
```

<!--- TEST -->

> Experimental unsigned numbers as well as other experimental inline classes are not supported by Kotlin Serialization yet. 


### Long numbers

Long integers are serializable, too.

```kotlin                
@Serializable
class Data(val signature: Long)

fun main() {
    val data = Data(0x1CAFE2FEED0BABE0)
    println(Json.encodeToString(data))
}
``` 

> You can get the full code [here](../guide/example/example-builtin-02.kt).

By default they are serialized to JSON as numbers.

```text
{"signature":2067120338512882656}
```

<!--- TEST -->

### Long numbers as strings

The JSON output from the previous example will get decoded normally by Kotlin Serialization running on Kotlin/JS.
However, if we try to parse this JSON by native JavaScript methods, we get this truncated result.

```
JSON.parse("{\"signature\":2067120338512882656}")
▶ {signature: 2067120338512882700} 
```

The full range of a Kotlin Long does not fit in the JavaScript number, so its precision gets lost in JavaScript.
A common workaround is to represent long numbers with full precision using the JSON string type.
This approach is optionally supported by Kotlin Serialization with [LongAsStringSerializer], which
can be specified for a given Long property using the [`@Serializable`][Serializable] annotation:

<!--- INCLUDE
import kotlinx.serialization.builtins.*
-->

```kotlin                
@Serializable
class Data(
    @Serializable(with=LongAsStringSerializer::class)
    val signature: Long
)

fun main() {
    val data = Data(0x1CAFE2FEED0BABE0)
    println(Json.encodeToString(data))
}
``` 

> You can get the full code [here](../guide/example/example-builtin-03.kt).

This JSON gets parsed natively by JavaScript without loss of precision.

```text
{"signature":"2067120338512882656"}
```                                         

> The section on [Specifying serializers for a file](serializers.md#specifying-serializers-for-a-file) explains how a 
> serializer like `LongAsStringSerializer` can be specified for all properties in a file.

<!--- TEST -->

### Enum classes

All enum classes are serializable out of the box without having to mark them `@Serializable`,
as the following example shows.

```kotlin          
// The @Serializable annotation is not needed for enum classes
enum class Status { SUPPORTED }
        
@Serializable
class Project(val name: String, val status: Status) 

fun main() {
    val data = Project("kotlinx.serialization", Status.SUPPORTED)
    println(Json.encodeToString(data))
}
```                                        

> You can get the full code [here](../guide/example/example-builtin-04.kt).

In JSON an enum gets encoded as a string.

```text
{"name":"kotlinx.serialization","status":"SUPPORTED"}
```   

<!--- TEST -->

### Serial names of enum entries

Serial names of enum entries can be customized with the [SerialName] annotation just like 
it was shown for properties in the [Serial field names](basic-serialization.md#serial-field-names) section.
However, in this case, the whole enum class must be marked with the [`@Serializable`][Serializable] annotation.

```kotlin
@Serializable // required because of @SerialName
enum class Status { @SerialName("maintained") SUPPORTED }
        
@Serializable
class Project(val name: String, val status: Status) 

fun main() {
    val data = Project("kotlinx.serialization", Status.SUPPORTED)
    println(Json.encodeToString(data))
}
```                                        

> You can get the full code [here](../guide/example/example-builtin-05.kt).

We see that the specified serial name is now used in the resulting JSON.

```text
{"name":"kotlinx.serialization","status":"maintained"}
```   

<!--- TEST -->

## Composites

A number of composite types from the standard library are supported by Kotlin Serialization.

### Pair and triple

The simple data classes [Pair] and [Triple] from the Kotlin standard library are serializable.

```kotlin
@Serializable
class Project(val name: String)

fun main() {
    val pair = 1 to Project("kotlinx.serialization")
    println(Json.encodeToString(pair))
}  
```                                

> You can get the full code [here](../guide/example/example-builtin-06.kt).

```text
{"first":1,"second":{"name":"kotlinx.serialization"}}
```

<!--- TEST -->
 
> Not all classes from the Kotlin standard library are serializable. In particular, ranges and the [Regex] class
> are not serializable at the moment. Support for their serialization may be added in the future.  

### Lists 

A [List] of serializable classes can be serialized.

```kotlin
@Serializable
class Project(val name: String)

fun main() {
    val list = listOf(
        Project("kotlinx.serialization"),
        Project("kotlinx.coroutines")    
    )
    println(Json.encodeToString(list))
}  
```

> You can get the full code [here](../guide/example/example-builtin-07.kt).

The result is represented as a list in JSON.

```text
[{"name":"kotlinx.serialization"},{"name":"kotlinx.coroutines"}]
```     

<!--- TEST -->

### Sets and other collections

Other collections, like [Set], are also serializable.

```kotlin
@Serializable
class Project(val name: String)

fun main() {
    val set = setOf(
        Project("kotlinx.serialization"),
        Project("kotlinx.coroutines")    
    )
    println(Json.encodeToString(set))
}  
```

> You can get the full code [here](../guide/example/example-builtin-08.kt).

[Set] is also represented as a list in JSON, like all other collections.

```text
[{"name":"kotlinx.serialization"},{"name":"kotlinx.coroutines"}]
```     

<!--- TEST -->

### Deserializing collections

During deserialization, the type of the resulting object is determined by the static type that was specified
in the source code&mdash;either as the type of the property or as the type parameter of the decoding function.
The following example shows how the same JSON list of integers is deserialized into two properties of
different Kotlin types.

```kotlin             
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
    println(data)
}
```    

> You can get the full code [here](../guide/example/example-builtin-09.kt).

Because the `data.b` property is a [Set], the duplicate values from it disappeared.

```text
Data(a=[42, 42], b=[42])
```

<!--- TEST -->

### Maps

A [Map] with primitive or enum keys and arbitrary serializable values can be serialized.

```kotlin
@Serializable
class Project(val name: String)

fun main() {
    val map = mapOf(
        1 to Project("kotlinx.serialization"),
        2 to Project("kotlinx.coroutines")    
    )
    println(Json.encodeToString(map))
}  
```                                

> You can get the full code [here](../guide/example/example-builtin-10.kt).

Kotlin maps in JSON are represented as objects. In JSON object keys are always strings, so keys are encoded as strings
even if they are numbers in Kotlin, as we can see below.

```text
{"1":{"name":"kotlinx.serialization"},"2":{"name":"kotlinx.coroutines"}}
```

<!--- TEST -->

> It is a JSON-specific limitation that keys cannot be composite. 
> It can be lifted as shown in the [Allowing structured map keys](json.md#allowing-structured-map-keys) section.


### Unit and singleton objects

The Kotlin builtin `Unit` type is also serializable. 
`Unit` is a Kotlin [singleton object](https://kotlinlang.org/docs/tutorials/kotlin-for-py/objects-and-companion-objects.html), 
and is handled equally with other Kotlin objects.

Conceptually, a singleton is a class with only one instance, meaning that state does not define the object, 
but the object defines its state. In JSON, objects are serialized as empty structures.

```kotlin
@Serializable
object SerializationVersion {
    val libraryVersion: String = "1.0.0"
}

fun main() {
    println(Json.encodeToString(SerializationVersion))
    println(Json.encodeToString(Unit))
}  
```                                

> You can get the full code [here](../guide/example/example-builtin-11.kt).

While it may seem useless at first glance, this comes in handy for sealed class serialization,
which is explained in the [Polymorphism. Objects](polymorphism.md#objects) section.

```text
{}
{}
```

<!--- TEST -->

> Serialization of objects is format specific. Other formats may represent objects differently, 
> e.g. using their fully-qualified names.
---

The next chapter covers [Serializers](serializers.md).

<!-- stdlib references -->
[Double.NaN]: https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-double/-na-n.html
[Pair]: https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-pair/ 
[Triple]: https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-triple/ 
[Regex]: https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.text/-regex/
[List]: https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/ 
[Set]: https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/ 
[Map]: https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/ 

<!--- MODULE /kotlinx-serialization-core -->
<!--- INDEX kotlinx-serialization-core/kotlinx.serialization -->

[Serializable]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-serializable/index.html
[SerialName]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-serial-name/index.html

<!--- MODULE /kotlinx-serialization-core -->
<!--- INDEX kotlinx-serialization-core/kotlinx.serialization.builtins -->

[LongAsStringSerializer]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.builtins/-long-as-string-serializer/index.html

<!--- END -->
 
