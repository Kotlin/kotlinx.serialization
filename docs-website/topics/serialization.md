[//]: # (title: Serialization)

**Serialization** is the process of converting data used by an application to a format that can be transferred over a
network, or stored in a database or a file. Deserialization is the opposite process of converting external data back into a runtime object.
Together, they are essential to most applications that exchange data with third parties.

Some data serialization formats, such as [JSON](https://www.json.org/json-en.html) and [Protocol Buffers](https://protobuf.dev/), are particularly common.
These formats are language-neutral and platform-neutral, so you can use them to exchange data between systems written in any modern language.
Kotlin provides this functionality through the [`kotlinx.serialization` libraries](#kotlin-serialization-libraries),
which support multiple platforms and data formats.

If you're new to serialization in Kotlin, we recommend starting with the [Get Started with Serialization](serialization-get-started.md) tutorial.
It walks you through adding the Kotlin serialization library to your project and shows you how to serialize and deserialize your first class.

<a href="serialization-get-started.md"><img src="get-started-serialization.svg" width="700" alt="Get started with serialization" style="block"/></a>

## Kotlin serialization libraries

Kotlin serialization offers support for all platforms, including JVM, JavaScript, and Native.
You can use the same [dependency declaration](serialization-get-started.md#add-plugins-and-dependencies-for-kotlin-serialization) regardless of the target platform.

Kotlin serialization supports various serialization formats, such as JSON, CBOR, and Protocol buffers through different serialization format libraries.
These libraries build on the core `kotlinx.serialization` library.
For the complete list of supported serialization formats, see [Supported serialization formats](#supported-serialization-formats).

All Kotlin serialization format libraries are part of the `org.jetbrains.kotlinx:` group, with names
starting with `kotlinx-serialization-` and suffixes that reflect the serialization format.
For example:

* `org.jetbrains.kotlinx:kotlinx-serialization-json` provides JSON serialization.
* `org.jetbrains.kotlinx:kotlinx-serialization-cbor` provides CBOR serialization.

The `kotlinx.serialization` libraries follow their own versioning, independent of Kotlin.
You can find the latest release versions on [GitHub](https://github.com/Kotlin/kotlinx.serialization/releases).

> When you're not using a specific format library, for example, when you're writing your own serialization format,
> use the `kotlinx-serialization-core` library as the dependency.
>
{style="tip"}

## Supported serialization formats

`kotlinx.serialization` includes serialization format libraries for various formats:

| Format       | Artifact ID                                                                                                                    | Platform                | Status       |
|--------------|--------------------------------------------------------------------------------------------------------------------------------|-------------------------|--------------|
| [JSON](https://www.json.org/json-en.html)            | [`kotlinx-serialization-json`](https://github.com/Kotlin/kotlinx.serialization/blob/master/formats/README.md#json)             | All supported platforms | Stable       |
| [HOCON](https://github.com/lightbend/config/blob/master/HOCON.md) | [`kotlinx-serialization-hocon`](https://github.com/Kotlin/kotlinx.serialization/blob/master/formats/README.md#hocon)           | JVM only                | Experimental |
| [Protocol Buffers](https://protobuf.dev/)            | [`kotlinx-serialization-protobuf`](https://github.com/Kotlin/kotlinx.serialization/blob/master/formats/README.md#protobuf)     | All supported platforms | Experimental |
| [CBOR](https://cbor.io/)                             | [`kotlinx-serialization-cbor`](https://github.com/Kotlin/kotlinx.serialization/blob/master/formats/README.md#cbor)             | All supported platforms | Experimental |
| [Properties](https://en.wikipedia.org/wiki/.properties) | [`kotlinx-serialization-properties`](https://github.com/Kotlin/kotlinx.serialization/blob/master/formats/README.md#properties) | All supported platforms | Experimental |

All serialization format libraries, except for the JSON serialization library (`kotlinx-serialization-json`), are [Experimental](components-stability.md). Their APIs might change at any time.
For more details about JSON serialization, see [JSON serialization overview](configure-json-serialization.md).

There are also community-maintained libraries that support more serialization formats, such as [YAML](https://yaml.org/) or [Apache Avro](https://avro.apache.org/).

You can find out more about experimental serialization formats in [Alternative and custom serialization formats](alternative-serialization-formats.md).

## Supported serialization types

Kotlin serialization supports a variety of built-in types, including all primitive types and most composite types from the Kotlin standard library like the `List` type.
For more information, see [Serialize built-in types](serialization-serialize-builtin-types.md).

Additionally, classes annotated with `@Serializable` are fully supported for serialization, enabling the conversion of class instances to and from formats like JSON.
For more information, see [Serialize classes](serialization-customization-options.md).

## What's next

* Learn the basics of Kotlin serialization in the [Get started with serialization tutorial](serialization-get-started.md).
* See how the Kotlin serialization library processes [primitives, collections, and other built-in types](serialization-serialize-builtin-types.md)
* Explore more complex JSON serialization scenarios in the [JSON serialization overview](configure-json-serialization.md).
* Dive into [Serialize classes](serialization-customization-options.md) to learn how to serialize classes and modify the default behavior of the `@Serializable` annotation.
* Learn how to define and customize your own serializers in [Create custom serializers](serialization-custom-serializers.md).
* See how to serialize different types through a shared base type in [Serialize polymorphic classes](serialization-polymorphism.md).