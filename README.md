# Kotlin serialization runtime library for JVM and JS

Kotlin serialization plugin consists of three parts: a gradle compiler plugin, an IntelliJ plugin and a runtime library. 
This is the runtime library. To build any project with serialization (including this one), you'll
need a [serialization gradle plugin](https://github.com/sandwwraith/kotlin/tree/startsev/kotlinx/serialization/libraries#kotlin-serialization-gradle-plugin).
To have a proper syntax highlighting in the Intellij IDEA, you'll need an [IDEA plugin](https://github.com/sandwwraith/kotlin/blob/startsev/kotlinx/serialization/plugins/kotlin-serialization/kotlin-serialization-compiler/README.md).

Runtime library provides basic classes:

* Interfaces which are called by compiler-generated code (`KInput`, `KOutput`)

* Basic skeleton implementations of these interfaces in which you should override some methods if you want to implement custom data format (`ElementValueInput/Output`, `NamedValueInput/Output`, `ElementValueTransformer`)

* Some internal classes like built-ins and collections serializers

Also, runtime library provides some ready-to-use serialization formats, see below.

## Building and usage

### From bintray

Public EAP is available at <https://bintray.com/kotlin/kotlinx/kotlinx.serialization.runtime>.
It requires EAP compiler with version higher than 1.1.50. Example configuration for [Gradle](examples/build.gradle) and [Maven](examples/pom.xml).

### From source

Run `./gradlew publishToMavenLocal`. After that, you can include this library in arbitrary projects like usual gradle dependency:

```gradle
repositories {
    jcenter()
    mavenLocal()
}

dependencies {
    compile "org.jetbrains.kotlinx:serialization-runtime-jvm:0.1"
}
```

### JavaScript

Replace `jvm` with `js` to use it in JavaScript projects. JavaScript example is located at [`example-js`](example-js) folder.

## Example

```kotlin

import kotlinx.serialization.*
import kotlinx.serialization.json.JSON

@Serializable
data class Data(val a: Int, @Optional val b: String = "42")

fun main(args: Array<String>) {
    println(JSON.stringify(Data(42))) // {"a": 42, "b": "42"}
    val obj = JSON.parse<Data>("""{"a":42}""") // Data(a=42, b="42")
}
```

Detailed documentation with more complicated examples of usage located in [DOC.md](DOC.md).

Example project with different implementations of custom formats can be found in [`examples`](examples) folder. 
You can run it with `cd examples; ./gradlew -q runApp`.

## Built-in formats

Runtime library provides three ready-to use formats: JSON, CBOR and ProtoBuf. Usage of the first two formats is pretty 
straightforward and obvious from the example above. Notes on them: because JSON doesn't support maps with keys other than 
strings (and primitives), Kotlin maps with non-trivial key types are serialized as JSON lists. CBOR doesn't have this limitation,
and Kotlin maps are serialized as CBOR maps, but some parsers (like `jackson-dataformat-cbor`) don't support this.

### Protobuf usage

Because protobuf relies on serial ids of fields, called 'tags', you have to provide this information, 
using serial annotation `@SerialId`:

```kotlin
@Serializable
data class KTestInt32(@SerialId(1) val a: Int)
```

This class is equivalent to the following proto definition:

```proto
message Int32 {
    required int32 a = 1;
}
```

Note that we are using proto2 semantics, where all fields are explicitly required or optional.

Number format is set via `@ProtoType` annotation. `ProtoNumberType.DEFAULT` is default varint encoding (`intXX`), `SIGNED`
is signed ZigZag representation (`sintXX`), and `FIXED` is `fixedXX` type. `uintXX` and `sfixedXX` are not supported yet.

Repeated fields represented as lists. Because format spec says that if the list is empty, there will be no elements in the stream with such tag,
you must explicitly mark any filed of list type with `@Optional` annotation with default ` = emptyList()`. Same for maps.

Other known issues and limitations:

* Packed repeated fields are not supported

* Unknows fields aren't skipped, exception is thrown instead

* If fields with list tag are going in the arbitrary order, they are not merged into one list, they got overwritten instead.

More examples of mappings from proto definitions to Koltin classes can be found in test data:
[here](jvm/src/test/proto/test_data.proto) and [here](jvm/src/test/kotlin/kotlinx/serialization/formats/RandomTests.kt#L47)