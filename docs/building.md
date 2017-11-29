# Building Kotlin Serialization from the source

## Runtime library

Kotlin Serialization runtime library itself is a [multiplatform](http://kotlinlang.org/docs/reference/multiplatform.html) project.
To build library from the source run `./gradlew publishToMavenLocal`. 
After that, you can include this library in arbitrary projects like usual gradle dependency:

```gradle
repositories {
    jcenter()
    mavenLocal()
}

dependencies {
    compile "org.jetbrains.kotlinx:kotlinx-serialization-runtime"
}
```

`master` branch of library should be binary compatible with latest available on bintary compiler plugin. In case you want to test some new features from other branches, which are still in development and may not be compatible in terms of bytecode produced by plugin, you'll need to build the plugin by yourself.

## Compiler plugin

Compiler plugin for Gradle/Maven and IntelliJ plugin are hosted in a separate branch of Kotlin compiler. 

Sources and steps to build it are located [here](https://github.com/JetBrains/kotlin/blob/rr/kotlinx.serialization/plugins/kotlin-serialization/kotlin-serialization-compiler/).
