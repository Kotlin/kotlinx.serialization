<!--- TEST_NAME PolymorphismTest -->

# Polymorphism

This is the fourth chapter of the [Kotlin Serialization Guide](serialization-guide.md).
In this chapter we'll see how Kotlin Serialization deals with polymorphic class hierarchies.

**Table of contents**

<!--- TOC -->

* [Closed polymorphism](#closed-polymorphism)
  * [Static types](#static-types)
  * [Designing serializable hierarchy](#designing-serializable-hierarchy)
  * [Sealed classes](#sealed-classes)
  * [Custom subclass serial name](#custom-subclass-serial-name)
  * [Concrete properties in a base class](#concrete-properties-in-a-base-class)
  * [Objects](#objects)
* [Open polymorphism](#open-polymorphism)
  * [Registered subclasses](#registered-subclasses)
  * [Serializing interfaces](#serializing-interfaces)
  * [Property of an interface type](#property-of-an-interface-type)
  * [Static parent type lookup for polymorphism](#static-parent-type-lookup-for-polymorphism)
  * [Explicitly marking polymorphic class properties](#explicitly-marking-polymorphic-class-properties)
  * [Registering multiple superclasses](#registering-multiple-superclasses)
  * [Polymorphism and generic classes](#polymorphism-and-generic-classes)
  * [Merging library serializers modules](#merging-library-serializers-modules)
  * [Default polymorphic type handler for deserialization](#default-polymorphic-type-handler-for-deserialization)

<!--- END -->

<!--- INCLUDE .*-poly-.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
-->

## Closed polymorphism

Let us start with basic introduction to polymorphism.

### Static types

Kotlin serialization is fully static with respect to types by default. The structure of encoded objects is determined 
by *compile-time* types of objects. Let's examine this aspect in more detail and learn how
to serialize polymorphic data structures, where the type of data is determined at runtime.

To show the static nature of Kotlin serialization let us make the following setup. An `open class Project`
has just the `name` property, while its derived `class OwnedProject` adds an `owner` property.
In the below example, we serialize `data` variable with a static type of
`Project` that is initialized with an instance of `OwnedProject` at runtime.

```kotlin
@Serializable
open class Project(val name: String)

class OwnedProject(name: String, val owner: String) : Project(name)

fun main() {
    val data: Project = OwnedProject("kotlinx.coroutines", "kotlin")
    println(Json.encodeToString(data))
}  
```                                

> You can get the full code [here](../guide/example/example-poly-01.kt).

Despite the runtime type of `OwnedProject`, only the `Project` class properties are getting serialized.  
 
```text
{"name":"kotlinx.coroutines"}
```   

<!--- TEST -->                                                                    

Let's change the compile-time type of `data` to `OwnedProject`.

```kotlin
@Serializable
open class Project(val name: String)

class OwnedProject(name: String, val owner: String) : Project(name)

fun main() {
    val data = OwnedProject("kotlinx.coroutines", "kotlin")
    println(Json.encodeToString(data))
}  
```                                

> You can get the full code [here](../guide/example/example-poly-02.kt).

We get an error, because the `OwnedProject` class is not serializable.  
 
```text
Exception in thread "main" kotlinx.serialization.SerializationException: Serializer for class 'OwnedProject' is not found.
Mark the class as @Serializable or provide the serializer explicitly.
```                                                                       

<!--- TEST LINES_START -->

### Designing serializable hierarchy

We cannot simply mark `OwnedProject` from the previous example as `@Serializable`. It does not compile, 
running into the [constructor properties requirement](basic-serialization.md#constructor-properties-requirement). 
To make hierarchy of classes serializable, the properties in the parent class have to be marked `abstract`, 
making the `Project` class `abstract`, too. 

```kotlin
@Serializable
abstract class Project {
    abstract val name: String
}

class OwnedProject(override val name: String, val owner: String) : Project()

fun main() {
    val data: Project = OwnedProject("kotlinx.coroutines", "kotlin")
    println(Json.encodeToString(data))
}  
```

> You can get the full code [here](../guide/example/example-poly-03.kt).

This is close to the best design for a serializable hierarchy of classes, but running it produces the following error:

```text 
Exception in thread "main" kotlinx.serialization.SerializationException: Class 'OwnedProject' is not registered for polymorphic serialization in the scope of 'Project'.
Mark the base class as 'sealed' or register the serializer explicitly.
```         

<!--- TEST LINES_START -->

### Sealed classes

The most straightforward way to use serialization with a polymorphic hierarchy is to mark the base class `sealed`.
_All_ subclasses of a sealed class must be explicitly marked as `@Serializable`.

```kotlin
@Serializable
sealed class Project {
    abstract val name: String
}
            
@Serializable
class OwnedProject(override val name: String, val owner: String) : Project()

fun main() {
    val data: Project = OwnedProject("kotlinx.coroutines", "kotlin")
    println(Json.encodeToString(data))
}  
```

> You can get the full code [here](../guide/example/example-poly-04.kt).

Now we can see a default way to represent polymorphism in JSON. 
A `type` key is added to the resulting JSON object as a _discriminator_.  

```text 
{"type":"example.examplePoly04.OwnedProject","name":"kotlinx.coroutines","owner":"kotlin"}
```                  

<!--- TEST -->

### Custom subclass serial name

A value of the `type` key is a fully qualified class name by default. We can put [SerialName] annotation onto 
the corresponding class to change it.
 
```kotlin
@Serializable
sealed class Project {
    abstract val name: String
}
            
@Serializable         
@SerialName("owned")
class OwnedProject(override val name: String, val owner: String) : Project()

fun main() {
    val data: Project = OwnedProject("kotlinx.coroutines", "kotlin")
    println(Json.encodeToString(data))
}  
```

> You can get the full code [here](../guide/example/example-poly-05.kt).

This way we can have a stable _serial name_ that is not affected by the class's name in the source code.

```text 
{"type":"owned","name":"kotlinx.coroutines","owner":"kotlin"}
```                   

<!--- TEST -->

> In addition to that, JSON can be configured to use a different key name for the class discriminator. 
> You can find an example in the [Class discriminator](json.md#class-discriminator) section.

### Concrete properties in a base class

A base class in a sealed hierarchy can have properties with backing fields. 

```kotlin
@Serializable
sealed class Project {
    abstract val name: String   
    var status = "open"
}
            
@Serializable   
@SerialName("owned")
class OwnedProject(override val name: String, val owner: String) : Project()

fun main() {
    val data: Project = OwnedProject("kotlinx.coroutines", "kotlin")
    println(Json.encodeToString(data))
}  
```

> You can get the full code [here](../guide/example/example-poly-06.kt).

The properties of the superclass are serialized before the properties of the subclass. 

```text 
{"type":"owned","status":"open","name":"kotlinx.coroutines","owner":"kotlin"}
```                                                                                  

<!--- TEST -->

### Objects

Sealed hierarchies can have objects as their subclasses and they also need to be marked as `@Serializable`.
Let's take a different example with a hierarchy of `Response` classes.

```kotlin
@Serializable
sealed class Response
                      
@Serializable
object EmptyResponse : Response()

@Serializable   
class TextResponse(val text: String) : Response()   
```

Let us serialize a list of different responses.

```kotlin
fun main() {
    val list = listOf(EmptyResponse, TextResponse("OK"))
    println(Json.encodeToString(list))
}  
```

> You can get the full code [here](../guide/example/example-poly-07.kt).

An object serializes as an empty class, also using its fully-qualified class name as type by default:

```text 
[{"type":"example.examplePoly07.EmptyResponse"},{"type":"example.examplePoly07.TextResponse","text":"OK"}]
```                            

<!--- TEST -->

> Even if object has properties, they are not serialized. 

## Open polymorphism

Serialization can work with arbitrary `open` classes or `abstract` classes. 
However, since this kind of polymorphism is open, there is a possibility that subclasses are defined anywhere in the 
source code, even in other modules, the list of subclasses that are serialized cannot be determined at compile-time and
must be explicitly registered at runtime. 

### Registered subclasses

Let us start with the code from the [Designing serializable hierarchy](#designing-serializable-hierarchy) section.
To make it work with serialization without making it `sealed`, we have to define a [SerializersModule] using the 
[SerializersModule {}][SerializersModule()] builder function. In the module the base class is specified 
in the [polymorphic] builder and each subclass is registered with the [subclass] function. Now, 
a custom JSON configuration can be instantiated with this module and used for serialization.

> Details on custom JSON configurations can be found in 
> the [JSON configuration](json.md#json-configuration) section. 

<!--- INCLUDE 
import kotlinx.serialization.modules.*
--> 

```kotlin 
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

fun main() {
    val data: Project = OwnedProject("kotlinx.coroutines", "kotlin")
    println(format.encodeToString(data))
}    
```

> You can get the full code [here](../guide/example/example-poly-08.kt).

This additional configuration makes our code work just as it worked with a sealed class in 
the [Sealed classes](#sealed-classes) section, but here subclasses can be spread arbitrarily throughout the code.

```text 
{"type":"owned","name":"kotlinx.coroutines","owner":"kotlin"}
```                  

<!--- TEST -->

### Serializing interfaces 

We can update the previous example and turn `Project` superclass into an interface. However, we cannot
mark an interface itself as `@Serializable`.
One possible practical default behaviour is to make them polymorphically serializable.
Thus all interfaces are considered to be implicitly serializable with the [PolymorphicSerializer]
strategy.

<!--- INCLUDE 
import kotlinx.serialization.modules.*

val module = SerializersModule {
    polymorphic(Project::class) {
        subclass(OwnedProject::class)
    }
}

val format = Json { serializersModule = module }
--> 

```kotlin 
interface Project {
    val name: String
}

@Serializable
@SerialName("owned")
class OwnedProject(override val name: String, val owner: String) : Project
```

It means that if we declare `data` with the type of `Project` we can simply call `format.encodeToString` as we did before.

```kotlin
fun main() {
    val data: Project = OwnedProject("kotlinx.coroutines", "kotlin")
    println(format.encodeToString(data))
}    
```

> You can get the full code [here](../guide/example/example-poly-09.kt).

```text 
{"type":"owned","name":"kotlinx.coroutines","owner":"kotlin"}
```

<!--- TEST LINES_START -->

### Property of an interface type

Continuing the previous example, let us see what happens if we use `Project` interface as a property in some
other serializable class. Interfaces are implicitly polymorphic, so we can just declare a property of an interface type.

<!--- INCLUDE 
import kotlinx.serialization.modules.*

val module = SerializersModule {
    polymorphic(Project::class) {
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
class Data(val project: Project) // Project is an interface

fun main() {
    val data = Data(OwnedProject("kotlinx.coroutines", "kotlin"))
    println(format.encodeToString(data))
}        
```

> You can get the full code [here](../guide/example/example-poly-10.kt).

As long as we've registered the actual subtype of the interface that is being serialized in
the [SerializersModule] of our `format`, we get it working at runtime.

```text 
{"project":{"type":"owned","name":"kotlinx.coroutines","owner":"kotlin"}}
```

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

> You can get the full code [here](../guide/example/example-poly-11.kt).
 
We get the exception.

```text
Exception in thread "main" kotlinx.serialization.SerializationException: Serializer for class 'Any' is not found.
Mark the class as @Serializable or provide the serializer explicitly.
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

> You can get the full code [here](../guide/example/example-poly-12.kt).

However, the `Any` is a class and it is not serializable:

```text 
Exception in thread "main" kotlinx.serialization.SerializationException: Serializer for class 'Any' is not found.
Mark the class as @Serializable or provide the serializer explicitly.
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

> You can get the full code [here](../guide/example/example-poly-13.kt).

With the explicit serializer it works as before.

```text 
{"type":"owned","name":"kotlinx.coroutines","owner":"kotlin"}
```                  

<!--- TEST -->

### Explicitly marking polymorphic class properties

The property of an interface type is implicitly considered polymorphic, since interfaces are all about runtime polymorphism. 
However, Kotlin serialization does not compile a serializable class with a property of a non-serializable class type. 
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

> You can get the full code [here](../guide/example/example-poly-14.kt).
 
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

> You can get the full code [here](../guide/example/example-poly-15.kt).

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
 
Kotlin serialization does not have a builtin strategy to represent the actually provided argument type for the
type parameter `T` when serializing a property of the polymorphic type `OkResponse<T>`. We have to provide this 
strategy explicitly when defining the serializers module for the `Response`. In the below example we
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

> You can get the full code [here](../guide/example/example-poly-16.kt).

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

> You can get the full code [here](../guide/example/example-poly-17.kt).

We get the following exception.

```text 
Exception in thread "main" kotlinx.serialization.json.internal.JsonDecodingException: Polymorphic serializer was not found for class discriminator 'unknown'
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

We register a default handler using the [`default`][PolymorphicModuleBuilder.default] function in
the [`polymorphic { ... }`][PolymorphicModuleBuilder] DSL that defines a strategy which maps the `type` string from the input
to the [deserialization strategy][DeserializationStrategy]. In the below example we don't use the type,
but always return the [Plugin-generated serializer](serializers.md#plugin-generated-serializer)
of the `BasicProject` class.  

```kotlin
val module = SerializersModule {
    polymorphic(Project::class) {
        subclass(OwnedProject::class)
        default { BasicProject.serializer() }
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

> You can get the full code [here](../guide/example/example-poly-18.kt).

Notice, how `BasicProject` had also captured the specified type key in its `type` property. 

```text
[BasicProject(name=example, type=unknown), OwnedProject(name=kotlinx.serialization, owner=kotlin)]
```

<!--- TEST -->

---

The next chapter covers [JSON features](json.md).

<!--- MODULE /kotlinx-serialization -->
<!--- INDEX kotlinx.serialization -->
[SerialName]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization/-serial-name/index.html
[PolymorphicSerializer]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization/-polymorphic-serializer/index.html
[Serializable]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization/-serializable/index.html
[Polymorphic]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization/-polymorphic/index.html
[DeserializationStrategy]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization/-deserialization-strategy/index.html
<!--- INDEX kotlinx.serialization.json -->
[Json.encodeToString]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.json/-json/encode-to-string.html
[Json]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.json/-json/index.html
<!--- INDEX kotlinx.serialization.modules -->
[SerializersModule]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.modules/-serializers-module/index.html
[SerializersModule()]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.modules/-serializers-module.html
[polymorphic]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.modules/polymorphic.html
[subclass]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.modules/subclass.html
[plus]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.modules/plus.html
[SerializersModuleBuilder.include]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.modules/-serializers-module-builder/include.html
[PolymorphicModuleBuilder.default]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.modules/-polymorphic-module-builder/default.html
[PolymorphicModuleBuilder]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.modules/-polymorphic-module-builder/index.html
<!--- END -->

