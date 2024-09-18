[//]: # (title: Serialize built-in types)

The `kotlinx.serialization` library provides support for various built-in types, including primitives, composite types,
and some standard library classes.
The following sections describe the various types that can be serialized and provide examples to demonstrate their usage.

## Primitive types

Kotlin serialization supports the following primitive types: `Boolean`, `Byte`, `Short`, `Int`, `Long`, `Float`, `Double`, `Char`, `String`, and `enums`.

For example, here’s how a `Long` is serialized:

### Numbers

You can serialize all types of integer and floating-point Kotlin numbers.

For example:

<!--- INCLUDE
import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*
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
    // {"answer":42,"pi":3.141592653589793}
}
```                                   

<!--- > You can get the full code [here](../../guide/example/example-builtin-01.kt). -->

Their natural representation in JSON is used.

<!---
```text
{"answer":42,"pi":3.141592653589793}
```
-->

<!--- TEST -->
