# Serialization formats

This area of repository contains different libraries with various add-on formats which
were not included in the main runtime library – because they are not so popular, or they're big,
or they contain other JVM runtime dependencies.

For convenience, they have same groupId, versioning and release cycle as main runtime.

## HOCON 

* Artifact id: `kotlinx-serialization-hocon`
* Since version: 0.4.1
* Platform: JVM only

Allows deserialization of `Config` object from popular [lightbend/config](https://github.com/lightbend/config) library 
into Kotlin objects.
You can learn about "Human-Optimized Config Object Notation" or HOCON from library's [readme](https://github.com/lightbend/config#using-hocon-the-json-superset).

## ProtoBuf

* Artifact id: `kotlinx-serialization-protobuf`
* Since version: 0.20.0
* Platform: all supported platforms
* Status: experimental

## CBOR

* Artifact id: `kotlinx-serialization-cbor`
* Since version: 0.20.0
* Platform: all supported platforms
* Status: experimental

## Properties

* Artifact id: `kotlinx-serialization-properties`
* Since version: 0.20.0
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

### XML
* GitHub repo: [pdvrieze/xmlutil](https://github.com/pdvrieze/xmlutil)
* Artifact ID: `net.devrieze:xmlutil-serialization`
* Platform: JVM, Android, JavaScript

This library allows for reading and writing of XML documents with the serialization library.
It is multiplatform, but as the xmlutil library (which handles the multiplatform xml bit) 
delegates to platform specific parsers each platform needs to  be implemented for each platform 
specifically. The library is designed to handle existing formats that use features that would 
not be available in other formats such as JSON.

### YAML

* GitHub repo: [charleskorn/kaml](https://github.com/charleskorn/kaml)
* Artifact ID: `com.charleskorn.kaml:kaml`
* Platform: JVM only

Allows serialization and deserialization of objects to and from [YAML](http://yaml.org).
