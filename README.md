# Kotlin cross-platform / multi-format reflectionless serialization 

[![JetBrains incubator project](http://jb.gg/badges/incubator.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)
[![TeamCity build](https://teamcity.jetbrains.com/app/rest/builds/buildType(id:KotlinTools_KotlinxSerialization_Ko)/statusIcon)](https://teamcity.jetbrains.com/viewType.html?buildTypeId=KotlinTools_KotlinxSerialization_Ko&guest=1)
[![Download](https://api.bintray.com/packages/kotlin/kotlinx/kotlinx.serialization.runtime/images/download.svg) ](https://bintray.com/kotlin/kotlinx/kotlinx.serialization.runtime/_latestVersion)

Kotlin serialization support consists of three parts: a gradle compiler plugin, which produces visitor/serializer code
for objects, an IntelliJ plugin and a runtime library.

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

Runtime library provides three ready-to use formats: JSON, CBOR and ProtoBuf.

### JSON usage

JSON format represented by `JSON` class from `kotlinx.serialization.json` package. It has constructor with four optional parameters:

* nonstrict - allow JSON parser skip fields which are not present in class. By default is false.
* unquoted - means that all field names and other objects (where it's possible) would not be wrapped in quotes. Useful for debugging.
* indented - classic pretty-printed multiline JSON.
* indent - size of indent, applicable if parameter above is true.

You can also use one of predefined instances, like `JSON.plain`, `JSON.indented`, `JSON.nonstrict` or `JSON.unquoted`. API is duplicated in companion object, so `JSON.parse(...)` equals to `JSON.plain.parse(...)`

JSON API:

```kotlin
fun <T> stringify(saver: KSerialSaver<T>, obj: T): String
inline fun <reified T : Any> stringify(obj: T): String = stringify(T::class.serializer(), obj)

fun <T> parse(loader: KSerialLoader<T>, str: String): T
inline fun <reified T : Any> parse(str: String): T = parse(T::class.serializer(), str)
```

`stringify` transforms object to string, `parse` parses. No surprises.

**Note**: because JSON doesn't support maps with keys other than
strings (and primitives), Kotlin maps with non-trivial key types are serialized as JSON lists.

**Caveat**: `T::class.serializer()` assumes that you use it on class defined as `@Serializable`,
so it wouldn't work with root-level collections or external serializers out of the box. For external serializers,
you must [register](docs/custom_serializers.md#registering-and-context) them and create json instance with corresponding scope.
For collection serializers, see this [feature](https://github.com/Kotlin/kotlinx.serialization/issues/27).

### CBOR usage

`CBOR` object doesn't support any tweaking and provides following functions:

```kotlin
fun <T : Any> dump(saver: KSerialSaver<T>, obj: T): ByteArray // saves object to bytes
inline fun <reified T : Any> dump(obj: T): ByteArray // same as above, resolves serializer by itself
inline fun <reified T : Any> dumps(obj: T): String // dump object and then pretty-print bytes to string

fun <T : Any> load(loader: KSerialLoader<T>, raw: ByteArray): T // load object from bytes
inline fun <reified T : Any> load(raw: ByteArray): T // save as above
inline fun <reified T : Any> loads(hex: String): T // inverse operation for dumps
```

**Note**: CBOR, unlike JSON, supports maps with non-trivial keys,
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

Using Kotlin Serialization requires Kotlin compiler `1.1.50` or higher. 
Example projects on JVM are available for [Gradle](example-jvm/build.gradle) and [Maven](example-jvm/pom.xml).

### Gradle/JVM

Ensure the proper version of Kotlin and add dependencies on plugin in addition to Kotlin compiler:

```gradle
buildscript {
    ext.kotlin_version = '1.1.50'
    ext.serialization_version = '0.2'
    repositories {
        jcenter()
        maven { url "https://kotlin.bintray.com/kotlinx" }
    }

    dependencies {
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
        classpath "org.jetbrains.kotlinx:kotlinx-gradle-serialization-plugin:$serialization_version"
    }
}
```

Don't forget to apply the plugin:

```gradle
apply plugin: 'kotlin'
apply plugin: 'kotlinx-serialization'
```

Add serialization runtime library in addition to Kotlin standard library and reflection (optional).
For now, library requires small amount of reflection on runtime to find corresponding serializer for root-level type. 
In the future, we plan to move all resolving to separate module so the runtime library itself would not 
contain dependency on kotlin-reflect.

```gradle
repositories {
    jcenter()
    maven { url "https://kotlin.bintray.com/kotlinx" }
}

dependencies {
    compile "org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version"
    compile "org.jetbrains.kotlin:kotlin-reflect:$kotlin_version"
    compile "org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serialization_version"
}
``` 

### Maven/JVM

Ensure the proper version of Kotlin and serialization version: 

```xml
<properties>
    <kotlin.version>1.1.50</kotlin.version>
    <serialization.version>0.2</serialization.version>
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

## IntelliJ IDEA

Unfortunately, embedded Kotlin compiler is not supported yet. To be able to run your project with serialization from within IDEA, perform following steps: 

`Settings - Build, Execution, Deployment - Build Tools - Gradle - Runner -` tick `Delegate IDE build/run actions to gradle`. 

For maven projects, create separate run configuration.

## Further reading

* [More examples of supported Kotlin classes](docs/examples.md)
* [Building library from source](docs/building.md)
* [Writing custom serializers](docs/custom_serializers.md)