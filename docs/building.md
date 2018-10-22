# Building Kotlin Serialization from the source

## Runtime library

Kotlin Serialization runtime library itself is a [multiplatform](http://kotlinlang.org/docs/reference/multiplatform.html) project,
but it is still using an old multiplatform system from Kotlin 1.2.
To build library from the source and install it into the local Maven repository run `./gradlew publishToMavenLocal`. 
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

To use snapshot version of compiler (if you have built it from sources), use flag `-Pbootstrap`. To compile and publish all Native artifacts, not only the host one, use `-Pnative.deploy=true`.

`master` branch of library should be binary compatible with latest released compiler plugin. In case you want to test some new features from other branches, which are still in development and may not be compatible in terms of bytecode produced by plugin, you'll need to build the plugin by yourself.

## Compiler plugin

Compiler plugin for Gradle/Maven and IntelliJ plugin, starting from Kotlin 1.3, are embedded into the Kotlin compiler. 

Sources and steps to build it are located [here](https://github.com/JetBrains/kotlin/blob/master/plugins/kotlin-serialization/kotlin-serialization-compiler/). In general, you'll just need to run `./gradlew dist install` to get `1.3-SNAPSHOT` versions of Kotlin compiler, stdlib and serialization plugins in the Maven local repository.
