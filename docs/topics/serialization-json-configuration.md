[//]: # (title: Customize JSON serialization)

The default [`Json`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json/) class enforces Kotlin type safety and only allows values that can be serialized into standard JSON.
However, you can handle non-standard JSON features by creating a _custom JSON format_.

To create a custom JSON format, use the [`Json()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json.html) builder function.
You can base it on an existing `Json` instance, such as the default `Json` object, and specify the desired configuration
using the [`JsonBuilder`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/) DSL.
The resulting `Json` instance is immutable and thread-safe, making it safe to store in a top-level property:

```kotlin
// Configures a Json instance to ignore unknown keys
val customJson = Json {
    ignoreUnknownKeys = true
}

// The customJson instance can now be used like the default one
val jsonString = customJson.encodeToString(user)
println(jsonString)
```

> Reusing custom `Json` instances improves performance by allowing them to cache class-specific information.
> 
{type="tip"}

The following sections cover the various configuration features supported by `Json`.

<!--- TEST_NAME JsonTest -->

## Pretty printing

By default, the `Json` output is a single line. You can configure it to pretty-print the output, adding indentations
and line breaks for better readability, by setting the [`prettyPrint`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/pretty-print.html) property to `true`:

<!--- CLEAR -->

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

// Creates a custom Json format
val format = Json { prettyPrint = true }

@Serializable
data class Project(val name: String, val language: String)

fun main() {
    val data = Project("kotlinx.serialization", "Kotlin")

    // Prints the pretty-printed JSON string
    println(format.encodeToString(data))
}
```

<!--- > You can get the full code [here](../../guide/example/example-json-01.kt). -->

It produces the following result:

```text
{
    "name": "kotlinx.serialization",
    "language": "Kotlin"
}
```

<!--- TEST -->

## Customize JSON parsing behavior

Kotlin’s `Json` parser offers various settings to customize how JSON data is parsed and deserialized.

### Lenient parsing

<!--- INCLUDE .*-json-.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
-->

By default, the `Json` parser enforces strict JSON rules to ensure compliance with the [RFC-4627](https://www.ietf.org/rfc/rfc4627.txt) specification. 
These rules require keys and string literals to be quoted.

To relax these restrictions, set the [`isLenient`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/is-lenient.html) property to `true`.
This allows the parser to handle more freely formatted data:

```kotlin
val format = Json { isLenient = true }

enum class Status { SUPPORTED }

@Serializable
data class Project(val name: String, val status: Status, val votes: Int)

fun main() {
    // Decodes a JSON string with lenient parsing
    // Lenient parsing allows unquoted keys, string and enum values, and quoted integers
    val data = format.decodeFromString<Project>("""
        {
            name   : kotlinx.serialization,
            status : SUPPORTED,
            votes  : "9000"
        }
    """)
    println(data)
    // Project(name=kotlinx.serialization, status=SUPPORTED, votes=9000)
}
```

<!--- >You can get the full code [here](../../guide/example/example-json-02.kt). -->

<!---
```text
Project(name=kotlinx.serialization, status=SUPPORTED, votes=9000)
```
-->

<!--- TEST -->

### Ignore unknown keys

The JSON format is often used to process data from third-party services or other dynamic environments where new properties may be added over time.
By default, unknown keys encountered during deserialization cause an error.
You can prevent these errors by setting the [`ignoreUnknownKeys`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/ignore-unknown-keys.html) property
to `true`, which ignores any unknown keys during deserialization:

```kotlin
// Configures a Json instance to ignore unknown keys
val format = Json { ignoreUnknownKeys = true }

@Serializable
data class Project(val name: String)

fun main() {
    val data = format.decodeFromString<Project>("""
        {"name":"kotlinx.serialization","language":"Kotlin"}
    """)
    // Decodes the object even though the `Project` class doesn't have the `language` property
    println(data)
    // Project(name=kotlinx.serialization)
}
```

<!--- > You can get the full code [here](../../guide/example/example-json-03.kt). -->

<!---
```text
Project(name=kotlinx.serialization)
```
-->

<!--- TEST -->

## Manage default and null values

When working with JSON, managing default and `null` values helps ensure your data stays consistent.
`kotlinx.serialization` provides control over when to include default values, how to handle nulls,
and how to coerce unexpected or missing data into valid values during deserialization.

### Encode default values

By default, the JSON serializer does not encode default property values because they are automatically assigned to missing fields during decoding.
This behavior is especially useful for nullable properties with null defaults, as it avoids writing unnecessary `null` values.
For more details, see the [Manage serialization of default properties](serialization-customization-options.md#manage-serialization-of-default-properties-with-encodeddefault) section.

You can change this default behavior by setting the [`encodeDefaults`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/encode-defaults.html) property to `true`:

```kotlin
// Configures a Json instance to encode default values
val format = Json { encodeDefaults = true }

@Serializable
class Project(
    val name: String,
    val language: String = "Kotlin",
    val website: String? = null
)

fun main() {
    val data = Project("kotlinx.serialization")

    // Encodes all the property values including the default ones
    println(format.encodeToString(data))
    // {"name":"kotlinx.serialization","language":"Kotlin","website":null}
}
```

<!--- > You can get the full code [here](../../guide/example/example-json-05.kt). -->

<!---
```text
{"name":"kotlinx.serialization","language":"Kotlin","website":null}
```
-->

<!--- TEST -->

### Omit explicit nulls

By default, all `null` values are encoded into JSON strings.
To omit `null` values, set the [`explicitNulls`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/explicit-nulls.html) property to `false`:

```kotlin
// Configures a Json instance to omit null values during serialization
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

    // The version, website, and description fields are omitted from the output JSON
    println(json)
    // {"name":"kotlinx.serialization","language":"Kotlin"}

    // Missing nullable fields without defaults are treated as null
    // Fields with defaults are filled with their default values
    println(format.decodeFromString<Project>(json))
    // Project(name=kotlinx.serialization, language=Kotlin, version=1.2.2, website=null, description=null)
}
```

Encoding and decoding can become asymmetrical when `explicitNulls` is set to `false`.
In the example above, the `version` field is `null` before encoding but decodes to `1.2.2`.

<!--- > You can get the full code [here](../../guide/example/example-json-06.kt). -->

<!---
```text
{"name":"kotlinx.serialization","language":"Kotlin"}
Project(name=kotlinx.serialization, language=Kotlin, version=1.2.2, website=null, description=null)
```
-->

> You can configure the decoder to handle certain invalid input values by treating them as missing fields using the [`coerceInputValues`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/coerce-input-values.html) property.
> For more information, see the [Coerce input values](#coerce-input-values) section.
>
{type="tip"}

<!--- TEST -->

### Coerce input values

When working with JSON data from third parties, the format can evolve over time, leading to changes in field types.
This can lead to exceptions during decoding when the actual values do not match the expected types.
The default `Json` implementation is strict about input types, as demonstrated in
the [@Serializable annotation](serialization-customization-options.md#the-serializable-annotation) section.
You can relax this restriction using the [coerceInputValues](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/coerce-input-values.html) property.

This property only affects decoding. It treats certain invalid input values as if the corresponding property were missing.
The current supported invalid values are:

* `null` inputs for non-nullable types
* unknown values for enums

> This list may be expanded in the future, making `Json` instances with this property even more permissive
> by replacing invalid values with defaults or `null`.
>
{type="note"}

If value is missing, it is replaced with a default property value if it exists.
For enums, if no default is defined and the [`explicitNulls`]((#omit-explicit-nulls)) property is set to `false`,
the value is replaced with `null` if the property is nullable:

```kotlin
val format = Json { coerceInputValues = true }

@Serializable
data class Project(val name: String, val language: String = "Kotlin")

fun main() {
    val data = format.decodeFromString<Project>("""
        {"name":"kotlinx.serialization","language":null}
    """)

    // The invalid `null` value for `language` is coerced to its default value
    println(data)
    // Project(name=kotlinx.serialization, language=Kotlin)
}
```

<!--- > You can get the full code [here](../../guide/example/example-json-07.kt). -->

<!---
```text
Project(name=kotlinx.serialization, language=Kotlin)
```
-->

<!--- TEST -->

You can use the `coerceInputValues` property together with the `explicitNulls` property to handle invalid enum values:

```kotlin
enum class Color { BLACK, WHITE }

@Serializable
data class Brush(val foreground: Color = Color.BLACK, val background: Color?)

val json = Json { 
  coerceInputValues = true
  explicitNulls = false
}

fun main() {

    // Decodes `foreground` to its default value and `background` to `null`
    val brush = json.decodeFromString<Brush>("""{"foreground":"pink", "background":"purple"}""")
    println(brush)
    // Brush(foreground=BLACK, background=null)
}
```

<!--- > You can get the full code [here](../../guide/example/example-json-08.kt). -->

<!---
```text
Brush(foreground=BLACK, background=null)
```
-->

<!--- TEST -->

## Encode structured map keys

The JSON format does not natively support maps with structured keys, as JSON keys are typically strings representing only primitives or enums.
You can use the [`allowStructuredMapKeys`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/allow-structured-map-keys.html) property to serialize maps with user-defined class keys:

```kotlin
val format = Json { allowStructuredMapKeys = true }

@Serializable
data class Project(val name: String)

fun main() {
    val map = mapOf(
        Project("kotlinx.serialization") to "Serialization",
        Project("kotlinx.coroutines") to "Coroutines"
    )
    // Serializes the map with structured keys as a JSON array:
    // `[key1, value1, key2, value2,...]`.
    println(format.encodeToString(map))
    // [{"name":"kotlinx.serialization"},"Serialization",{"name":"kotlinx.coroutines"},"Coroutines"]
}
```

<!--- > You can get the full code [here](../../guide/example/example-json-09.kt). -->

<!---
```text
[{"name":"kotlinx.serialization"},"Serialization",{"name":"kotlinx.coroutines"},"Coroutines"]
```
-->

<!--- TEST -->

## Encode special floating-point values

By default, special floating-point values like [`Double.NaN`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-double/-na-n.html)
and infinities are not supported in JSON because the JSON specification does not permit them.
You can allow their encoding using the [allowSpecialFloatingPointValues](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/allow-special-floating-point-values.html)
property:

```kotlin
// Configures a Json instance to allow special floating-point values
val format = Json { allowSpecialFloatingPointValues = true }

@Serializable
class Data(
    val value: Double
)

fun main() {
    val data = Data(Double.NaN)
    // This example produces the following non-standard JSON output, yet it is a widely used encoding for
    // special values in JVM world:
    println(format.encodeToString(data))
    // {"value":NaN}
}
```

<!--- > You can get the full code [here](../../guide/example/example-json-10.kt). -->

<!---
```text
{"value":NaN}
```
-->

<!--- TEST -->

## Specify class discriminator for polymorphism

When working with polymorphic data, you can use the
[`classDiscriminator`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/class-discriminator.html) property to specify a key name that indicates the type of the polymorphic object being serialized.
In combination with an [explicitly specified serial name using the `@SerialName` annotation](serialization-customization-options.md#customize-serial-names),
this approach provides full control over the resulting JSON structure:

```kotlin
// Configures a Json instance to use a custom class discriminator
val format = Json { classDiscriminator = "#class" }

@Serializable
sealed class Project {
    abstract val name: String
}

// Specifies a custom serial name for the OwnedProject class
@Serializable
@SerialName("owned")
class OwnedProject(override val name: String, val owner: String) : Project()

// Specifies a custom serial name for the SimpleProject class
@Serializable
@SerialName("simple")
class SimpleProject(override val name: String) : Project()

fun main() {
  val simpleProject: Project = SimpleProject("kotlinx.serialization")
  val ownedProject: Project = OwnedProject("kotlinx.coroutines", "kotlin")

  // Serializes SimpleProject with #class: "simple"
  println(format.encodeToString(simpleProject))
  // {"#class":"simple","name":"kotlinx.serialization"}

  // Serializes OwnedProject with #class: "owned"
  println(format.encodeToString(ownedProject))
  // {"#class":"owned","name":"kotlinx.coroutines","owner":"kotlin"}
}
```

<!--- > You can get the full code [here](../../guide/example/example-json-11.kt). -->

<!---
```text
{"#class":"simple","name":"kotlinx.serialization"}
{"#class":"owned","name":"kotlinx.coroutines","owner":"kotlin"}
```
-->

<!--- TEST -->

While the `classDiscriminator` property in a `Json` instance allows you to specify a single discriminator key for all polymorphic types, the [`@JsonClassDiscriminator`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-class-discriminator/) annotation provides greater flexibility.
It allows you to define a custom discriminator directly on the base class, which is automatically inherited by all its subclasses.
This behavior is enabled by the [`@InheritableSerialInfo`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-inheritable-serial-info/) meta-annotation:

```kotlin
// The @JsonClassDiscriminator annotation is inheritable, so all subclasses of `Base` will have the same discriminator
@Serializable
@JsonClassDiscriminator("message_type")
sealed class Base

// Class discriminator is inherited from Base
@Serializable
sealed class ErrorClass: Base()

// Defines a class that combines a message and an optional error
@Serializable
data class Message(val message: Base, val error: ErrorClass?)

@Serializable
@SerialName("my.app.BaseMessage")
data class BaseMessage(val message: String) : Base()

@Serializable
@SerialName("my.app.GenericError")
data class GenericError(@SerialName("error_code") val errorCode: Int) : ErrorClass()

val format = Json { classDiscriminator = "#class" }

fun main() {
    val data = Message(BaseMessage("not found"), GenericError(404))
    // The discriminator from the `Base` class is used
    println(format.encodeToString(data))
    // {"message":{"message_type":"my.app.BaseMessage","message":"not found"},"error":{"message_type":"my.app.GenericError","error_code":404}}
}
```

> It is not possible to specify different class discriminators explicitly within subclasses of a sealed base class.
> Only hierarchies with distinct, non-overlapping subclasses can have different discriminators.
> The discriminator specified in the `@JsonClassDiscriminator` annotation takes priority over any discriminator set in the `Json` configuration.
> 
{type="note"}

<!--- > You can get the full code [here](../../guide/example/example-json-12.kt). -->

<!---
```text
{"message":{"message_type":"my.app.BaseMessage","message":"not found"},"error":{"message_type":"my.app.GenericError","error_code":404}}
```
-->

<!--- TEST -->

### Set class discriminator output mode

You can use the [`JsonBuilder.classDiscriminatorMode`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/class-discriminator-mode.html) property to control how class discriminators are added to your JSON output.
As described in the [Specify class discriminator for polymorphism](#specify-class-discriminator-for-polymorphism) section, the default behavior adds the discriminator only for polymorphic types, which is useful when working with [polymorphic class hierarchies](polymorphism.md#sealed-classes).
Depending on your specific requirements, you can choose from the following [`ClassDiscriminatorMode`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-class-discriminator-mode/) options:

* [`POLYMORPHIC`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-class-discriminator-mode/-p-o-l-y-m-o-r-p-h-i-c/): Adds the class discriminator only for polymorphic types. This is the default behavior.
* [`ALL_JSON_OBJECTS`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-class-discriminator-mode/-a-l-l_-j-s-o-n_-o-b-j-e-c-t-s/): Adds the class discriminator to every JSON object, wherever possible.
* [`NONE`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-class-discriminator-mode/-n-o-n-e/): Omits the class discriminator in the output.


For example, setting the `ClassDiscriminatorMode` to `NONE` produces an output without a discriminator:

```kotlin
// Configures a Json instance to omit the class discriminator from the output
val format = Json { classDiscriminatorMode = ClassDiscriminatorMode.NONE }

@Serializable
sealed class Project {
    abstract val name: String
}

@Serializable
class OwnedProject(override val name: String, val owner: String) : Project()

fun main() {
    val data: Project = OwnedProject("kotlinx.coroutines", "kotlin")
    // Serializes without a discriminator
    println(format.encodeToString(data))
    // {"name":"kotlinx.coroutines","owner":"kotlin"}
}
```

> Without the discriminator, `kotlinx.serialization` cannot deserialize this output back into the appropriate type.
>
{type="note"}

<!--- > You can get the full code [here](../../guide/example/example-json-13.kt). -->

<!---
```text
{"name":"kotlinx.coroutines","owner":"kotlin"}
```
-->

<!--- TEST -->

## Naming conventions and formats

In some scenarios, JSON data may not perfectly align with Kotlin’s naming conventions or expected formats.
To address these challenges, `kotlinx.serialization` provides several tools to manage naming discrepancies,
handle multiple JSON field names, and ensure consistent naming strategies across your serialized data.

### Handle multiple JSON field names with @JsonNames

When JSON fields are renamed due to schema version changes,
you can [use the `@SerialName` annotation to change the name of a JSON field](serialization-customization-options.md#customize-serial-names).
However, this approach prevents decoding data with the old name.
To support multiple JSON names for a single Kotlin property, use the [`@JsonNames`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-names/) annotation:

```kotlin
@Serializable
// Maps both "name" and "title" JSON fields to the `name` property
data class Project(@JsonNames("title") val name: String)

fun main() {
    val project = Json.decodeFromString<Project>("""{"name":"kotlinx.serialization"}""")
    println(project)
    // Project(name=kotlinx.serialization)

    val oldProject = Json.decodeFromString<Project>("""{"title":"kotlinx.coroutines"}""")
    // Both `name` and `title` Json fields correspond to `name` property
    println(oldProject)
    // Project(name=kotlinx.coroutines)
}
```

<!--- > You can get the full code [here](../../guide/example/example-json-04.kt). -->

<!---
```text
Project(name=kotlinx.serialization)
Project(name=kotlinx.coroutines)
```
-->

> The `@JsonNames` annotation is enabled by the [`useAlternativeNames`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/use-alternative-names.html) property in [`JsonBuilder`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/).
> This property is set to `true` by default and allows Json to recognize and handle multiple names for a single property.
> If you are not using `@JsonNames` and want to optimize performance,
> especially when skipping many unknown fields with `ignoreUnknownKeys`, you can set this property to `false`.
>
{type="note"}

<!--- TEST -->

### Decode enums in a case-insensitive manner

[Kotlin's naming policy recommends](coding-conventions.md#property-names) naming enum values
using either uppercase underscore-separated names or upper camel case names.
By default, `Json` uses these exact Kotlin enum values names for decoding.
However, third-party JSONs might have enum values in lowercase or mixed case.
To handle such cases, you can configure the `Json` instance to decode enum values in a case-insensitive way using the [`JsonBuilder.decodeEnumsCaseInsensitive`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/decode-enums-case-insensitive.html) property:

```kotlin
// Configures a Json instance to decode enum values in a case-insensitive way
val format = Json { decodeEnumsCaseInsensitive = true }

enum class Cases { VALUE_A, @JsonNames("Alternative") VALUE_B }

@Serializable
data class CasesList(val cases: List<Cases>)

fun main() {
    // Decodes enum values regardless of their case, affecting both serial names and alternative names
    println(format.decodeFromString<CasesList>("""{"cases":["value_A", "alternative"]}""")) 
    // CasesList(cases=[VALUE_A, VALUE_B])
}
```

> This property affects both serial names and alternative names specified with the `@JsonNames` annotation,
> ensuring that all values are successfully decoded. This property does not affect encoding.
> 
{type="note"}

<!--- > You can get the full code [here](../../guide/example/example-json-14.kt). -->

<!---
```text
CasesList(cases=[VALUE_A, VALUE_B])
```
-->

<!--- TEST -->

### Apply a global naming strategy

When the property names in JSON input differ from those in Kotlin, you can specify the name for each property explicitly using the [`@SerialName`](serialization-customization-options.md#customize-serial-names) annotation.
However, in cases like migrating from other frameworks or a legacy codebase, you might need to apply a transformation to every serial name.
For these scenarios, you can specify a global naming strategy using the [JsonBuilder.namingStrategy](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/naming-strategy.html) property in a `Json` instance.
`kotlinx.serialization` provides a built-in strategy, [JsonNamingStrategy.SnakeCase](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-naming-strategy/-builtins/-snake-case.html):

```kotlin
@Serializable
data class Project(val projectName: String, val projectOwner: String)

// Configures a Json instance to apply SnakeCase naming strategy
val format = Json { namingStrategy = JsonNamingStrategy.SnakeCase }

fun main() {
    val project = format.decodeFromString<Project>("""{"project_name":"kotlinx.coroutines", "project_owner":"Kotlin"}""")
    // Serializes and deserializes as if all serial names are transformed from camel case to snake case
    println(format.encodeToString(project.copy(projectName = "kotlinx.serialization")))
    // {"project_name":"kotlinx.serialization","project_owner":"Kotlin"}
}
```

<!--- > You can get the full code [here](../../guide/example/example-json-15.kt). -->

<!---
```text
{"project_name":"kotlinx.serialization","project_owner":"Kotlin"}
```
-->

When using a global naming strategy like [JsonNamingStrategy](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-naming-strategy/), keep the following in mind:

* The naming strategy transformation applies to all properties, whether the serial name comes from the property name or is specified by the [`@SerialName`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-serial-name/) annotation. 
This means you cannot avoid the transformation by explicitly specifying a serial name.
To ensure certain names are kept during serialization, consider using the `@JsonNames` annotation instead.
* If a transformed name conflicts with other transformed property names or with alternative names specified by the `@JsonNames` annotation,
it will result in a deserialization exception.
* Global naming strategies are implicit.
By just looking at a class definition, it’s hard to tell what the serialized names will be.
This can make tasks like **Find Usages**, **Rename** in IDEs, or full-text search by tools like `grep` challenging, potentially introducing bugs or increasing maintenance efforts.

Given these factors, it’s important to weigh the pros and cons before implementing global naming strategies in your application.

<!--- TEST -->
