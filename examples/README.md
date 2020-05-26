# Kotlinx.serialization library samples

This folder contains various samples of setup and usage of kotlinx.serialization library, namely: 

### Example-multiplatform

Provides setup for multiplatform library with kotlinx.serialization on JVM, JS and Native targets using
new multiplatform model from Kotlin 1.3 ('kotlin-multiplatform' plugin). Also features how one can
setup tests for JS.

### Example-jvm

Provides setup for Gradle and Maven build systems for JVM target.
Contains examples of implementing your own formats.

To launch this sample with Maven, use `mvn package exec:java` within `example-jvm` folder.

### Example-js

Provides setup for Gradle using 'org.jetbrains.kotlin.js' plugin.
Contains examples of using different formats (JSON, CBOR, Protobuf).

- run with hot reload `./gradlew run -t`
- build with `./gradlew build` and find distribution in `example-js/build/distributions`

### Example-visitors

Provides setup for Gradle using **Kotlin Gradle DSL** and 'kotlin.jvm' plugin.
Showcases how one can use functionality of visiting serializable class descriptors tree to extract
different metadata from the class, e.g. Json schema or .proto definition.

## Launching samples

You can use `./gradlew run` to run all samples or `./gradlew :folder-name:run` to run a particular sample.

## Opening samples in IDEA

All samples are subprojects of the `kotlinx-serialization-examples` project, located in this folder. 
Just import the `build.gradle` file into IDEA.

## Grabbing samples

Feel free to copy-paste any particular sample you need.
Copy folder with sample and its `build.gradle`;
you can also take the `gradle.properties` file from this folder, since it provides project properties
with kotlin & serialization versions.
For `example-multiplatform` and `example-visitors` you may also need 
`pluginManagement` block from `settings.gradle`.
