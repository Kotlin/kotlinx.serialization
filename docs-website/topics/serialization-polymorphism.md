[//]: # (title: Serialize polymorphic classes)

Polymorphism allows working with objects of different types through a common interface or base class.
Kotlin serialization supports subtype polymorphism, allowing different types to be serialized through a common declared supertype.

For an overview of how interfaces and inheritance work in Kotlin, see [Interfaces](interfaces.md) and [Inheritance](inheritance.md).

Kotlin serialization is _static_ by default.
It uses the _static type_ of the serialized value, which is the declared type of the variable, to determine which properties are encoded.

This means that only the properties defined in that static type are serialized, even if the value is an instance of a subclass at runtime.

Here's an example that uses a polymorphic class hierarchy:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
open class Project(val name: String)

class OwnedProject(name: String, val owner: String) : Project(name)

fun main() {
    // Uses Project as the declared static type
    val data: Project = OwnedProject("kotlinx.coroutines", "kotlin")

    // Serializes only properties defined in the static type
    println(Json.encodeToString(data))
    // {"name":"kotlinx.coroutines"}
}
```
{kotlin-runnable="true"}

In this example, even though the runtime value is the `OwnedProject` subclass, only the properties of the declared `Project` static type are serialized.

To serialize values in a polymorphic class hierarchy, Kotlin serialization provides two approaches:

* [Closed polymorphism](#serialize-closed-polymorphic-classes), where a `sealed` base class or interface ensures that all subclasses are known at compile time.
* [Open polymorphism](#serialize-open-polymorphic-classes), where you specify subclasses explicitly for an `open` or `abstract` base class.

## Serialize closed polymorphic classes

A class hierarchy is considered to use closed polymorphism when all possible subclasses are guaranteed to be known at compile time.

To provide this guarantee, [use a `sealed class` or `sealed interface`](sealed-classes.md) as the base type.
Mark it as `@Serializable`, and mark all subclasses with `@Serializable`:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
// Defines a sealed class as the base type
@Serializable
sealed class Project {
    abstract val name: String
}

// Marks the subclass as serializable
@Serializable
class OwnedProject(override val name: String, val owner: String) : Project()

// Serializes data using the base type as the static type
fun main() {
    // Uses the base type as the static type
    val data: Project = OwnedProject("kotlinx.coroutines", "kotlin")

    // A type property is added to identify the serialized subclass
    println(Json.encodeToString(data))
    // {"type":"OwnedProject","name":"kotlinx.coroutines","owner":"kotlin"}
}
//sampleEnd
```
{kotlin-runnable="true"}

By default, the `type` property in the JSON output acts as the class discriminator and identifies the serialized subclass.
This property is only included if you use the base class as the static type during serialization.

> You can also configure JSON to use a different key name for the class discriminator.
> For more information, see the [Specify class discriminator for polymorphism](serialization-json-configuration.md#specify-class-discriminator-for-polymorphism) section.
>
{style="tip"}

Here's an example where a subclass is the static type, so the type property is omitted:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
sealed class Project {
    abstract val name: String
}

@Serializable
class OwnedProject(override val name: String, val owner: String) : Project()

fun main() {
    // The inferred static type is OwnedProject
    val data = OwnedProject("kotlinx.coroutines", "kotlin")
    
    // The type property is omitted because the static type is OwnedProject
    println(Json.encodeToString(data))
    // {"name":"kotlinx.coroutines","owner":"kotlin"}
}
//sampleEnd
```
{kotlin-runnable="true"}

You can explicitly specify the base type when serializing objects to ensure the type property is included in the output.
To do so, pass the base type as a type argument to the `encodeToString()` function:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*


@Serializable
sealed class Project {
    abstract val name: String
}

@Serializable
class OwnedProject(override val name: String, val owner: String) : Project()

//sampleStart
fun main() {
    // The inferred static type is OwnedProject
    val data = OwnedProject("kotlinx.coroutines", "kotlin")

    // Specifies the base type explicitly to make use of polymorphism
    println(Json.encodeToString<Project>(data))
    // {"type":"OwnedProject","name":"kotlinx.coroutines","owner":"kotlin"}
}
//sampleEnd
```
{kotlin-runnable="true"}

## Define custom serial names for subclasses

By default, Kotlin serialization uses the fully qualified class name as the class discriminator value for polymorphic subclasses in both closed and open polymorphic hierarchies.
This value can change if you refactor the class.

To keep the class discriminator value stable, define a custom serial name with the [`@SerialName`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-serial-name/) annotation.

> Each serializable class must have a unique `@SerialName`.
> Don't reuse the same name for different classes, even across separate polymorphic hierarchies.
> 
> Otherwise, encoding or decoding functions throw an `IllegalStateException` if the same name is reused within the same hierarchy, while reusing the same name across separate hierarchies can lead to subtle serialization issues.
>
{style="warning"}

Use `@SerialName` to define a stable identifier for a subclass that doesn't depend on its source code:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
sealed class Project {
    abstract val name: String
}

// Assigns a custom serial name to the subclass
@Serializable
@SerialName("owned")
class OwnedProject(override val name: String, val owner: String) : Project()

fun main() {
    val data: Project = OwnedProject("kotlinx.coroutines", "kotlin")
    println(Json.encodeToString(data))
    // {"type":"owned","name":"kotlinx.coroutines","owner":"kotlin"}
}
//sampleEnd
```
{kotlin-runnable="true"}

## Base class properties with backing fields

In a polymorphic class hierarchy, the base class can define properties with backing fields that are serialized together with subclass properties, for example:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
sealed class Project {
    abstract val name: String
    var status = "open"
}
            
@Serializable   
@SerialName("owned")
class OwnedProject(override val name: String, val owner: String) : Project()

fun main() {
    // Configures a Json instance to encode default values
    val json = Json { encodeDefaults = true }

    val data: Project = OwnedProject("kotlinx.coroutines", "kotlin")
    // Serializes base class properties together with subclass properties
    println(json.encodeToString(data))
    // {"type":"owned","status":"open","name":"kotlinx.coroutines","owner":"kotlin"}
}
//sampleEnd
```
{kotlin-runnable="true"}

## Serialize objects in polymorphic class hierarchies

A polymorphic class hierarchy can have objects as subclasses.
During serialization, an object is treated similarly to a class without properties, and its class name is used as the value of the `type` property by default.

To include these objects in serialization, annotate them with `@Serializable`:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
// Defines a sealed base class
@Serializable
sealed class Response

// Defines an object subclass
@Serializable
object EmptyResponse : Response()

// Defines a class that extends Response
@Serializable   
class TextResponse(val text: String) : Response()

// Serializes a list containing different subclasses
fun main() {
    val list = listOf(EmptyResponse, TextResponse("OK"))
    println(Json.encodeToString(list))
    // [{"type":"EmptyResponse"},{"type":"TextResponse","text":"OK"}]
}
```
{kotlin-runnable="true"}

## Serialize open polymorphic classes

Kotlin serialization supports open polymorphism for `open` and `abstract` classes and interfaces.
In this model, subclasses can be defined anywhere in the codebase, even in other modules.
Since the compiler can't determine all subclasses at compile time, you must specify them explicitly.

Use open polymorphism when not all subclasses are known at compile time.
To register all subclasses at runtime, you have to specify the base type and every subclass in a [`SerializersModule`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.modules/-serializers-module/) class:

1. Create a `SerializersModule` with [`SerializersModule()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.modules/-serializers-module.html) builder function.
2. Inside the `SerializersModule`, use the [`polymorphic()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.modules/polymorphic.html) function to specify the base type.
3. Inside the `polymorphic()` block, _register_ each subclass using the [`subclass()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.modules/subclass.html) function.
   Registering a subclass adds it to the set of subclasses associated with the base type, which makes it available for polymorphic serialization and deserialization.

4. Add the `SerializersModule` to a `Json` instance with the `serializersModule` property.
5. Optionally, use the `@Serialname` annotation to define a stable identifier for a subclass instead of using its fully qualified class name.

To include the `type` property, use the base type as the static type, for example:

```kotlin 
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*

//sampleStart
// Defines a SerializersModule
val module = SerializersModule {

    // Specifies Project as the base type
    polymorphic(Project::class) {

        // Registers OwnedProject as a subclass of Project
        subclass(OwnedProject::class)
    }
}

// Adds the SerializersModule to a Json instance
val format = Json { serializersModule = module }

// Defines the base type used as the static type during serialization
@Serializable
abstract class Project {
    abstract val name: String
}

// Defines a serializable subclass of Project
@Serializable
@SerialName("owned")
class OwnedProject(override val name: String, val owner: String) : Project()

fun main() {
    // Uses Project as the static type to make use of polymorphism
    val data: Project = OwnedProject("kotlinx.coroutines", "kotlin")

    println(format.encodeToString(data))
    // {"type":"owned","name":"kotlinx.coroutines","owner":"kotlin"}
}
//sampleEnd
```
{kotlin-runnable="true"}

This configuration produces the same JSON structure as the example in the [Serialize closed polymorphic classes section](#serialize-closed-polymorphic-classes), but it supports `open` and `abstract` classes.

> This example works only on the JVM because of `serializer()` function restrictions.
> In Kotlin/JS and Kotlin/Native, use an explicit serializer: `format.encodeToString(PolymorphicSerializer(Project::class), data)`
> You can keep track of this issue on [GitHub](https://github.com/Kotlin/kotlinx.serialization/issues/1077).
>
{style="note"}

### Serialize interfaces in open polymorphic hierarchies

You can't annotate interfaces with `@Serializable`. Kotlin serialization treats interfaces as polymorphic, using [`PolymorphicSerializer`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-polymorphic-serializer/) as their serializer by default.

To use an interface as the base type in open polymorphic serialization, mark the classes that implement it with `@Serializable` and register them in a `SerializersModule`:

```kotlin 
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*

//sampleStart
// Defines a SerializersModule
val module = SerializersModule {
    polymorphic(Project::class) {
        subclass(OwnedProject::class)
    }
}

val format = Json { serializersModule = module }

// Defines an interface for polymorphic serialization
interface Project {
    val name: String
}

// OwnedProject implements the Project interface 
@Serializable
@SerialName("owned")
class OwnedProject(override val name: String, val owner: String) : Project

fun main() {
    // Uses the interface for serialization
    val data: Project = OwnedProject("kotlinx.coroutines", "kotlin")
    println(format.encodeToString(data))
    // {"type":"owned","name":"kotlinx.coroutines","owner":"kotlin"}
}
//sampleEnd
```
{kotlin-runnable="true"}

> In Kotlin/JS and Kotlin/Native, you must explicitly specify the serializer with
> `format.encodeToString(PolymorphicSerializer(Project::class), data)` because of the platform's limited reflection capabilities.
>
{style="note"}

You can also use an interface as a property in a serializable class.
In this case, Kotlin serialization also applies `PolymorphicSerializer` to that property:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*

val module = SerializersModule {
    polymorphic(Project::class) {
        subclass(OwnedProject::class)
    }
}

val format = Json { serializersModule = module }

//sampleStart
interface Project {
    val name: String
}

@Serializable
@SerialName("owned")
class OwnedProject(override val name: String, val owner: String) : Project

// Defines a serializable class with an interface property
@Serializable
class Data(val project: Project)

fun main() {
    val data = Data(OwnedProject("kotlinx.coroutines", "kotlin"))
    println(format.encodeToString(data))
    // {"project":{"type":"owned","name":"kotlinx.coroutines","owner":"kotlin"}}
}
//sampleEnd
```
{kotlin-runnable="true"}

### Select the base type of a polymorphic hierarchy

Kotlin serialization is fully static. It determines the base type of a polymorphic hierarchy at compile time.
You can select any base type, including `Any`, as long as you configure it explicitly.

To do so:

1. Specify the base type and its subclasses in a `SerializersModule`.
2. If the base type doesn't have its own serializer, pass `PolymorphicSerializer` for that type when calling the [`encodeToString()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json/encode-to-string.html) function.

Here's an example:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*

//sampleStart
val module = SerializersModule {
    // Registers OwnedProject for the Any base type
    polymorphic(Any::class) {
        subclass(OwnedProject::class)
    }
}

val format = Json { serializersModule = module }

@Serializable
abstract class Project {
    abstract val name: String
}

@Serializable
@SerialName("owned")
class OwnedProject(override val name: String, val owner: String) : Project()

fun main() {
    // Uses Any as the static type
    val data: Any = OwnedProject("kotlinx.coroutines", "kotlin")

    // Specifies a PolymorphicSerializer for the Any base type
    println(format.encodeToString(PolymorphicSerializer(Any::class), data))
    // {"type":"owned","name":"kotlinx.coroutines","owner":"kotlin"}
}
//sampleEnd
```
{kotlin-runnable="true"}

If you don't provide a `PolymorphicSerializer`, the following exception is thrown:

```text
Exception in thread "main" kotlinx.serialization.SerializationException: Serializer for class 'Any' is not found.
Please ensure that class is marked as '@Serializable' and that the serialization compiler plugin is applied.
```

You can also use multiple base types for the same subclass, as long as you register the subclass for each base type explicitly.

To avoid repeating `subclass()` calls for each base type, you can define a helper function that registers the subclasses and use it in each `polymorphic()` block.

Here's an example that uses both `Project` and `Any` as base types:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.reflect.KClass

//sampleStart
val module = SerializersModule {
    // Defines a helper function that registers subclasses
    fun PolymorphicModuleBuilder<Project>.registerProjectSubclasses() {
        subclass(OwnedProject::class)
    }
    // Registers the same subclass for each base type Project and Any
    polymorphic(Any::class) { registerProjectSubclasses() }
    polymorphic(Project::class) { registerProjectSubclasses() }
}
//sampleEnd

val format = Json { serializersModule = module }

interface Project {
    val name: String
}

@Serializable
@SerialName("owned")
class OwnedProject(override val name: String, val owner: String) : Project

@Serializable
class Data(
    val project: Project,
    @Polymorphic val any: Any
)

fun main() {
    val project = OwnedProject("kotlinx.coroutines", "kotlin")
    val data = Data(project, project)
    println(format.encodeToString(data))
    // {"project":{"type":"owned","name":"kotlinx.coroutines","owner":"kotlin"},"any":{"type":"owned","name":"kotlinx.coroutines","owner":"kotlin"}}
}
```
{kotlin-runnable="true"}

### Serialize properties polymorphically

Interfaces and abstract classes use polymorphic serialization by default.

To serialize a property with a non-serializable type, such as `Any`,
or when the property's type is an open class, annotate the property with [`@Polymorphic`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-polymorphic/).

This applies `PolymorphicSerializer` to the property.

Here's an example:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*

val module = SerializersModule {
    polymorphic(Any::class) {
        subclass(OwnedProject::class)
    }
}

val format = Json { serializersModule = module }

interface Project {
    val name: String
}

@Serializable
@SerialName("owned")
class OwnedProject(override val name: String, val owner: String) : Project

//sampleStart
@Serializable
class Data(
    // Applies PolymorphicSerializer to the property
    @Polymorphic
    val project: Any 
)

fun main() {
    val data = Data(OwnedProject("kotlinx.coroutines", "kotlin"))
    println(format.encodeToString(data))
    // {"project":{"type":"owned","name":"kotlinx.coroutines","owner":"kotlin"}}
}
//sampleEnd
```
{kotlin-runnable="true"}

### Register implementations of a sealed class or interface
<primary-label ref="experimental-opt-in"/>

You can combine open and closed polymorphism in the same hierarchy, for example when the root of the hierarchy is an `interface` and various sub-hierarchies are represented as sealed classes.
Instead of registering each sealed subclass individually, you can register all implementations of a sealed class or interface with the `subclassesOfSealed()` function.

This function also registers subclasses recursively when a subclass is itself a sealed serializable class.
All subclasses of that sealed class or interface must be either concrete or sealed. Otherwise, an `IllegalArgumentException` is thrown.

Here's an example:

```kotlin
@file:OptIn(ExperimentalSerializationApi::class)

// Imports declarations from the serialization library
import kotlinx.serialization.* 
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*

//sampleStart
interface Base

@Serializable
sealed interface Sub: Base

@Serializable
class Sub1(val data: String): Sub

val module1 = SerializersModule {
    polymorphic(Base::class) {
        subclassesOfSealed(Sub.serializer())
    }
}

val format1 = Json { serializersModule = module1 }

val module2 = SerializersModule {
    polymorphic(Base::class) {
        // Uses the reified version of subclassesOfSealed() to specify the same sealed type
        subclassesOfSealed<Sub>()
    }
}

val format2 = Json { serializersModule = module2 }


fun main() {
    val data: Base = Sub1("kotlin")
    println(format1.encodeToString(data))
    println(format2.encodeToString(data))
}
//sampleEnd
```
<!--{kotlin-runnable="true"} -->

### Serialize generic subtypes in a polymorphic hierarchy

Kotlin serialization can't automatically determine the concrete type of a generic type parameter at runtime.
As a result, it can't pick a serializer for that parameter without explicit configuration.

To provide this configuration for a generic polymorphic subtype,, register the subtype in a `SerializersModule` with an explicit serializer.
`PolymorphicSerializer(Any::class)` has the broadest scope, but you can use a more specific serializer when you know which concrete types the generic value can have.

> This configuration isn't needed when the generic type is serialized without polymorphism, or when the type parameter is part of the base type and the generated serializer can receive it directly.
> 
{style="note"}

Here's an example:

```kotlin 
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*

//sampleStart
@Serializable
abstract class Response<out T>

// Defines a generic polymorphic subtype
@Serializable
@SerialName("OkResponse")
data class OkResponse<out T>(val data: T) : Response<T>()

@Serializable
abstract class Project {
    abstract val name: String
}

// Defines a subtype used as a generic value
@Serializable
@SerialName("OwnedProject")
data class OwnedProject(override val name: String, val owner: String) : Project()

// Defines serializers for a polymorphic hierarchy with generic subtypes
val responseModule = SerializersModule {
    polymorphic(Response::class) {
        // Registers the generic subtype
        // with a serializer that specifies a PolymorphicSerializer
        subclass(OkResponse.serializer(PolymorphicSerializer(Any::class)))
    }
    polymorphic(Any::class) {
        // Registers the subtype used as the generic value
        subclass(OwnedProject::class)
    }
    polymorphic(Project::class) {
        // Registers the same subtype for the static base type
        subclass(OwnedProject::class)
    }
}

// Creates a Json instance with the registered serializers
val format = Json { serializersModule = responseModule }

fun main() {
    // Uses a generic polymorphic type with a concrete subtype
    val data: Response<Project> = OkResponse(OwnedProject("kotlinx.serialization", "kotlin"))

    val jsonString = format.encodeToString(data)
    println(jsonString)
    // {"type":"OkResponse","data":{"type":"OwnedProject","name":"kotlinx.serialization","owner":"kotlin"}}
   
    val deserializedData = format.decodeFromString<Response<Project>>(jsonString)
    println(deserializedData)
    // OkResponse(data=OwnedProject(name=kotlinx.serialization, owner=kotlin))
}
```
{kotlin-runnable="true"}

In this example, `PolymorphicSerializer(Any::class)` allows the generic subtype `OkResponse` to be serialized with
any value that's registered polymorphically as a subtype of `Any`.

### Merge multiple `SerializerModule` instances

As an application grows and splits into multiple source code modules, managing all class hierarchies within a single `SerializerModule` can become difficult.

You can merge multiple `SerializersModule` instances using the [`plus`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.modules/plus.html) operator,
allowing them to be used together in the same `Json` format instance.

Here's an example:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*

@Serializable
abstract class Response<out T>

@Serializable
data class OkResponse<out T>(val data: T) : Response<T>()

val responseModule = SerializersModule {
    polymorphic(Response::class) {
        subclass(OkResponse.serializer(PolymorphicSerializer(Any::class)))
    }
}

@Serializable
abstract class Project {
   abstract val name: String
}

@Serializable
data class OwnedProject(override val name: String, val owner: String) : Project()


val projectModule = SerializersModule {
    fun PolymorphicModuleBuilder<Project>.registerProjectSubclasses() {
        subclass(OwnedProject::class)
    }
    polymorphic(Any::class) { registerProjectSubclasses() }
    polymorphic(Project::class) { registerProjectSubclasses() }
}
//sampleStart
// Merges the SerializerModule instances from both hierarchies
val format = Json { serializersModule = projectModule + responseModule }
//sampleEnd

fun main() {
    val data: Response<Project> = OkResponse(OwnedProject("kotlinx.serialization", "kotlin"))
    val string = format.encodeToString(data)
    println(string)
    // {"type":"OkResponse","data":{"type":"OwnedProject","name":"kotlinx.serialization","owner":"kotlin"}}

    println(format.decodeFromString<Response<Project>>(string))
    // OkResponse(data=OwnedProject(name=kotlinx.serialization, owner=kotlin))
}
```
{kotlin-runnable="true"}

To merge modules inside a `SerializersModule` block, use the [`include()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.modules/-serializers-module-builder/include.html) function:

```kotlin
// Merges multiple SerializersModule instances using include()
val combinedModule = SerializersModule {
    include(projectModule)
    include(responseModule)
}
```

> If you expose the `SerializersModule` of your library or shared module, consumers can merge it with their own `SerializersModule`.
> 
{style="tip"}

### Deserialize unknown polymorphic subtypes with a default deserializer

When deserializing polymorphic data, Kotlin serialization resolves the subtype from the `type` property.
If the subtype isn't registered, deserialization fails with a `SerializationException`.

You can define a default deserializer to handle unregistered or unknown polymorphic subtypes.

Let's look at the following example where `Project` is the base type, `OwnedProject` is a registered subtype, and `BasicProject` represents unknown project subtypes:

```kotlin
@Serializable
abstract class Project {
    abstract val name: String
}

// Represents unknown project types
@Serializable
data class BasicProject(override val name: String, val type: String): Project()

@Serializable
@SerialName("OwnedProject")
data class OwnedProject(override val name: String, val owner: String) : Project()
```

To handle unknown polymorphic subtypes of `Project`, configure a default deserializer for the base type.
Use the [`defaultDeserializer()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.modules/-polymorphic-module-builder/default-deserializer.html) function inside the `polymorphic()` block to define a fallback for these unregistered subtypes:

```kotlin
val module = SerializersModule {
    polymorphic(Project::class) {
        subclass(OwnedProject::class)
        defaultDeserializer { BasicProject.serializer() }
    }
}
```

You can use this `SerializersModule` configuration to deserialize both registered and unregistered subtypes:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*

@Serializable
abstract class Project {
    abstract val name: String
}

// Represents unknown project types
@Serializable
data class BasicProject(override val name: String, val type: String): Project()

@Serializable
@SerialName("OwnedProject")
data class OwnedProject(override val name: String, val owner: String) : Project()

// Registers a default deserializer for unknown Project subtypes
val module = SerializersModule {
    polymorphic(Project::class) {
        subclass(OwnedProject::class)
        defaultDeserializer { BasicProject.serializer() }
    }
}

//sampleStart
val format = Json { serializersModule = module }

fun main() {
    // Deserializes both a known and an unknown Project subtype
    println(format.decodeFromString<List<Project>>("""
        [
            {"type":"unknown","name":"example"},
            {"type":"OwnedProject","name":"kotlinx.serialization","owner":"kotlin"} 
        ]
    """))
    // [BasicProject(name=example, type=unknown), OwnedProject(name=kotlinx.serialization, owner=kotlin)]
}
//sampleEnd
```
{kotlin-runnable="true"}

In this example, deserialization doesn't rely on the `type` field to differentiate subtypes and instead uses the plugin-generated serializer for `BasicProject`.
This approach assumes that unknown subtypes follow a known structure.

For JSON, you can also configure the format to [ignore unknown keys](serialization-json-configuration.md#ignore-unknown-keys) when unknown subtypes contain extra properties but still match the structure expected by the default deserializer.
If unknown input varies in structure, use a [custom serializer](create-custom-serializers.md) instead.

> For more flexible handling of JSON input during deserialization, see [Modify JSON structure](serialization-transform-json.md#modify-json-structure).
>
{style="tip"}

### Serialize polymorphic types with a default serializer

You can serialize values of a polymorphic base type without registering every concrete subtype.
Use this when you don't have access to the full type hierarchy, or when it changes a lot.

To do so:

1. Use the [`polymorphicDefaultSerializer()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.modules/-serializers-module-builder/polymorphic-default-serializer.html) function in a `SerializersModule` block.
2. Specify a lambda in `polymorphicDefaultSerializer()` that returns a [`SerializationStrategy`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-serialization-strategy/)` for the runtime value.

Let's look at an example with two private classes, `CatImpl` and `DogImpl`.
To avoid raising their visibility, register a default serializer for `Animal` that selects a serializer based on the runtime type through public interfaces:

```kotlin
interface Animal

interface Cat : Animal {
    val catType: String
}

interface Dog : Animal {
    val dogType: String
}

private class CatImpl : Cat {
    override val catType: String = "Tabby"
}

private class DogImpl : Dog {
    override val dogType: String = "Husky"
}

object AnimalProvider {
    fun createCat(): Cat = CatImpl()
    fun createDog(): Dog = DogImpl()
}

// Registers a default serializer for unknown Animal subtypes
val module = SerializersModule {
    polymorphicDefaultSerializer(Animal::class) { instance ->
        @Suppress("UNCHECKED_CAST")
        // Determines the appropriate serializer using a when block
        when (instance) {
            is Cat -> CatSerializer as SerializationStrategy<Animal>
            is Dog -> DogSerializer as SerializationStrategy<Animal>
            else -> null
        }
    }
}
```

Define serializers for `Cat` and `Dog`, then create a `Json` instance that uses the `SerializersModule` with the `serializersModule` property to enable polymorphic serialization of `Animal` values:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*

interface Animal

interface Cat : Animal {
    val catType: String
}

interface Dog : Animal {
    val dogType: String
}

private class CatImpl : Cat {
    override val catType: String = "Tabby"
}

private class DogImpl : Dog {
    override val dogType: String = "Husky"
}

object AnimalProvider {
    fun createCat(): Cat = CatImpl()
    fun createDog(): Dog = DogImpl()
}

// Registers a default serializer for unknown Animal subtypes
val module = SerializersModule {
    polymorphicDefaultSerializer(Animal::class) { instance ->
        @Suppress("UNCHECKED_CAST")
        // Determines the appropriate serializer using a when block based on the runtime value
        when (instance) {
            is Cat -> CatSerializer as SerializationStrategy<Animal>
            is Dog -> DogSerializer as SerializationStrategy<Animal>
            else -> null
        }
    }
}

//sampleStart
// Defines custom serializers
object CatSerializer : SerializationStrategy<Cat> {
    override val descriptor = buildClassSerialDescriptor("Cat") {
        element<String>("catType")
    }

    override fun serialize(encoder: Encoder, value: Cat) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.catType)
        }
    }
}

object DogSerializer : SerializationStrategy<Dog> {
    override val descriptor = buildClassSerialDescriptor("Dog") {
        element<String>("dogType")
    }

    override fun serialize(encoder: Encoder, value: Dog) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.dogType)
        }
    }
}

val format = Json { serializersModule = module }

fun main() {
    // Serializes an instance of Cat
    println(format.encodeToString<Animal>(AnimalProvider.createCat()))
    // {"type":"Cat","catType":"Tabby"}
}
//sampleEnd
```
{kotlin-runnable="true"}

## What's next

* Learn how to define and customize your own serializers in [Create custom serializers](serialization-custom-serializers.md).
