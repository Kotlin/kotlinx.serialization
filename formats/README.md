# Serialization formats

This area of repository contains different libraries with various add-on formats which
were not included in the main runtime library – because they are not so popular, or they're big,
or they contain other JVM runtime dependencies.

For convenience, they have same groupId, versioning and release cycle as main runtime.

## HOCON config parser

* Artifact id: `kotlinx-serialization-runtime-configparser`
* Since version: 0.4.1
* Platform: JVM only

Allows deserialization of `Config` object from popular [lightbend/config](https://github.com/lightbend/config) library 
into Kotlin objects.
You can learn about "Human-Optimized Config Object Notation" or HOCON from library's [readme](https://github.com/lightbend/config#using-hocon-the-json-superset).

## Other community-supported formats

### YAML

* GitHub repo: [charleskorn/kaml](https://github.com/charleskorn/kaml)
* Artifact ID: `com.charleskorn.kaml:kaml`
* Platform: JVM only

Allows serialization and deserialization of objects to and from [YAML](http://yaml.org).
