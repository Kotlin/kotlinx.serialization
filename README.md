# Kotlin serialization runtime library

Kotlin serialization plugin consists of three parts: a gradle compiler plugin, an IntelliJ plugin and a runtime library. 
This is the runtime library. To build any project with serialization (including this one), you'll
need a [serialization gradle plugin](https://github.com/sandwwraith/kotlin/tree/startsev/kotlinx/serialization/libraries#kotlin-serialization-gradle-plugin).
To have a proper syntax highlighting in the Intellij IDEA, you'll need an [IDEA plugin](https://github.com/sandwwraith/kotlin/blob/startsev/kotlinx/serialization/plugins/kotlin-serialization/kotlin-serialization-compiler/README.md).

Runtime library provides basic classes:

* Interfaces which are called by compiler-generated code (`KInput`, `KOutput`)

* Basic skeleton implementations of these interfaces in which you should override some methods if you want to implement custom data format (`ElementValueInput/Output`, `NamedValueInput/Output`, `ElementValueTransformer`)

* Some internal classes like built-ins and collections serializers

Also, runtime library provides some ready-to-use serialization formats: JSON and CBOR.

## Building and usage

Make sure you have serialization gradle plugin installed to your local maven repository. Then, run `./gradlew publishToMavenLocal`. After that, you can include this library in arbitrary projects like usual gradle dependency:

```gradle
repositories {
    jcenter()
    mavenLocal()
}

dependencies {
    compile "org.jetbrains.kotlinx:serialization-runtime:1.1-SNAPSHOT"
}
```

## Example

```kotlin

import kotlinx.serialization.*

@Serializable
data class Data(val a: Int, @Optional val b: String = "42")

fun main(args: Array<String>) {
    println(JSON.stringify(Data(42))) // {"a": 42, "b": "42"}
    val obj = JSON.parse<Data>("""{"a":42}""") // Data(a=42, b="42")
}
```

More complicated examples and examples of implementing custom formats can be found in `examples` folder. Detailed documentation located in [DOC.md](DOC.md).