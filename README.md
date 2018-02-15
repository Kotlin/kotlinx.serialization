# Kotlin cross-platform / multi-format reflectionless serialization 

[![JetBrains incubator project](http://jb.gg/badges/incubator.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)
[![TeamCity build](https://img.shields.io/teamcity/http/teamcity.jetbrains.com/s/KotlinTools_KotlinxSerialization_Ko.svg)](https://teamcity.jetbrains.com/viewType.html?buildTypeId=KotlinTools_KotlinxSerialization_Ko&guest=1)
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
* Ready-to-use serialization formats.
* Other useful classes that benefit from serialization framework (e.g. object-to-Map transformer)

You can open example projects for [JVM](example-jvm) or [JS](example-js) to get started playing with it.

## Table of contents

* [Quick example](#quick-example)
* [Library installing](#setup)
* [Working in IntelliJ IDEA](#working-in-intellij-idea)
* [Usage](docs/runtime_usage.md)
* [More examples of supported Kotlin classes](docs/examples.md)
* [Writing custom serializers](docs/custom_serializers.md)
* [Add-on formats](formats/README.md)
* [Building library and compiler plugin from source](docs/building.md)


## Quick example

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

To learn more about JSON usage and other formats, see [usage](docs/runtime_usage.md).
More examples of various kinds of Kotlin classes that can be serialized can be found [here](docs/examples.md).

## Setup

Using Kotlin Serialization requires Kotlin compiler `1.1.50` or higher, recommended version is `1.2.21`.
Also, it's recommended to install [additional IDEA plugin](#working-in-intellij-idea) for better IDE experience. Otherwise,
some valid code will be shown as red and builds will have to be launched from console or build system tasks panel.
Example projects on JVM are available for [Gradle](example-jvm/build.gradle) and [Maven](example-jvm/pom.xml).

### Gradle/JVM

Ensure the proper version of Kotlin and add dependencies on plugin in addition to Kotlin compiler:

```gradle
buildscript {
    ext.kotlin_version = '1.2.10'
    ext.serialization_version = '0.4.1'
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

Add serialization runtime library in addition to Kotlin standard library.

```gradle
repositories {
    jcenter()
    maven { url "https://kotlin.bintray.com/kotlinx" }
}

dependencies {
    compile "org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version"
    compile "org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serialization_version"
}
``` 

### Android/JVM

Library should work on Android "as is". If you're using proguard, you need
to add this to your `proguard-rules.pro`:

```proguard
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keep,includedescriptorclasses class com.yourcompany.yourpackage.**$$serializer { *; } # <-- change package name to your app's
-keepclassmembers class com.yourcompany.yourpackage.** { # <-- change package name to your app's
    *** Companion;
}
-keepclasseswithmembers class com.yourcompany.yourpackage.** { # <-- change package name to your app's
    kotlinx.serialization.KSerializer serializer();
}
```

You may also want to keep all custom serializers you've defined.

### Maven/JVM

Ensure the proper version of Kotlin and serialization version: 

```xml
<properties>
    <kotlin.version>1.2.10</kotlin.version>
    <serialization.version>0.4.1</serialization.version>
</properties>
```

Include bintray repository for both library and plugin:

```xml
<repositories>
    <repository>
        <id>bintray-kotlin-kotlinx</id>
        <name>bintray</name>
        <url>https://kotlin.bintray.com/kotlinx</url>
    </repository>
</repositories>
<pluginRepositories>
    <pluginRepository>
        <id>bintray-kotlin-kotlinx</id>
        <name>bintray-plugins</name>
        <url>https://kotlin.bintray.com/kotlinx</url>
    </pluginRepository>
</pluginRepositories>
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

### JavaScript and common

Replace dependency on `kotlinx-serialization-runtime` with `kotlinx-serialization-runtime-js` or `kotlinx-serialization-runtime-common`
to use it in JavaScript and common projects, respectively.
JavaScript example is located at [`example-js`](example-js) folder.

## Working in IntelliJ IDEA

Instead of using Gradle or Maven, IntelliJ IDEA relies on its own build system when analyzing and running code from within IDE.
Because serialization is still highly experimental, it is shipped as a separate artifact from "big" Kotlin IDEA plugin.
You can download additional IDEA plugin for working with projects that uses serialization from its [TeamCity build page](https://teamcity.jetbrains.com/viewLog.html?buildId=lastPinned&buildTypeId=KotlinTools_KotlinxSerialization_KotlinCompilerWithSerializationPlugin&tab=artifacts&guest=1).
In IDEA, open `Settings - Plugins - Install plugin from disk...` and select downloaded .zip or .jar file.
This installation will allow you to run code/tests from IDEA.

In case of issues with IDE, try to use gradle for running builds:
`Settings - Build, Execution, Deployment - Build Tools - Gradle - Runner -` tick `Delegate IDE build/run actions to gradle`; or launch builds from console.
