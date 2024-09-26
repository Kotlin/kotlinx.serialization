[//]: # (title: Serialize polymorphic classes)

<!--- TEST_NAME PolymorphismTest -->

Polymorphism allows objects of different types to be treated as objects of a common supertype.
In Kotlin, polymorphism enables you to work with various subclasses through a shared interface or superclass.
This is useful for designing flexible and reusable code, especially when the exact type of objects isn't known at compile time.
In the context of serialization, polymorphism helps manage data structures where the runtime type of data can vary.

## Closed polymorphism in Kotlin serialization

A class hierarchy is considered to use closed polymorphism when all possible subclasses are guaranteed to be known at compile-time.
This makes it a reliable and predictable approach for serializing and deserializing polymorphic data structures.
The best way to serialize closed polymorphic classes is to use [`sealed classes`](#serialize-closed-polymorphic-classes), which restrict subclassing to the same file.

Kotlin Serialization is fully static by default, where the structure of encoded objects is based on their compile-time types.
This means that only the properties defined in the static type are serialized, even if the object is initialized with a subclass at runtime.

When serializing open classes, only the properties of the base class are serialized, and any additional properties in subclasses are ignored:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
// Defines an open class Project with a name property
@Serializable
open class Project(val name: String)

// Defines a derived class OwnedProject with an additional owner property
class OwnedProject(name: String, val owner: String) : Project(name)

fun main() {
    val data: Project = OwnedProject("kotlinx.coroutines", "kotlin")
    // Serializes data based on the static type Project, ignoring the OwnedProject properties
    println(Json.encodeToString(data))
    // {"name":"kotlinx.coroutines"}
}
//sampleEnd
```
{kotlin-runnable="true"}

<!--- > You can get the full code [here](../../guide/example/example-poly-01.kt). -->

<!---
```text
{"name":"kotlinx.coroutines"}
```
-->

<!--- TEST -->

Alternatively, you might consider defining the base class as an [`abstract class`](classes.md#abstract-classes) with abstract properties,
allowing subclasses to provide actual values for those properties.
However, this approach still doesn't resolve the issue with serialization,
and attempting to serialize instances of subclasses will result in an exception:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
abstract class Project {
    abstract val name: String
}

class OwnedProject(override val name: String, val owner: String) : Project()

fun main() {
    val data: Project = OwnedProject("kotlinx.coroutines", "kotlin")
    println(Json.encodeToString(data))
    // Exception in thread "main" kotlinx.serialization.SerializationException: Serializer for subclass 'OwnedProject' is not found in the polymorphic scope of 'Project'.
}
//sampleEnd
```
{kotlin-runnable="true"}

<!---  > You can get the full code [here](../../guide/example/example-poly-03.kt). -->

<!--- 
```text 
Exception in thread "main" kotlinx.serialization.SerializationException: Serializer for subclass 'OwnedProject' is not found in the polymorphic scope of 'Project'.
Check if class with serial name 'OwnedProject' exists and serializer is registered in a corresponding SerializersModule.
To be registered automatically, class 'OwnedProject' has to be '@Serializable', and the base class 'Project' has to be sealed and '@Serializable'.
```
-->

<!--- TEST LINES_START -->

This exception highlights that using an `abstract class` still doesn't solve the issue.
To fix this and ensure proper serialization of subclasses, you need to [use sealed classes](#serialize-closed-polymorphic-classes),
which provide the necessary compile-time guarantees for closed polymorphic serialization.

> To handle polymorphism with `open` and `abstract` classes, you must use [open polymorphism with explicit configuration](#open-polymorphism-in-kotlin-serialization).
>
{type="note"}

### Serialize closed polymorphic classes

You can serialize closed polymorphic classes by using a `sealed class` as the base.
All subclasses of a sealed class must be explicitly marked as `@Serializable`.
This approach ensures that polymorphism is represented in JSON with a type discriminator:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
sealed class Project {
    abstract val name: String
}

@Serializable
class OwnedProject(override val name: String, val owner: String) : Project()

// Serializes data of compile-time type Project
fun main() {
    val data: Project = OwnedProject("kotlinx.coroutines", "kotlin")
    // A `type` key is added to the resulting JSON object as a discriminator.
    println(Json.encodeToString(data))
    // {"type":"OwnedProject","name":"kotlinx.coroutines","owner":"kotlin"}
}
//sampleEnd
```
{kotlin-runnable="true"}

<!--- > You can get the full code [here](../../guide/example/example-poly-04.kt). -->

<!---
```text 
{"type":"OwnedProject","name":"kotlinx.coroutines","owner":"kotlin"}
```
-->

<!--- TEST -->

The `type` key in the JSON output identifies the specific subclass being serialized,
which is useful for distinguishing subclasses in polymorphic hierarchies.

It's important to ensure that the [static type](#closed-polymorphism-in-kotlin-serialization) of the serialized object refers to the base class,
which allows the `type` discriminator to be included in the JSON output.
If the static type refers to a subclass, the discriminator is omitted:

```kotlin
// Imports the necessary libraries
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
    // The static type is OwnedProject
    val data = OwnedProject("kotlinx.coroutines", "kotlin")
    
    // The `type` discriminator is not included because the static type is OwnedProject.
    println(Json.encodeToString(data))
    // {"name":"kotlinx.coroutines","owner":"kotlin"}
}
//sampleEnd
```
{kotlin-runnable="true"}

<!--- > You can get the full code [here](../../guide/example/example-poly-05.kt). -->

<!---
```text 
{"name":"kotlinx.coroutines","owner":"kotlin"}
```
-->

<!--- TEST -->

To ensure consistency in polymorphic serialization, the type used during serialization must match the expected type during deserialization.
You can explicitly specify the base type when serializing objects to ensure the `type` discriminator is included in the output:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
sealed class Project {
    abstract val name: String
}
            
@Serializable
class OwnedProject(override val name: String, val owner: String) : Project()

fun main() {
    // Sets the static type as OwnedProject
    val data = OwnedProject("kotlinx.coroutines", "kotlin")

//sampleStart    
    // Specifies the base type Project, which includes the `type` discriminator in the output.
    println(Json.encodeToString<Project>(data))
    // {"type":"OwnedProject","name":"kotlinx.coroutines","owner":"kotlin"}
}
//sampleEnd
```
{kotlin-runnable="true"}

<!--- > You can get the full code [here](../../guide/example/example-poly-06.kt). -->

<!---
```text 
{"type":"OwnedProject","name":"kotlinx.coroutines","owner":"kotlin"}
```
-->

<!--- TEST -->

#### Define custom serial names for subclasses

A value of the `type` key is a fully qualified class name by default. 
You can change this by using the [`@SerialName`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-serial-name/) annotation on the 
corresponding class. This allows you to define stable _serial name_ that remains consistent,
regardless of changes to the class name in the source code:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
sealed class Project {
    abstract val name: String
}

// Assigns the custom serial name "owned" to OwnedProject for JSON output
@Serializable         
@SerialName("owned")
class OwnedProject(override val name: String, val owner: String) : Project()

fun main() {
    val data: Project = OwnedProject("kotlinx.coroutines", "kotlin")
    // Serializes the object with the custom `type` key "owned" instead of the class name
    println(Json.encodeToString(data))
    // {"type":"owned","name":"kotlinx.coroutines","owner":"kotlin"}
}
//sampleEnd
```
{kotlin-runnable="true"}

<!--- > You can get the full code [here](../../guide/example/example-poly-07.kt). -->

<!---
```text 
{"type":"owned","name":"kotlinx.coroutines","owner":"kotlin"}
```
-->

<!--- TEST -->

> Additionally, you can configure JSON to use a different key name for the class discriminator.
> For more information, see the [Specify class discriminator for polymorphism](serialization-json-configuration.md#specify-class-discriminator-for-polymorphism) section.
> 
{type="tip"}

#### Base class properties with backing fields

In a sealed hierarchy, the base class can define properties with backing fields that are serialized along with subclass properties:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
sealed class Project {
    abstract val name: String
    // Defines a property with a backing field in the base class
    var status = "open"
}
            
@Serializable   
@SerialName("owned")
class OwnedProject(override val name: String, val owner: String) : Project()

fun main() {
    // Includes default values like "status"
    val json = Json { encodeDefaults = true }
    val data: Project = OwnedProject("kotlinx.coroutines", "kotlin")
    // Serializes superclass properties before subclass properties
    println(json.encodeToString(data))
    // {"type":"owned","status":"open","name":"kotlinx.coroutines","owner":"kotlin"}
}
//sampleEnd
```
{kotlin-runnable="true"}

<!--- > You can get the full code [here](../../guide/example/example-poly-08.kt). -->

<!---
```text 
{"type":"owned","status":"open","name":"kotlinx.coroutines","owner":"kotlin"}
```
-->

<!--- TEST -->

#### Serialize objects in sealed hierarchies

Sealed hierarchies can have objects as their subclasses, and these objects must also be annotated with `@Serializable`.
When serialized, objects are treated as empty classes, and their class name is used as the type by default:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*

//sampleStart
@Serializable
sealed class Response

// Declares an object that extends Response
@Serializable
object EmptyResponse : Response()

// Declares a class that extends Response
@Serializable   
class TextResponse(val text: String) : Response()

// Serializes a list of different responses
fun main() {
    val list = listOf(EmptyResponse, TextResponse("OK"))
    println(Json.encodeToString(list))
    // [{"type":"EmptyResponse"},{"type":"TextResponse","text":"OK"}]
}
```
{kotlin-runnable="true"}

<!--- > You can get the full code [here](../../guide/example/example-poly-09.kt). -->

> The properties of objects are not serialized.
>
{type="note"}

<!---
```text 
[{"type":"EmptyResponse"},{"type":"TextResponse","text":"OK"}]
```
-->

<!--- TEST -->

## Open polymorphism in Kotlin serialization

Kotlin Serialization supports polymorphism with `open` and `abstract` classes.
In open polymorphism, subclasses can be defined anywhere in the codebase, even in other modules.
As a result, the list of subclasses cannot be determined at compile time and must be explicitly specified at runtime.

### Serialize open polymorphic classes

To serialize and deserialize instances of open polymorphic classes, you need to provide all subclasses in a `SerializersModule` class.

You can use the [`SerializersModule {}`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.modules/-serializers-module.html) builder function to create an instance of the `SerializersModule`.
Within this module, specify the base class in the [`polymorphic()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.modules/polymorphic.html) builder function.
Then, _register_ each subclass with the [`subclass()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.modules/subclass.html) function.
Registering means specifying the subclass so the serialization framework can recognize and handle these types.
Once the subclasses are registered, the module is then passed to the `Json` configuration:

```kotlin 
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*

//sampleStart
// Defines a SerializersModule with polymorphic serialization
val module = SerializersModule {
    polymorphic(Project::class) {
        // Registers OwnedProject as a subclass of Project
        subclass(OwnedProject::class)
    }
}

// Creates a custom JSON format with the module
val format = Json { serializersModule = module }

// Defines an abstract serializable class Project with an abstract property `name`
@Serializable
abstract class Project {
    abstract val name: String
}

// Defines a subclass OwnedProject with an additional property `owner` and a SerialName
@Serializable
@SerialName("owned")
class OwnedProject(override val name: String, val owner: String) : Project()

fun main() {
    val data: Project = OwnedProject("kotlinx.coroutines", "kotlin")
    // Serializes data using the custom format
    println(format.encodeToString(data))
    // {"type":"owned","name":"kotlinx.coroutines","owner":"kotlin"}
}
//sampleEnd
```
{kotlin-runnable="true"}

> For more information about custom JSON configurations, see the [JSON configuration](serialization-json-configuration.md) section.
>
{type="tip"}

<!--- > You can get the full code [here](../../guide/example/example-poly-09.kt). -->

This configuration achieves the same effect as [using sealed classes for closed polymorphic classes](#serialize-closed-polymorphic-classes),
but allows the serialization of open polymorphic classes.

<!---
```text 
{"type":"owned","name":"kotlinx.coroutines","owner":"kotlin"}
```
-->

<!--- TEST -->

> This example works only on the JVM because of `serializer()` function restrictions.
> For JS and Native platforms, use an explicit serializer: `format.encodeToString(PolymorphicSerializer(Project::class), data)`
> You can keep track of this issue [here](https://github.com/Kotlin/kotlinx.serialization/issues/1077).
> 
{type="note"}

### Serialize interfaces

Although you cannot annotate an interface with `@Serializable`, interfaces are implicitly serializable with `PolymorphicSerializer`. 
To do so, mark the classes that implement the interface as `@Serializable` and register them in a `SerializersModule`:

```kotlin 
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*

//sampleStart
// Creates a SerializersModule to register the implementing classes of the interface
val module = SerializersModule {
    polymorphic(Project::class) {
        subclass(OwnedProject::class)
    }
}

val format = Json { serializersModule = module }

// Declares an interface used for polymorphic serialization
interface Project {
    val name: String
}

// OwnedProject implements the Project interface 
@Serializable
@SerialName("owned")
class OwnedProject(override val name: String, val owner: String) : Project

fun main() {
    // Declares `data` with the type of `Project`, which is assigned an instance of `OwnedProject`
    val data: Project = OwnedProject("kotlinx.coroutines", "kotlin")
    println(format.encodeToString(data))
    // {"type":"owned","name":"kotlinx.coroutines","owner":"kotlin"}
}
//sampleEnd
```
{kotlin-runnable="true"}

<!--- > You can get the full code [here](../../guide/example/example-poly-10.kt). -->

<!---
```text 
{"type":"owned","name":"kotlinx.coroutines","owner":"kotlin"}
```
-->

> For Kotlin/Native, you need to explicitly specify the serializer using
> `format.encodeToString(PolymorphicSerializer(Project::class), data))` due to the platform's limited reflection capabilities.
> 
{type="note"}

<!--- TEST LINES_START -->

Additionally, you can use an interface as a property in a serializable class:

```kotlin
// Imports the necessary libraries
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

// Data class contains a property of type Project, which is an interface
@Serializable
class Data(val project: Project) // Project is an interface

fun main() {
    val data = Data(OwnedProject("kotlinx.coroutines", "kotlin"))
    println(format.encodeToString(data))
    // {"project":{"type":"owned","name":"kotlinx.coroutines","owner":"kotlin"}}
}
//sampleEnd
```
{kotlin-runnable="true"}

<!--- > You can get the full code [here](../../guide/example/example-poly-11.kt). -->

<!---
```text 
{"project":{"type":"owned","name":"kotlinx.coroutines","owner":"kotlin"}}
```
-->

<!--- TEST --> 

### Serialize polymorphic types with generic base types

Since Kotlin Serialization is fully static, the root type of a polymorphic hierarchy is determined at compile time.
When the base type is generic, such as `Any`, the system cannot resolve the correct subclass without additional instructions.

To serialize a variable with a generic base type:

1. Specify the generic base type and its subclasses in a `SerializersModule`.
2. Use `PolymorphicSerializer` as the first parameter to the [`encodeToString()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json/encode-to-string.html) function. 
This explicitly informs the system how to handle the polymorphic structure at runtime.

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*

//sampleStart
val module = SerializersModule {
    // Registers OwnedProject as a subclass of Any for polymorphic serialization
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
    // Declares data as Any, requiring explicit handling of polymorphism
    val data: Any = OwnedProject("kotlinx.coroutines", "kotlin")
    // Uses PolymorphicSerializer to serialize data of type Any
    println(format.encodeToString(PolymorphicSerializer(Any::class), data))
    // {"type":"owned","name":"kotlinx.coroutines","owner":"kotlin"}
}
//sampleEnd
```
{kotlin-runnable="true"}

<!--- > You can get the full code [here](../../guide/example/example-poly-14.kt). -->

<!---
```text 
{"type":"owned","name":"kotlinx.coroutines","owner":"kotlin"}
```
-->

<!--- TEST -->

If the subclass is not registered or the `PolymorphicSerializer` is not provided, an exception is thrown:

```text
Exception in thread "main" kotlinx.serialization.SerializationException: Serializer for class 'Any' is not found.
Please ensure that class is marked as '@Serializable' and that the serialization compiler plugin is applied.
```

### Serialize non-serializable properties polymorphically

When a property has a non-serializable type, such as `Any`, you can explicitly mark it with
the [`@Polymorphic`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-polymorphic/) annotation to serialize it.
This applies [`PolymorphicSerializer`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-polymorphic-serializer/) to the property, allowing it to be serialized at runtime:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*

val module = SerializersModule {
    polymorphic(Any::class) {
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

@Serializable
class Data(
    // Ensures the project property is serialized using PolymorphicSerializer
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

<!--- > You can get the full code [here](../../guide/example/example-poly-15.kt). -->

<!--- TEST 
{"project":{"type":"owned","name":"kotlinx.coroutines","owner":"kotlin"}}
-->

### Register multiple superclasses

When the same class is serialized as a value of properties with different compile-time types from its list of
superclasses, you must register it in the [SerializersModule](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.modules/-serializers-module/) for each of its superclasses separately.
To do so, you can use the [`subclass()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.modules/subclass.html) function.

