[//]: # (title: Serialize polymorphic classes)

<!--- TEST_NAME PolymorphismTest -->

Polymorphism allows objects of different types to be treated as objects of a common supertype.
In Kotlin, polymorphism enables you to work with various subclasses through a shared interface or superclass.
This is useful for designing flexible and reusable code, especially when the exact type of objects isn't known at compile time.
In the context of serialization, polymorphism helps manage data structures where the runtime type of data can vary.

## Closed polymorphism in Kotlin serialization

Closed polymorphism ensures that all possible subclasses are known at compile-time,
making it a reliable and predictable approach for serializing and deserializing polymorphic data structures.
The best way to serialize closed polymorphic classes is to use [`sealed classes`](#serialize-closed-polymorphic-classes), which restrict subclassing to the same file.

Kotlin Serialization is fully static by default, where the structure of encoded objects is based on their compile-time types.
This means that only the properties defined in the static type are serialized, even if the object is initialized with a subclass at runtime.

Let's look at an example where an `open class Project` has a `name` property, and its subclass `class OwnedProject` adds an `owner` property.
Although the `data` variable is initialized with an instance of `OwnedProject` at runtime, its compile-time type is `Project`.
As a result, only the properties of the `Project` class are serialized:

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

In this example, the `owner` property is not serialized because `data` is statically typed as `Project`.

Changing the compile-time type of `data` to `OwnedProject` throws an exception:

```kotlin
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*


@Serializable
open class Project(val name: String)

class OwnedProject(name: String, val owner: String) : Project(name)

fun main() { 
//sampleStart
    val data = OwnedProject("kotlinx.coroutines", "kotlin")
    // Throws an exception, because the `OwnedProject` class is not serializable
    println(Json.encodeToString(data))
    // Exception in thread "main" kotlinx.serialization.SerializationException: Serializer for class 'OwnedProject' is not found.
}
//sampleEnd
```
{kotlin-runnable="true" validate="false"}

This exception occurs because the `OwnedProject` class is not serializable and cannot be annotated with `@Serializable`
as its primary constructor parameters (`name` and `owner`) are not properties.
To resolve this, you can [define constructor properties for serialization](serialization-customization-options.md#define-constructor-properties-for-serialization).

<!--- > You can get the full code [here](../../guide/example/example-poly-02.kt). -->

<!---
```text
Exception in thread "main" kotlinx.serialization.SerializationException: Serializer for class 'OwnedProject' is not found.
Please ensure that class is marked as '@Serializable' and that the serialization compiler plugin is applied.
```
-->

<!--- TEST LINES_START -->

Alternatively, you might consider defining `Project` as an [`abstract class`](classes.md#abstract-classes) with abstract properties.
That way, instead of `Project` providing actual values for its properties, its subclasses are responsible for defining them.
Unfortunately, this still throws an exception:

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

This exception indicates that defining `Project` as an abstract class doesn't resolve the serialization issue.
To fix this, you need to [use sealed classes](#serialize-closed-polymorphic-classes).

<!---  > You can get the full code [here](../../guide/example/example-poly-03.kt). -->

<!--- 
```text 
Exception in thread "main" kotlinx.serialization.SerializationException: Serializer for subclass 'OwnedProject' is not found in the polymorphic scope of 'Project'.
Check if class with serial name 'OwnedProject' exists and serializer is registered in a corresponding SerializersModule.
To be registered automatically, class 'OwnedProject' has to be '@Serializable', and the base class 'Project' has to be sealed and '@Serializable'.
```
-->

<!--- TEST LINES_START -->

### Serialize closed polymorphic classes

The simplest way to handle polymorphic serialization is to use a `sealed class` as the base.
_All_ subclasses of a sealed class must be explicitly marked as `@Serializable`.
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
Within this module, specify the base class in the [`polymorphic()`] builder function and specify each subclass with the [`subclass()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.modules/subclass.html) function.
This module is then passed to the `Json` configuration:

```kotlin 
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*

//sampleStart
// Defines a SerializersModule with polymorphic serialization
val module = SerializersModule {
    polymorphic(Project::class) {
        // Specifies OwnedProject as a subclass of Project
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
To do so, mark the classes that implement the interface as `@Serializable` and specify them in a `SerializersModule`:

```kotlin 
// Imports the necessary libraries
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*

//sampleStart
// Creates a SerializersModule to specify the implementing classes of the interface
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

<!--- > You can get the full code [here](../../guide/example/example-poly-11.kt). -->

<!---
```text 
{"project":{"type":"owned","name":"kotlinx.coroutines","owner":"kotlin"}}
```
-->

<!--- TEST --> 

### Static parent type lookup for polymorphism

During serialization of a polymorphic class the root type of the polymorphic hierarchy (`Project` in our example)
is determined statically. Let us take the example with the serializable `abstract class Project`,
but change the `main` function to declare `data` as having a type of `Any`:

<!--- INCLUDE 
import kotlinx.serialization.modules.*

val module = SerializersModule {
    polymorphic(Project::class) {
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
--> 

```kotlin 
fun main() {
    val data: Any = OwnedProject("kotlinx.coroutines", "kotlin")
    println(format.encodeToString(data))
}    
```

> You can get the full code [here](../../guide/example/example-poly-12.kt).

We get the exception.

```text
Exception in thread "main" kotlinx.serialization.SerializationException: Serializer for class 'Any' is not found.
Please ensure that class is marked as '@Serializable' and that the serialization compiler plugin is applied.
```

<!--- TEST LINES_START -->

We have to register classes for polymorphic serialization with respect for the corresponding static type we
use in the source code. First of all, we change our module to register a subclass of `Any`:

<!--- INCLUDE 
import kotlinx.serialization.modules.*
-->

```kotlin
val module = SerializersModule {
    polymorphic(Any::class) {
        subclass(OwnedProject::class)
    }
}
```                                                                                    

<!--- INCLUDE 
val format = Json { serializersModule = module }

@Serializable
abstract class Project {
    abstract val name: String
}
            
@Serializable
@SerialName("owned")
class OwnedProject(override val name: String, val owner: String) : Project()
-->

Then we can try to serialize the variable of type `Any`:

```kotlin
fun main() {
    val data: Any = OwnedProject("kotlinx.coroutines", "kotlin")
    println(format.encodeToString(data))
}    
```

> You can get the full code [here](../../guide/example/example-poly-13.kt).

However, `Any` is a class and it is not serializable:

```text 
Exception in thread "main" kotlinx.serialization.SerializationException: Serializer for class 'Any' is not found.
Please ensure that class is marked as '@Serializable' and that the serialization compiler plugin is applied.
```

<!--- TEST LINES_START -->

We must to explicitly pass an instance of [PolymorphicSerializer] for the base class `Any` as the
first parameter to the [encodeToString][Json.encodeToString] function.

<!--- INCLUDE 
import kotlinx.serialization.modules.*

val module = SerializersModule {
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
-->

```kotlin
fun main() {
    val data: Any = OwnedProject("kotlinx.coroutines", "kotlin")
    println(format.encodeToString(PolymorphicSerializer(Any::class), data))
}    
```

> You can get the full code [here](../../guide/example/example-poly-14.kt).

With the explicit serializer it works as before.

```text 
{"type":"owned","name":"kotlinx.coroutines","owner":"kotlin"}
```                  

<!--- TEST -->

### Explicitly marking polymorphic class properties

The property of an interface type is implicitly considered polymorphic, since interfaces are all about runtime polymorphism.
However, Kotlin Serialization does not compile a serializable class with a property of a non-serializable class type.
If we have a property of `Any` class or other non-serializable class, then we must explicitly provide its serialization
strategy via the [`@Serializable`][Serializable] annotation as we saw in
the [Specifying serializer on a property](serializers.md#specifying-serializer-on-a-property) section.
To specify a polymorphic serialization strategy of a property, the special-purpose [`@Polymorphic`][Polymorphic]
annotation is used.

<!--- INCLUDE 
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
-->

```kotlin
@Serializable
class Data(
    @Polymorphic // the code does not compile without it 
    val project: Any 
)

fun main() {
    val data = Data(OwnedProject("kotlinx.coroutines", "kotlin"))
    println(format.encodeToString(data))
}
```

> You can get the full code [here](../../guide/example/example-poly-15.kt).

<!--- TEST 
{"project":{"type":"owned","name":"kotlinx.coroutines","owner":"kotlin"}}
-->

### Registering multiple superclasses

When the same class gets serialized as a value of properties with different compile-time type from the list of
its superclasses, we must register it in the [SerializersModule] for each of its superclasses separately.
It is convenient to extract registration of all the subclasses into a separate function and
use it for each superclass. You can use the following template to write it.

<!--- INCLUDE 
import kotlinx.serialization.modules.*
import kotlin.reflect.KClass
-->

```kotlin
val module = SerializersModule {
    fun PolymorphicModuleBuilder<Project>.registerProjectSubclasses() {
        subclass(OwnedProject::class)
    }
    polymorphic(Any::class) { registerProjectSubclasses() }
    polymorphic(Project::class) { registerProjectSubclasses() }
}        
```

<!--- INCLUDE

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
}        
-->

> You can get the full code [here](../../guide/example/example-poly-16.kt).

<!--- TEST 
{"project":{"type":"owned","name":"kotlinx.coroutines","owner":"kotlin"},"any":{"type":"owned","name":"kotlinx.coroutines","owner":"kotlin"}}
-->

### Polymorphism and generic classes

Generic subtypes for a serializable class require a special handling. Consider the following hierarchy.

<!--- INCLUDE
import kotlinx.serialization.modules.*
-->

```kotlin 
@Serializable
abstract class Response<out T>
            
@Serializable
@SerialName("OkResponse")
data class OkResponse<out T>(val data: T) : Response<T>()
```

Kotlin Serialization does not have a builtin strategy to represent the actually provided argument type for the
type parameter `T` when serializing a property of the polymorphic type `OkResponse<T>`. We have to provide this
strategy explicitly when defining the serializers module for `Response`. In the below example we
use `OkResponse.serializer(...)` to retrieve
the [Plugin-generated generic serializer](serializers.md#plugin-generated-generic-serializer) of
the `OkResponse` class and instantiate it with the [PolymorphicSerializer] instance with
`Any` class as its base. This way, we can serialize an instance of `OkResponse` with any `data` property that
was polymorphically registered as a subtype of `Any`.

```kotlin   
val responseModule = SerializersModule {
    polymorphic(Response::class) {
        subclass(OkResponse.serializer(PolymorphicSerializer(Any::class)))
    }
}
```

### Merging library serializers modules

When the application grows in size and splits into source code modules,
it may become inconvenient to store all class hierarchies in one serializers module.
Let us add a library with the `Project` hierarchy to the code from the previous section.

```kotlin
val projectModule = SerializersModule {
    fun PolymorphicModuleBuilder<Project>.registerProjectSubclasses() {
        subclass(OwnedProject::class)
    }
    polymorphic(Any::class) { registerProjectSubclasses() }
    polymorphic(Project::class) { registerProjectSubclasses() }
}
```

<!--- INCLUDE

@Serializable
abstract class Project {
    abstract val name: String
}
            
@Serializable
@SerialName("OwnedProject")
data class OwnedProject(override val name: String, val owner: String) : Project()
-->

We can compose those two modules together using the  [plus] operator to merge them,
so that we can use them both in the same [Json] format instance.

> You can also use the [include][SerializersModuleBuilder.include] function
> in the [SerializersModule {}][SerializersModule()] DSL.

```kotlin
val format = Json { serializersModule = projectModule + responseModule }
````                                                                  

Now classes from both hierarchies can be serialized together and deserialized together.

```kotlin 
fun main() {
    // both Response and Project are abstract and their concrete subtypes are being serialized
    val data: Response<Project> =  OkResponse(OwnedProject("kotlinx.serialization", "kotlin"))
    val string = format.encodeToString(data)
    println(string)
    println(format.decodeFromString<Response<Project>>(string))
}

```

> You can get the full code [here](../../guide/example/example-poly-17.kt).

The JSON that is being produced is deeply polymorphic.

```text
{"type":"OkResponse","data":{"type":"OwnedProject","name":"kotlinx.serialization","owner":"kotlin"}}
OkResponse(data=OwnedProject(name=kotlinx.serialization, owner=kotlin))
```

<!--- TEST -->

If you're writing a library or shared module with an abstract class and some implementations of it,
you can expose your own serializers module for your clients to use so that a client can combine your
module with their modules.

### Default polymorphic type handler for deserialization

What happens when we deserialize a subclass that was not registered?

<!--- INCLUDE
import kotlinx.serialization.modules.*

@Serializable
abstract class Project {
    abstract val name: String
}

@Serializable
@SerialName("OwnedProject")
data class OwnedProject(override val name: String, val owner: String) : Project()

val module = SerializersModule {
    polymorphic(Project::class) {
        subclass(OwnedProject::class)
    }
}

val format = Json { serializersModule = module }
-->  

```kotlin
fun main() {
    println(format.decodeFromString<Project>("""
        {"type":"unknown","name":"example"}
    """))
}
```

> You can get the full code [here](../../guide/example/example-poly-18.kt).

We get the following exception.

```text 
Exception in thread "main" kotlinx.serialization.json.internal.JsonDecodingException: Unexpected JSON token at offset 0: Serializer for subclass 'unknown' is not found in the polymorphic scope of 'Project' at path: $
Check if class with serial name 'unknown' exists and serializer is registered in a corresponding SerializersModule.
```

<!--- TEST LINES_START -->

When reading a flexible input we might want to provide some default behavior in this case. For example,
we can have a `BasicProject` subtype to represent all kinds of unknown `Project` subtypes.

<!--- INCLUDE
import kotlinx.serialization.modules.*
-->

```kotlin
@Serializable
abstract class Project {
    abstract val name: String
}

@Serializable
data class BasicProject(override val name: String, val type: String): Project()

@Serializable
@SerialName("OwnedProject")
data class OwnedProject(override val name: String, val owner: String) : Project()
```

We register a default deserializer handler using the [`defaultDeserializer`][PolymorphicModuleBuilder.defaultDeserializer] function in
the [`polymorphic { ... }`][PolymorphicModuleBuilder] DSL that defines a strategy which maps the `type` string from the input
to the [deserialization strategy][DeserializationStrategy]. In the below example we don't use the type,
but always return the [Plugin-generated serializer](serializers.md#plugin-generated-serializer)
of the `BasicProject` class.

```kotlin
val module = SerializersModule {
    polymorphic(Project::class) {
        subclass(OwnedProject::class)
        defaultDeserializer { BasicProject.serializer() }
    }
}
```

Using this module we can now deserialize both instances of the registered `OwnedProject` and
any unregistered one.

```kotlin
val format = Json { serializersModule = module }

fun main() {
    println(format.decodeFromString<List<Project>>("""
        [
            {"type":"unknown","name":"example"},
            {"type":"OwnedProject","name":"kotlinx.serialization","owner":"kotlin"} 
        ]
    """))
}
```

> You can get the full code [here](../../guide/example/example-poly-19.kt).

Notice, how `BasicProject` had also captured the specified type key in its `type` property.

```text
[BasicProject(name=example, type=unknown), OwnedProject(name=kotlinx.serialization, owner=kotlin)]
```

<!--- TEST -->

We used a plugin-generated serializer as a default serializer, implying that
the structure of the "unknown" data is known in advance. In a real-world API it's rarely the case.
For that purpose a custom, less-structured serializer is needed. You will see the example of such serializer in the future section
on [Maintaining custom JSON attributes](json.md#maintaining-custom-json-attributes).

### Default polymorphic type handler for serialization

Sometimes you need to dynamically choose which serializer to use for a polymorphic type based on the instance, for example if you
don't have access to the full type hierarchy, or if it changes a lot. For this situation, you can register a default serializer.

<!--- INCLUDE
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*
-->

```kotlin
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
```

We register a default serializer handler using the [`polymorphicDefaultSerializer`][SerializersModuleBuilder.polymorphicDefaultSerializer] function in
the [`SerializersModule { ... }`][SerializersModuleBuilder] DSL that defines a strategy which takes an instance of the base class and
provides a [serialization strategy][SerializationStrategy]. In the below example we use a `when` block to check the type of the
instance, without ever having to refer to the private implementation classes.

```kotlin
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
```

Using this module we can now serialize instances of `Cat` and `Dog`.

```kotlin
val format = Json { serializersModule = module }

fun main() {
    println(format.encodeToString<Animal>(AnimalProvider.createCat()))
}
```

> You can get the full code [here](../../guide/example/example-poly-20.kt)

```text
{"type":"Cat","catType":"Tabby"}
```


<!--- TEST -->
