[//]: # (title: Serialize polymorphic classes)

Polymorphism allows objects of different types to be treated as objects of a common supertype.
This enables you to work with various subclasses through a shared interface or superclass.

You can use polymorphism to design flexible and reusable code when the exact type isn't known at compile time.
In Kotlin serialization, polymorphism helps you manage data structures whose runtime type can vary.

## Closed polymorphism in Kotlin serialization

A class hierarchy is considered to use closed polymorphism when all possible subclasses are guaranteed to be known at compile-time.

Kotlin serialization is _static_ by default.
It uses the _static type_ of the serialized value, which is the declared type of the variable, to determine which properties are encoded.
This means that only the properties defined in that static type are serialized, even if the value is an instance of a subclass at runtime.

Because of this, closed polymorphic serialization requires using a base type where all subclasses are known at compile time.
To provide this structure, [use a `sealed class` as the base type](#serialize-closed-polymorphic-classes).

> You can also serialize hierarchies based on `open` or `abstract` classes by using [open polymorphism with explicit configuration](#open-polymorphism-in-kotlin-serialization).
>
{style="tip"}

### Serialize closed polymorphic classes

To serialize a closed polymorphic hierarchy, use a `sealed class` as the base type and mark all subclasses with `@Serializable`:

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

// Marks a sealed subclass as serializable
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

The `type` property in the JSON output identifies the serialized subclass.
This property is only included if you use the base class as the static type during serialization.

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
    // Sets the static type as OwnedProject
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

//sampleStart
@Serializable
sealed class Project {
    abstract val name: String
}
            
@Serializable
class OwnedProject(override val name: String, val owner: String) : Project()

fun main() {
    // Sets the static type as OwnedProject
    val data = OwnedProject("kotlinx.coroutines", "kotlin")
   
    // Specifies the base type explicitly to include the type property
    println(Json.encodeToString<Project>(data))
    // {"type":"OwnedProject","name":"kotlinx.coroutines","owner":"kotlin"}
}
//sampleEnd
```
{kotlin-runnable="true"}

#### Define custom serial names for subclasses

By default, Kotlin serialization uses the class name as the type discriminator property value for polymorphic subclasses.

You can [customize their serial names](serialization-customization-options.md#customize-serial-names) with the [`@SerialName`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-serial-name/) annotation.
Use it to define a stable identifier for a subclass that does not depend on its source code:

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

> You can also configure JSON to use a different key name for the class discriminator.
> For more information, see the [Specify class discriminator for polymorphism](serialization-json-configuration.md#specify-class-discriminator-for-polymorphism) section.
>
{style="tip"}

#### Base class properties with backing fields

In a sealed class hierarchy, the base class can define properties with backing fields that are serialized together with subclass properties, for example:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
sealed class Project {
    abstract val name: String
    // Defines a base-class property with a backing field
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

#### Serialize objects in sealed class hierarchies

A sealed class hierarchy can have objects as subclasses.
During serialization, an object is treated as a class without properties, and its class name is used as the value of the `type` property by default.

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

> The properties of objects are not serialized.
>
{style="note"}

## Open polymorphism in Kotlin serialization

Kotlin serialization supports open polymorphism for `open` and `abstract` classes.
In this model, subclasses can be defined anywhere in the codebase, even in other modules.
Since the compiler can't determine all subclasses at compile time, you must specify them explicitly.

### Serialize open polymorphic classes

Use open polymorphism when not all subclasses are known at compile time.
Specify the base type and every subclass in a [`SerializersModule`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.modules/-serializers-module/) class.
To do so:

1. Create a `SerializersModule` with [`SerializersModule()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.modules/-serializers-module.html) builder function.
2. Inside the `SerializersModule`, use the [`polymorphic()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.modules/polymorphic.html) function to specify the base type.
3. Inside the `polymorphic()` block, _register_ each subclass using the [`subclass()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.modules/subclass.html) function. Registering a subclass adds it to the set of subclasses associated with the base type, so the serialization framework can serialize and deserialize it when the static type is the base type.
4. Add the `SerializersModule` to a `Json` instance with the `serializersModule` property.

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
    // Uses Project as the static type to include the type property
    val data: Project = OwnedProject("kotlinx.coroutines", "kotlin")

    println(format.encodeToString(data))
    // {"type":"owned","name":"kotlinx.coroutines","owner":"kotlin"}
}
//sampleEnd
```
{kotlin-runnable="true"}

This configuration produces the same JSON structure as [using sealed classes for closed polymorphic classes](#serialize-closed-polymorphic-classes), but it supports open polymorphic classes.

> This example works only on the JVM because of `serializer()` function restrictions.
> In Kotlin/JS and Kotlin/Native, use an explicit serializer: `format.encodeToString(PolymorphicSerializer(Project::class), data)`
> You can keep track of this issue on [GitHub](https://github.com/Kotlin/kotlinx.serialization/issues/1077).
>
{style="note"}

### Register subclasses for multiple base types

When the same subclass is serialized using different static types, register it for each base type in the `SerializersModule` with the `subclass()` function.

To avoid repeating `subclass()` calls for each base type, you can define a helper function that registers the subclasses and use it in each `polymorphic()` block.

Here's an example:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.reflect.KClass

//sampleStart
val module = SerializersModule {
    // Defines a helper function
    // that registers subclasses for the Project base type
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

#### Register sealed children as subclasses
<primary-label ref="experimental-opt-in"/>

To register all implementations of a sealed class or interface, use the `subclassesOfSealed()` function:

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
<!-- I couldn't get this to work on the playground yet so no kotlin-runnable=true, but it works locally -->

### Serialize interfaces

Although you can't annotate an interface with `@Serializable`, you can serialize interfaces using polymorphism.

To do so, mark the classes that implement the interface as `@Serializable` and register them in a `SerializersModule`:

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

> In Kotlin/Native, you must explicitly specify the serializer with
> `format.encodeToString(PolymorphicSerializer(Project::class), data)` because of the platform's limited reflection capabilities.
>
{style="note"}

You can also use an interface as a property in a serializable class:

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

### Serialize polymorphic types with generic base types

Kotlin serialization is fully static, so it determines the base type of a polymorphic hierarchy at compile time.
When the base type is generic, such as `Any`, the serializer can't resolve the concrete subclass without explicit configuration.

To serialize values with a generic base type:

1. Specify the generic base type and its subclasses in a `SerializersModule`.
2. Use `PolymorphicSerializer` as the base type when calling the [`encodeToString()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json/encode-to-string.html) function.

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

