# Kotlin multiplatform / multi-format reflectionless serialization

[![JetBrains incubator project](https://jb.gg/badges/incubator.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)
[![TeamCity build](https://img.shields.io/teamcity/http/teamcity.jetbrains.com/s/KotlinTools_KotlinxSerialization_Ko.svg)](https://teamcity.jetbrains.com/viewType.html?buildTypeId=KotlinTools_KotlinxSerialization_Ko&guest=1)
[![Download](https://api.bintray.com/packages/kotlin/kotlinx/kotlinx.serialization.runtime/images/download.svg) ](https://bintray.com/kotlin/kotlinx/kotlinx.serialization.runtime/_latestVersion)

Kotlin serialization consists of a compiler plugin, that generates visitor code for serializable classes,
 runtime library with core serialization API and JSON format, and support libraries with ProtoBuf, CBOR and properties formats.

* Supports Kotlin classes marked as `@Serializable` and standard collections.
* Provides JSON, [CBOR](formats/README.md#CBOR), and [Protobuf](formats/README.md#ProtoBuf) formats.
* Complete multiplatform support: JVM, JS and Native.

## Table of contents

* [Quick example](#quick-example)
* [Runtime overview](#runtime-overview)
* [Current status](#current-project-status)
* [Library installing](#setup)
    + [Gradle](#gradle)
    + [Android/JVM](#androidjvm)
    + [Multiplatform (common, JS, Native)](#multiplatform-common-js-native)
    + [Maven/JVM](#mavenjvm)
    + [Incompatible changes from older versions](#incompatible-changes)
* [Troubleshooting IntelliJ IDEA](#troubleshooting-intellij-idea)
* [Usage](docs/runtime_usage.md)
* [More examples of supported Kotlin classes](docs/examples.md)
* [Writing JSON transformations](docs/json_transformations.md)
* [Writing custom serializers](docs/custom_serializers.md)
* [Multiplatform polymorphic serialization](docs/polymorphism.md)
* [Add-on formats](formats/README.md)
* [Building library and compiler plugin from source](docs/building.md)
* [Instructions for old versions under Kotlin 1.2 and migration guide](docs/old12.md)


## Quick example

```kotlin
import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
data class Data(val a: Int, val b: String = "42")

fun main() {
    // Json also has .Default configuration which provides more reasonable settings,
    // but is subject to change in future versions
    val json = Json(JsonConfiguration.Stable)
    // serializing objects
    val jsonData = json.stringify(Data.serializer(), Data(42))
    // serializing lists
    val jsonList = json.stringify(Data.serializer().list, listOf(Data(42)))
    println(jsonData) // {"a": 42, "b": "42"}
    println(jsonList) // [{"a": 42, "b": "42"}]

    // parsing data back
    val obj = json.parse(Data.serializer(), """{"a":42}""") // b is optional since it has default value
    println(obj) // Data(a=42, b="42")
}
```

## Runtime overview

This project contains the runtime library. Runtime library provides:

* Ready-to-use JSON serialization format.
* Core primitives for plugin-generated code (`Encoder`, `Decoder`, `SerialDescriptor`).
* Basic skeleton implementations of core primitives for custom serialization formats. 
* Built-ins and collections serializers.

You can open example projects for [JS](examples/example-js) and [JVM](examples/example-jvm).

To learn more about JSON usage and other formats, see [usage](docs/runtime_usage.md).
More examples of various kinds of Kotlin classes that can be serialized can be found [here](docs/examples.md).

## Current project status

Starting from Kotlin 1.3-RC2, serialization plugin is shipped with the rest of Kotlin compiler distribution, and the IDEA plugin is bundled into the Kotlin plugin.

Runtime library is under reconstruction to match the corresponding [KEEP](https://github.com/Kotlin/KEEP/blob/serialization/proposals/extensions/serialization.md), so some features described there can be not implemented yet. While library is stable and has successfully been used in various scenarios, there is no API compatibility guarantees between versions, that's why it is called experimental.
This document describes setup for Kotlin 1.3 and higher. To watch instructions regarding 1.2, follow [this document](docs/old12.md).

## Setup

Using Kotlin Serialization requires Kotlin compiler `1.3.30` or higher.
Make sure that you have corresponding Kotlin plugin installed in the IDE.
Since serialization is now bundled into Kotlin plugin, no additional plugins for IDE are required (but make sure you have deleted old additional plugin for 1.2, if you had one).
Example projects on JVM are available for [Gradle](examples/example-jvm/build.gradle) and [Maven](examples/example-jvm/pom.xml).

### Gradle

#### Using the `plugins` block

You can setup the serialization plugin with the Kotlin plugin using [Gradle plugins DSL](https://docs.gradle.org/current/userguide/plugins.html#sec:plugins_block):

Kotlin DSL:

```kotlin
plugins {
    kotlin("multiplatform") // or kotlin("jvm") or any other kotlin plugin
    kotlin("plugin.serialization") version "1.3.70"
}
```
Groovy DSL:

```gradle
plugins {
    id 'org.jetbrains.kotlin.multiplatform' version '1.3.70' // or any other kotlin plugin
    id 'org.jetbrains.kotlin.plugin.serialization' version '1.3.70'
}
```

Note: plugin marker for serialization has been published in Kotlin 1.3.50. If you need to use the earlier Kotlin version, see [KT-27612](https://youtrack.jetbrains.com/issue/KT-27612) for workaround with [plugin resolution rules](https://docs.gradle.org/current/userguide/plugins.html#sec:plugin_resolution_rules).

#### Using `apply plugin` (the old way)

First, you have to add the serialization plugin to your classpath as the other [compiler plugins](https://kotlinlang.org/docs/reference/compiler-plugins.html):

Kotlin DSL:

```kotlin
buildscript {
    repositories { jcenter() }

    dependencies {
        val kotlinVersion = "1.3.70"
        classpath(kotlin("gradle-plugin", version = kotlinVersion))
        classpath(kotlin("serialization", version = kotlinVersion))
    }
}
```

Groovy DSL:

```gradle
buildscript {
    ext.kotlin_version = '1.3.70'
    repositories { jcenter() }

    dependencies {
        classpath "org.jetbrains.kotlin:kotlin-serialization:$kotlin_version"
    }
}
```

Then you can `apply plugin` (example in Groovy):

```gradle
apply plugin: 'kotlin' // or 'kotlin-multiplatform' for multiplatform projects
apply plugin: 'kotlinx-serialization'
```

#### Dependency on the runtime library

After setting up the plugin one way or another, you have to add a dependency on the serialization runtime library. Note that while the plugin has version the same as the compiler one, runtime library has different coordinates, repository and versioning.

Kotlin DSL:

```kotlin
repositories {
    // artifacts are published to JCenter
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib", KotlinCompilerVersion.VERSION)) // or "stdlib-jdk8"
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.20.0") // JVM dependency
}
```

Groovy DSL:

```gradle
repositories {
    jcenter()
}

dependencies {
    implementation "org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version" // or "kotlin-stdlib-jdk8"
    implementation "org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.20.0" // JVM dependency
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
    kotlinx.serialization.KSerializer serializer(...);
}
```

You may also want to keep all custom serializers you've defined.

### Multiplatform (common, JS, Native)

Typically, you need the following dependencies in your multiplatform project (don't forget to rename [source sets](https://kotlinlang.org/docs/reference/building-mpp-with-gradle.html#configuring-source-sets) according to your setup):

```gradle
sourceSets {
    commonMain {
        dependencies {
            implementation kotlin('stdlib-common')
            implementation "org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serialization_version"
        }
    }
    commonTest {
        dependencies {
            implementation kotlin('test-common')
            implementation kotlin('test-annotations-common')
        }
    }
    jvmMain {
        dependencies {
            implementation kotlin('stdlib-jdk8')
        }
    }
    jvmTest {
        dependencies {
            implementation kotlin('test')
            implementation kotlin('test-junit')
        }
    }
    jsMain {
        dependencies {
            implementation kotlin('stdlib-js')
        }
    }
    jsTest {
        dependencies {
            implementation kotlin('test-js')
        }
    }
    nativeMain {}
    nativeTest {}
}
```

JavaScript example is located at [`example-js`](examples/example-js) folder.
Multiplatform example is located at [`example-multiplatform`](examples/example-multiplatform) folder.

### Maven/JVM

Ensure the proper version of Kotlin and serialization version:

```xml
<properties>
    <kotlin.version>1.3.70</kotlin.version>
    <serialization.version>0.20.0</serialization.version>
</properties>
```

You can also use JCenter or `https://kotlin.bintray.com/kotlinx` Bintray repository.

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
                    <groupId>org.jetbrains.kotlin</groupId>
                    <artifactId>kotlin-maven-serialization</artifactId>
                    <version>${kotlin.version}</version>
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

### Incompatible changes

Library versions `0.20.0` and higher require Kotlin 1.3.70 and higher and incompatible with previous versions.

Library version `0.14.0` require Kotlin 1.3.60/61 and incompatible with other versions.

All versions of library before `0.13.0` are using Gradle metadata v0.4 and therefore it is recommended to use Gradle 4.8-5.1 to build.

Library versions `0.11.0` and higher require Kotlin 1.3.30 and higher and incompatible with previous versions.

All versions of library before `0.10.0` are using Gradle metadata v0.3 and therefore require Gradle 4.7 for build.

Maven plugin coordinates before Kotlin 1.3.20 were `kotlinx-maven-serialization-plugin`.


## Troubleshooting IntelliJ IDEA

Serialization support should work out of the box, if you have `1.3.x` Kotlin plugin installed and have imported the project from Maven or Gradle with serialization enabled in their build scripts. 
If you have Kotlin `1.3.10` or lower, you have to delegate build to Gradle (`Settings - Build, Execution, Deployment - Build Tools - Gradle - Runner -` tick `Delegate IDE build/run actions to gradle`). 
Starting from `1.3.11`, no delegation is required.
In case of problems, force project re-import from Gradle.
