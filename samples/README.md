# Kotlinx.serialization library samples

This folder contains various samples of setup and usage of kotlinx.serialization library, namely: 

### Example-jvm

Provides setup for Gradle and Maven build systems for JVM target.
Contains examples of implementing your own formats.

To launch this sample with Maven, use `mvn package exec:java`

### Example-js

Provides setup for Gradle using 'kotlin2js' plugin.
Contains examples of using different formats (JSON, CBOR, Protobuf).

To launch this sample, first run `./gradlew build` and then point your browser to `web/index.html`.

### Example-native

Provides setup for Gradle using 'kotlin-platform-native' plugin and Gradle metadata (Gradle 4.8+ required, used one is 4.10).

### Example-visitors

Provides setup for Gradle using Kotlin Gradle DSL and 'kotlin.jvm' plugin.
Showcases how one can use functionality of visiting serializable class descriptors tree to extract different metadata from the class, e.g. Json schema or .proto definition.
It also features ability to use kotlinx.serialization library in Gradle's [composite build with dependency substitution](https://docs.gradle.org/current/userguide/composite_builds.html#included_build_declaring_substitutions).

## Launching samples

You can use `./gradlew run`  in a folder with particular sample.
