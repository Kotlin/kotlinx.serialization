# Multiplatform polymorphic serialization

## Introduction

Polymorphic serialization is usually a complicated and dangerous feature due to the amount of reflection it brings
and security concerns you should address in your application
(like "what if you accidentally load or deserialize a class that is not allowed to be in this part of the program").

To overcome these drawbacks, we've focused our design on a _serializers registration_. It will eliminate need in cross-platform `Class.forName` analog and will help avoid security problems.

So, to be able to serialize class hierarchies and restore them using a type's fully-qualified name, you should perform these steps:

1. Register all subclasses that may appear in serialization and deserialization in some `SerialModule`.
2. Pass that serial module to a format instance.
3. Mark some properties or classes as `@Polymorphic`.

Step 3 is required if you want to polymorphically serialize an `open` class.
If class is `abstract` or `interface`, `@Polymorphic` annotation inferred automatically
(see more in section [Differences for interfaces, abstract and open classes](#differences-for-interfaces-abstract-and-open-classes)).
Moreover, if the class is `sealed`, we do not need to register all subclasses manually – since they are known at compile time,
compiler plugin can enumerate them automatically (see more in section [Sealed classes](#sealed-classes)).

## Table of contents

 * [Quick start](#quick-start)
 * [Customization](#customization)
    + [Class name](#class-name)
    + [Default serializer](#default-serializer)
 * [Differences for interfaces, abstract and open classes](#differences-for-interfaces-abstract-and-open-classes)
 * [Sealed classes](#sealed-classes)
   + [Sealed classes: before 0.14.0](#sealed-classes-before-0140)
 * [Complex hierarchies with several base classes](#complex-hierarchies-with-several-base-classes)
 * [A word for multi-project applications and library developers](#a-word-for-multi-project-applications-and-library-developers)

## Quick start

Let's break down a basic case with a simple class hierarchy:

```kotlin

interface Message

@Serializable
data class StringMessage(val message: String): Message

@Serializable
data class IntMessage(val number: Int): Message
```

To be able to serialize and deserialize both `StringMessage` and `IntMessage`, we need the following module:

```kotlin
val messageModule = SerializersModule { // 1
    polymorphic(Message::class) { // 2
        subclass<StringMessage>() // 3
        subclass<IntMessage>() // 4
    }
}
```

Line 1) Creates a DSL builder. Line 2) indicates that the nested block describes subclasses of `Message`
(you can have as many `polymorphic` blocks as you'd like, for different classes).
It means that these subclasses can only be serialized/deserialized when the framework encounters `Message` class.
In other words, this module will work with `@Serializable class MessageWrapper(val m: Message)` definition and will NOT work with `@Serializable class MessageWrapper(val m: Any)`.
This design decision was made for security and type-safety – it encourages you to use more specific types instead of `Any`.

Lines 3) and 4) register corresponding actual serializers.
Again, only classes `StringMessage` and `IntMessage` are allowed in the stream – even if you have
`@Serializable class MyInternalSecretMessage: Message`, which should not be exposed to external clients, you shouldn't worry about it.
Kotlinx.serialization throws an exception on an attempt to serialize or deserialize a class polymorphically if it is not registered.

The only thing left to do is to create a format instance with `messageModule`:

```kotlin
val json = Json(context = messageModule)
```

After that, you can use the `json` object as usual:

```kotlin
@Serializable
data class MessageWrapper(val m: Message)

json.stringify(MessageWrapper.serializer(), MessageWrapper(StringMessage("string")))
// {"m":{"type":"package.StringMessage","message":"string"}}

json.stringify(MessageWrapper.serializer(), MessageWrapper(IntMessage(121)))
// {"m":{"type":"package.IntMessage","number":121}}
```

Such an approach works on JVM, JS, and Native without reflection (only with `KClass` comparison and `KClass.isInstance` calls)!

> Pro tip: to use `Message` without a wrapper, you can pass `PolymorphicSerializer(Message::class)` to parse/stringify.

## Customization

### Class name
By default, encoded _type name_ is equal to class' fully-qualified name. To change that, you can annotate the class with `@SerialName` annotation:

```kotlin
// If we have...
@Serializable
@SerialName("msg_number")
data class IntMessage(val number: Int): Message

// then...

json.stringify(MessageWrapper.serializer(), MessageWrapper(IntMessage(121)))
// {"m":{"type":"msg_number","number":121}}
```

JSON with its `JsonConfiguration.Stable` and `JsonConfiguration.Default` offers you
to store the type name inside the object itself with the key `type`.
You can override key name by creating your own configuration: `JsonConfiguration(classDiscriminator = "class")` or
by copying an existing one: `JsonConfiguration.Stable.copy(classDiscriminator = "class")`.

There is also a possibility to change type name storage location to the first element of wrapping array, i.e. to form `[className, object]`:

```kotlin
val json = Json(
    configuration = JsonConfiguration(useArrayPolymorphism = true),
    context = messageModule
)

json.stringify(MessageWrapper.serializer(), MessageWrapper(IntMessage(121)))
// {"m":["msg_number",{"number":121}]}
```

> Note: this form is default and can't be changed for formats that do not support polymorphism natively, e.g., Protobuf.

### Default serializer

It is possible to register a factory of default serializers, e.g. in order to return error object in 
case of unknown subclasses or to migrate API from one version to another.

The following module allows us to deserialize polymorphically both JSON with `successful_response_v2` and `successful_response_v3`
 as its type discriminator:
 
```kotlin
abstract class ApiResponse

@SerialName("successful_response_v3")
class SuccessfulApiResponse(val code: Int) : ApiResponse()

val responseModule = SerializersModule {
    polymorphic(ApiResponse::class) {
        subclass<SuccessfulApiResponse>()
        default { className -> 
            if (className == "successful_response_v2") SuccessfulApiResponse.serializer() // 1
            else null
        }
    }
}
```

Line `1` implies that objects of type `successful_response_v3` and `successful_response_v2` have the same serialized form,
though nothing prevents us to return a different serializer here, potentially customized with runtime behaviour, such as
[JsonContentPolymorphicSerializer](json_transformations.md#json-parametric-polymorphic-deserialization) .

## Differences for interfaces, abstract and open classes

As you know, interfaces and abstract classes can't be instantiated.
It also means that they can't be deserialized, and therefore, they're by default polymorphic. So if we have

```kotlin
interface Message
```

or

```kotlin
@Serializable
abstract class Message
```

These declarations are equivalent:

```kotlin
@Serializable
class MessageWrapper(val message: Message)

@Serializable
class MessageWrapper(@Polymorphic val message: Message)
```

Open classes have a state, can be instantiated and have a usual serializer. So, for

```kotlin
@Serializable
open class Message
```

This declaration will use `Message.serializer()`:
```kotlin
@Serializable
class MessageWrapper(val message: Message)
```

And this will use `PolymorphicSerializer`:
```kotlin
@Serializable
class MessageWrapper(@Polymorphic val message: Message)
```

You can also make `Message` class polymorphic by default by annotating the class itself:

```kotlin
@Serializable
@Polymorphic
open class Message
```

## Sealed classes

Sealed classes in Kotlin use the same inheritance mechanism as abstract and open ones,
so kotlinx.serialization works with them in the same way, too.

However, they have one big difference: compiler knows all subclasses of sealed class anyway,
therefore, the serialization plugin knows them too, and it is possible to serialize subclasses without a user's intervention correctly.
Practically, it means that you do not need `@Polymorphic` and `SerialModule`:

```kotlin
@Serializable
sealed class SimpleSealed {
    @Serializable
    public data class SubSealedA(val s: String) : SimpleSealed()

    @Serializable
    public data class SubSealedB(val i: Int) : SimpleSealed()
}

// will perform correct polymorphic serialization and deserialization:
Json.stringify(SimpleSealed.serializer(), SubSealedA("foo"))
// output will be
// {"type":"package.SimpleSealed.SubSealedA", "s":"foo"}
```

You can use all polymorphism-related settings (see section [A bit of customizing](#a-bit-of-customizing))
and combine sealed hierarchies with abstract ones
(see section [Complex hierarchies with several base classes](#complex-hierarchies-with-several-base-classes) and documentation for `SealedClassSerializer`).

Note that if you use `@Polymorphic` on a property with a type of sealed class,
it will be serialized using a regular polymorphism mechanism (which requires `SerialModule`).
This was an approach in previous versions of the library (see the next section).

### Sealed classes: before 0.14.0

If you, for some reason, use an older version of the library, to serialize sealed hierarchies polymorphically,
you have to perform the same steps as for any other class hierarchy: make a `SerialModule`, register all subclasses, etc., etc.
Moreover, you should explicitly mark every usage of sealed class in `@Serializable` classes as `@Polymorphic`:

```kotlin
// @Serializable is not needed here
sealed class Message {
    // subclasses...
}

val messageModule = SerializersModule {
    polymorphic<Message>() {
        // subclasses...
    }
}

@Serializable
class MessageWrapper(@Polymorphic val m: Message)
// @Polymorphic is required!
```

So, we can say that polymorphic serialization (with explicit `SerialModule`) of sealed classes requires explicit opt-in on use site.
To migrate to newer versions, you'll have to remove all `@Polymorphic` annotations from `val`s.

## Complex hierarchies with several base classes

If you want to register subclasses for multiple base classes, e.g., in a situation like that:

```kotlin
interface Message

@Serializable
abstract class TimestampedMessage : Message {
    abstract val timestamp: Int
}

@Serializable
class Wrapper(
    val request: Message,
    val response: TimestampedMessage
)
```

You can use `polymorphic` overload which accepts several base classes:

```kotlin
val messageModule = SerializersModule {
    polymorphic(Message::class, TimestampedMessage::class) {
        // subclasses...
    }
}
```

You can even add `Any::class` to this list. By doing this, you'll make possible deserialization
of your classes to `@Serializable class Wrapper(@Polymorphic val any: Any)`. Use this feature with caution.

If the base class itself needs serializer (in case it is `open`), you can use `polymorphic`
overload with `KSerializer<BaseClass>` in arguments or just register serializer as usual inside DSL block.

## A word for multi-project applications and library developers

When the application grows in size, it may become inconvenient to store all class hierarchies in one serial module.
And you don't have to: serial modules are composable. They even have `plus` operator:

```kotlin
val json = Json(context = messageModule + anotherModule)
```

Or you can use `include` in the `SerializersModule {}` DSL.

If you're writing a library or shared module with an abstract class and some implementations of it,
you can export your own `MyLibrarySerialModule` for your clients to use so that a client can combine your module with their modules.
