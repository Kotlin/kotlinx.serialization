<!--- TEST_NAME JsonTest -->

# JSON features

This is the fifth chapter of the [Kotlin Serialization Guide](serialization-guide.md).
In this chapter, we'll walk through features of [JSON](https://www.json.org/json-en.html) serialization available in the [Json] class.

**Table of contents**

<!--- TOC -->

* [Json configuration](#json-configuration)
  * [Pretty printing](#pretty-printing)
  * [Lenient parsing](#lenient-parsing)
  * [Ignoring unknown keys](#ignoring-unknown-keys)
  * [Alternative Json names](#alternative-json-names)
  * [Coercing input values](#coercing-input-values)
  * [Encoding defaults](#encoding-defaults)
  * [Explicit nulls](#explicit-nulls)
  * [Allowing structured map keys](#allowing-structured-map-keys)
  * [Allowing special floating-point values](#allowing-special-floating-point-values)
  * [Class discriminator for polymorphism](#class-discriminator-for-polymorphism)
  * [Class discriminator output mode](#class-discriminator-output-mode)
  * [Decoding enums in a case-insensitive manner](#decoding-enums-in-a-case-insensitive-manner)
  * [Global naming strategy](#global-naming-strategy)
* [Json elements](#json-elements)
  * [Parsing to Json element](#parsing-to-json-element)
  * [Types of Json elements](#types-of-json-elements)
  * [Json element builders](#json-element-builders)
  * [Decoding Json elements](#decoding-json-elements)
  * [Encoding literal Json content (experimental)](#encoding-literal-json-content-experimental)
    * [Serializing large decimal numbers](#serializing-large-decimal-numbers)
    * [Using `JsonUnquotedLiteral` to create a literal unquoted value of `null` is forbidden](#using-jsonunquotedliteral-to-create-a-literal-unquoted-value-of-null-is-forbidden)
* [Json transformations](#json-transformations)
  * [Array wrapping](#array-wrapping)
  * [Array unwrapping](#array-unwrapping)
  * [Manipulating default values](#manipulating-default-values)
  * [Content-based polymorphic deserialization](#content-based-polymorphic-deserialization)
  * [Under the hood (experimental)](#under-the-hood-experimental)
  * [Maintaining custom JSON attributes](#maintaining-custom-json-attributes)

<!--- END -->

## Json configuration

The default [Json] implementation is quite strict with respect to invalid inputs. It enforces Kotlin type safety and
restricts Kotlin values that can be serialized so that the resulting JSON representations are standard.
Many non-standard JSON features are supported by creating a custom instance of a JSON _format_.

To use a custom JSON format configuration, create your own [Json] class instance from an existing
instance, such as a default `Json` object, using the [Json()] builder function. Specify parameter values
in the parentheses via the [JsonBuilder] DSL. The resulting `Json` format instance is immutable and thread-safe;
it can be simply stored in a top-level property.

> We recommend that you store and reuse custom instances of formats for performance reasons because format implementations
> may cache format-specific additional information about the classes they serialize.

This chapter shows configuration features that [Json] supports.

<!--- INCLUDE .*-json-.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
-->

### Pretty printing

By default, the [Json] output is a single line. You can configure it to pretty print the output (that is, add indentations
and line breaks for better readability) by setting the [prettyPrint][JsonBuilder.prettyPrint] property to `true`:

```kotlin
val format = Json { prettyPrint = true }

@Serializable
data class Project(val name: String, val language: String)

fun main() {
    val data = Project("kotlinx.serialization", "Kotlin")
    println(format.encodeToString(data))
}
```

> You can get the full code [here](../guide/example/example-json-01.kt).

It gives the following nice result:

```text
{
    "name": "kotlinx.serialization",
    "language": "Kotlin"
}
```

<!--- TEST -->

### Lenient parsing

By default, [Json] parser enforces various JSON restrictions to be as specification-compliant as possible
(see [RFC-4627]). Particularly, keys must be quoted, while literals must be unquoted. Those restrictions can be relaxed with
the [isLenient][JsonBuilder.isLenient] property. With `isLenient = true`, you can parse quite freely-formatted data:

```kotlin
val format = Json { isLenient = true }

enum class Status { SUPPORTED }

@Serializable
data class Project(val name: String, val status: Status, val votes: Int)

fun main() {
    val data = format.decodeFromString<Project>("""
        {
            name   : kotlinx.serialization,
            status : SUPPORTED,
            votes  : "9000"
        }
    """)
    println(data)
}
```

> You can get the full code [here](../guide/example/example-json-02.kt).

You get the object, even though all keys of the source JSON, string, and enum values are unquoted, while an
integer is quoted:

```text
Project(name=kotlinx.serialization, status=SUPPORTED, votes=9000)
```

<!--- TEST -->

### Ignoring unknown keys

JSON format is often used to read the output of third-party services or in other dynamic environments where
new properties can be added during the API evolution. By default, unknown keys encountered during deserialization produce an error.
You can avoid this and just ignore such keys by setting the [ignoreUnknownKeys][JsonBuilder.ignoreUnknownKeys] property
to `true`:

```kotlin
val format = Json { ignoreUnknownKeys = true }

@Serializable
data class Project(val name: String)

fun main() {
    val data = format.decodeFromString<Project>("""
        {"name":"kotlinx.serialization","language":"Kotlin"}
    """)
    println(data)
}
```

> You can get the full code [here](../guide/example/example-json-03.kt).

It decodes the object despite the fact that the `Project` class doesn't have the `language` property:

```text
Project(name=kotlinx.serialization)
```

<!--- TEST -->

### Alternative Json names

It's not a rare case when JSON fields are renamed due to a schema version change.
You can use the [`@SerialName` annotation](basic-serialization.md#serial-field-names) to change the name of a JSON field,
but such renaming blocks the ability to decode data with the old name.
To support multiple JSON names for the one Kotlin property, there is the [JsonNames] annotation:

```kotlin
@Serializable
data class Project(@JsonNames("title") val name: String)

fun main() {
  val project = Json.decodeFromString<Project>("""{"name":"kotlinx.serialization"}""")
  println(project)
  val oldProject = Json.decodeFromString<Project>("""{"title":"kotlinx.coroutines"}""")
  println(oldProject)
}
```

> You can get the full code [here](../guide/example/example-json-04.kt).

As you can see, both `name` and `title` Json fields correspond to `name` property:

```text
Project(name=kotlinx.serialization)
Project(name=kotlinx.coroutines)
```

Support for [JsonNames] annotation is controlled by the [JsonBuilder.useAlternativeNames] flag.
Unlike most of the configuration flags, this one is enabled by default and does not need attention
unless you want to do some fine-tuning.

<!--- TEST -->

### Coercing input values

JSON formats that from third parties can evolve, sometimes changing the field types.
This can lead to exceptions during decoding when the actual values do not match the expected values.
The default [Json] implementation is strict with respect to input types as was demonstrated in
the [Type safety is enforced](basic-serialization.md#type-safety-is-enforced) section. You can relax this restriction
using the [coerceInputValues][JsonBuilder.coerceInputValues] property.

This property only affects decoding. It treats a limited subset of invalid input values as if the
corresponding property was missing and uses the default value of the corresponding property instead.
The current list of supported invalid values is:

* `null` inputs for non-nullable types
* unknown values for enums

> This list may be expanded in the future, so that [Json] instance configured with this property becomes even more
> permissive to invalid value in the input, replacing them with defaults.

See the example from the [Type safety is enforced](basic-serialization.md#type-safety-is-enforced) section:

```kotlin
val format = Json { coerceInputValues = true }

@Serializable
data class Project(val name: String, val language: String = "Kotlin")

fun main() {
    val data = format.decodeFromString<Project>("""
        {"name":"kotlinx.serialization","language":null}
    """)
    println(data)
}
```

> You can get the full code [here](../guide/example/example-json-05.kt).

The invalid `null` value for the `language` property was coerced into the default value:

```text
Project(name=kotlinx.serialization, language=Kotlin)
```

<!--- TEST -->


### Encoding defaults

Default values of properties are not encoded by default because they will be assigned to missing fields during decoding anyway.
See the [Defaults are not encoded](basic-serialization.md#defaults-are-not-encoded-by-default) section for details and an example.
This is especially useful for nullable properties with null defaults and avoids writing the corresponding null values.
The default behavior can be changed by setting the [encodeDefaults][JsonBuilder.encodeDefaults] property to `true`:

```kotlin
val format = Json { encodeDefaults = true }

@Serializable
class Project(
    val name: String,
    val language: String = "Kotlin",
    val website: String? = null
)

fun main() {
    val data = Project("kotlinx.serialization")
    println(format.encodeToString(data))
}
```

> You can get the full code [here](../guide/example/example-json-06.kt).

It produces the following output which encodes all the property values including the default ones:

```text
{"name":"kotlinx.serialization","language":"Kotlin","website":null}
```

<!--- TEST -->

### Explicit nulls

By default, all `null` values are encoded into JSON strings, but in some cases you may want to omit them.
The encoding of `null` values can be controlled with the [explicitNulls][JsonBuilder.explicitNulls] property.

If you set property to `false`, fields with `null` values are not encoded into JSON even if the property does not have a
default `null` value. When decoding such JSON, the absence of a property value is treated as `null` for nullable properties
without a default value.

```kotlin
val format = Json { explicitNulls = false }

@Serializable
data class Project(
    val name: String,
    val language: String,
    val version: String? = "1.2.2",
    val website: String?,
    val description: String? = null
)

fun main() {
    val data = Project("kotlinx.serialization", "Kotlin", null, null, null)
    val json = format.encodeToString(data)
    println(json)
    println(format.decodeFromString<Project>(json))
}
```

> You can get the full code [here](../guide/example/example-json-07.kt).

As you can see, `version`, `website` and `description` fields are not present in output JSON on the first line.
After decoding, the missing nullable property `website` without a default values has received a `null` value,
while nullable properties `version` and `description` are filled with their default values:

```text
{"name":"kotlinx.serialization","language":"Kotlin"}
Project(name=kotlinx.serialization, language=Kotlin, version=1.2.2, website=null, description=null)
```

`explicitNulls` is `true` by default as it is the default behavior across different versions of the library.

<!--- TEST -->

### Allowing structured map keys

JSON format does not natively support the concept of a map with structured keys. Keys in JSON objects
are strings and can be used to represent only primitives or enums by default.
You can enable non-standard support for structured keys with
the [allowStructuredMapKeys][JsonBuilder.allowStructuredMapKeys] property.

This is how you can serialize a map with keys of a user-defined class:

```kotlin
val format = Json { allowStructuredMapKeys = true }

@Serializable
data class Project(val name: String)

fun main() {
    val map = mapOf(
        Project("kotlinx.serialization") to "Serialization",
        Project("kotlinx.coroutines") to "Coroutines"
    )
    println(format.encodeToString(map))
}
```

> You can get the full code [here](../guide/example/example-json-08.kt).

The map with structured keys gets represented as JSON array with the following items: `[key1, value1, key2, value2,...]`.

```text
[{"name":"kotlinx.serialization"},"Serialization",{"name":"kotlinx.coroutines"},"Coroutines"]
```

<!--- TEST -->

### Allowing special floating-point values

By default, special floating-point values like [Double.NaN] and infinities are not supported in JSON because
the JSON specification prohibits it.
You can enable their encoding using the [allowSpecialFloatingPointValues][JsonBuilder.allowSpecialFloatingPointValues]
property:

```kotlin
val format = Json { allowSpecialFloatingPointValues = true }

@Serializable
class Data(
    val value: Double
)

fun main() {
    val data = Data(Double.NaN)
    println(format.encodeToString(data))
}
```

> You can get the full code [here](../guide/example/example-json-09.kt).

This example produces the following non-stardard JSON output, yet it is a widely used encoding for
special values in JVM world:

```text
{"value":NaN}
```

<!--- TEST -->

### Class discriminator for polymorphism

A key name that specifies a type when you have a polymorphic data can be specified
in the [classDiscriminator][JsonBuilder.classDiscriminator] property:

```kotlin
val format = Json { classDiscriminator = "#class" }

@Serializable
sealed class Project {
    abstract val name: String
}

@Serializable
@SerialName("owned")
class OwnedProject(override val name: String, val owner: String) : Project()

fun main() {
    val data: Project = OwnedProject("kotlinx.coroutines", "kotlin")
    println(format.encodeToString(data))
}
```

> You can get the full code [here](../guide/example/example-json-10.kt).

In combination with an explicitly specified [SerialName] of the class it provides full
control over the resulting JSON object:

```text
{"#class":"owned","name":"kotlinx.coroutines","owner":"kotlin"}
```

<!--- TEST -->

It is also possible to specify different class discriminators for different hierarchies. Instead of Json instance property, use [JsonClassDiscriminator] annotation directly on base serializable class:

```kotlin
@Serializable
@JsonClassDiscriminator("message_type")
sealed class Base
```

This annotation is _inheritable_, so all subclasses of `Base` will have the same discriminator:

```kotlin
@Serializable // Class discriminator is inherited from Base
sealed class ErrorClass: Base()
```

> To learn more about inheritable serial annotations, see documentation for [InheritableSerialInfo].

Note that it is not possible to explicitly specify different class discriminators in subclasses of `Base`. Only hierarchies with empty intersections can have different discriminators.

Discriminator specified in the annotation has priority over discriminator in Json configuration:

<!--- INCLUDE

@Serializable
data class Message(val message: Base, val error: ErrorClass?)

@Serializable
@SerialName("my.app.BaseMessage")
data class BaseMessage(val message: String) : Base()

@Serializable
@SerialName("my.app.GenericError")
data class GenericError(@SerialName("error_code") val errorCode: Int) : ErrorClass()
-->

```kotlin

val format = Json { classDiscriminator = "#class" }

fun main() {
    val data = Message(BaseMessage("not found"), GenericError(404))
    println(format.encodeToString(data))
}
```

> You can get the full code [here](../guide/example/example-json-11.kt).

As you can see, discriminator from the `Base` class is used:

```text
{"message":{"message_type":"my.app.BaseMessage","message":"not found"},"error":{"message_type":"my.app.GenericError","error_code":404}}
```

<!--- TEST -->

### Class discriminator output mode

Class discriminator provides information for serializing and deserializing [polymorphic class hierarchies](polymorphism.md#sealed-classes).
As shown above, it is only added for polymorphic classes by default.
In case you want to encode more or less information for various third party APIs about types in the output, it is possible to control
addition of the class discriminator with the [JsonBuilder.classDiscriminatorMode] property.

For example, [ClassDiscriminatorMode.NONE] does not add class discriminator at all, in case the receiving party is not interested in Kotlin types:

```kotlin
val format = Json { classDiscriminatorMode = ClassDiscriminatorMode.NONE }

@Serializable
sealed class Project {
    abstract val name: String
}

@Serializable
class OwnedProject(override val name: String, val owner: String) : Project()

fun main() {
    val data: Project = OwnedProject("kotlinx.coroutines", "kotlin")
    println(format.encodeToString(data))
}
```

> You can get the full code [here](../guide/example/example-json-12.kt).

Note that it would be impossible to deserialize this output back with kotlinx.serialization.

```text
{"name":"kotlinx.coroutines","owner":"kotlin"}
```

Two other available values are [ClassDiscriminatorMode.POLYMORPHIC] (default behavior) and [ClassDiscriminatorMode.ALL_JSON_OBJECTS] (adds discriminator whenever possible).
Consult their documentation for details.

<!--- TEST -->

### Decoding enums in a case-insensitive manner

[Kotlin's naming policy recommends](https://kotlinlang.org/docs/coding-conventions.html#property-names) naming enum values
using either uppercase underscore-separated names or upper camel case names.
[Json] uses exact Kotlin enum values names for decoding by default.
However, sometimes third-party JSONs have such values named in lowercase or some mixed case.
In this case, it is possible to decode enum values in a case-insensitive manner using [JsonBuilder.decodeEnumsCaseInsensitive] property:

```kotlin
val format = Json { decodeEnumsCaseInsensitive = true }

enum class Cases { VALUE_A, @JsonNames("Alternative") VALUE_B }

@Serializable
data class CasesList(val cases: List<Cases>)

fun main() {
  println(format.decodeFromString<CasesList>("""{"cases":["value_A", "alternative"]}""")) 
}
```

> You can get the full code [here](../guide/example/example-json-13.kt).

It affects serial names as well as alternative names specified with [JsonNames] annotation, so both values are successfully decoded:

```text
CasesList(cases=[VALUE_A, VALUE_B])
```

This property does not affect encoding in any way.

<!--- TEST -->

### Global naming strategy

If properties' names in Json input are different from Kotlin ones, it is recommended to specify the name 
for each property explicitly using [`@SerialName` annotation](basic-serialization.md#serial-field-names).
However, there are certain situations where transformation should be applied to every serial name — such as migration
from other frameworks or legacy codebase. For that cases, it is possible to specify a [namingStrategy][JsonBuilder.namingStrategy]
for a [Json] instance. `kotlinx.serialization` provides one strategy implementation out of the box, the [JsonNamingStrategy.SnakeCase](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-naming-strategy/-builtins/-snake-case.html):

```kotlin
@Serializable
data class Project(val projectName: String, val projectOwner: String)

val format = Json { namingStrategy = JsonNamingStrategy.SnakeCase }

fun main() {
    val project = format.decodeFromString<Project>("""{"project_name":"kotlinx.coroutines", "project_owner":"Kotlin"}""")
    println(format.encodeToString(project.copy(projectName = "kotlinx.serialization")))
}
```

> You can get the full code [here](../guide/example/example-json-14.kt).

As you can see, both serialization and deserialization work as if all serial names are transformed from camel case to snake case:

```text
{"project_name":"kotlinx.serialization","project_owner":"Kotlin"}
```

There are some caveats one should remember while dealing with a [JsonNamingStrategy]:

* Due to the nature of the `kotlinx.serialization` framework, naming strategy transformation is applied to all properties regardless 
of whether their serial name was taken from the property name or provided by [SerialName] annotation.
Effectively, it means one cannot avoid transformation by explicitly specifying the serial name. To be able to deserialize
non-transformed names, [JsonNames] annotation can be used instead.

* Collision of the transformed name with any other (transformed) properties serial names or any alternative names
specified with [JsonNames] will lead to a deserialization exception.

* Global naming strategies are very implicit: by looking only at the definition of the class,
it is impossible to determine which names it will have in the serialized form.
As a consequence, naming strategies are not friendly to actions like Find Usages/Rename in IDE, full-text search by grep, etc.
For them, the original name and the transformed are two different things;
changing one without the other may introduce bugs in many unexpected ways and lead to greater maintenance efforts for code with global naming strategies.

Therefore, one should carefully weigh the pros and cons before considering adding global naming strategies to an application.

<!--- TEST -->

## Json elements

Aside from direct conversions between strings and JSON objects, Kotlin serialization offers APIs that allow
other ways of working with JSON in the code. For example, you might need to tweak the data before it can parse
or otherwise work with such an unstructured data that it does not readily fit into the typesafe world of Kotlin
serialization.

The main concept in this part of the library is [JsonElement]. Read on to learn what you can do with it.

### Parsing to Json element

A string can be _parsed_ into an instance of [JsonElement] with the [Json.parseToJsonElement] function.
It is called neither decoding nor deserialization because none of that happens in the process.
It just parses a JSON and forms an object representing it:

```kotlin
fun main() {
    val element = Json.parseToJsonElement("""
        {"name":"kotlinx.serialization","language":"Kotlin"}
    """)
    println(element)
}
```

> You can get the full code [here](../guide/example/example-json-15.kt).

A `JsonElement` prints itself as a valid JSON:

```text
{"name":"kotlinx.serialization","language":"Kotlin"}
```

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

> You can get the full code [here](../guide/example/example-json-16.kt).

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

> You can get the full code [here](../guide/example/example-json-17.kt).

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

> You can get the full code [here](../guide/example/example-json-18.kt).

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

> You can get the full code [here](../guide/example/example-json-19.kt).

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

> You can get the full code [here](../guide/example/example-json-20.kt).

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

> You can get the full code [here](../guide/example/example-json-21.kt).

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

> You can get the full code [here](../guide/example/example-json-22.kt).

```text
Exception in thread "main" kotlinx.serialization.json.internal.JsonEncodingException: Creating a literal unquoted value of 'null' is forbidden. If you want to create JSON null literal, use JsonNull object, otherwise, use JsonPrimitive
```

<!--- TEST LINES_START -->


## Json transformations

To affect the shape and contents of JSON output after serialization, or adapt input to deserialization,
it is possible to write a [custom serializer](serializers.md). However, it may be inconvenient to
carefully follow [Encoder] and [Decoder] calling conventions, especially for relatively small and easy tasks.
For that purpose, Kotlin serialization provides an API that can reduce the burden of implementing a custom
serializer to a problem of manipulating a Json elements tree.

We recommend that you get familiar with the [Serializers](serializers.md) chapter: among other things, it
explains how custom serializers are bound to classes.

Transformation capabilities are provided by the abstract [JsonTransformingSerializer] class which implements [KSerializer].
Instead of direct interaction with `Encoder` or `Decoder`, this class asks you to supply transformations for JSON tree
represented by the [JsonElement] class using the`transformSerialize` and
`transformDeserialize` methods. Let's take a look at the examples.

### Array wrapping

The first example is an implementation of JSON array wrapping for lists.

Consider a REST API that returns a JSON array of `User` objects, or a single object (not wrapped into an array) if there
is only one element in the result.

In the data model, use the [`@Serializable`][Serializable] annotation to specify a custom serializer for a
`users: List<User>` property.

<!--- INCLUDE
import kotlinx.serialization.builtins.*
-->

```kotlin
@Serializable
data class Project(
    val name: String,
    @Serializable(with = UserListSerializer::class)
    val users: List<User>
)

@Serializable
data class User(val name: String)
```

Since this example covers only the deserialization case, you can implement `UserListSerializer` and override only the
`transformDeserialize` function. The `JsonTransformingSerializer` constructor takes an original serializer
as parameter (this approach is shown in the section [Constructing collection serializers](serializers.md#constructing-collection-serializers)):

```kotlin
object UserListSerializer : JsonTransformingSerializer<List<User>>(ListSerializer(User.serializer())) {
    // If response is not an array, then it is a single object that should be wrapped into the array
    override fun transformDeserialize(element: JsonElement): JsonElement =
        if (element !is JsonArray) JsonArray(listOf(element)) else element
}
```

Now you can test the code with a JSON array or a single JSON object as inputs.

```kotlin
fun main() {
    println(Json.decodeFromString<Project>("""
        {"name":"kotlinx.serialization","users":{"name":"kotlin"}}
    """))
    println(Json.decodeFromString<Project>("""
        {"name":"kotlinx.serialization","users":[{"name":"kotlin"},{"name":"jetbrains"}]}
    """))
}
```

> You can get the full code [here](../guide/example/example-json-23.kt).

The output shows that both cases are correctly deserialized into a Kotlin [List].

```text
Project(name=kotlinx.serialization, users=[User(name=kotlin)])
Project(name=kotlinx.serialization, users=[User(name=kotlin), User(name=jetbrains)])
```

<!--- TEST -->

### Array unwrapping

You can also implement the `transformSerialize` function to unwrap a single-element list into a single JSON object
during serialization:

<!--- INCLUDE
import kotlinx.serialization.builtins.*

@Serializable
data class Project(
    val name: String,
    @Serializable(with = UserListSerializer::class)
    val users: List<User>
)

@Serializable
data class User(val name: String)

object UserListSerializer : JsonTransformingSerializer<List<User>>(ListSerializer(User.serializer())) {
-->

```kotlin
    override fun transformSerialize(element: JsonElement): JsonElement {
        require(element is JsonArray) // this serializer is used only with lists
        return element.singleOrNull() ?: element
    }
```

<!--- INCLUDE
}
-->

Now, if you serialize a single-element list of objects from Kotlin:

```kotlin
fun main() {
    val data = Project("kotlinx.serialization", listOf(User("kotlin")))
    println(Json.encodeToString(data))
}
```

> You can get the full code [here](../guide/example/example-json-24.kt).

You end up with a single JSON object, not an array with one element:

```text
{"name":"kotlinx.serialization","users":{"name":"kotlin"}}
```

<!--- TEST -->

### Manipulating default values

Another kind of useful transformation is omitting specific values from the output JSON, for example, if it
is used as default when missing or for other reasons.

Imagine that you cannot specify a default value for the `language` property in the `Project` data model for some reason,
but you need it omitted from the JSON when it is equal to `Kotlin` (we can all agree that Kotlin should be default anyway).
You can fix it by writing the special `ProjectSerializer` based on
the [Plugin-generated serializer](serializers.md#plugin-generated-serializer) for the `Project` class.

```kotlin
@Serializable
class Project(val name: String, val language: String)

object ProjectSerializer : JsonTransformingSerializer<Project>(Project.serializer()) {
    override fun transformSerialize(element: JsonElement): JsonElement =
        // Filter out top-level key value pair with the key "language" and the value "Kotlin"
        JsonObject(element.jsonObject.filterNot {
            (k, v) -> k == "language" && v.jsonPrimitive.content == "Kotlin"
        })
}
```

In the example below, we are serializing the `Project` class at the top-level, so we explicitly
pass the above `ProjectSerializer` to [Json.encodeToString] function as was shown in
the [Passing a serializer manually](serializers.md#passing-a-serializer-manually) section:

```kotlin
fun main() {
    val data = Project("kotlinx.serialization", "Kotlin")
    println(Json.encodeToString(data)) // using plugin-generated serializer
    println(Json.encodeToString(ProjectSerializer, data)) // using custom serializer
}
```

> You can get the full code [here](../guide/example/example-json-25.kt).

See the effect of the custom serializer:

```text
{"name":"kotlinx.serialization","language":"Kotlin"}
{"name":"kotlinx.serialization"}
```

<!--- TEST -->

### Content-based polymorphic deserialization

Typically, [polymorphic serialization](polymorphism.md) requires a dedicated `"type"` key
(also known as _class discriminator_) in the incoming JSON object to determine the actual serializer
which should be used to deserialize Kotlin class.

However, sometimes the `type` property may not be present in the input. In this case, you need to guess
the actual type by the shape of JSON, for example by the presence of a specific key.

[JsonContentPolymorphicSerializer] provides a skeleton implementation for such a strategy.
To use it, override its `selectDeserializer` method.
Let's start with the following class hierarchy.

> Note that is does not have to be `sealed` as recommended in the [Sealed classes](polymorphism.md#sealed-classes) section,
> because we are not going to take advantage of the plugin-generated code that automatically selects the
> appropriate subclass, but are going to implement this code manually.

<!--- INCLUDE
import kotlinx.serialization.builtins.*
-->

```kotlin
@Serializable
abstract class Project {
    abstract val name: String
}

@Serializable
data class BasicProject(override val name: String): Project()


@Serializable
data class OwnedProject(override val name: String, val owner: String) : Project()
```

You can distinguish the `BasicProject` and `OwnedProject` subclasses by the presence of
the `owner` key in the JSON object.

```kotlin
object ProjectSerializer : JsonContentPolymorphicSerializer<Project>(Project::class) {
    override fun selectDeserializer(element: JsonElement) = when {
        "owner" in element.jsonObject -> OwnedProject.serializer()
        else -> BasicProject.serializer()
    }
}
```

When you use this serializer to serialize data, either [registered](polymorphism.md#registered-subclasses) or
the default serializer is selected for the actual type at runtime:

```kotlin
fun main() {
    val data = listOf(
        OwnedProject("kotlinx.serialization", "kotlin"),
        BasicProject("example")
    )
    val string = Json.encodeToString(ListSerializer(ProjectSerializer), data)
    println(string)
    println(Json.decodeFromString(ListSerializer(ProjectSerializer), string))
}
```

> You can get the full code [here](../guide/example/example-json-26.kt).

No class discriminator is added in the JSON output:

```text
[{"name":"kotlinx.serialization","owner":"kotlin"},{"name":"example"}]
[OwnedProject(name=kotlinx.serialization, owner=kotlin), BasicProject(name=example)]
```

<!--- TEST -->

### Under the hood (experimental)

Although abstract serializers mentioned above can cover most of the cases, it is possible to implement similar machinery
manually, using only the [KSerializer] class.
If tweaking the abstract methods `transformSerialize`/`transformDeserialize`/`selectDeserializer` is not enough,
then altering `serialize`/`deserialize` is a way to go.

Here are some useful things about custom serializers with [Json]:

* [Encoder] can be cast to [JsonEncoder], and [Decoder] to [JsonDecoder], if the current format is [Json].
* `JsonDecoder` has the [decodeJsonElement][JsonDecoder.decodeJsonElement] method and `JsonEncoder`
  has the [encodeJsonElement][JsonEncoder.encodeJsonElement] method,
  which basically retrieve an element from and insert an element to a current position in the stream.
* Both [`JsonDecoder`][JsonDecoder.json] and [`JsonEncoder`][JsonEncoder.json] have the `json` property,
  which returns [Json] instance with all settings that are currently in use.
* [Json] has the [encodeToJsonElement][Json.encodeToJsonElement] and [decodeFromJsonElement][Json.decodeFromJsonElement] methods.

Given all that, it is possible to implement two-stage conversion `Decoder -> JsonElement -> value` or
`value -> JsonElement -> Encoder`.
For example, you can implement a fully custom serializer for the following `Response` class so that its
`Ok` subclass is represented directly, but the `Error` subclass is represented by an object with the error message:

<!--- INCLUDE
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
-->

```kotlin
@Serializable(with = ResponseSerializer::class)
sealed class Response<out T> {
    data class Ok<out T>(val data: T) : Response<T>()
    data class Error(val message: String) : Response<Nothing>()
}

class ResponseSerializer<T>(private val dataSerializer: KSerializer<T>) : KSerializer<Response<T>> {
    override val descriptor: SerialDescriptor = buildSerialDescriptor("Response", PolymorphicKind.SEALED) {
        element("Ok", dataSerializer.descriptor)
        element("Error", buildClassSerialDescriptor("Error") {
          element<String>("message")
        })
    }

    override fun deserialize(decoder: Decoder): Response<T> {
        // Decoder -> JsonDecoder
        require(decoder is JsonDecoder) // this class can be decoded only by Json
        // JsonDecoder -> JsonElement
        val element = decoder.decodeJsonElement()
        // JsonElement -> value
        if (element is JsonObject && "error" in element)
            return Response.Error(element["error"]!!.jsonPrimitive.content)
        return Response.Ok(decoder.json.decodeFromJsonElement(dataSerializer, element))
    }

    override fun serialize(encoder: Encoder, value: Response<T>) {
        // Encoder -> JsonEncoder
        require(encoder is JsonEncoder) // This class can be encoded only by Json
        // value -> JsonElement
        val element = when (value) {
            is Response.Ok -> encoder.json.encodeToJsonElement(dataSerializer, value.data)
            is Response.Error -> buildJsonObject { put("error", value.message) }
        }
        // JsonElement -> JsonEncoder
        encoder.encodeJsonElement(element)
    }
}
```

Having this serializable `Response` implementation, you can take any serializable payload for its data
and serialize or deserialize the corresponding responses:

```kotlin
@Serializable
data class Project(val name: String)

fun main() {
    val responses = listOf(
        Response.Ok(Project("kotlinx.serialization")),
        Response.Error("Not found")
    )
    val string = Json.encodeToString(responses)
    println(string)
    println(Json.decodeFromString<List<Response<Project>>>(string))
}
```

> You can get the full code [here](../guide/example/example-json-27.kt).

This gives you fine-grained control on the representation of the `Response` class in the JSON output:

```text
[{"name":"kotlinx.serialization"},{"error":"Not found"}]
[Ok(data=Project(name=kotlinx.serialization)), Error(message=Not found)]
```

<!--- TEST -->

### Maintaining custom JSON attributes

A good example of custom JSON-specific serializer would be a deserializer
that packs all unknown JSON properties into a dedicated field of `JsonObject` type.

Let's add `UnknownProject` &ndash; a class with the `name` property and arbitrary details flattened into the same object:

<!--- INCLUDE
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
-->

```kotlin
data class UnknownProject(val name: String, val details: JsonObject)
```

However, the default plugin-generated serializer requires details
to be a separate JSON object and that's not what we want.

To mitigate that, write an own serializer that uses the fact that it works only with the `Json` format:

```kotlin
object UnknownProjectSerializer : KSerializer<UnknownProject> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("UnknownProject") {
        element<String>("name")
        element<JsonElement>("details")
    }

    override fun deserialize(decoder: Decoder): UnknownProject {
        // Cast to JSON-specific interface
        val jsonInput = decoder as? JsonDecoder ?: error("Can be deserialized only by JSON")
        // Read the whole content as JSON
        val json = jsonInput.decodeJsonElement().jsonObject
        // Extract and remove name property
        val name = json.getValue("name").jsonPrimitive.content
        val details = json.toMutableMap()
        details.remove("name")
        return UnknownProject(name, JsonObject(details))
    }

    override fun serialize(encoder: Encoder, value: UnknownProject) {
        error("Serialization is not supported")
    }
}
```

Now it can be used to read flattened JSON details as `UnknownProject`:

```kotlin
fun main() {
    println(Json.decodeFromString(UnknownProjectSerializer, """{"type":"unknown","name":"example","maintainer":"Unknown","license":"Apache 2.0"}"""))
}
```

> You can get the full code [here](../guide/example/example-json-28.kt).

```text
UnknownProject(name=example, details={"type":"unknown","maintainer":"Unknown","license":"Apache 2.0"})
```

<!--- TEST -->

---

The next chapter covers [Alternative and custom formats (experimental)](formats.md).


<!-- references -->
[RFC-4627]: https://www.ietf.org/rfc/rfc4627.txt
[BigDecimal]: https://docs.oracle.com/javase/8/docs/api/java/math/BigDecimal.html

<!-- stdlib references -->
[Double]: https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-double/
[Double.NaN]: https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-double/-na-n.html
[List]: https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/
[Map]: https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/

<!--- MODULE /kotlinx-serialization-core -->
<!--- INDEX kotlinx-serialization-core/kotlinx.serialization -->

[SerialName]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-serial-name/index.html
[InheritableSerialInfo]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-inheritable-serial-info/index.html
[KSerializer]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-k-serializer/index.html
[Serializable]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-serializable/index.html

<!--- INDEX kotlinx-serialization-core/kotlinx.serialization.encoding -->

[Encoder]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-encoder/index.html
[Decoder]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-decoder/index.html

<!--- MODULE /kotlinx-serialization-json -->
<!--- INDEX kotlinx-serialization-json/kotlinx.serialization.json -->

[Json]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json/index.html
[Json()]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json.html
[JsonBuilder]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/index.html
[JsonBuilder.prettyPrint]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/pretty-print.html
[JsonBuilder.isLenient]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/is-lenient.html
[JsonBuilder.ignoreUnknownKeys]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/ignore-unknown-keys.html
[JsonNames]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-names/index.html
[JsonBuilder.useAlternativeNames]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/use-alternative-names.html
[JsonBuilder.coerceInputValues]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/coerce-input-values.html
[JsonBuilder.encodeDefaults]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/encode-defaults.html
[JsonBuilder.explicitNulls]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/explicit-nulls.html
[JsonBuilder.allowStructuredMapKeys]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/allow-structured-map-keys.html
[JsonBuilder.allowSpecialFloatingPointValues]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/allow-special-floating-point-values.html
[JsonBuilder.classDiscriminator]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/class-discriminator.html
[JsonClassDiscriminator]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-class-discriminator/index.html
[JsonBuilder.classDiscriminatorMode]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/class-discriminator-mode.html
[ClassDiscriminatorMode.NONE]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-class-discriminator-mode/-n-o-n-e/index.html
[ClassDiscriminatorMode.POLYMORPHIC]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-class-discriminator-mode/-p-o-l-y-m-o-r-p-h-i-c/index.html
[ClassDiscriminatorMode.ALL_JSON_OBJECTS]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-class-discriminator-mode/-a-l-l_-j-s-o-n_-o-b-j-e-c-t-s/index.html
[JsonBuilder.decodeEnumsCaseInsensitive]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/decode-enums-case-insensitive.html
[JsonBuilder.namingStrategy]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/naming-strategy.html
[JsonNamingStrategy]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-naming-strategy/index.html
[JsonElement]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-element/index.html
[Json.parseToJsonElement]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json/parse-to-json-element.html
[JsonPrimitive]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-primitive/index.html
[JsonPrimitive.content]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-primitive/content.html
[JsonPrimitive()]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-primitive.html
[JsonArray]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-array/index.html
[JsonObject]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-object/index.html
[_jsonPrimitive]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/json-primitive.html
[_jsonArray]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/json-array.html
[_jsonObject]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/json-object.html
[int]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/int.html
[intOrNull]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/int-or-null.html
[long]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/long.html
[longOrNull]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/long-or-null.html
[buildJsonArray]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/build-json-array.html
[buildJsonObject]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/build-json-object.html
[Json.decodeFromJsonElement]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/decode-from-json-element.html
[JsonUnquotedLiteral]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-unquoted-literal.html
[JsonNull]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-null/index.html
[JsonTransformingSerializer]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-transforming-serializer/index.html
[Json.encodeToString]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json/encode-to-string.html
[JsonContentPolymorphicSerializer]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-content-polymorphic-serializer/index.html
[JsonEncoder]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-encoder/index.html
[JsonDecoder]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-decoder/index.html
[JsonDecoder.decodeJsonElement]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-decoder/decode-json-element.html
[JsonEncoder.encodeJsonElement]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-encoder/encode-json-element.html
[JsonDecoder.json]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-decoder/json.html
[JsonEncoder.json]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-encoder/json.html
[Json.encodeToJsonElement]: https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/encode-to-json-element.html

<!--- END -->
