# Building Kotlin Serialization from the source

## Runtime library

To build Kotlin Serialization library from the source run `./gradlew publishToMavenLocal`. 
After that, you can include this library in arbitrary projects like usual gradle dependency:

```gradle
repositories {
    jcenter()
    mavenLocal()
}

dependencies {
    compile "org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.1"
}
```

## Compiler plugin for Gradle

Compiler plugin for Gradle is hosted in a separate branch of Kotlin compiler. 
Steps to build it are explained 
[here](https://github.com/JetBrains/kotlin/tree/rr/kotlinx.serialization/libraries#kotlin-serialization-gradle-plugin).

## IDEA plugin

IDEA plugin for is hosted in serialization is likewise hosted in a separate branch of Kotlin compiler.
Steps to build it are explained   
[here](https://github.com/JetBrains/kotlin/blob/rr/kotlinx.serialization/plugins/kotlin-serialization/kotlin-serialization-compiler/README.md).
