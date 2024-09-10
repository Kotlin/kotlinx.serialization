<!--- TEST_NAME JsonTestElements -->
[//]: # (title: Managing JSON elements)

Kotlin serialization provides APIs that allow more than just direct conversions between strings and JSON objects.
For example, you might want to modify data before it can be parsed or work with unstructured data that doesn't fit neatly into the type-safe world of Kotlin serialization.

This section focuses on [`JsonElement`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-element/), a core concept used to represent and manipulate unstructured JSON data.

Before diving into specific features, ensure that the necessary libraries are imported:

```kotlin
import kotlinx.serialization.*
import kotlinx.serialization.json.*
```

## Parse to Json element

You can parse a string into an instance of `JsonElement` with the [`Json.parseToJsonElement()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json/parse-to-json-element.html) function.
This process is not considered decoding or deserialization, as it simply parses the JSON and creates an object that represents it:

<!--- INCLUDE .*-json-.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
-->

```kotlin
fun main() {
    val element = Json.parseToJsonElement("""
        {"name":"kotlinx.serialization","language":"Kotlin"}
    """)
    // Prints the `JsonElement` as a valid JSON string
    println(element)
    // {"name":"kotlinx.serialization","language":"Kotlin"}
}
```

<!--- > You can get the full code [here](../../guide/example/example-json-elements-01.kt). -->

<!--- 
```text
{"name":"kotlinx.serialization","language":"Kotlin"}
```
-->

<!--- TEST -->

## Types of Json elements

A `JsonElement` class has three direct subtypes, which align with JSON structure:

* [`JsonPrimitive`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-primitive/) represents primitive JSON elements, such as strings, numbers, booleans, and nulls.
  Each `JsonPrimitive` holds a string representation of its value, accessible through its [`JsonPrimitive.content`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-primitive/content.html) property.
  You can also create a `JsonPrimitive` by passing primitive Kotlin types to the [`JsonPrimitive()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-primitive.html) constructor.
* [`JsonArray`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-array/) represents a JSON `[...]` array. It is a Kotlin [`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/) of `JsonElement` items.
* [`JsonObject`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-object/) represents a JSON `{...}` object. It is a Kotlin [`Map`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/#kotlin.collections.Map) consisting of `String` keys and `JsonElement` values.

The `JsonElement` class provides convenience functions to check the type of an element.
If the element is not of the expected type, an IllegalArgumentException is thrown:

* [`jsonPrimitive`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/json-primitive.html) checks if the element is a `JsonPrimitive`.
* [`jsonArray`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/json-array.html) checks if the element is a `JsonArray`.
* [`jsonObject`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/json-object.html) checks if the element is a `JsonObject`.

The `JsonPrimitive` class,
class also includes functions to convert its value into various Kotlin primitive types.
For example, `int` converts the value to an `Int`, throwing an exception if the value is not a valid integer, while `intOrNull` safely converts the value to an `Int`, returning `null` if the conversion fails.
Similar functions are available for other types, such as `long`, `longOrNull`, `double`, and `doubleOrNull`.

Here is an example of how you can use these functions when processing JSON data:

```kotlin
fun main() {
    val element = Json.parseToJsonElement("""
        {
            "name": "kotlinx.serialization",
            "forks": [{"votes": 42}, {"votes": 9000}, {}]
        }
    """)
    // Sums `votes` in all objects in the `forks` array, ignoring the objects without `votes`
    val sum = element
        // Accesses the "forks" key from the root JsonObject
        .jsonObject["forks"]!!

        // Checks that "forks" is a JsonArray and sums the "votes" from each JsonObject
        .jsonArray.sumOf { it.jsonObject["votes"]?.jsonPrimitive?.int ?: 0 }
    println(sum)
    // 9042
}
```

<!--- > You can get the full code [here](../../guide/example/example-json-elements-02.kt). -->

<!---
```text
9042
```
-->

<!--- TEST -->

## Construct JSON elements with builder functions

You can construct instances of specific `JsonElement` subtypes using the respective builder functions
[`buildJsonArray()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/build-json-array.html) and [`buildJsonObject()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/build-json-object.html).
These provide a DSL for defining the JSON structure, similar to Kotlin’s standard library collection builders but with JSON-specific overloads and inner builder functions.

Here is an example demonstrating the key features:

```kotlin
fun main() {
    val element = buildJsonObject {
        // Adds a simple key-value pair to the JsonObject
        put("name", "kotlinx.serialization")
        // Adds a nested JsonObject under the "owner" key
        putJsonObject("owner") {
            put("name", "kotlin")
        }
        // Adds a JsonArray with multiple JsonObjects
        putJsonArray("forks") {
            // Adds a JsonObject to the JsonArray
            addJsonObject {
                put("votes", 42)
            }
            addJsonObject {
                put("votes", 9000)
            }
        }
    }
    // Prints the resulting JSON string
    println(element)
    // {"name":"kotlinx.serialization","owner":{"name":"kotlin"},"forks":[{"votes":42},{"votes":9000}]}
}
```

<!--- > You can get the full code [here](../../guide/example/example-json-elements-03.kt). -->

<!---
```text
{"name":"kotlinx.serialization","owner":{"name":"kotlin"},"forks":[{"votes":42},{"votes":9000}]}
```
-->

<!--- TEST -->

## Decode Json elements

You can decode an instance of the `JsonElement` class into a serializable object using
the [`Json.decodeFromJsonElement()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/decode-from-json-element.html) function:

```kotlin
@Serializable
data class Project(val name: String, val language: String)

fun main() {
    val element = buildJsonObject {
        put("name", "kotlinx.serialization")
        put("language", "Kotlin")
    }

    // Decodes the JsonElement into a Project object
    val data = Json.decodeFromJsonElement<Project>(element)
    println(data)
    // Project(name=kotlinx.serialization, language=Kotlin)
}
```

<!--- > You can get the full code [here](../../guide/example/example-json-elements-04.kt). -->

<!---
```text
Project(name=kotlinx.serialization, language=Kotlin)
```
-->

<!--- TEST -->

## Encode literal Json content (experimental)

> The [`JsonUnquotedLiteral`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-unquoted-literal.html) functionality is [Experimental](components-stability.md#stability-levels-explained). To opt in, use the `@ExperimentalSerializationApi` annotation or the compiler option -opt-in=kotlinx.serialization.ExperimentalSerializationApi.
>
{type="warning"}

You can encode an arbitrary unquoted value with [`JsonUnquotedLiteral`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-unquoted-literal.html).

While the JSON specification does not restrict the size or precision of numbers, serializing
numbers of arbitrary size or precision using [`JsonPrimitive()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-primitive.html) is limited.

If you use [`Double`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-double/), the value might be truncated, which can lead to a loss of accuracy for large numbers.
Kotlin/JVM [`BigDecimal`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/java.math.-big-decimal/) can represent
large numbers without loss of precision, but using `JsonPrimitive()` encodes the value as a string rather than as a number:

```kotlin
import java.math.BigDecimal

val format = Json { prettyPrint = true }

fun main() {
    val pi = BigDecimal("3.141592653589793238462643383279")
    
    // Converts the BigDecimal to a Double, causing potential truncation
    val piJsonDouble = JsonPrimitive(pi.toDouble())
    // Converts the BigDecimal to a String, preserving the precision but treating it as a string in JSON
    val piJsonString = JsonPrimitive(pi.toString())
  
    val piObject = buildJsonObject {
        put("pi_double", piJsonDouble)
        put("pi_string", piJsonString)
    }

    println(format.encodeToString(piObject))
    // "pi_double": 3.141592653589793,
    // "pi_string": "3.141592653589793238462643383279"
}
```

<!--- > You can get the full code [here](../../guide/example/example-json-elements-05.kt). -->

In the example above, even though `pi` was defined as a number with 30 decimal places, the resulting JSON does not reflect this.
The `Double` value is truncated to 15 decimal places, and the `String` is wrapped in quotes, making it a string instead of a JSON number.

<!---
```text
{
    "pi_double": 3.141592653589793,
    "pi_string": "3.141592653589793238462643383279"
}
```
-->

<!--- TEST -->

To avoid precision loss, you can encode an arbitrary unquoted value, such as the string value of `pi` in this example, using [`JsonUnquotedLiteral`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-unquoted-literal.html):

```kotlin
import java.math.BigDecimal

val format = Json { prettyPrint = true }

fun main() {
    val pi = BigDecimal("3.141592653589793238462643383279")

    // Encodes the raw JSON content using JsonUnquotedLiteral
    @OptIn(ExperimentalSerializationApi::class)
    val piJsonLiteral = JsonUnquotedLiteral(pi.toString())

    // Converts to Double and String
    val piJsonDouble = JsonPrimitive(pi.toDouble())
    val piJsonString = JsonPrimitive(pi.toString())

    val piObject = buildJsonObject {
        put("pi_literal", piJsonLiteral)
        put("pi_double", piJsonDouble)
        put("pi_string", piJsonString)
    }

    // `pi_literal` now accurately matches the value defined.
    println(format.encodeToString(piObject))
    // "pi_literal": 3.141592653589793238462643383279,
    // "pi_double": 3.141592653589793,
    // "pi_string": "3.141592653589793238462643383279"
}
```

<!--- > You can get the full code [here](../../guide/example/example-json-elements-06.kt). -->

<!---
```text
{
    "pi_literal": 3.141592653589793238462643383279,
    "pi_double": 3.141592653589793,
    "pi_string": "3.141592653589793238462643383279"
}
```
-->

<!--- TEST -->

To decode `pi` back to a `BigDecimal`, you can extract the string content of the `JsonPrimitive`:

```kotlin
import java.math.BigDecimal

fun main() {
    val piObjectJson = """
          {
              "pi_literal": 3.141592653589793238462643383279
          }
      """.trimIndent()

    // Decodes the JSON string into a JsonObject
    val piObject: JsonObject = Json.decodeFromString(piObjectJson)

    // Extracts the string content from the JsonPrimitive
    val piJsonLiteral = piObject["pi_literal"]!!.jsonPrimitive.content

    // Converts the string to a BigDecimal
    val pi = BigDecimal(piJsonLiteral)
    // Prints the decoded value of pi, preserving all 30 decimal places
    println(pi)
    // 3.141592653589793238462643383279
}
```

> This example uses a `JsonPrimitive` for simplicity. For a more reusable method of handling serialization, see
> [Json Transformations](serialization-transform-json.md).)
>
{type="note"}

<!--- > You can get the full code [here](../../guide/example/example-json-elements-07.kt). -->

<!---
```text
3.141592653589793238462643383279
```
-->

<!--- TEST -->

Finally, to avoid creating an inconsistent state, encoding a string equal to `"null"` with `JsonUnquotedLiteral` results in an exception.

```kotlin
@OptIn(ExperimentalSerializationApi::class)
fun main() {
    // Caution: creating null with JsonUnquotedLiteral causes an exception!
    JsonUnquotedLiteral("null")
    // Exception in thread "main" kotlinx.serialization.json.internal.JsonEncodingException
}
```

<!--- > You can get the full code [here](../../guide/example/example-json-elements-08.kt). -->

<!---
```text
Exception in thread "main" kotlinx.serialization.json.internal.JsonEncodingException: Creating a literal unquoted value of 'null' is forbidden. If you want to create JSON null literal, use JsonNull object, otherwise, use JsonPrimitive
```
-->

<!--- TEST LINES_START -->

You can use [`JsonNull`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-null/) or `JsonPrimitive` to represent a proper JSON `null` value instead:

```kotlin
fun main() {
    val possiblyNull = JsonNull
  
    println(possiblyNull)
    // null
}
```

<!--- > You can get the full code [here](../../guide/example/example-json-elements-09.kt). -->

<!---
```text
null
```
-->

<!--- TEST -->
