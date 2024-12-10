# Building Kotlin Serialization from the source

## JDK version

To build Kotlin Serialization JDK version 11 or higher is required. Make sure this is your default JDK (`JAVA_HOME` is set accordingly).
This is needed to compile the `module-info` file included for JPMS support.

In case you are determined to use a different JDK version or experience problems with JPMS, you can turn off compilation of module-info files
completely with `disableJPMS` property: add `disableJPMS=true` to gradle.properties or `-PdisableJPMS` to Gradle CLI invocation.

## Runtime library

Kotlin Serialization runtime library itself is a [multiplatform](http://kotlinlang.org/docs/reference/multiplatform.html) project.
To build the library from the source and run all tests, use `./gradlew build`. Corresponding platform tasks like `jvmTest,` `jsTest,` `macosArm64Test`, and so on are also available.

The project can be opened in IntelliJ IDEA without additional prerequisites.
In case you want to work with Protobuf tests, you may need to run `./gradlew generateTestProto` beforehand.


To install runtime library into the local Maven repository, run `./gradlew publishToMavenLocal`. 
After that, you can include this library in arbitrary projects like usual gradle dependency:

```gradle
repositories {
    mavenLocal()
}

dependencies {
    compile "org.jetbrains.kotlinx:kotlinx-serialization-core:$serialization_version"
}
```

To use snapshot version of compiler (if you have built and installed it from sources), use flag `-Pbootstrap`.
If you have built both Kotlin and Kotlin/Native compilers, set `kotlin.native.home` property in `gradle.properties` to the path with Kotlin/Native distribution
(usually `kotlin-native/dist` folder inside Kotlin project).

The `master` and `dev` branches of the library should be binary compatible with the latest released compiler plugin. In case you want to test some new features from other branches,
which are still in development and may not be compatible in terms of bytecode produced by plugin, you'll need to build the plugin by yourself.

## Compiler plugin

Compiler plugins for Gradle/Maven and IntelliJ plugin, starting from Kotlin 1.3, are embedded into the Kotlin compiler. 

Sources and steps to build it are located [here](https://github.com/JetBrains/kotlin/tree/master/plugins/kotlinx-serialization).
In short, you'll just need to run `./gradlew dist install` to get `2.x.255-SNAPSHOT` versions of Kotlin compiler, stdlib, and serialization plugins in the Maven local repository.
