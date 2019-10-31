# Building Kotlin Serialization from the source

## Runtime library

Kotlin Serialization runtime library itself is a [multiplatform](http://kotlinlang.org/docs/reference/multiplatform.html) project.
To build library from the source and run all tests, use `./gradlew build`. Corresponding platform tasks like `jvmTest`, `jsTest` and so on are also available.

To install it into the local Maven repository, run `./gradlew publishToMavenLocal`. 
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

To open project in Intellij IDEA, first run `./gradlew generateTestProto` from console.
Make sure you've set an option 'Use Gradle wrapper' on import to use a correct version of Gradle.
You may also need to mark `runtime/build/generated/source/proto/test/java` as 'Generated source root' to build project/run tests in IDE without delegating a build to Gradle.
This requires Kotlin 1.3.11 and higher.

To use snapshot version of compiler (if you have built it from sources), use flag `-Pbootstrap`. To compile and publish all Native artifacts, not only the host one, use `-Pnative.deploy=true`.

`master` branch of library should be binary compatible with latest released compiler plugin. In case you want to test some new features from other branches, which are still in development and may not be compatible in terms of bytecode produced by plugin, you'll need to build the plugin by yourself.

## Compiler plugin

Compiler plugin for Gradle/Maven and IntelliJ plugin, starting from Kotlin 1.3, are embedded into the Kotlin compiler. 

Sources and steps to build it are located [here](https://github.com/JetBrains/kotlin/blob/master/plugins/kotlin-serialization/kotlin-serialization-compiler/). In general, you'll just need to run `./gradlew dist install` to get `1.3-SNAPSHOT` versions of Kotlin compiler, stdlib and serialization plugins in the Maven local repository.
