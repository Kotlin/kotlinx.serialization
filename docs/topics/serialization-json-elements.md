<!--- TEST_NAME JsonTestElements -->
[//]: # (title: Managing JSON elements)

Aside from direct conversions between strings and JSON objects, Kotlin serialization offers APIs that allow
other ways of working with JSON in the code. For example, you might need to tweak the data before it can parse
or otherwise work with such an unstructured data that it does not readily fit into the typesafe world of Kotlin
serialization.

The main concept in this part of the library is [JsonElement]. Read on to learn what you can do with it.

### Parsing to Json element

A string can be _parsed_ into an instance of [JsonElement] with the [Json.parseToJsonElement] function.
It is called neither decoding nor deserialization because none of that happens in the process.
It just parses a JSON and forms an object representing it:

<!--- INCLUDE .*-json-.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
-->

```kotlin
fun main() {
    val element = Json.parseToJsonElement("""
        {"name":"kotlinx.serialization","language":"Kotlin"}
    """)
    println(element)
}
```

<!--- > You can get the full code [here](../../guide/example/example-json-elements-01.kt). -->

A `JsonElement` prints itself as a valid JSON:

<!--- 
```text
{"name":"kotlinx.serialization","language":"Kotlin"}
```
-->

<!--- TEST -->

### Types of Json elements

A [JsonElement] class has three direct subtypes, closely following JSON grammar:

* [JsonPrimitive] represents primitive JSON elements, such as string, number, boolean, and null.
  Each primitive has a simple string [content][JsonPrimitive.content]. There is also a
  [JsonPrimitive()] constructor function overloaded to accept various primitive Kotlin types and
  to convert them to `JsonPrimitive`.

* [JsonArray] represents a JSON `[...]` array. It is a Kotlin [List] of `JsonElement` items.

* [JsonObject] represents a JSON `{...}` object. It is a Kotlin [Map] from `String` keys to `JsonElement` values.

The `JsonElement` class has extensions that cast it to its corresponding subtypes:
[jsonPrimitive][_jsonPrimitive], [jsonArray][_jsonArray], [jsonObject][_jsonObject]. The `JsonPrimitive` class,
in turn, provides converters to Kotlin primitive types: [int], [intOrNull], [long], [longOrNull],
and similar ones for other types. This is how you can use them for processing JSON whose structure you know:

```kotlin
fun main() {
    val element = Json.parseToJsonElement("""
        {
            "name": "kotlinx.serialization",
            "forks": [{"votes": 42}, {"votes": 9000}, {}]
        }
    """)
    val sum = element
        .jsonObject["forks"]!!
        .jsonArray.sumOf { it.jsonObject["votes"]?.jsonPrimitive?.int ?: 0 }
    println(sum)
}
```

> You can get the full code [here](../../guide/example/example-json-elements-02.kt).

The above example sums `votes` in all objects in the `forks` array, ignoring the objects that have no `votes`:

```text
9042
```

<!--- TEST -->

Note that the execution will fail if the structure of the data is otherwise different.

### Json element builders

You can construct instances of specific [JsonElement] subtypes using the respective builder functions
[buildJsonArray] and [buildJsonObject]. They provide a DSL to define the resulting JSON structure. It
is similar to Kotlin standard library collection builders, but with a JSON-specific convenience
of more type-specific overloads and inner builder functions. The following example shows
all the key features:

```kotlin
fun main() {
    val element = buildJsonObject {
        put("name", "kotlinx.serialization")
        putJsonObject("owner") {
            put("name", "kotlin")
        }
        putJsonArray("forks") {
            addJsonObject {
                put("votes", 42)
            }
            addJsonObject {
                put("votes", 9000)
            }
        }
    }
    println(element)
}
```

> You can get the full code [here](../../guide/example/example-json-elements-03.kt).

As a result, you get a proper JSON string:

```text
{"name":"kotlinx.serialization","owner":{"name":"kotlin"},"forks":[{"votes":42},{"votes":9000}]}
```

<!--- TEST -->

### Decoding Json elements

An instance of the [JsonElement] class can be decoded into a serializable object using
the [Json.decodeFromJsonElement] function:

```kotlin
@Serializable
data class Project(val name: String, val language: String)

fun main() {
    val element = buildJsonObject {
        put("name", "kotlinx.serialization")
        put("language", "Kotlin")
    }
    val data = Json.decodeFromJsonElement<Project>(element)
    println(data)
}
```

> You can get the full code [here](../../guide/example/example-json-elements-04.kt).

The result is exactly what you would expect:

```text
Project(name=kotlinx.serialization, language=Kotlin)
```

<!--- TEST -->

### Encoding literal Json content (experimental)

> This functionality is experimental and requires opting-in to [the experimental Kotlinx Serialization API](compatibility.md#experimental-api).

In some cases it might be necessary to encode an arbitrary unquoted value.
This can be achieved with [JsonUnquotedLiteral].

#### Serializing large decimal numbers

The JSON specification does not restrict the size or precision of numbers, however it is not possible to serialize
numbers of arbitrary size or precision using [JsonPrimitive()].

If [Double] is used, then the numbers are limited in precision, meaning that large numbers are truncated.
When using Kotlin/JVM [BigDecimal] can be used instead, but [JsonPrimitive()] will encode the value as a string, not a
number.

```kotlin
import java.math.BigDecimal

val format = Json { prettyPrint = true }

fun main() {
    val pi = BigDecimal("3.141592653589793238462643383279")
    
    val piJsonDouble = JsonPrimitive(pi.toDouble())
    val piJsonString = JsonPrimitive(pi.toString())
  
    val piObject = buildJsonObject {
        put("pi_double", piJsonDouble)
        put("pi_string", piJsonString)
    }

    println(format.encodeToString(piObject))
}
```

> You can get the full code [here](../../guide/example/example-json-elements-05.kt).

Even though `pi` was defined as a number with 30 decimal places, the resulting JSON does not reflect this.
The [Double] value is truncated to 15 decimal places, and the String is wrapped in quotes - which is not a JSON number.

```text
{
    "pi_double": 3.141592653589793,
    "pi_string": "3.141592653589793238462643383279"
}
```

<!--- TEST -->

To avoid precision loss, the string value of `pi` can be encoded using [JsonUnquotedLiteral].

```kotlin
import java.math.BigDecimal

val format = Json { prettyPrint = true }

fun main() {
    val pi = BigDecimal("3.141592653589793238462643383279")

    // use JsonUnquotedLiteral to encode raw JSON content
    val piJsonLiteral = JsonUnquotedLiteral(pi.toString())

    val piJsonDouble = JsonPrimitive(pi.toDouble())
    val piJsonString = JsonPrimitive(pi.toString())
  
    val piObject = buildJsonObject {
        put("pi_literal", piJsonLiteral)
        put("pi_double", piJsonDouble)
        put("pi_string", piJsonString)
    }

    println(format.encodeToString(piObject))
}
```

> You can get the full code [here](../../guide/example/example-json-elements-06.kt).

`pi_literal` now accurately matches the value defined.

```text
{
    "pi_literal": 3.141592653589793238462643383279,
    "pi_double": 3.141592653589793,
    "pi_string": "3.141592653589793238462643383279"
}
```

<!--- TEST -->

To decode `pi` back to a [BigDecimal], the string content of the [JsonPrimitive] can be used.

(This demonstration uses a [JsonPrimitive] for simplicity. For a more re-usable method of handling serialization, see
[Json Transformations](#json-transformations) below.)


```kotlin
import java.math.BigDecimal

fun main() {
    val piObjectJson = """
          {
              "pi_literal": 3.141592653589793238462643383279
          }
      """.trimIndent()
    
    val piObject: JsonObject = Json.decodeFromString(piObjectJson)
    
    val piJsonLiteral = piObject["pi_literal"]!!.jsonPrimitive.content
    
    val pi = BigDecimal(piJsonLiteral)
    
    println(pi)
}
```

> You can get the full code [here](../../guide/example/example-json-elements-07.kt).

The exact value of `pi` is decoded, with all 30 decimal places of precision that were in the source JSON.

```text
3.141592653589793238462643383279
```

<!--- TEST -->

#### Using `JsonUnquotedLiteral` to create a literal unquoted value of `null` is forbidden

To avoid creating an inconsistent state, encoding a String equal to `"null"` is forbidden.
Use [JsonNull] or [JsonPrimitive] instead.

```kotlin
fun main() {
    // caution: creating null with JsonUnquotedLiteral will cause an exception! 
    JsonUnquotedLiteral("null")
}
```

> You can get the full code [here](../../guide/example/example-json-elements-08.kt).

```text
Exception in thread "main" kotlinx.serialization.json.internal.JsonEncodingException: Creating a literal unquoted value of 'null' is forbidden. If you want to create JSON null literal, use JsonNull object, otherwise, use JsonPrimitive
```

<!--- TEST LINES_START -->
