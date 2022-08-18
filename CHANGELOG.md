1.4.0 / 2022-08-18
==================

This release contains all features and bugfixes from 1.4.0-RC plus some bugfixes on its own (see below).
Kotlin 1.7.10 is used as a default.

### Bugfixes
  * Fixed decoding of huge JSON data for okio streams (#2006)


1.4.0-RC / 2022-07-20
==================

This is a candidate for the next big release with many new exciting features to try.
It uses Kotlin 1.7.10 by default.

### Integration with Okio's BufferedSource and BufferedSink

[Okio library by Square](https://square.github.io/okio/) is a popular solution for fast and efficient IO operations on JVM, K/N and K/JS.
In this version, we have added functions that parse/write JSON directly to Okio's input/output classes, saving you the overhead of copying data to `String` beforehand.
These functions are called `Json.decodeFromBufferedSource` and `Json.encodeToBufferedSink`, respectively.
There's also `decodeBufferedSourceToSequence` that behaves similarly to `decodeToSequence` from Java streams integration, so you can lazily decode multiple objects the same way as before.

Note that these functions are located in a separate new artifact, so users who don't need them wouldn't find themselves dependent on Okio.
To include this artifact in your project, use the same group id `org.jetbrains.kotlinx` and artifact id `kotlinx-serialization-json-okio`.
To find out more about this integration, check new functions' documentation and corresponding pull requests:
[#1901](https://github.com/Kotlin/kotlinx.serialization/pull/1901) and [#1982](https://github.com/Kotlin/kotlinx.serialization/pull/1982).

### Inline classes and unsigned numbers do not require experimental annotations anymore

Inline classes and unsigned number types have been promoted to a Stable feature in Kotlin 1.5,
and now we are promoting support for them in kotlinx.serialization to Stable status, too.
To be precise, [we've removed all](https://github.com/Kotlin/kotlinx.serialization/pull/1963) `@ExperimentalSerializationApi` annotations from functions related to inline classes encoding and decoding,
namely `SerialDescriptor.isInline`, `Encoder.encodeInline`, and some others. We've also updated related [documentation article](docs/value-classes.md).

Additionally, all `@ExperimentalUnsignedTypes` annotations [were removed](https://github.com/Kotlin/kotlinx.serialization/pull/1962) completely,
so you can freely use types such as `UInt` and their respective serializers as a stable feature
without opt-in requirement.

### Part of SerializationException's hierarchy is public now

When kotlinx.serialization 1.0 was released, all subclasses of `SerializationException` were made internal,
since they didn't provide helpful information besides the standard message.
Since then, we've received a lot of feature requests with compelling use-cases for exposing some of these internal types to the public.
In this release, we are starting to fulfilling these requests by making `MissingFieldException` public.
One can use it in the `catch` clause to better understand the reasons of failure — for example, to return 400 instead of 500 from an HTTP server —
and then use its `fields` property to communicate the message better.
See the details in the corresponding [PR](https://github.com/Kotlin/kotlinx.serialization/pull/1983).

In future releases, we'll continue work in this direction, and we aim to provide more useful public exception types & properties.
In the meantime, we've [revamped KDoc](https://github.com/Kotlin/kotlinx.serialization/pull/1980) for some methods regarding the exceptions — all of them now properly declare which exception types are allowed to be thrown.
For example, `KSerializer.deserialize` is documented to throw `IllegalStateException` to indicate problems unrelated to serialization, such as data validation in classes' constructors.

### @MetaSerializable annotation

This release introduces a new `@MetaSerializable` annotation that adds `@Serializable` behavior to user-defined annotations — i.e., those annotations would also instruct the compiler plugin to generate a serializer for class. In addition, all annotations marked with `@MetaSerializable` are saved in the generated `@SerialDescriptor`
as if they are annotated with `@SerialInfo`.

This annotation will be particularly useful for various format authors who require adding some metadata to the serializable class — this can now be done using a single annotation instead of two, and without the risk of forgetting `@Serializable`. Check out details & examples in the KDoc and corresponding [PR](https://github.com/Kotlin/kotlinx.serialization/pull/1979).

> Note: Kotlin 1.7.0 or higher is required for this feature to work.

### Moving documentation from GitHub pages to kotlinlang.org

As a part of a coordinated effort to unify kotlinx libraries users' experience, Dokka-generated documentation pages (KDoc) were moved from https://kotlin.github.io/kotlinx.serialization/ to https://kotlinlang.org/api/kotlinx.serialization/. No action from you is required — there are proper redirects at the former address, so there is no need to worry about links in your blogpost getting obsolete or broken.

Note that this move does not affect guides written in Markdown in the `docs` folder. We aim to move them later, enriching text with runnable examples as in the Kotlin language guides.

### Other improvements

  * Allow Kotlin's null literal in JSON DSL (#1907) (thanks to [Lukellmann](https://github.com/Lukellmann))
  * Stabilize EmptySerializersModule (#1921)
  * Boost performance of polymorphic deserialization in optimistic scenario (#1919)
  * Added serializer for the `kotlin.time.Duration` class (plugin support comes in Kotlin 1.7.20) (#1960)
  * Support tagged not null marks in TaggedEncoder/Decoder (#1954) (thanks to [EdwarDDay](https://github.com/EdwarDDay))

### Bugfixes

  * Support quoting unsigned integers when used as map keys (#1969)
  * Fix protocol buffer enum schema generation (#1967) (thanks to [mogud](https://github.com/mogud))
  * Support diamond inheritance of sealed interfaces in SealedClassSerializer (#1958)
  * Support retrieving serializer for sealed interface  (#1968)
  * Fix misleading token description in JSON errors (#1941) (thanks to [TheMrMilchmann](https://github.com/TheMrMilchmann))

1.3.3 / 2022-05-11
==================

This release contains support for Protocol Buffers packed fields, as well as several bugfixes.
It uses Kotlin 1.6.21 by default.

### Protobuf packed fields

It is now possible to encode and decode Kotlin classes to/from Protobuf messages with [packed repeated fields](https://developers.google.com/protocol-buffers/docs/encoding#packed).
To mark the field as packed, use `@ProtoPacked` annotation on it.
Note it affects only `List` and primitive collection such as `IntArray` types.
With this feature, it is now possible to decode Proto3 messages, where all repeated fields are packed by default.
[Protobuf schema generator](https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization-protobuf/kotlinx.serialization.protobuf.schema/-proto-buf-schema-generator/index.html) also supports new `@ProtoPacked` annotation.

Many thanks to [Paul de Vrieze](https://github.com/pdvrieze) for his valuable contribution!

### Other improvements & small features

  * Incorporate JsonPath into exception messages (#1841)
  * Mark block in corresponding encodeStructure/decodeStructure extensions as crossinline to reduce amount of bytecode (#1917)
  * Support serialization of compile-time `Collection<E>` properties that are not lists at the runtime (#1821)
  * Best-effort kotlin reflect avoidance in serializer(Type) (#1819)

### Bugfixes

  * Iterate over element indices in ObjectSerializer in order to let the format skip unknown keys (#1916)
  * Correctly support registering both default polymorphic serializer & deserializer (#1849)
  * Make error message for captured generic type parameters much more straightforward (#1863)

1.3.2 / 2021-12-23
==================

This release contains several features and bugfixes for core API as well as for HOCON format.
It uses Kotlin 1.6.10 by default.

### Serializing objects to HOCON

It's now possible to encode Kotlin objects to `Config` values with new `Hocon.encodeToConfig` function.
This feature may help edit existing configs inside Kotlin program or generate new ones.

Big thanks to [Osip Fatkullin](https://github.com/osipxd) for [implementing](https://github.com/Kotlin/kotlinx.serialization/pull/1740) this.

### Polymorphic default serializers

As of now, `polymorphicDefault` clause inside `SerializersModule { }` builder specifies a
fallback serializer to be used only during deserialization process. A new function has been introduced to allow setting
fallback serializer for serialization: `polymorphicDefaultSerializer`.
This function should ease serializing vast hierarchies of third-party or Java classes.

Note that there are two new experimental functions, `polymorphicDefaultSerializer` and `polymorphicDefaultDeserializer`.
To avoid naming confusion, we are going to deprecate `polymorphicDefault` in favor of `polymorphicDefaultDeserializer` in the next minor release (1.4.0).

Credit for [the PR](https://github.com/Kotlin/kotlinx.serialization/pull/1686) goes to our contributor [Joseph Burton](https://github.com/Earthcomputer).

### Other improvements

  * HOCON: parse strings into integers and booleans if possible (#1795) (thanks to [tobiaslieber](https://github.com/tobiaslieber))
  * Add an encodeCollection extensions (#1749) (thanks to [Nicklas Ansman Giertz](https://github.com/ansman))

### Bugfixes

  * Properly handle top-level value classes in encodeToJsonElement (#1777)
  * Fix incorrect handling of object end when JsonTreeReader (JsonElement) is used with decodeToSequence (#1782)

1.3.1 / 2021-11-11
==================

This release mainly contains bugfixes for 1.3.0 and provides new experimental `Json.decodeToSequence` function.

### Improvements

  * Provide decodeToSequence to read multiple objects from stream lazily (#1691)

### Bugfixes

  * Correctly handle buffer boundaries while decoding escape sequences from json stream (#1706)
  * Properly skip unknown keys for objects and structures with zero properties (#1720)
  * Fix merging for maplikeSerializer when the map is not empty (by using the actual size * 2). (#1712) (thanks to [pdvrieze](https://github.com/pdvrieze))
  * Fix lookup of primitive array serializers by Java type token (#1708)


1.3.0 / 2021-09-23
==================

This release contains all of the cool new features from 1.3.0-RC (see below) as well as minor improvements.
It uses Kotlin 1.5.31 by default.

### Bugfixes and improvements

  * Promote JsonConfiguration and its usages to stable (#1690)
  * Remove opt-in annotations from SerialFormat, StringFormat, BinaryFormat (#1688)
  * Correctly throw SerializationException instead of IOOBE for some cases with EOF in streams (#1677)
  * CBOR: ignore tags when reading (#1614) (thanks to [David Robertson](https://github.com/DavidJRobertson))

1.3.0-RC / 2021-09-06
==================

This is a release candidate for the next version. It contains a lot of interesting features and improvements,
so we ask you to evaluate it and share your feedback.
Kotlin 1.5.30 is used by default.

### Java IO stream-based JSON serialization

Finally, in `kotlinx.serialization` 1.3.0 we’re presenting the first experimental version of the serialization API for IO streams:
`Json.encodeToStream` and `Json.decodeFromStream` extension functions.
With this API, you can decode objects directly from files, network connections, and other data sources without reading the data to strings beforehand.
The opposite operation is also available: you can send encoded objects directly to files and other streams in a single API call.
IO stream serialization is available only on the JVM platform and for the JSON format for now.

Check out more in the [PR](https://github.com/Kotlin/kotlinx.serialization/pull/1569).

### Property-level control over defaults values encoding

Previous versions of the library allowed to specify whether to encode or drop default properties values with
format configuration flags such as `Json { encodeDefaults = false }`.
In 1.3.0 we’re extending this feature by adding a new way to fine-tune the serialization of default values:
you can now control it on the property level using the new `@EncodeDefaults` annotation.

`@EncodeDefaults` annotation has a higher priority over the `encodeDefaults` property and takes one of two possible values:
- `ALWAYS` (default value) encodes a property value even if it equals to default.
- `NEVER` doesn’t encode the default value regardless of the format configuration.

Encoding of the annotated properties is not affected by `encodeDefault` format flag
and works as described for all serialization formats, not only JSON.

To learn more, check corresponding [PR](https://github.com/Kotlin/kotlinx.serialization/pull/1528).

### Excluding null values from JSON serialization

In 1.3.0, we’re introducing one more way to reduce the size of the generated JSON strings: omitting null values.
A new JSON configuration property `explicitNulls` defines whether `null` property values should be included in the serialized JSON string.
The difference from `encodeDefaults` is that `explicitNulls = false` flag drops null values even if the property does not have a default value.
Upon deserializing such a missing property, a `null` or default value (if it exists) will be used.

To maintain backwards compatibility, this flag is set to `true` by default.
You can learn more in the [documentation](docs/json.md#explicit-nulls) or the [PR](https://github.com/Kotlin/kotlinx.serialization/pull/1535).

### Per-hierarchy polymorphic class discriminators

In previous versions, you could change the discriminator name using the
[classDiscriminator](https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/json.md#class-discriminator) property of the `Json` instance.
In 1.3.0, we’re adding a way to set a custom discriminator name for each class hierarchy to enable more flexible serialization.
You can do it by annotating a class with `@JsonClassDiscriminator` with the discriminator name as its argument.
A custom discriminator is applied to the annotated class and its subclasses.
Only one custom discriminator can be used in each class hierarchy, thanks to the new `@InheritableSerialInfo` annotation.

Check out corresponding [PR](https://github.com/Kotlin/kotlinx.serialization/pull/1608) for details.

### Support for Java module system

Now all kotlinx.serialization runtime libraries are shipped as a multi-release JAR with `module-info.class` file for Java versions 9 and higher.
This enables possibilities to use kotlinx.serialization with modern tools such as `jlink` and various technologies such as `TorandoFX`.

Many thanks to our contributor [Gerard de Leeuw](https://github.com/lion7) and his [PR](https://github.com/Kotlin/kotlinx.serialization/pull/1624) for making this possible.

### Native targets for Apple Silicon

This release includes klibs for new targets, introduced in Kotlin/Native 1.5.30 —
`macosArm64`, `iosSimulatorArm64`, `watchosSimulatorArm64`, and `tvosSimulatorArm64`.

### Bugfixes and improvements

  * Properly handle quoted 'null' literals in lenient mode (#1637)
  * Switch on deep recursive function when nested level of JSON is too deep (#1596)
  * Support for local serializable classes in IR compiler
  * Support default values for `@SerialInfo` annotations in IR compiler
  * Improve error message for JsonTreeReader (#1597)
  * Add guide for delegating serializers and wrapping serial descriptor (#1591)
  * Set target JVM version to 8 for Hocon module in Gradle metadata (#1661)

1.2.2 / 2021-07-08
==================

This release contains various bugfixes, some useful features and important performance improvements.
It also uses Kotlin 1.5.20 as default.

### Features

  * Support for `@JsonNames` and `coerceInputValues` in `Json.decodeFromDynamic` (#1479)
  * Add factory function to wrap a serial descriptor with a custom name for custom delegating serializers (#1547) (thanks to [Fadenfire](https://github.com/Fadenfire))
  * Allow contextually serialized types to be used as map keys in Json (#1552) (thanks to [pdvrieze](https://github.com/pdvrieze))

### Bugfixes and performance improvements

  * Update size in `JsonStringBuilder` slow-path to avoid excessive array-copies for large strings with escape symbols (#1491)
  * Optimize integer encoding length in CBOR (#1570) (thanks to [davertay](https://github.com/davertay))
  * Throw `JsonDecodingException` instead of `ClassCastException` during unexpected null in `TreeJsonDecoder` (#1550)
  * Prohibit 'null' strings in lenient mode in order to get rid of 'null' and "null" ambiguity (#1549)
  * Avoid usage of reflective-like `serialDescriptor<KType>` in production sources (#1540)
  * Added correct error message when deserializing missing enum member for Properties format (#1539)
  * Make `DescriptorSchemaCache` in Json thread-local on Native (#1484)

1.2.1 / 2021-05-14
==================

This release mainly contains bugfixes for various issues, including important [broken thread-safety](https://github.com/Kotlin/kotlinx.serialization/issues/1455) and [improper encoding](https://github.com/Kotlin/kotlinx.serialization/issues/1441).

### Features

  * Added support for nullable values, nested and empty collections in protobuf (#1430)

### Bugfixes

  * Support @JsonNames for enum values (#1473)
  * Handle EOF in skipElement correctly (#1475)
  * Allow using value classes with primitive carriers as map keys (#1470)
  * Read JsonNull only for non-string literals in JsonTreeReader (#1466)
  * Properly reuse JsonStringBuilders in CharArrayPool (#1455)
  * Properly ensure capacity of the string builder on the append slow-path (#1441)

1.2.0 / 2021-04-27
==================

**This release has some known critical bugs, so we advise to use 1.2.1 instead.**

This release contains a lot of new features and important improvements listed below;
Kotlin 1.5.0 is used as a default compiler and language version.

### JSON performance improvements

JSON encoder and decoder were revisited and significantly rewritten,
which lead us to up to 2-3x times speedup in certain cases.
Additional details can be found in the corresponding issues: [[1]](https://github.com/Kotlin/kotlinx.serialization/pull/1343), [[2]](https://github.com/Kotlin/kotlinx.serialization/pull/1354).

### Ability to specify alternative names during JSON decoding

[The one of the most voted issues](https://github.com/Kotlin/kotlinx.serialization/issues/203) is fixed now — it is possible to specify multiple names for one property
using new `@JsonNames` annotation.
Unlike `@SerialName`, it only affects JSON decoding, so it is useful when dealing with different versions of the API.
We've prepared a [documentation](https://github.com/Kotlin/kotlinx.serialization/blob/dev/docs/json.md#alternative-json-names) for you about it.

### JsonConfiguration in public API

`JsonConfiguration` is exposed as a property of `Json` instance. You can use it to adjust behavior in
your [custom serializers](https://github.com/Kotlin/kotlinx.serialization/blob/dev/docs/json.md#maintaining-custom-json-attributes).
Check out more in the corresponding [issue](https://github.com/Kotlin/kotlinx.serialization/issues/1361) and the [PR](https://github.com/Kotlin/kotlinx.serialization/pull/1409).

### Generator for .proto files based on serializable Kotlin classes

Our implementation of Protocol Buffers format uses `@Serializable` Kotlin classes as a source of schema.
This is very convenient for Kotlin-to-Kotlin communication, but makes interoperability between languages complicated.
To resolve this [issue](https://github.com/Kotlin/kotlinx.serialization/issues/34), we now have a
schema generator that can produce .proto files out of Kotlin classes. Using it, you can keep Kotlin
classes as a source of truth and use traditional protoc compilers for other languages at the same time.
To learn more, check out the documentation for the new `ProtoBufSchemaGenerator` class or
visit the [corresponding PR](https://github.com/Kotlin/kotlinx.serialization/pull/1255).

>Note: this generator is on its experimental stage and any feedback is very welcomed.

### Contextual serialization of generic classes

Before 1.2.0, it was [impossible](https://github.com/Kotlin/kotlinx.serialization/issues/1407) to register context serializer for generic class,
because `contextual` function accepted a single serializer.
Now it is possible to register a provider — lambda that allows to construct a serializer for generic class
out of its type arguments serializers. See the details in the [documentation](https://github.com/Kotlin/kotlinx.serialization/blob/dev/docs/serializers.md#contextual-serialization-and-generic-classes).

### Other features

  * Support for watchosX64 target (#1366).
  * Introduce kotlinx-serialization-bom (#1356).
  * Support serializer<T> on JS IR when T is an interface (#1431).

### Bugfixes

  * Fix serializer lookup by KType for third party classes (#1397) (thanks to [mvdbos](https://github.com/mvdbos)).
  * Fix inability to encode/decode inline class with string to JsonElement (#1408).
  * Throw SerializationException instead of AIOB in ProtoBuf (#1373).
  * Fix numeric overflow in JsonLexer (#1367) (thanks to [EdwarDDay](https://github.com/EdwarDDay)).


1.1.0 / 2021-02-17
==================

This release contains all features and bugfixes from 1.1.0-RC plus an additional fix for incorrect exception type
(#1325 — Throw `SerializationException` instead of `IllegalStateException` in `EnumSerializer`) and uses release version of Kotlin 1.4.30.

In the light of [JCenter shutdown](https://jfrog.com/blog/into-the-sunset-bintray-jcenter-gocenter-and-chartcenter/), starting from 1.1.0-RC and now on,
all new releases of kotlinx.serialization are published directly to Maven Central and therefore are not available in `https://kotlin.bintray.com/kotlinx/` repository.
We suggest you to remove `jcenter()` and other kotlin bintray repositories from your buildscripts and to use `mavenCentral()` repository instead.

1.1.0-RC / 2021-02-03
==================

This is a release candidate of 1.1.0 version. Note that final 1.1.0 version may include more features and bugfixes,
which would be listed in the corresponding changelog.

### Kotlin version requirement updated

Due to changes in calling conventions between compiler plugin and serialization core runtime, this release requires
Kotlin version at least 1.4.30-M1. However, this changes should not affect your code,
because only deprecated functions were removed from public API.
See [corresponding PR](https://github.com/Kotlin/kotlinx.serialization/pull/1260) for the details.

### Experimental support for inline classes (IR only)

Using 1.1.0-RC, you can mark inline classes as `@Serializable` and use them in other serializable classes.
Unsigned integer types (`UByte`, `UShort`, `UInt` and `ULong`) are serializable as well and have special support in JSON.
This feature requires Kotlin compiler 1.4.30-RC and enabling new IR compilers for [JS](https://kotlinlang.org/docs/reference/js-ir-compiler.html) and [JVM](https://kotlinlang.org/docs/reference/whatsnew14.html#new-jvm-ir-backend).

You can learn more in the [documentation](docs/value-classes.md)
and corresponding [pull request](https://github.com/Kotlin/kotlinx.serialization/pull/1244).

### Other features

  * Add `serializerOrNull` function for `KType` and `Type` arguments (#1164)
  * Allow shared prefix names in `Properties` (#1183) (thanks to [TorRanfelt](https://github.com/TorRanfelt))
  * Add support for encoding/decoding `Properties` values as Strings (#1158) (thanks to [daniel-jasinski](https://github.com/daniel-jasinski))

### Bugfixes and performance improvements

  * Support contextual serialization for derived classes (#1277) (thanks to [Martin Raison](https://github.com/martinraison))
  * Ensure serialization is usable from K/N background thread (#1282)
  * Fail on primitive type overflow during `JsonElement` deserialization (#1300)
  * Throw `SerializationException` instead of ISE when encountering an invalid boolean in JSON (#1299)
  * Optimize the loop for writing large varints in `ProtoBuf` (#1294)
  * Fix serializing property with custom accessors and backing field (#1197)
  * Optimize check for missing fields in deserialization and improve `MissingFieldException` message (#1153)
  * Improved support of nullable serializer in `@UseSerializers` annotation  (#1195)
  * Correctly escape keys in `JsonObject.toString()` (#1246) (thanks to [Karlatemp](https://github.com/Karlatemp))
  * Treat `Collection` as `ArrayList` in serializer by type lookups (#1257)
  * Do not try to end structure in encode/decode structure extensions if an exception has been thrown, so the original exception will be propagated (#1201)
  * Properly cache serial names in order to improve performance of JSON parser with strict mode (#1209)
  * Fix dynamic serialization for nullable values (#1199) (thanks to [ankushg](https://github.com/ankushg))

1.0.1 / 2020-10-28
==================

This patch release contains several feature improvements as well as bugfixes and performance improvements.

### Features
  * Add object-based serialization and deserialization of polymorphic types for `dynamic` conversions on JS platform  (#1122)
  * Add support for object polymorphism in HOCON decoder (#1136)
  * Add support of decoding map in the root of HOCON config (#1106)

### Bugfixes
  * Properly cache generated serializers in PluginGeneratedSerialDescriptor (#1159)
  * Add Pair and Triple to serializer resolving from Java type token (#1160)
  * Fix deserialization of half-precision, float and double types in CBOR  (#1112)
  * Fix ByteString annotation detection when ByteArray is nullable (#1139)

1.0.0 / 2020-10-08
==================

The first public stable release, yay!
The definitions of stability and backwards compatibility guarantees are located in the [corresponding document](docs/compatibility.md).
We now also have a GitHub Pages site with [full API reference](https://kotlinlang.org/api/kotlinx.serialization/).

Compared to RC2, no new features apart from #947 were added and all previously deprecated declarations and migrations were deleted.
If you are using RC/RC2 along with deprecated declarations, please, migrate before updating to 1.0.0.
In case you are using pre-1.0 versions (e.g. 0.20.0), please refer to our [migration guide](docs/migration.md).

### Bugfixes and improvements

  * Support nullable types at top-level for JsonElement decoding (#1117)
  * Add CBOR ignoreUnknownKeys option (#947) (thanks to [Travis Wyatt](https://github.com/twyatt))
  * Fix incorrect documentation of `encodeDefaults` (#1108) (thanks to [Anders Carling](https://github.com/anderscarling))

1.0.0-RC2 / 2020-09-21
==================

Second release candidate for 1.0.0 version. This RC contains tweaks and changes based on users feedback after 1.0.0-RC.

### Major changes

* JSON format is now located in different artifact (#994)

In 1.0.0-RC, the `kotlinx-serialization-core` artifact contained core serialization entities as well as `Json` serial format.
We've decided to change that and to make `core` format-agnostic.
It would make the life easier for those who use other serial formats and also make possible to write your own implementation of JSON
or another format without unnecessary dependency on the default one.

In 1.0.0-RC2, `Json` class and related entities are located in `kotlinx-serialization-json` artifact.
To migrate, simply replace `kotlinx-serialization-core` dependency with `-json`. Core library then will be included automatically
as the transitive dependency.

For most use-cases, you should use new `kotlinx-serialization-json` artifact. Use `kotlinx-serialization-core` if you are
writing a library that depends on kotlinx.serialization in a format-agnostic way of provides its own serial format.

* `encodeDefaults` flag is now set to `false` in the default configuration for JSON, CBOR and Protocol Buffers.

The change is motivated by the fact that in most real-life scenarios, this flag is set to `false` anyway,
because such configuration reduces visual clutter and saves amount of data being serialized.
Other libraries, like GSON and Moshi, also have this behavior by default.

This may change how your serialized data looks like, if you have not set value for `encodeDefaults` flag explicitly.
We anticipate that most users already had done this, so no migration is required.
In case you need to return to the old behavior, simply add `encodeDefaults = true` to your configuration while creating `Json/Cbor/ProtoBuf` object.

* Move `Json.encodeToDynamic/Json.decodeFromDynamic` functions to json package

Since these functions are no longer exposed via `DynamicObjectParser/Serializer` and they are now `Json` class extensions,
they should be moved to `kotlinx.serialization.json` package.
To migrate, simply add `import kotlinx.serialization.json.*` to your files.


### Bugfixes and improvements

  * Do not provide default implementation for serializersModule in AbstractEncoder/Decoder (#1089)
  * Support JsonElement hierarchy in `dynamic` encoding/decoding (#1080)
  * Support top-level primitives and primitive map keys in `dynamic` encoding/decoding
  * Change core annotations retention (#1083)
  * Fix 'Duplicate class ... found in modules' on Gradle != 6.1.1 (#996)
  * Various documentation clarifications
  * Support deserialization of top-level nullable types (#1038)
  * Make most serialization exceptions eligible for coroutines exception recovery (#1054)
  * Get rid of methods that do not present in Android API<24 (#1013, #1040)
  * Throw JsonDecodingException on empty string literal at the end of the input (#1011)
  * Remove new lines in deprecation warnings that caused errors in ObjC interop (#990)

1.0.0-RC / 2020-08-17
==================

Release candidate for 1.0.0 version. The goal of RC release is to collect feedback from users
and provide 1.0.0 release with bug fixes and improvements based on that feedback.

While working on 1.0.0 version, we carefully examined every public API declaration of the library and
split it to stable API, that we promise to be source and binary-compatible,
and experimental API, that may be changed in the future.
Experimental API is annotated with `@ExperimentalSerializationApi` annotation, which requires opt-in.
For a more detailed description of the guarantees, please refer to the [compatibility guide](docs/compatibility.md).

The id of the core artifact with `@Serializable` annotation and `Json` format was changed
from `kotlinx-serialization-runtime` to `kotlinx-serialization-core` to be more clear and aligned with other kotlinx libraries.

A significant part of the public API was renamed or extracted to a separate package.
To migrate from the previous versions of the library, please refer to the [migration guide](docs/migration.md).

### API changes

#### Json

* Core API changes
    * `stringify` and `parse` are renamed to `encodeToString` and `decodeFromString`
    * `parseJson` and `fromJson` are renamed to `parseToJsonElement` and `decodeFromJsonElement`
    * Reified versions of methods are extracted to extensions

* `Json` constructor is replaced with `Json {}` builder function, `JsonConfiguration` is deprecated in favor
of `Json {}` builder
    * All default `Json` implementations are removed
   * `Json` companion object extends `Json`

* Json configuration
    * `prettyPrintIndent` allows only whitespaces
    * `serializeSpecialFloatingPointValues` is renamed to `allowSpecialFloatingPointValues`. It now affects both serialization and deserialization behaviour
    * `unquoted` JSON flag is deprecated for removal
    * New `coerceInputValues` option for null-defaults and unknown enums (#90, #246)

* Simplification of `JsonElement` API
    * Redundant members of `JsonElement` API are deprecated or extracted to extensions
    * Potential error-prone API is removed
    * `JsonLiteral` is deprecated in favor of `JsonPrimitive` constructors with nullable parameter

* `JsonElement` builders rework to be aligned with stdlib collection builders (#418, #627)
    * Deprecated infix `to` and unaryPlus in JSON DSL in favor of `put`/`add` functions
    * `jsonObject {}` and `json {}` builders are renamed to `buildJsonObject {}` and `buildJsonArray {}`
    * Make all builders `inline` (#703)

* JavaScript support
    * `DynamicObjectParser` is deprecated in the favor of `Json.decodeFromDynamic` extension functions
    * `Json.encodeToDynamic` extension is added as a counterpart to `Json.decodeFromDynamic` (former `DynamicObjectParser`) (#116)

* Other API changes:
    * `JsonInput` and `JsonOutput` are renamed to `JsonDecoder` and `JsonEncoder`
    * Methods in `JsonTransformingSerializer` are renamed to `transformSerialize` and `transformDeserialize`
    * `JsonParametricSerializer` is renamed to `JsonContentPolymorphicSerializer`
    * `JsonEncodingException` and `JsonDecodingException` are made internal

* Bug fixes
    * `IllegalStateException` when `null` occurs in JSON input in the place of an expected non-null object (#816)
    * java.util.NoSuchElementException when deserializing twice from the same JsonElement (#807)

#### Core API for format authoring

* The new naming scheme for `SerialFormats`
   *  Core functions in `StringFormat` and `BinaryFormat` are renamed and now follow the same naming scheme
   * `stringify`/`parse` are renamed to `encodeToString`/`decodeFromString`
   * `encodeToByteArray`/`encodeToHexString`/`decodeFromByteArray`/`decodeFromHexString` in `BinaryFormat` are introduced instead of `dump`/`dumps`/`load`/`loads`

* New format instances building convention
   * Constructors replaced with builder-function with the same name to have the ability to add new configuration parameters,
   while preserving both source and binary compatibility
   * Format's companion objects now extend format class and can be used interchangeably

* SerialDescriptor-related API
    * `SerialDescriptor` and `SerialKind` are moved to a separate `kotlinx.serialization.descriptors` package
    * `ENUM` and `CONTEXTUAL` kinds now extend `SerialKind` directly
    * `PrimitiveDescriptor` is renamed to `PrimitiveSerialDescriptor`
    * Provide specific `buildClassSerialDescriptor` to use with classes' custom serializers, creating other kinds is considered experimental for now
    * Replace extensions that returned lists (e.g. `elementDescriptors`) with properties that return iterable as an optimization
    * `IndexOutOfBoundsException` in `descriptor.getElementDescriptor(index)` for `List` after upgrade to 0.20.0 is fixed (#739)

* SerializersModule-related API
    * `SerialModule` is renamed to `SerializersModule`
    * `SerialModuleCollector` is renamed to `SerializersModuleCollector`
    * All builders renamed to be aligned with a single naming scheme (e.g. `SerializersModule {}` DSL)
    * Deprecate infix `with` in polymorphic builder in favor of subclass()
    * Helper-like API is extracted to extension functions where possible.
    * `polymorphicDefault` API for cases when type discriminator is not registered or absent (#902)

* Contextual serialization
    * `@ContextualSerialization` is split into two annotations: `@Contextual` to use on properties and `@UseContextualSerialization` to use on file
    *  New `SerialDescriptor.capturedKClass` API to introspect SerializersModule-based contextual and polymorphic kinds (#515, #595)

* Encoding-related API
    * Encoding-related classes (`Encoder`, `Decoder`, `AbstractEncoder`, `AbstractDecoder`) are moved to a separate `kotlinx.serialization.encoding` package
    * Deprecated `typeParameters` argument in `beginStructure`/`beginCollectio`n methods
    * Deprecated `updateSerializableValue` and similar methods and `UpdateMode` enum
    * Renamed `READ_DONE` to `DECODE_DONE`
    * Make extensions `inline` where applicable
    * `kotlinx.io` mockery (`InputStream`, `ByteArrayInput`, etc) is removed

* Serializer-related API
    * `UnitSerializer` is replaced with `Unit.serializer()`
    * All methods for serializers retrieval are renamed to `serializer`
    * Context is used as a fallback in `serializer` by KType/Java's Reflect Type functions (#902, #903)
    * Deprecated all exceptions except `SerializationException`.
    * `@ImplicitReflectionSerializer` is deprecated
    * Support of custom serializers for nullable types is added (#824)

#### ProtoBuf

* `ProtoBuf` constructor is replaced with `ProtoBuf {}` builder function
* `ProtoBuf` companion object now extends `ProtoBuf`
* `ProtoId` is renamed to `ProtoNumber`, `ProtoNumberType` to `ProtoIntegerType` to be consistent with ProtoBuf specification
* ProtoBuf performance is significantly (from 2 to 10 times) improved (#216)
* Top-level primitives, classes and objects are supported in ProtoBuf as length-prefixed tagless messages (#93)
* `SerializationException` is thrown instead of `IllegalStateException` on incorrect input (#870)
* `ProtobufDecodingException` is made internal

#### Other formats
   * All format constructors are migrated to builder scheme
   * Properties serialize and deserialize enums as strings (#818)
   * CBOR major type 2 (byte string) support (#842)
   * `ConfigParser` is renamed to `Hocon`, `kotlinx-serialization-runtime-configparser` artifact is renamed to `kotlinx-serialization-hocon`
   * Do not write/read size of collection into Properties' map (#743)

0.20.0 / 2020-03-04
==================

### Release notes

0.20.0 release is focused on giving a library its final and stable API shape.

We have carefully evaluated every `public` declaration and
decided whether it should be publicly available. As a result, some declarations were deprecated with an intention of removing
them from public API because they are going to be replaced with others, more valuable and useful for users.

Deprecated symbols include:
 - Pre-defined JSON instances like `nonStrict` — `strictMode` was split to 3 separate, more granular, flags.
Users are encouraged to create their own configuration;
 - Top-level serializers like `IntSerializer` and `ArrayListSerializer`.
They were replaced with constructor-like factory functions.
 - `SerialClassDescImpl` creation class replaced with `SerialDescriptor`
builder function to ease writing of custom serializers and maintain `SerialDescriptor` contract.
 - Internal utilities, like HexConverter and ByteBuffer, were deprecated as not relevant to serialization public API.
 - Add-on formats like Protobuf, CBOR and Properties (formerly Mapper)
are now extracted to [separate artifacts](formats/README.md#protobuf) to keep the core API lightweight.

We have spent a lot of effort into the quality,
documenting most of the core interfaces, establishing their contracts,
fixing numerous of bugs, and even introducing new features that may be useful for those of you who
write custom serializers — see [JsonTransformingSerializer](docs/json_transformations.md).

Such API changes, of course, may be not backwards-compatible in some places, in particular, between compiler plugin and runtime.
Given that the library is still is in the experimental phase, we took the liberty to introduce breaking changes in order to give users
the better, more convenient API. Therefore, this release has number `0.20.0` instead of `0.15.0`;
Kotlin 1.3.70 is compatible _only_ with this release.

To migrate:
1. Replace `import kotlinx.serialization.internal.*` with `import kotlinx.serialization.builtins.*`.
This action is sufficient for most of the cases, except primitive serializers — instead of using `IntSerializer`, use `Int.serializer()`.
For other object-like declarations, you may need to transform it to function call: `ByteArraySerializer` => `ByteArraySerializer()`.

2. Pay attention to the changed `JsonConfiguration` constructor arguments: instead of `strictMode`,
now three different flags are available: `ignoreUnknownKeys`, `isLenient`, and `serializeSpecialFloatingPointValues`.

3. If you used formats other than JSON, make sure you've included the corresponding artifact as dependency,
because now they're located outside of core module. See [formats list](formats/README.md) for particular artifact coordinates.

4. Other corresponding deprecation replacements are available via standard `@Deprecated(replaceWith=..)` mechanism.
(use Alt+Enter for quickfix replacing).

### Full changelog (by commit):

  * This release is compatible with Kotlin 1.3.70
  * Rework polymorphic descriptors: polymorphic and sealed descriptor elements are now aligned with an actual serialization process (#731)
  * Hide internal collection and map serializers
  * Introduce factories for ArraySerializers as well, deprecate top-level array serializers
  * Extract ElementValue encoder and decoder to builtins and rename it to AbstractEncoder and AbstractDecoder respectively
  * Hide as much internal API as possible for collections. Now ListSerializer(), etc factories should be used
  * Replace top-level primitive serializers with corresponding companion functions from builtins
  * Move Tagged.kt to internal package
  * Hide tuple serializers from the public usages and replace them with factory methods in builtins package
  * Deprecate top-level format instances, leave only companion objects
  * Document contracts for JsonInput/JsonOutput (#715)
  * Ensure that serialization exception is thrown from JSON parser on invalid inputs (#704)
  * Do best-effort input/output attach to exceptions to simplify debugging
  * JSON configuration rework: strictMode is splitted into three flags.
  * Make strictMode even more restrictive, prohibit unquoted keys and values by default, always use strict boolean parser (#498, #467)
  * Preserve quotation information during JsonLiteral parsing (#536, #537)
  * Change MapEntrySerializer.descriptor to be truly map-like. Otherwise, it cannot be properly serialized by TaggedDecoder (-> to JsonObject)
  * Cleanup ConfigParser: move to proper package to be consistent with other formats
  * Support primitive and reference arrays in serializer(KType)
  * Add option to use HOCON naming convention
  * Allow DynamicObjectParser to handle polymorphic types (array-mode polymorphism only)
  * Get rid of PrimitiveKind.UNIT and corresponding encoder methods. Now UNIT encoded as regular object.
  * JsonParametricSerializer and JsonTransformingSerializer implementation
  * Remove AbstractSerialFormat superclass since it is useless
  * Deprecate most of the functions intended for internal use
  * Document core kotlinx.serialization.* package
  * Introduce UnionKind.CONTEXTUAL to cover Polymorphic/Contextual serializers, get rid of elementsCount in builders
  * SerialDescriptor for enums rework: now each enum member has object kind
  * Introduce DSL for creating user-defined serial descriptors
  * Update README with Gradle Kotlin DSL (#638)
  * Fix infinite recursion in EnumDescriptor.hashCode() (#666)
  * Allow duplicating serializers during SerialModule concatenation if they are equal (#616)
  * Rework sealed class discriminator check to reduce the footprint of the check when no JSON is used
  * Detect collisions with class discriminator and for equal serial names within the same sealed class hierarchy (#457)
  * Detect name conflicts in polymorphic serialization during setup phase (#461, #457, #589)
  * Extract all mutable state in modules package to SerialModuleBuilder to have a single mutable point and to ensure that SerialModule can never be modified
  * Omit nulls in Properties.store instead of throwing an exception
  * Add optionals handling to Properties reader (#460, #79)
  * Support StructureKind.MAP in Properties correctly (#406)
  * Move Mapper to separate 'properties' module and rename it to Properties
  * Reified extensions for registering serializers in SerialModule (#671, #669)
  * Promote KSerializer.nullable to public API
  * Object serializer support in KType and Type based serializer lookups on JVM (#656)
  * Deprecate HexConverter
  * Supply correct child descriptors for Pair and Triple
  * Rename SerialId to ProtoId to better reflect its semantics
  * Support of custom generic classes in typeOf()/serializer() API (except JS)
  * Allow setting `ProtoBuf.shouldEncodeElementDefault` to false (#397, #71)
  * Add Linux ARM 32 and 64 bit targets
  * Reduce number of internal dependencies: deprecate IOException, mark IS/OS as internal serialization API (so it can be removed in the future release)
  * Reduce number of internal dependencies and use bitwise operations in ProtoBuf/Cbor instead of ByteBuffer. Deprecate ByteBuffer for removal
  * Extract ProtoBuf & CBOR format to the separate module
  * READ_ALL rework (#600)
  * SerialDescriptor API standartization (#626, #361, #410)
  * Support polymorphism in CBOR correctly (fixes #620)
  * Add forgotten during migration WASM32 target (#625)
  * Fix exception messages & typos in JsonElement (#621)

v0.14.0 / 2019-11-19
==================

  * Bump version to 0.14.0 @ Kotlin 1.3.60
  * Add empty javadoc artifact to linking with Maven Central
  * Mark more things as @InternalSerializationApi.
  * Support @SerialId on enum members in protobuf encoding
  * Move Polymorphic and sealed kinds from UnionKind to special PolymorphicKind
  * Sealed classes serialization & generated serializers for enum classes (@SerialInfo support)
  * Objects serialization
  * Don't use deprecated UTF8<>ByteArray conversions in Native
  * Improve error message when static non-generic serializer can't be found
  * Support optional values for typesafe config format

v0.13.0 / 2019-09-12
==================

  * Add mingwX86 target (#556)
  * Replace KClass.simpleName with artificial expect/actual with java.lang.Class.simpleName on JVM to overcome requirement for kotlin-reflect.jar (#549)
  * Update Gradle to 5.6.1 (therefore Gradle metadata to 1.0)
  * Fix incorrect index supply during map deserialization when READ_ALL was used (#526)
  * Serializers for primitive arrays (ByteArray etc)
  * Hide NullableSerializer, introduce '.nullable' extension instead
  * Fix the library to not create a stack overflow exception when creating a MissingDescriptorException. (#545)

v0.12.0 / 2019-08-23
==================

  * Set up linuxArm32Hfp target (#535)
  * wasm32 is added as a build target (#518)
  * MPP (JVM & Native) serializer resolving from KType (via typeOf()/serializer() function)
  * Support maps and objects decoding when map size present in stream (fix #517)
  * Add proper SerialClassDescImpl.toString
  * Make JSON parser much more stricter; e.g. Prohibit all excessive separators in objects and maps
  * Robust JsonArray parsing
  * Improve json exceptions, add more contextual information, get rid of obsolete exception types
  * Prohibit trailing commas in JSON parser
  * Make the baseclass of the polymorphic serializer public to allow formats (#520)
  * Fix decoding for ProtoBuf when there are missing properties in the model. (#506)
  * Rework JsonException and related subclasses
  * Fix #480 (deserialization of complex map keys). Add tests for structured map keys in conjuction with polymorphism
  * Implement 'allowStructuredMapKeys' flag. Now this flag is required for serializing into JSON maps which keys are not primitive.

v0.11.1 / 2019-06-19
==================

  * Fixed some bugs in compiler plugin for Native (#472, #478) (Kotlin 1.3.40 required)
  * Remove dependency on stdlib-jvm from common source set (Fixes #481)
  * Fix @UseSerializers argument type and clarify some docs
  * Support primitives (ints, strings, JsonLiterals, JsonNull, etc) on a top-level when saving/restoring JSON AST (#451)
  * Migrate to the new (Kotlin 1.3) MPP model
  * Add @SharedImmutable to default json module. Fixes #441 and #446

v0.11.0 / 2019-04-12
====================

#### Plugin:

  * **Semantic change**: Now properties with default values are @Optional by default, and properties without backing fields are @Transient by default.
  * Allow '@Serializable' on a type usage (fixes #367)
  * Auto-applying @Polymorphic for interfaces and serializable abstract classes
  * Do not enable PolymorphicSerializer without special annotation
  * Fix missing optionality of property when generating descriptor in Native
  * Fix impossibility to make @Optional field in a class hierarchy on JS
  * Add synthetic companion with .serializer() getter even if default serializer is overridden. (fixes #228)
  * Ban primitive arrays in JVM codegen too (fixes #260)
  * Don't generate writeSelf/internal constructor if corresponding serialize/deserialize aren't auto-generated
  * Support Serializable class hierarchies on Native and JS
  * Replace @Optional with @Required
  * Support classes with more than 32 serializable properties (fixes #164)
  * Make enums and interfaces not serializable internally. However, they still can be serialized using custom companion object. Fixes #138 and #304

#### Runtime:
  * Introduce JsonBuilder and JsonConfiguration as a better mechanism for configuring and changing configuration of the JSON
  * Implement polymorphic serialization in JSON using class discriminator key
  * Force quoting for map keys (fixes #379)
  * Fix bug with endianness in Native for Longs/Doubles
  * Do not allow to mutate SerialModule in formats
  * Implement multiplatform (JVM, JS and Native) PolymorphicSerializer
  * Remove obsolete and poorly designed global class cache. Remove JVM-only PolymorphicSerializer
  * Replace old SerialModule with new one which: - Can not be installed, should be passed in format constructor - Has polymorphic resolve and contextual resolve - Has DSL for creation - Immutable, but can be combined or overwritten
  * Improve error message for unknown enum constant
  * Deprecate @Optional, introduce @Required
  * Use long instead of int in JsonLiteralSerializer
  * Json and protobuf schemas recording prototype
  * Change JsonObject so it would comply to a Map interface: .get should return null for a missing key Incompatibility with standard Map contract may bring a lot of problems, e.g. broken equals.
  * Make JsonElementSerializer public

0.10.0 / 2019-01-22
==================

  * Migrate to Gradle 4.10 and metadata 0.4
  * Update to 1.3.20
  * Reorder Json parameter for consistency
  * Make JsonElement.toString() consistent with stringify (#325)
  * Reader.read(): Int should return -1 on EOF.
  * Optimize the Writer.write(String) case.
  * Update the docs with new annotations

0.10.0-eap-1 / 2018-12-19
==================

#### Plugin:

  * Support @SerialInfo annotation for Native
  * Remove redundant check for 'all parameters are properties' in a case of fully-customized serializer.
  * Fix unresolved symbol to SerialDescriptor in KSerializer if it was referenced from user custom serializer code (#290)
  * Support for @UseSerializers annotation
  * Restrict auto-implementing serializers methods to certain type of classes
  * Increase priority of overridden serializer on type (#252)
  * Fix instantiation of generic serializers on JS (#244)
  * .shouldEncodeElementDefault for JVM (#58)
  * Support skipping values equals to defaults in output stream for JS and Native backends (#58)
  * Support enums in Native
  * Support reference array and context serializers in Native
  * Fix order of overriding @Serializable(with) on property: check override, than @ContextualSerialization.
  * Support @Transient properties initializers and init blocks in Native
  * Better lookup for `serializer()` function in companion for generic classes because user can define a parameterless shorthand one (#228)
  * Generics serialization in Native
  * .getElementDescriptor for JVM, JS and Native
  * Respect @ContextualSerialization on file
  * Remove auto-applying ContextSerializer. @ContextualSerialization should be used instead.

#### Runtime:

  * Turn around messed endianness names (#308)
  * Update to Kotlin 1.3.20 EAP 2
  * Get rid of protobuf-platform functions since @SerialInfo annotations are supported now. Auto-assign ids starting with 1 because 0 is not a valid protobuf ID.
  * Delegates `equals`, `hashCode` of `JsonObject` and `JsonArray`.
  * Test for fixed #190 in plugin
  * UseSerializers annotation
  * Introduce LongAsStringSerializer
  * Add validation for parsing dynamic to Long Fixes #274
  * Merge pull request #294 from Kotlin/recursive_custom_parsing
  * Fix recursive serialization for JsonOutputs/Inputs
  * Production-ready JSON API
  * Remove ValueTransformer
  * Json improvements
  * @Serializable support for JsonArray
  * @Serializable support for JsonObject
  * @Serializable support for JsonNull and JsonPrimitive
  * Hide JsonTreeParser, provide Json.parseJson as replacement, implement basic JsonElementSerializer.deserialize
  * Migrate the rest of the test on JsonTestBase, implement nullable result in tree json
  * Implement custom serializers support for TreeJsonInput
  * Implement JsonArray serialization
  * Implement strict mode for double in TreeJsonOutput (fixes JsonModesTest)
  * Introduce JsonTestBase in order to ensure streaming and tree json compatibility, transient and strict support in TreeJsonInput
  * Make JsonElement serializable via common machinery
  * Json rework, consolidate different parsing mechanisms, hide implementation details
  * Polymorphic serializer improvements
  * Renamed identifiers to align with Kotlin's coding conventions. https://kotlinlang.org/docs/reference/coding-conventions.html#naming-rules
  * Changed JSON -> Json and CBOR -> Cbor

v0.9.1 / 2018-11-19
==================

  * Update lib to 0.9.1/Kotlin to 1.3.10
  * Make some clarifications about Gradle plugin DSL and serialization plugin distribution
  * Primitive descriptor with overriden name
  * Add missing shorthands for float and char serializers (Fixes #263)
  * Fix bug where primitive non-string values created by hand and created by parser could be inequal due to a redundant type comparison.
  * Don't look at default serializer too early during reflective lookup (Fixes #250)

v0.9.0 / 2018-10-24
==================

  * Fix bug where `.simpleName` was not available for primitives' KClasses.
  * Improve Mapper: it is now a class (with default instance in Companion) which extends AbstractSerialFormat and therefore have context and proper reflectionless API.
  * Introduce @ImplicitReflectionSerializer for API which involves reflection.
  * Add Boolean.Companion.serializer() extension method.
  * Refactor surface API: introduce interfaces for different formats, move some inline functions for serialization start to extensions. As a minor change, now nulls can be serialized at top-level, where it is supported by the format.
  * Add AbstractSerialFormat as a base class to all major formats
  * Update general readme and versions: Library to 0.9, K/N to 1.0 beta
  * Update documentation for the new API
  * Updated info about eap13 releases

v0.8.3-rc13 / 2018-10-19
==================

  * Set default byte order to BigEndian (to be more platform-independent and get rid of posix.BYTE_ORDER dependency)
  * Update Kotlin version to 1.3-RC4
  * Remove Gradle metadata from non-native modules
  * Add missing targets (Fixes #232)
  * Add license, developer and scm information in Maven pom in publication (Fixes #239)
  * Add builder for JsonArray
  * Redesign and unify exceptions from parsers (Fixes #238)
  * Move json parser back to monolith module (drops `jsonparser` artifact)
  * Little improvement of error messages
  > Not working until plugin is updated:
  * Initial support for skipping defaults: JSON
  * Replace choicesNames to Array to be easily instantiated from generated IR

v0.8.2-rc13 / 2018-10-03
========================

  * Update to RC-3
  * Add @SharedImmutable from K/N to some global declarations in JSON parser, so it is now accessible from multiple workers (Fixes #225)
  > Not working until plugin is updated:
  * Tests for generic descriptors
  * Generated serializer and stuff for providing descriptors from plugin
  * Tests on @ContextualSerialization on file

v0.8.1-rc13 / 2018-09-24
========================

  * Upgrade Kotlin/Native version

v0.8.0-rc13 / 2018-09-19
========================

  * Add (currently) no-op annotations to the kibrary for smoother migration
  * Update migration guide and versions to RCs.
  * Support WildcardType in serializerByTypeToken (#212)
  > Not working until plugin is updated:
  * Added experimental support of reference arrays for Native

v0.7.3-eap-13 / 2018-09-18
==========================

  * New enum serializing model
  * New context: SerialModules draft. Renaming and mutable/immutable hierarchy
  * Remove untyped encoding
  * Improve serializers resolving by adding primitive serializers. Also add some helper methods to JSON to serialize lists without pain
  * Fix protobuf by adapting MapLikeSerializer to HashSetSerializer(MapEntrySerializer). Elements' serializers in collection serializers are now accessible for such adaptions.
  * Prohibit NaN and infinite values in JSON strict mode
  * Cleanup JSON, reflect opt-in strict mode in naming
  * Get rid of StructureKind.SET and StructureKind.ENTRY
  * Remove SIZE_INDEX
  * Remove inheritance from Encoder and CompositeEncoder
  * Working over primitive kinds and enums
  * Reworked SerialDescriptor and kinds
  * Renaming of ElementValue* and Tagged*
  * Renaming: KOutput -> Encoder/CompositeEncoder KInput -> Decoder/CompositeDecoder
  * Renaming: KSerialClassDesc -> SerialDescriptor SerialSaver, SerialLoader -> *Strategy
  > Not working until plugin is updated:
  * Provide limited `equals` on collections' descriptors
  * Support for `isElementOptional`

v0.6.2 / 2018-09-12
===================

  * Updated Kotlin to 1.2.70 and Kotlin/Native to 0.9

v0.6.1 / 2018-08-06
===================

  * Compatibility release for 1.2.60
  * Don't throw NoSuchElement if key is missing in the map in `Mapper.readNotNullMark`,
  because tag can be only prefix for nested object. Fixes #182
  * Update ios sample with latest http client

v0.6.0 / 2018-07-13
===================

  Plugin:

  * Allow @SerialName and @SerialInfo on classes
  * Fix resolving serializers for classes from other modules (#153 and #166)

  Runtime:

  * Use new 0.8 K/N DSL
  * Simplify JSON AST API, Provide JSON builder, provide useful extensions, add documentation, update K/N
  * Get rid of JsonString to align json primitives with each other. Provide JSON AST pojo parser which exposes current design issues
  * [JSON-AST] Introduce non-nullable methods throwing exceptions for getting json elements
  * [JSON-AST] Add ability to parse JSONInput element as tree. Symmetric functionality for JsonOutput + JsonTree
  * [JSON-AST] Docs writeup
  * [JSON-AST] Publishing native artifact on bintray
  * [JSON-AST] Saving AST back to JSON
  * [JSON-AST] JsonAstMapper to serializable classes
  * Remove annoying "for class class" message in not found serializer exception
  * Introduce module for benchmarks
  * Add notes about snapshot versions
  * Tests for bugs fixed in latest published plugin (#118 and #125)
  * Auto-assign proto ids using field index

v0.5.1 / 2018-06-13
===================

  Plugin:

  * Fix 1.2.50 compatibility
  * Workaround for recursive resolve on @Serializable(with) and @Serializer(for) pair annotations
  * Don't generate additional constructor if @SerialInfo has no properties
  * Fix order of resolving serializers: user-overriden should go before polymorphic and default
  * While creating descriptors, add type arguments not from serializable class definition but from actual KSerializer implementation. This provides better support for user-defined or external generic serializers
  * Don't generate constructor for passing generic serializers if user already defined proper one.
  * Respect `@Serializable(with)` on properties on JS too.
  * Fix for Kotlin/kotlinx.serialization/136
  * Fix for Kotlin/kotlinx.serialization/125
  * Fix for Kotlin/kotlinx.serialization/118
  * Fix for Kotlin/kotlinx.serialization/123: resolve annotation parameters in-place

  Runtime:

  * Added some shorthands for standard serializers
  * Fix for bug #141 that uses an extra boolean to determine whether to write a separating comma rather than assuming that the element with the index 0 is written first(or at all) in all cases.
  * Move mode cache to output class to make .stringify stateless and thread-safe (#139)
  * Bugfix #95: Can't locate default serializer for classes with named co… (#130)
  * Updated versions in docs and examples Add changelog

v0.5.0 / 2018-04-26
===================

  * Improve buildscript and bumped kotlin version to 1.2.40
  * Remove code warnings
  * Add note about different IDEA plugin versions
  * Add null check to Companion when looking up serializer.
  * Improved performance of JSON.stringify
  * Improved performance of JSON.parse
  * Added compatibility note
  * Fix #107 and #112. #76 awaits next compiler release.

v0.4.2 / 2018-03-07
===================

  * Update runtime library version to match plugin version. Update examples to use latest version of compiler, plugin and runtime. Update Gradle to run on build agents with Java 9.
  * Fix ProGuard rules docs for serialization of classes with generic types
  * Fix ProGuard rules docs for serialization 0.4.1 version
  * Add support for @Serializable classes that are private and live out of kotlinx.serialization package. In such case the Companion field is not visible and must be set accessible before use.
  * update jvm-example to latest versions
