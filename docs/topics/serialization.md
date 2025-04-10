[//]: # (title: Serialization)

**Serialization** is the process of converting data used by an application to a format that can be transferred over a
network or stored in a database or a file. Deserialization is the opposite process of converting external data back into a runtime object.
Together, they are essential to most applications that exchange data with third parties.

Some data serialization formats, such as [JSON](https://www.json.org/json-en.html) and [Protocol Buffers](https://protobuf.dev/) are particularly common.
Being language-neutral and platform-neutral, these formats enable data exchange between systems written in any modern language.

To convert an object tree to a string or to a sequence of bytes, it must go through two mutually intertwined processes: 

1. Serialization: Objects are transformed into a sequence of their primitive values.
This universal process varies depending on the object and is managed by a serializer.
2. Encoding: The primitive sequence is converted into the desired output format, controlled by an encoder.

![Serialization flow](serialization.svg){width=700}

The reverse process involves parsing the input format, decoding the primitive values, and then deserializing the resulting
stream into objects.

If you're new to serialization in Kotlin, we recommend starting with the [Get Started with Serialization](serialization-get-started.md) guide.
This section provides a step-by-step guide to help you set up and use Kotlin serialization in your projects.
By following these steps, you can quickly get up to speed with the basics before diving into more complex topics.

## Kotlin serialization libraries

The `kotlinx.serialization` library offers support for all platforms, including JVM, JavaScript, Native.
It works with various serialization formats, such as JSON, CBOR, and Protocol buffers. For the complete list of supported serialization,
see the [supported formats](#supported-serialization-formats) section.

All Kotlin serialization libraries are part of the `org.jetbrains.kotlinx:` group, with names
starting with `kotlinx-serialization-` and suffixes that reflect the serialization format.
For example:

* `org.jetbrains.kotlinx:kotlinx-serialization-json` provides JSON serialization for Kotlin projects.
* `org.jetbrains.kotlinx:kotlinx-serialization-cbor` provides CBOR serialization.

Platform-specific dependencies are automatically managed, so you don’t need to add them manually.
Use the same dependencies for JVM, JavaScript, Native, and multiplatform projects.

The `kotlinx.serialization` libraries follow their own versioning structure, independent of Kotlin.
You can check out the releases on [GitHub](https://github.com/Kotlin/kotlinx.serialization/releases) to find the latest versions.

## Supported serialization formats

`kotlinx.serialization` includes libraries for various serialization formats:

* [JSON](https://www.json.org/json-en.html): [`kotlinx-serialization-json`](https://github.com/Kotlin/kotlinx.serialization/blob/master/formats/README.md#json)
* [Protocol buffers](https://protobuf.dev/): [`kotlinx-serialization-protobuf`](https://github.com/Kotlin/kotlinx.serialization/blob/master/formats/README.md#protobuf)
* [CBOR](https://cbor.io/): [`kotlinx-serialization-cbor`](https://github.com/Kotlin/kotlinx.serialization/blob/master/formats/README.md#cbor)
* [Properties](https://en.wikipedia.org/wiki/.properties): [`kotlinx-serialization-properties`](https://github.com/Kotlin/kotlinx.serialization/blob/master/formats/README.md#properties)
* [HOCON](https://github.com/lightbend/config/blob/master/HOCON.md): [`kotlinx-serialization-hocon`](https://github.com/Kotlin/kotlinx.serialization/blob/master/formats/README.md#hocon) (only on JVM)

All libraries except JSON serialization (`kotlinx-serialization-json`) are [experimental](components-stability.md), which means their API can be changed without notice.
For more details about JSON serialization, see [JSON serialization overview](configure-json-serialization.md).

There are also community-maintained libraries that support more serialization formats, such as [YAML](https://yaml.org/) or [Apache Avro](https://avro.apache.org/).

You can find out more about experimental serialization formats in [Alternative and custom serialization formats](alternative-serialization-formats.md).

## Supported serialization types

Kotlin serialization supports a variety of built-in types, including all primitive types and composite types from the Kotlin standard library like the `List` type.
For more information, see [Serialize built-in types](serialization-serialize-builtin-types.md).

> Not all types from the Kotlin standard library are serializable. In particular, [ranges](ranges.md) and the [`Regex`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.text/-regex/) class are not serializable at the moment.
> Support for their serialization may be added in the future.
>
{style="note"}

Additionally, classes annotated with `@Serializable` are fully supported for serialization, enabling the conversion of class instances to and from formats like JSON.
For more information, see [Serialize classes](serialization-customization-options.md).

## What's next

* Learn the basics of Kotlin serialization in the [Get started with serialization guide](serialization-get-started.md).
* To explore more complex JSON serialization scenarios, see [JSON serialization overview](configure-json-serialization.md).
* Dive into the [Serialize classes](serialization-customization-options.md) section to learn how to serialize classes and how to modify the default behavior of the `@Serializable` annotation.
