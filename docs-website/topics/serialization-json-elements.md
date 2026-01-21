[//]: # (title: JSON elements)

Besides converting between JSON strings and Kotlin objects, the Kotlin serialization library also supports working with JSON at a structural level.
To do this, you can use the [`JsonElement`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-element/) API, which lets you inspect, modify, and construct JSON structure directly before converting it into a Kotlin type.

`JsonElement` has three direct subtypes that represent the core JSON structures:

* [`JsonPrimitive`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-primitive/) handles primitive JSON elements such as strings, numbers, booleans, and `null`. `null` is represented by a special subclass of `JsonPrimitive`, [`JsonNull`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-null/).
  Each `JsonPrimitive` stores a string representation of its value, which you can access through its [`JsonPrimitive.content`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-primitive/content.html) property.
* [`JsonArray`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-array/) is a JSON array. It's a Kotlin [`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/) of `JsonElement` items.
* [`JsonObject`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-object/) is a JSON object. It's a Kotlin [`Map`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/#kotlin.collections.Map) with `String` keys and `JsonElement` values.

## Parse to JSON elements

You can parse a string into a `JsonElement` to work with the JSON structure before converting it into a Kotlin type or string.
To do so, use the [`Json.parseToJsonElement()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json/parse-to-json-element.html) function.
This function parses the input into a JSON element tree without decoding or deserializing it.

Here's an example:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
fun main() {
    val element = Json.parseToJsonElement("""
        {"name":"kotlinx.serialization","language":"Kotlin"}
    """)
    // JsonElement.toString() gives you a valid JSON string
    println(element)
    // {"name":"kotlinx.serialization","language":"Kotlin"}
}
//sampleEnd
```
{kotlin-runnable="true"}

## Access JSON element contents

You can access the contents of a JSON element directly through the extension properties of the `JsonElement` API.
These extension properties cast the element to a specific subtype,
and throw an `IllegalArgumentException` if the element doesn't have the expected JSON structure.

The available extension properties are:

* [`jsonPrimitive`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/json-primitive.html) returns a `JsonPrimitive`.
* [`jsonArray`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/json-array.html) returns a `JsonArray`.
* [`jsonObject`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/json-object.html) returns a `JsonObject`.

Similarly, [`JsonPrimitive`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-primitive/) has extension properties for parsing the value as Kotlin primitive types, such as
[`int`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/int.html), [`intOrNull`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/int-or-null.html), [`long`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/long.html), and [`longOrNull`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/long-or-null.html).

Here's an example of how you can use these extension properties when processing JSON data with a known structure:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
fun main() {
    val element = Json.parseToJsonElement("""
        {
            "name": "kotlinx.serialization",
            "forks": [{"votes": 42}, {"votes": 9000}, {}]
        }
    """)
    val sum = element
        // Accesses the forks key from the JsonObject
        .jsonObject["forks"]!!

        // Accesses the value as a JsonArray and sums the votes values from each JsonObject as Int
        .jsonArray.sumOf { it.jsonObject["votes"]?.jsonPrimitive?.int ?: 0 }
    println(sum)
    // 9042
}
//sampleEnd
```
{kotlin-runnable="true"}

If you don't know the JSON structure in advance, you can check the element type and handle each `JsonElement` subtype explicitly.
For example, you can use a helper function with a `when` expression:

```kotlin
fun checkElement(element: JsonElement): String = when (element) {
    is JsonObject -> "JsonObject with keys: ${element.keys}"
    is JsonArray -> "JsonArray with ${element.size} elements"
    is JsonPrimitive -> "JsonPrimitive with content: ${element.content}"
}
```

## Create JSON elements

You can create instances of specific `JsonElement` subtypes directly.

To create a `JsonPrimitive`, use the `JsonPrimitive()` function:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
fun main() {
    // Creates JsonPrimitive values from different Kotlin primitives
    val number = JsonPrimitive(42)
    val text = JsonPrimitive("kotlinx.serialization")

    println(number)
    // 42
    println(text)
    // "kotlinx.serialization"
}
//sampleEnd
```
{kotlin-runnable="true"}

To create `JsonArray` or `JsonObject` elements, use the
[`buildJsonArray()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/build-json-array.html) and [`buildJsonObject()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/build-json-object.html) builder functions respectively.
These provide a DSL to define the JSON structure, similar to [Kotlin's standard library collection builders](constructing-collections.md#create-with-collection-builder-functions), but with JSON-specific overloads and inner builder functions.

> You can also directly create a `JsonArray` from a `List` with [`JsonArray()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-array/-json-array.html) or a `JsonObject` from a `Map` with [`JsonObject()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-object/-json-object.html).
> 
{style="tip"}

Let's look at an example that highlights the key features:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
fun main() {
    val element = buildJsonObject {
        // Adds a simple key-value pair to the JsonObject
        put("name", "kotlinx.serialization")
        // Adds a nested JsonObject under the owner key
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
//sampleEnd
```
{kotlin-runnable="true"}

### Encode literal JSON content

While the JSON specification doesn't restrict the size or precision of numbers, serializing numbers of arbitrary size with the `JsonPrimitive()` function might lead to some issues.

For example, if you use [`Double`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-double/) for large numbers, the value might get truncated and you lose precision.
If you use Kotlin/JVM [`BigDecimal`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/java.math.-big-decimal/), the value stays precise, but `JsonPrimitive()` encodes the value as a string rather than as a number:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.math.BigDecimal

//sampleStart
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
//sampleEnd
```
{kotlin-runnable="true"}

In this example, even though `pi` is defined as a number with 30 decimal places, the resulting JSON doesn't reflect this.
The `Double` value is truncated to 15 decimal places, and the `String` is wrapped in quotes, making it a string instead of a JSON number.

To avoid these issues, you can encode an arbitrary unquoted value, such as the string value of `pi` in this example, using the [`JsonUnquotedLiteral()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-unquoted-literal.html) function:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.math.BigDecimal

//sampleStart
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

    // pi_literal now accurately matches the value defined.
    println(format.encodeToString(piObject))
    // "pi_literal": 3.141592653589793238462643383279,
    // "pi_double": 3.141592653589793,
    // "pi_string": "3.141592653589793238462643383279"
}
//sampleEnd
```
{kotlin-runnable="true"}

To decode `pi` back to a `BigDecimal`, extract the string content of the `JsonPrimitive`:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.math.BigDecimal

//sampleStart
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
//sampleEnd
```
{kotlin-runnable="true"}

> This example uses a `JsonPrimitive` for simplicity. For more reusable approaches, see
> [Json Transformations](serialization-transform-json.md).
>
{style="tip"}

#### JSON null literal

To avoid creating an inconsistent state, you can't encode the string `"null"` with `JsonUnquotedLiteral`.
Attempting to do so results in an exception:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
@OptIn(ExperimentalSerializationApi::class)
fun main() {
    JsonUnquotedLiteral("null")
    // Exception in thread "main" kotlinx.serialization.json.internal.JsonEncodingException
}
//sampleEnd
```
{kotlin-runnable="true" validate="false"}

To represent a JSON `null` literal value, use [`JsonNull`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-null/) instead:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
fun main() {
    val possiblyNull = JsonNull
  
    println(possiblyNull)
    // null
}
//sampleEnd
```
{kotlin-runnable="true"}

## Decode Json elements

To decode an instance of the `JsonElement` class into a serializable object, use
the [`Json.decodeFromJsonElement()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/decode-from-json-element.html) function:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
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
//sampleEnd
```
{kotlin-runnable="true"}

## What's next

* Discover how to [transform JSON during serialization and deserialization](serialization-transform-json.md) for more control over your data.
* Learn how to [serialize classes](serialization-customization-options.md) and how to modify the default behavior of the `@Serializable` annotation.
