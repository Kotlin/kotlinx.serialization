# Serialization formats

This area of repository contains different libraries with various add-on formats which 
were not included in the core library.

For convenience, they have same `groupId`, versioning and release cycle as core library.

## JSON

* Artifact id: `kotlinx-serialization-json`
* Platform: all supported platforms
* Status: stable

## HOCON 

* Artifact id: `kotlinx-serialization-hocon`
* Platform: JVM only
* Status: experimental

Allows deserialization of `Config` object from popular [lightbend/config](https://github.com/lightbend/config) library 
into Kotlin objects.
You can learn about "Human-Optimized Config Object Notation" or HOCON from library's [readme](https://github.com/lightbend/config#using-hocon-the-json-superset).

## ProtoBuf

* Artifact id: `kotlinx-serialization-protobuf`
* Platform: all supported platforms
* Status: experimental

## CBOR

* Artifact id: `kotlinx-serialization-cbor`
* Platform: all supported platforms
* Status: experimental

## Properties

* Artifact id: `kotlinx-serialization-properties`
* Platform: all supported platforms
* Status: experimental

Allows converting arbitrary hierarchy of Kotlin classes to a flat key-value structure à la Java Properties.

## Other community-supported formats

### Avro

* GitHub repo: [sksamuel/avro4k](https://github.com/sksamuel/avro4k)
* Artifact ID: `com.sksamuel.avro4k:avro4k`
* Platform: JVM only

This library allows serialization and deserialization of objects to and from [Avro](https://avro.apache.org). It will read and write from Avro binary or json streams or generate Avro Generic Records directly. It will also generate Avro schemas from data classes. The library allows for easy extension and overrides for custom schema formats, compatiblity with schemas defined outside out of the JVM and for types not supported out of the box.

### Bson

* GitHub repo: [jershell/kbson](https://github.com/jershell/kbson)
* Artifact ID: `com.github.jershell:kbson`
* Platform: JVM only

Allows serialization and deserialization of objects to and from [BSON](https://docs.mongodb.com/manual/reference/bson-types/).

### Ktoml 
* GitHub repo: [akuleshov7/ktoml](https://github.com/akuleshov7/ktoml)
* Artifact ID: `com.akuleshov7:ktoml-core`
* Platforms: multiplatform, all Kotlin supported platforms

Fully Native and Multiplatform Kotlin serialization library for serialization/deserialization of TOML format.
This library contains no Java code and no Java dependencies and it implements multiplatform parser, decoder and encoder of TOML.

### Minecraft NBT (Multiplatform)

* GitHub repo: [BenWoodworth/knbt](https://github.com/BenWoodworth/knbt)
* Artifact ID: `net.benwoodworth.knbt:knbt`
* Platform: all supported platforms

Implements the [NBT format](https://minecraft.fandom.com/wiki/NBT_format) for kotlinx.serialization, and
provides a type-safe DSL for constructing NBT tags.

### MsgPack (Multiplatform)

* GitHub repo: [esensar/kotlinx-serialization-msgpack](https://github.com/esensar/kotlinx-serialization-msgpack)
* Artifact ID: `com.ensarsarajcic.kotlinx:serialization-msgpack`
* Platform: all supported platforms

Allows serialization and deserialization of objects to and from [MsgPack](https://msgpack.org/).

### SharedPreferences

* GitHub repo: [EdwarDDay/serialization.kprefs](https://github.com/EdwarDDay/serialization.kprefs)
* Artifact ID: `net.edwardday.serialization:kprefs`
* Platform: Android only

This library allows serialization and deserialization of objects into and from Android
[SharedPreferences](https://developer.android.com/reference/android/content/SharedPreferences).

### XML
* GitHub repo: [pdvrieze/xmlutil](https://github.com/pdvrieze/xmlutil)
* Artifact ID: `io.github.pdvrieze.xmlutil:serialization`
* Platform: all supported platforms

This library allows for reading and writing of XML documents with the serialization library.
It is multiplatform, providing both a shared parser/writer for xml as well as platform-specific
parsers where available. The library is designed to handle existing xml formats that use features that would 
not be available in other formats such as JSON.

### YAML

* GitHub repo: [charleskorn/kaml](https://github.com/charleskorn/kaml)
* Artifact ID: `com.charleskorn.kaml:kaml`
* Platform: JVM only

Allows serialization and deserialization of objects to and from [YAML](http://yaml.org).

### YAML (Multiplatform)

* GitHub repo: [him188/yamlkt](https://github.com/him188/yamlkt)
* Artifact ID: `net.mamoe.yamlkt:yamlkt`
* Platform: all supported platforms

Allows serialization and deserialization of objects to and from [YAML](http://yaml.org). 
Basic serial operations have been implemented, but some features such as compound keys and polymorphism are still work in progress.

### CBOR

* GitHub repo: [L-Briand/obor](https://github.com/L-Briand/obor)
* Artifact ID: `net.orandja.obor:obor`
* Platform: JVM, Android

Allow serialization and deserialization of objects to and from [CBOR](https://cbor.io/). This codec can be used to read and write from Java InputStream and OutputStream.

### Amazon Ion (binary only)

* GitHub repo: [dimitark/kotlinx-serialization-ion](https://github.com/dimitark/kotlinx-serialization-ion)
* Artifact ID: `com.github.dimitark:kotlinx-serialization-ion`
* Platform: JVM

Allow serialization and deserialization of objects to and from [Amazon Ion](https://amzn.github.io/ion-docs/). It stores the data in a flat binary format. Upon destialization, it retains the references between the objects.

### android.os.Bundle

* GitHub repo: [AhmedMourad0/bundlizer](https://github.com/AhmedMourad0/bundlizer)
* Artifact ID: `dev.ahmedmourad.bundlizer:bundlizer-core`
* Platform: Android

Allow serialization and deserialization of objects to and from [android.os.Bundle](https://developer.android.com/reference/android/os/Bundle).  