To avoid repeating `subclass()` calls for each superclass, you can create a function that registers the subclasses and apply it to each superclass.
For example:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.reflect.KClass

//sampleStart
val module = SerializersModule {
    // Creates a function to register subclasses for each superclass
    fun PolymorphicModuleBuilder<Project>.registerProjectSubclasses() {
        subclass(OwnedProject::class)
    }
    // Applies the subclass registration to Any and Project
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

<!--- > You can get the full code [here](../../guide/example/example-poly-16.kt). -->

<!--- TEST 
{"project":{"type":"owned","name":"kotlinx.coroutines","owner":"kotlin"},"any":{"type":"owned","name":"kotlinx.coroutines","owner":"kotlin"}}
-->

### Serialize generic subtypes in a polymorphic hierarchy

When serializing generic subtypes in a polymorphic class hierarchy, Kotlin Serialization requires explicit handling.
Since the framework cannot automatically determine the actual type of generic parameters,
you can use a `PolymorphicSerializer` to handle these cases.

To serialize generic subtypes, register the appropriate serializers within a `SerializersModule`, ensuring that
each polymorphic type is correctly serialized and deserialized within the hierarchy:

```kotlin 
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*

// Defines an abstract response class with a generic parameter T
@Serializable
abstract class Response<out T>

// Represents a successful response with a generic data type
@Serializable
@SerialName("OkResponse")
data class OkResponse<out T>(val data: T) : Response<T>()

// Defines the abstract class Project
@Serializable
abstract class Project {
    abstract val name: String
}

// Concrete subclass of Project
@Serializable
@SerialName("OwnedProject")
data class OwnedProject(override val name: String, val owner: String) : Project()

//sampleStart
// Defines a serializers module for polymorphic classes
val responseModule = SerializersModule {
    polymorphic(Response::class) {
        // Registers the polymorphic serializer for OkResponse
        subclass(OkResponse.serializer(PolymorphicSerializer(Any::class)))
    }
    polymorphic(Any::class) {
        // Registers OwnedProject as a subclass of Any
        subclass(OwnedProject::class)
    }
    polymorphic(Project::class) {
        // Registers OwnedProject as a subclass of Project
        subclass(OwnedProject::class)
    }
}

// Creates a Json format with the registered serializers
val format = Json { serializersModule = responseModule }

fun main() {
    // Creates an instance of OkResponse with a Project subtype
    val data: Response<Project> = OkResponse(OwnedProject("kotlinx.serialization", "kotlin"))

    // Serializes the data to JSON
    val jsonString = format.encodeToString(data)
    println(jsonString)
    // {"type":"OkResponse","data":{"type":"OwnedProject","name":"kotlinx.serialization","owner":"kotlin"}}

    // Deserializes the JSON back to Response<Project>
    val deserializedData = format.decodeFromString<Response<Project>>(jsonString)
    println(deserializedData)
    // OkResponse(data=OwnedProject(name=kotlinx.serialization, owner=kotlin))
}
```
{kotlin-runnable="true"}

<!--- > You can get the full code [here](../../guide/example/example-poly-16.kt). -->

<!---
```text 
{"type":"OkResponse","data":{"type":"OwnedProject","name":"kotlinx.serialization","owner":"kotlin"}}
OkResponse(data=OwnedProject(name=kotlinx.serialization, owner=kotlin))
```
-->

<!--- TEST -->

### Merge multiple SerializerModule instances

As an application grows and splits into multiple source code modules, managing all class hierarchies within a single `SerializerModule` may become cumbersome.
You can combine multiple `SerializersModule` instances using the [`plus`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.modules/plus.html) operator,
allowing them to be used together in the same `Json` format instance:

```kotlin
// Imports the necessary libraries
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

// Now classes from both hierarchies can be serialized together and deserialized together.
val format = Json { serializersModule = projectModule + responseModule }
// The JSON that is being produced is deeply polymorphic.
//sampleEnd

fun main() {
    // both Response and Project are abstract and their concrete subtypes are being serialized
    val data: Response<Project> =  OkResponse(OwnedProject("kotlinx.serialization", "kotlin"))
    val string = format.encodeToString(data)
    println(string)
    // {"type":"OkResponse","data":{"type":"OwnedProject","name":"kotlinx.serialization","owner":"kotlin"}}
    println(format.decodeFromString<Response<Project>>(string))
    // OkResponse(data=OwnedProject(name=kotlinx.serialization, owner=kotlin))
}
```
{kotlin-runnable="true"}

> You can also use the [`include()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.modules/-serializers-module-builder/include.html) function
> within the [`SerializersModule`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.modules/-serializers-module.html) block:
> 
> ```kotlin
> // Combines the two modules using the `include()` function
> val combinedModule = SerializersModule {
> include(projectModule)
> include(responseModule)
> }
> ```
>
{type="note"}

If you're building a library or shared module that defines an abstract class and its implementations,
you can expose your `SerializersModule` to your clients.
This allows them to combine your module with their own `SerializersModule` for seamless integration.

<!--- > You can get the full code [here](../../guide/example/example-poly-17.kt). -->

<!---
```text
{"type":"OkResponse","data":{"type":"OwnedProject","name":"kotlinx.serialization","owner":"kotlin"}}
OkResponse(data=OwnedProject(name=kotlinx.serialization, owner=kotlin))
```
-->

<!--- TEST -->

### Default polymorphic type handler for deserialization

When deserializing a subclass that's not registered, a `JsonDecodingException` exception is thrown. 
To handle this more gracefully, you can register a default deserializer for unrecognized subclasses using the [`defaultDeserializer()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.modules/-polymorphic-module-builder/default-deserializer.html) function
in the [`PolymorphicModuleBuilder { ... }`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.modules/-polymorphic-module-builder/) DSL.
This allows you to map unrecognized types to a specific deserialization strategy.

In the following example, the `BasicProject` class is used to represent unknown subtypes.
The example doesn't rely on the `type` field to differentiate subtypes and instead uses the [plugin-generated serializer]((serializers.md#plugin-generated-serializer)) for `BasicProject`:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*

//sampleStart
@Serializable
abstract class Project {
    abstract val name: String
}

// Represents unknown project types, capturing the type and name
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

Using a default serializer assumes that the structure of the "unknown" data is known in advance.
In cases where the structure may vary, you need to create a custom serializer to handle more flexible or less-structured data.
For more details on working with custom JSON structures, see the [Maintaining custom JSON attributes]((serialization-json-transform-json.md#maintain-custom-json-attributes)) section.

<!--- > You can get the full code [here](../../guide/example/example-poly-19.kt). -->

<!---
```text
[BasicProject(name=example, type=unknown), OwnedProject(name=kotlinx.serialization, owner=kotlin)]
```
-->

<!--- TEST -->

### Default polymorphic type handler for serialization

To dynamically choose a serializer for a polymorphic type based on the instance, use the [`polymorphicDefaultSerializer()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.modules/-serializers-module-builder/polymorphic-default-serializer.html) function within the [`SerializersModule { ... }`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.modules/-serializers-module-builder/) DSL.
This function defines a strategy that takes an instance of the base class and provides a corresponding  [`SerializationStrategy`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-serialization-strategy/).

Let's look at an example where a `when` block is used to determine the appropriate serializer based on the instance, without needing to reference private implementation classes:

```kotlin
// Imports the necessary libraries
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

// Provides instances of Cat and Dog
object AnimalProvider {
    fun createCat(): Cat = CatImpl()
    fun createDog(): Dog = DogImpl()
}

// Registers a default serializer for unknown Animal subtypes
val module = SerializersModule {
    polymorphicDefaultSerializer(Animal::class) { instance ->
        @Suppress("UNCHECKED_CAST")
        when (instance) {
            is Cat -> CatSerializer as SerializationStrategy<Animal>
            is Dog -> DogSerializer as SerializationStrategy<Animal>
            else -> null
        }
    }
}

// Defines custom serializers for Cat and Dog
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
// Creates a Json format using the registered module
val format = Json { serializersModule = module }

fun main() {
    // Serializes an instance of Cat
    println(format.encodeToString<Animal>(AnimalProvider.createCat()))
    // {"type":"Cat","catType":"Tabby"}
}
//sampleEnd
```
{kotlin-runnable="true"}

<!--- > You can get the full code [here](../../guide/example/example-poly-20.kt) -->

<!--- 
```text
{"type":"Cat","catType":"Tabby"}
```
-->

<!--- TEST -->
