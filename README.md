# Kotlin cross-platform / multi-format serialization 

[![JetBrains incubator project](http://jb.gg/badges/incubator.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)
[![Download](https://api.bintray.com/packages/kotlin/kotlinx/kotlinx.serialization.runtime/images/download.svg) ](https://bintray.com/kotlin/kotlinx/kotlinx.serialization.runtime/_latestVersion)

Kotlin serialization support consists of three parts: a gradle compiler plugin, an IntelliJ plugin and a runtime library.

* Supports Kotlin classes marked as `@Serializable` and standard collections. 
* Supports JSON, CBOR, and Protobuf formats out-of-the-box.
* The same code works on Kotlin/JVM and Kotlin/JS.

## Runtime overview

This project contains the runtime library. Runtime library provides:

* Interfaces which are called by compiler-generated code (`KInput`, `KOutput`).
* Basic skeleton implementations of these interfaces in which you should override some methods if you want to 
  implement custom data format (`ElementValueInput/Output`, `NamedValueInput/Output`, `ElementValueTransformer`)
* Some internal classes like built-ins and collections serializers.
* Ready-to-use [serialization formats](#serialization-formats).

You can open example projects for [JVM](example-jvm) or [JS](example-js) to get started playing with it.

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

More examples of various kinds of Kotlin classes that can be serialized can be found [here](docs/examples.md).

## Serialization formats

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
* If fields with list tag are going in the arbitrary order, they are not merged into one list, they got overwritten instead.

More examples of mappings from proto definitions to Koltin classes can be found in test data:
[here](runtime/jvm/src/test/proto/test_data.proto) and [here](runtime/jvm/src/test/kotlin/kotlinx/serialization/formats/RandomTests.kt#L47)

## Usage

Using Kotlin Serialization requires compiler with version higher than `1.1.50`. 
Example projects on JVM are available for [Gradle](example-jvm/build.gradle) and [Maven](example-jvm/pom.xml).

### Gradle/JVM

Ensure the proper version of Kotlin and add dependencies on plugin in addition to Kotlin compiler:

```gradle
buildscript {
    ext.kotlinVersion = '1.1.50'
    ext.serializationVersion = '0.1'
    repositories {
        jcenter()
        maven { url "https://kotlin.bintray.com/kotlinx" }
    }
}

dependencies {
    classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion"
    classpath "org.jetbrains.kotlinx:kotlinx-gradle-serialization-plugin:$serializationVersion"
}

apply plugin: 'kotlin'
apply plugin: 'kotlinx-serialization'
```

Add serialization runtime library in addition to Kotlin standard library and reflection (optional).

```gradle
dependencies {
    compile "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion"
    compile "org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion"
    compile "org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serializationVersion"
}

``` 

### Maven/JVM

Ensure the proper version of Kotlin and serialization version: 

```xml
<properties>
    <kotlin.version>1.1.50</kotlin.version>
    <serialization.version>0.1</serialization.version>
</properties>
```

Include bintray repository:

```xml
<repositories>
    <repository>
        <id>bintray-kotlin-kotlinx</id>
        <name>bintray</name>
        <url>https://kotlin.bintray.com/kotlinx</url>
    </repository>
</repositories>
```

Add serialization plugin to Kotlin compiler plugin:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.jetbrains.kotlin</groupId>
            <artifactId>kotlin-maven-plugin</artifactId>
            <version>${kotlin.version}</version>
            <executions>
                <execution>
                    <id>compile</id>
                    <phase>compile</phase>
                    <goals>
                        <goal>compile</goal>
                    </goals>
                </execution>
            </executions>
            <configuration>
                <compilerPlugins>
                    <plugin>kotlinx-serialization</plugin>
                </compilerPlugins>
            </configuration>
            <dependencies>
                <dependency>
                    <groupId>org.jetbrains.kotlinx</groupId>
                    <artifactId>kotlinx-maven-serialization-plugin</artifactId>
                    <version>${serialization.version}</version>
                </dependency>
            </dependencies>
        </plugin>
    </plugins>
</build>
```

Add dependency on serialization runtime library:

```xml
<dependency>
    <groupId>org.jetbrains.kotlinx</groupId>
    <artifactId>kotlinx-serialization-runtime</artifactId>
    <version>${serialization.version}</version>
</dependency>
```

## JavaScript

Replace `kotlinx-serialization-runtime` with `kotlinx-serialization-runtime-js` to use it in JavaScript projects.
JavaScript example is located at [`example-js`](example-js) folder.

