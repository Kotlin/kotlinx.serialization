[//]: # (title: Serialization)

**Serialization** is the process of converting data used by an application to a format that can be transferred over a
network or stored in a database or a file. In turn, deserialization is the opposite process of reading data from an
external source and converting it into a runtime object.
Together, they are essential to most applications that exchange data with third parties.

Some data serialization formats, such as [JSON](https://www.json.org/json-en.html) and [protocol buffers](https://protobuf.dev/) are particularly common.
Being language-neutral and platform-neutral, they enable data exchange between systems written in any modern language.

To convert an object tree to a string or to a sequence of bytes, it must go through two mutually intertwined processes: 

1. Serialization: Objects are transformed into a sequence of their primitive values.
This universal process varies depending on the object and is managed by a serializer.
2. Encoding: The primitive sequence is converted into the desired output format, controlled by an encoder.

![Serialization flow](serialization.svg){width=700}

The reverse process involves parsing the input format, decoding the primitive values, and then deserializing the resulting
stream into objects.

In Kotlin, data serialization tools are available in a separate component, kotlinx.serialization.
It consists of several parts: the `org.jetbrains.kotlin.plugin.serialization` Gradle plugin, runtime libraries, and compiler
plugins.

Compiler plugins, `kotlinx-serialization-compiler-plugin` and `kotlinx-serialization-compiler-plugin-embeddable`,
are published directly to Maven Central. The second plugin is designed for working with the `kotlin-compiler-embeddable`
artifact, which is the default option for scripting artifacts.
Gradle adds compiler plugins to your projects as compiler arguments.

If you're new to serialization in Kotlin, we recommend starting with the [Get Started with Serialization](serialization-get-started.md) guide.
This section provides a step-by-step guide to help you set up and use Kotlin serialization in your projects.
By following these steps, you can quickly get up to speed with the basics before diving into more complex topics.

## Libraries

`kotlinx.serialization` provides sets of libraries for all supported platforms: JVM, JavaScript, Native, and for various
serialization formats, such as JSON, CBOR, and protocol buffers. For the complete list of supported serialization,
see the [supported formats](#supported-formats) section.

All Kotlin serialization libraries belong to the `org.jetbrains.kotlinx:` group.
Their names start with `kotlinx-serialization-` and have suffixes that reflect the serialization format.
For example:

* `org.jetbrains.kotlinx:kotlinx-serialization-json` provides JSON serialization for Kotlin projects.
* `org.jetbrains.kotlinx:kotlinx-serialization-cbor` provides CBOR serialization.

Platform-specific artifacts are handled automatically; you don't need to add them manually.
Use the same dependencies in JVM, JS, Native, and multiplatform projects.

Note that the `kotlinx.serialization` libraries use their own versioning structure, which doesn't match Kotlin's versioning.
Check out the releases on [GitHub](https://github.com/Kotlin/kotlinx.serialization/releases) to find the latest versions.

## Supported formats

`kotlinx.serialization` includes libraries for various serialization formats:

* [JSON](https://www.json.org/json-en.html): [`kotlinx-serialization-json`](https://github.com/Kotlin/kotlinx.serialization/blob/master/formats/README.md#json)
* [Protocol buffers](https://protobuf.dev/): [`kotlinx-serialization-protobuf`](https://github.com/Kotlin/kotlinx.serialization/blob/master/formats/README.md#protobuf)
* [CBOR](https://cbor.io/): [`kotlinx-serialization-cbor`](https://github.com/Kotlin/kotlinx.serialization/blob/master/formats/README.md#cbor)
* [Properties](https://en.wikipedia.org/wiki/.properties): [`kotlinx-serialization-properties`](https://github.com/Kotlin/kotlinx.serialization/blob/master/formats/README.md#properties)
* [HOCON](https://github.com/lightbend/config/blob/master/HOCON.md): [`kotlinx-serialization-hocon`](https://github.com/Kotlin/kotlinx.serialization/blob/master/formats/README.md#hocon) (only on JVM)

Note that all libraries except JSON serialization (`kotlinx-serialization-json`) are [Experimental](https://kotlinlang.org/docs/components-stability.html), which means their API can be changed without notice.

There are also community-maintained libraries that support more serialization formats, such as [YAML](https://yaml.org/) or [Apache Avro](https://avro.apache.org/).

For more details about the available serialization formats, see [Serialization formats](https://github.com/Kotlin/kotlinx.serialization/blob/master/formats/README.md).

## Supported types

Kotlin serialization supports the following built-in primitive types:

* `Boolean`
* `Char`
* Integer types: `Byte`, `Short`, `Int`, and `Long`
* Floating-point types: `Float` and `Double`
* `String`
* `enum`

In addition to primitives Kotlin serialization also supports a number of composite types:

* Pair and Triple: The simple data classes [`Pair`]((https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-pair/)) and [`Triple`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-triple/) from the Kotlin standard library are serializable.
* Lists, Sets, and other collections: The [`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/), [`Set`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-set/), and other [collection](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-collection/) types can be serialized.
These types are represented as lists in formats like JSON. A JSON list can be deserialized into both a `List` and a `Set`, with the `Set` automatically removing duplicate values.
* Maps: A [`Map`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/) with primitive or `enum` keys and arbitrary serializable values can be serialized. In JSON, object keys are always strings, so keys are encoded as strings even if they are numbers in Kotlin. Composite keys are not supported by JSON but can be used in other formats.
* Unit and singleton objects: The built-in [`Unit`]() type and singleton objects are serializable. Singletons are serialized as empty structures.
* Duration: Since Kotlin 1.7.20, the [`Duration`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.time/-duration/) class of the `kotlin.time` package is serializable. It is serialized as a string in the ISO-8601-2 format. For example, "PT16M40S" is 16 minutes and 40 seconds.
* Nothing: The [`Nothing`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-nothing.html) type is also serializable. However, since there are no instances of this class, it is impossible to encode or decode its values. This serializer is used when syntactically some type is needed, but it is not actually used in serialization.

> Not all types from the Kotlin standard library are serializable. In particular, [ranges](ranges.md) and the [`Regex`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.text/-regex/) class are not serializable at the moment.
> Support for their serialization may be added in the future.
> 
{type="note"}

Additionally, classes annotated with `@Serializable` are fully supported for serialization, enabling the conversion of class instances to and from formats like JSON.
For more information, see [Serializable types](serialization-customization-options.md).

For more details and examples about the supported built-in types, see [Serialization GitHub repository](https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/builtin-classes.md).

## What's next

* Learn the basics of Kotlin serialization in the [Get started with serialization guide](serialization-get-started-overview.md).
* To explore more complex scenarios of JSON serialization, see [JSON serialization overview](configure-json-serialization.md).
* Dive into the [Serializable classes](serialization-customization-options.md) section to learn how to modify the default behavior of the `@Serializable` annotation.