If you don't register the subclass or don't provide a `PolymorphicSerializer`, the following exception is thrown:

```text
Exception in thread "main" kotlinx.serialization.SerializationException: Serializer for class 'Any' is not found.
Please ensure that class is marked as '@Serializable' and that the serialization compiler plugin is applied.
```

### Serialize non-serializable properties polymorphically

To serialize a property with a non-serializable type, such as `Any`, annotate the property with
[`@Polymorphic`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-polymorphic/).
This applies [`PolymorphicSerializer`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-polymorphic-serializer/) to the property.

Here's an example:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*

//sampleStart
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

### Serialize generic subtypes in a polymorphic hierarchy

Kotlin serialization can't automatically determine the concrete type of a generic type parameter at runtime.
As a result, it can't pick a serializer for that parameter without explicit configuration.

To provide this configuration, register the subtype in a `SerializersModule` using a serializer that specifies a `PolymorphicSerializer` for the generic parameter.

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

// Defines a concrete generic subtype used as a generic value
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

//sampleStart
@Serializable
abstract class Response<out T>

@Serializable
@SerialName("OkResponse")
data class OkResponse<out T>(val data: T) : Response<T>()

val responseModule = SerializersModule {
    polymorphic(Response::class) {
        subclass(OkResponse.serializer(PolymorphicSerializer(Any::class)))
    }
}

val projectModule = SerializersModule {
    fun PolymorphicModuleBuilder<Project>.registerProjectSubclasses() {
        subclass(OwnedProject::class)
    }
    polymorphic(Any::class) { registerProjectSubclasses() }
    polymorphic(Project::class) { registerProjectSubclasses() }
}

@Serializable
abstract class Project {
    abstract val name: String
}

@Serializable
@SerialName("OwnedProject")
data class OwnedProject(override val name: String, val owner: String) : Project()

// Merges the SerializerModule instances from both hierarchies
val format = Json { serializersModule = projectModule + responseModule }
//sampleEnd

fun main() {
    val data: Response<Project> =  OkResponse(OwnedProject("kotlinx.serialization", "kotlin"))
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
If the subtype isn't registered, deserialization fails with a `JsonDecodingException`.

To handle unknown polymorphic subtypes, configure a default deserializer for the base type.
Use the [`defaultDeserializer()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.modules/-polymorphic-module-builder/default-deserializer.html) function inside the `polymorphic()` block to define a fallback for unregistered subtypes.

Let's look at an example where deserialization uses `BasicProject` for unknown subtypes:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*

//sampleStart
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

In this example, deserialization doesn't rely on the `type` field to differentiate subtypes and instead uses the plugin-generated serializer for `BasicProject`.
This approach assumes that unknown subtypes follow a known structure.
If unknown input varies in structure, use a [custom serializer](create-custom-serializers.md) instead.

> For more flexible handling of JSON input during deserialization, see the [Maintain custom JSON attributes](serialization-transform-json.md#maintain-custom-json-attributes) section.
>
{style="tip"}

### Serialize polymorphic types with a default serializer

To serialize values of a polymorphic base type without registering every concrete subtype:

1. Use the [`polymorphicDefaultSerializer()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.modules/-serializers-module-builder/polymorphic-default-serializer.html) function in a `SerializersModule` block.
2. Specify a lambda in `polymorphicDefaultSerializer()` that returns a [`SerializationStrategy`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-serialization-strategy/)` for the runtime value.

Here's an example: 

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*

// sampleStart
interface Animal {
}

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
