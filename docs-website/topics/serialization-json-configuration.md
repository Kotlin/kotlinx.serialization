[//]: # (title: Customize the Json instance)

The default [`Json`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json/-default/) instance strictly follows the JSON specification and the declarations in Kotlin classes.

You can use more flexible JSON features or type conversions by creating a custom `Json` instance with the [`Json()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json.html) builder function:

```kotlin
// Creates a Json instance based on the default configuration, allowing special floating-point values
val customJson = Json {
    allowSpecialFloatingPointValues = true
}

// Use the customJson instance with the same syntax as the default one to encode a string
val jsonString = customJson.encodeToString(Data(Double.NaN))
println(jsonString)
```

`Json` instances created this way are immutable and thread-safe, which makes them safe to store in a top-level property for reuse.

> Reusing custom `Json` instances improves performance by allowing them to cache class-specific information.
>
{style="tip"}

You can also base a new `Json` instance on an existing one and modify its settings using the same builder syntax:

```kotlin
// Creates a new instance based on an existing Json
val lenientJson = Json(customJson) {
    isLenient = true
    prettyPrint = true
}
```

The following sections cover the various `Json` class configuration features.

## Customize JSON structure

You can customize how a `Json` instance structures data during encoding and decoding.
This allows you to control which values appear in the output and how specific types are represented.

### Encode default values

By default, the JSON encoder omits default property values because they are automatically applied to missing properties during decoding.
This behavior is especially useful for nullable properties with null defaults, as it avoids writing unnecessary `null` values.
For more details, see the [Manage serialization of default properties](serialization-customization-options.md#manage-the-serialization-of-default-properties-with-encodeddefault) section.

To change this default behavior, set the [`encodeDefaults`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/encode-defaults.html) property to `true` in a `Json` instance:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
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
//sampleEnd
```
{kotlin-runnable="true"}

### Omit explicit nulls

By default, all `null` values are encoded into JSON output.
To omit `null` values, set the [`explicitNulls`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/explicit-nulls.html) property to `false` in a `Json` instance:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
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

    // Omits version, website, and description properties from the JSON output
    println(json)
    // {"name":"kotlinx.serialization","language":"Kotlin"}

    // Treats missing nullable properties without defaults as null
    // Fills properties that have defaults with their default values
    println(format.decodeFromString<Project>(json))
    // Project(name=kotlinx.serialization, language=Kotlin, version=1.2.2, website=null, description=null)
}
//sampleEnd
```
{kotlin-runnable="true"}

When `explicitNulls` is set to `false` encoding and decoding can become asymmetrical.
In this example, the `version` property is `null` before encoding but decodes to `1.2.2`.

> You can configure the decoder to handle certain invalid input values by treating them as missing properties with the [`coerceInputValues`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/coerce-input-values.html) property.
> For more information, see the [Coerce input values](#coerce-input-values) section.
>
{style="tip"}

### Allow structured map keys

The JSON format doesn't natively support maps with structured keys, because JSON keys are strings that represent only primitives or enums.
To serialize and deserialize maps with user-defined class keys, use the [`allowStructuredMapKeys`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/allow-structured-map-keys.html) property:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
// Configures a Json instance to encode maps with structured keys
val format = Json { allowStructuredMapKeys = true }

@Serializable
data class Project(val name: String)

fun main() {
    val map = mapOf(
        Project("kotlinx.serialization") to "Serialization",
        Project("kotlinx.coroutines") to "Coroutines"
    )
    // Serializes the map with structured keys as a JSON array:
    // [key1, value1, key2, value2,...]
    println(format.encodeToString(map))
    // [{"name":"kotlinx.serialization"},"Serialization",{"name":"kotlinx.coroutines"},"Coroutines"]
}
//sampleEnd
```
{kotlin-runnable="true"}

### Allow special floating-point values

By default, special floating-point values like [`Double.NaN`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-double/-na-n.html)
and infinities aren't supported in JSON because the JSON specification prohibits them.

To enable their encoding and decoding, set the [`allowSpecialFloatingPointValues`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/allow-special-floating-point-values.html) property to `true` in a `Json` instance:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
// Configures a Json instance to allow special floating-point values
val format = Json { allowSpecialFloatingPointValues = true }

@Serializable
class Data(
    val value: Double
)

fun main() {
    val data = Data(Double.NaN)
    // Produces a non-standard JSON output used for representing special floating-point values
    println(format.encodeToString(data))
    // {"value":NaN}
}
//sampleEnd
```
{kotlin-runnable="true"}

### Specify class discriminator for polymorphism

When working with [polymorphic data](serialization-polymorphism.md), you can use the
[`classDiscriminator`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/class-discriminator.html) property to specify a key name that identifies the type of the serialized polymorphic object.
Combined with an [explicit serial name defined with the `@SerialName` annotation](serialization-customization-options.md#customize-serial-names),
this approach gives you full control over the resulting JSON structure:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
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
//sampleEnd
```
{kotlin-runnable="true"}


While the `classDiscriminator` property in a `Json` instance lets you specify a single discriminator key for all polymorphic types, the [Experimental](components-stability.md#stability-levels-explained) [`@JsonClassDiscriminator`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-class-discriminator/) annotation offers more flexibility.
It allows you to define a custom discriminator directly on the base class, which is automatically inherited by all its subclasses.

> To learn more about inheritable serial annotations, see [`@InheritableSerialInfo`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-inheritable-serial-info/).
> 
{style="tip"}

Here's an example:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
// The @JsonClassDiscriminator annotation is inheritable, so all subclasses of Base will have the same discriminator
@Serializable
@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("message_type")
sealed class Base

// Inherits the discriminator from Base
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
    // Uses the discriminator from Base for all subclasses
    println(format.encodeToString(data))
    // {"message":{"message_type":"my.app.BaseMessage","message":"not found"},"error":{"message_type":"my.app.GenericError","error_code":404}}
}
//sampleEnd
```
{kotlin-runnable="true"}

> You can't specify different class discriminators in subclasses of a sealed base class.
> Only hierarchies with distinct, non-overlapping subclasses can define their own discriminators.
> 
> When both specify a discriminator, `@JsonClassDiscriminator` takes priority over the one in the `Json` configuration.
>
{style="note"}

### Set class discriminator output mode

Use the [`JsonBuilder.classDiscriminatorMode`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/class-discriminator-mode.html) property to control how class discriminators are added to your JSON output.
By default, the [discriminator is only added for polymorphic types](#specify-class-discriminator-for-polymorphism), which is useful when working with [polymorphic class hierarchies](serialization-polymorphism.md).

To adjust this behavior, set the [`ClassDiscriminatorMode`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-class-discriminator-mode/) property to one of these options:

* [`POLYMORPHIC`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-class-discriminator-mode/-p-o-l-y-m-o-r-p-h-i-c/): (Default) Adds the class discriminator only for polymorphic types.
* [`ALL_JSON_OBJECTS`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-class-discriminator-mode/-a-l-l_-j-s-o-n_-o-b-j-e-c-t-s/): Adds the class discriminator to all JSON objects, wherever possible.
* [`NONE`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-class-discriminator-mode/-n-o-n-e/): Omits the class discriminator entirely.

Here's an example with the `ClassDiscriminatorMode` property set to `NONE`:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
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
//sampleEnd
```
{kotlin-runnable="true"}

> Without the discriminator, the Kotlin serialization library can't deserialize this output back into the appropriate type.
>
{style="note"}

## Customize JSON deserialization

Kotlin's `Json` parser provides several settings that let you customize how JSON data is parsed and deserialized.

### Coerce input values

When working with JSON data from third-party services or other dynamic sources, the format can evolve over time.
This may cause exceptions during decoding when actual values don't match the expected types.

The default `Json` implementation is [strict about input types](serialization-customization-options.md#the-serializable-annotation).
To relax this restriction, set the [`coerceInputValues`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/coerce-input-values.html) property to `true` in a `Json` instance:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
val format = Json { coerceInputValues = true }

@Serializable
data class Project(val name: String, val language: String = "Kotlin")

fun main() {
    val data = format.decodeFromString<Project>("""
        {"name":"kotlinx.serialization","language":null}
    """)

    // Coerces the invalid null value for language to its default value
    println(data)
    // Project(name=kotlinx.serialization, language=Kotlin)
}
//sampleEnd
```
{kotlin-runnable="true"}

The `coerceInputValues` property only affects decoding. It treats certain invalid input values as if the corresponding property were missing.
Currently, it applies to:

* `null` inputs for non-nullable types
* unknown values for enums

> This list may be expanded in future versions, making `Json` instances with this property even more permissive
> by replacing invalid values with defaults or `null`.
>
{style="note"}

If a value is missing, it's replaced with a default property value if one exists.

For enums the value is replaced with `null` only if:

* No default is defined.
* The [`explicitNulls`](#omit-explicit-nulls) property is set to `false`.
* The property is nullable.

You can combine `coerceInputValues` with the `explicitNulls` property to handle invalid enum values:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
enum class Color { BLACK, WHITE }

@Serializable
data class Brush(val foreground: Color = Color.BLACK, val background: Color?)

val json = Json { 
  coerceInputValues = true
  explicitNulls = false
}

fun main() {

    // Coerces the unknown foreground value to its default and background to null
    val brush = json.decodeFromString<Brush>("""{"foreground":"pink", "background":"purple"}""")
    println(brush)
    // Brush(foreground=BLACK, background=null)
}
//sampleEnd
```
{kotlin-runnable="true"}

### Lenient parsing

By default, the `Json` parser enforces strict JSON rules to ensure compliance with the [RFC-4627](https://www.ietf.org/rfc/rfc4627.txt) specification, 
which requires keys and string literals to be quoted.

To relax these restrictions, set the [`isLenient`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/is-lenient.html) property to `true` in a `Json` instance:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
val format = Json { isLenient = true }

enum class Status { SUPPORTED }

@Serializable
data class Project(val name: String, val status: Status, val votes: Int)

fun main() {
    // Decodes a JSON string with lenient parsing
    // Lenient parsing allows unquoted keys, string and enum values
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
//sampleEnd
```
{kotlin-runnable="true"}

### Allow trailing commas

To allow trailing commas in JSON input, set the [`allowTrailingComma`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/allow-trailing-comma.html) property to `true`:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
// Allows trailing commas in JSON objects and arrays
val format = Json { allowTrailingComma = true }

fun main() {
    val numbers = format.decodeFromString<List<Int>>(
        """
            [1, 2, 3,]
        """
    )
    println(numbers)
    // [1, 2, 3]
}
//sampleEnd
```
{kotlin-runnable="true"}

### Allow comments in JSON

Use the [`allowComments`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/allow-comments.html) property to allow comments in JSON input.
When this property is enabled, the parser accepts the following comment forms in the input:

* `//` line comments that end at a newline `\n`
* `/* */` block comments

Here's an example:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
// Allows comments in JSON input
val format = Json { allowComments = true }

fun main() {
    val numbers = format.decodeFromString<List<Int>>(
        """
            [
                // first element
                1,
                /* second element */
                2
            ]
        """
    )
    println(numbers)
    // [1, 2]
}
//sampleEnd
```
{kotlin-runnable="true"}

### Ignore unknown keys

When working with JSON data from third-party services or other dynamic sources, new properties may be added to the JSON objects over time.

By default, unknown keys (the property names in the JSON input) result in an error during deserialization.
To prevent this, set the [`ignoreUnknownKeys`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/ignore-unknown-keys.html) property
to `true` in a `Json` instance:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
// Creates a Json instance to ignore unknown keys
val format = Json { ignoreUnknownKeys = true }

@Serializable
data class Project(val name: String)

fun main() {
    val data = format.decodeFromString<Project>("""
        {"name":"kotlinx.serialization","language":"Kotlin"}
    """)
    // The language key is ignored because it's not in the Project class
    println(data)
    // Project(name=kotlinx.serialization)
}
//sampleEnd
```
{kotlin-runnable="true"}

#### Ignore unknown keys for specific classes
<primary-label ref="experimental-general"/>

> This feature is [Experimental](components-stability.md#stability-levels-explained). To opt in, use the `@OptIn(ExperimentalSerializationApi::class)` annotation or the compiler option `-opt-in=kotlinx.serialization.ExperimentalSerializationApi`.
> 
{style="warning"}

Instead of [enabling `ignoreUnknownKeys` for all classes](#ignore-unknown-keys),
you can ignore unknown keys only for specific classes by annotating them with the `@JsonIgnoreUnknownKeys` annotation.
This lets you keep strict deserialization by default while allowing leniency only where you need it.

Here's an example:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
@OptIn(ExperimentalSerializationApi::class)
@Serializable
// Unknown properties in Outer are ignored during deserialization
@JsonIgnoreUnknownKeys
data class Outer(val a: Int, val inner: Inner)

@Serializable
data class Inner(val x: String)

fun main() {
    val outer = Json.decodeFromString<Outer>(
        """{"a":1,"inner":{"x":"value"},"unknownKey":42}"""
    )
    println(outer)
    // Outer(a=1, inner=Inner(x=value))

    // Throws an exception
    // unknownKey inside inner is NOT ignored because Inner is not annotated
    println(
        Json.decodeFromString<Outer>(
            """{"a":1,"inner":{"x":"value","unknownKey":"unexpected"}}"""
        )
    )
}
//sampleEnd
```
{kotlin-runnable="true" min-compiler-version="2.2" validate="false"}

In this example, `Inner` throws a `SerializationException` for unknown keys because it isn't annotated with `@JsonIgnoreUnknownKeys`.

## Customize name mapping between JSON and Kotlin

Some JSON data may not perfectly align with Kotlin's naming conventions or expected formats.
To address these challenges, the Kotlin serialization library provides several tools to manage naming discrepancies,
handle multiple JSON property names, and ensure consistent naming strategies across serialized data.

### Accept alternative JSON property names for a single Kotlin property

When JSON property names change between schema versions,
you can [use the `@SerialName` annotation to rename a JSON property](serialization-customization-options.md#customize-serial-names).

However, this prevents decoding data that still uses previous property names. 
To accept alternative JSON property names for a single property, use the [`@JsonNames`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-names/) annotation:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
// Maps both name and title JSON properties to the name property
data class Project(@JsonNames("title") val name: String)

fun main() {
    val project = Json.decodeFromString<Project>("""{"name":"kotlinx.serialization"}""")
    println(project)
    // Project(name=kotlinx.serialization)

    val oldProject = Json.decodeFromString<Project>("""{"title":"kotlinx.coroutines"}""")
    // Both name and title Json properties correspond to name property
    println(oldProject)
    // Project(name=kotlinx.coroutines)
}
//sampleEnd
```
{kotlin-runnable="true"}

> The `@JsonNames` annotation is enabled by the [`useAlternativeNames`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/use-alternative-names.html) property in [`JsonBuilder`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/).
> This property is set to `true` by default and allows `Json` to recognize and decode multiple names for a single property.
> 
> If you aren't using `@JsonNames` and want to improve performance,
> especially when skipping many unknown properties with `ignoreUnknownKeys`, you can set this property to `false`.
>
{style="note"}

### Decode enums in a case-insensitive manner

[Kotlin's naming conventions](coding-conventions.md#property-names) recommend naming enum values
in uppercase with underscores or in upper camel case.
By default, `Json` uses the exact names of Kotlin enum constants when decoding.

However, JSON data from external sources might use lowercase or mixed-case names.
To handle such cases, configure the `Json` instance to decode enum values case-insensitively with the [`JsonBuilder.decodeEnumsCaseInsensitive`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/decode-enums-case-insensitive.html) property:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
// Configures a Json instance to decode enum values in a case-insensitive way
val format = Json { decodeEnumsCaseInsensitive = true }

enum class Cases { VALUE_A, @JsonNames("Alternative") VALUE_B }

@Serializable
data class CasesList(val cases: List<Cases>)

fun main() {
    // Decodes enum values regardless of their case, including alternative names
    println(format.decodeFromString<CasesList>("""{"cases":["value_A", "alternative"]}""")) 
    // CasesList(cases=[VALUE_A, VALUE_B])
}
//sampleEnd
```
{kotlin-runnable="true"}

> This property applies to both [serial names](serialization-customization-options.md#customize-serial-names) and [alternative names](#accept-alternative-json-property-names-for-a-single-kotlin-property) specified with the `@JsonNames` annotation,
> ensuring that all values are decoded successfully. This property doesn't affect encoding.
>
{style="note"}

### Apply a global naming strategy

When property names in JSON input differ from those in Kotlin, you can specify the name of each property explicitly using the [`@SerialName`](serialization-customization-options.md#customize-serial-names) annotation.
However, when migrating from other frameworks or a legacy codebase, you might need to transform every serial name in the same way.

For these scenarios, you can specify a global naming strategy using the [JsonBuilder.namingStrategy](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/naming-strategy.html) property in a `Json` instance.
The Kotlin serialization library provides built-in strategies, such as [JsonNamingStrategy.SnakeCase](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-naming-strategy/-builtins/-snake-case.html):

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
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
//sampleEnd
```
{kotlin-runnable="true"}

When using a global naming strategy with [`JsonNamingStrategy`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-naming-strategy/), keep the following in mind:

* **The transformation applies to all properties**, whether the serial name is derived from the property name or explicitly defined with the [`@SerialName`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-serial-name/) annotation.
  You can't exclude a property from transformation by specifying a serial name.
  To keep specific names unchanged during serialization, use the `@JsonNames` annotation instead.
* If a transformed name conflicts with other transformed property names or with alternative names specified by the `@JsonNames` annotation,
  deserialization fails with an exception.
* Global naming strategies are implicit.
  It makes it difficult to determine the serialized names by looking at the class definition.
  This can make tasks like **Find Usages**, **Rename** in IDEs, or full-text searches using tools like `grep`, more difficult, potentially increasing the risk of bugs and maintenance costs.

Given these factors, consider the trade-offs carefully before implementing global naming strategies in your application.

## Pretty printing

By default, `Json` produces a compact, single-line output.

You can add indentations and line breaks for better readability by enabling pretty printing in the output.
To do so, set the [`prettyPrint`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/pretty-print.html) property to `true` in a `Json` instance:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
// Creates a custom Json format
val format = Json { prettyPrint = true }

@Serializable
data class Project(val name: String, val language: String)

fun main() {
    val data = Project("kotlinx.serialization", "Kotlin")

    // Prints the JSON output with line breaks and indentations
    println(format.encodeToString(data))
}
//sampleEnd
```
{kotlin-runnable="true"}

This example prints the following result:

```text
{
    "name": "kotlinx.serialization",
    "language": "Kotlin"
}
```

> You can use the [`prettyPrintIndent`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/pretty-print-indent.html) option to customize the indentation used in pretty-printed JSON.
>
> For example, you can replace the default four spaces with any allowed whitespace characters, such as `\t` or `\n`.
>
{style="note"}

## What's next

* Explore [advanced JSON element handling](serialization-json-elements.md) to manipulate and work with JSON data before it's parsed or serialized.
* Discover how to [transform JSON during serialization and deserialization](serialization-transform-json.md) for more control over your data.
