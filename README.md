# Kotlin cross-platform / multi-format reflectionless serialization

[![JetBrains incubator project](https://jb.gg/badges/incubator.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)
[![TeamCity build](https://img.shields.io/teamcity/http/teamcity.jetbrains.com/s/KotlinTools_KotlinxSerialization_Ko.svg)](https://teamcity.jetbrains.com/viewType.html?buildTypeId=KotlinTools_KotlinxSerialization_Ko&guest=1)
[![Download](https://api.bintray.com/packages/kotlin/kotlinx/kotlinx.serialization.runtime/images/download.svg) ](https://bintray.com/kotlin/kotlinx/kotlinx.serialization.runtime/_latestVersion)

Kotlin serialization consists of a compiler plugin, which automatically produces visitor code for classes, and runtime library, which uses generated code to serialize objects without reflection.

* Supports Kotlin classes marked as `@Serializable` and standard collections.
* Supports JSON, CBOR, and Protobuf formats out-of-the-box.
* The same code works on Kotlin/JVM, Kotlin/JS and Kotlin/Native

## Runtime overview

This project contains the runtime library. Runtime library provides:

* Interfaces which are called by compiler-generated code (`Encoder`, `Decoder`).
* Basic skeleton implementations of these interfaces in which you should override some methods if you want to
  implement custom data format.
* Some internal classes like built-ins and collections serializers.
* Ready-to-use serialization formats.
* Other useful classes that benefit from serialization framework (e.g. object-to-Map transformer)

You can open example projects for [JS](examples/example-js), [JVM](examples/example-jvm) or [Native](examples/example-native) to get started playing with it.

## Table of contents

* [Quick example](#quick-example)
* [Current status](#current-project-status)
* [Library installing](#setup)
    + [Gradle](#gradle)
    + [Gradle (with `plugins` block)](#gradle-with-plugins-block)
    + [Android/JVM](#androidjvm)
    + [Multiplatform (common, JS, Native)](#multiplatform-common-js-native)
    + [Maven/JVM](#mavenjvm)
    + [Incompatible changes from older versions](#incompatible-changes)
* [Troubleshooting IntelliJ IDEA](#troubleshooting-intellij-idea)
* [Usage](docs/runtime_usage.md)
* [More examples of supported Kotlin classes](docs/examples.md)
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

You have to add the serialization plugin as the other [compiler plugins](https://kotlinlang.org/docs/reference/compiler-plugins.html):

```gradle
buildscript {
    ext.kotlin_version = '1.3.50'
    repositories { jcenter() }

    dependencies {
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
        classpath "org.jetbrains.kotlin:kotlin-serialization:$kotlin_version"
    }
}
```

Don't forget to apply the plugin:

```gradle
apply plugin: 'kotlin' // or 'kotlin-multiplatform' for multiplatform projects
apply plugin: 'kotlinx-serialization'
```

Next, you have to add dependency on the serialization runtime library. Note that while plugin have version the same as compiler one, runtime library has different coordinates, repository and versioning.

```gradle
repositories {
    // artifacts are published to JCenter
    jcenter()
}

dependencies {
    compile "org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version"
    compile "org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.13.0" // JVM dependency
}
```

### Gradle (with `plugins` block)

You can setup serialization plugin with the kotlin plugin using [Gradle plugins DSL](https://docs.gradle.org/current/userguide/plugins.html#sec:plugins_block) **instead** of traditional `apply plugin`:

```gradle
plugins {
    id 'org.jetbrains.kotlin.multiplatform' version '1.3.50' // or any other kotlin plugin
    id 'org.jetbrains.kotlin.plugin.serialization' version '1.3.50'
}
```

Note: plugin marker for serialization has been published in Kotlin 1.3.50. If you need to use the earlier Kotlin version, see [KT-27612](https://youtrack.jetbrains.com/issue/KT-27612) for workaround with [plugin resolution rules](https://docs.gradle.org/current/userguide/plugins.html#sec:plugin_resolution_rules).

Runtime library should be added to dependencies the same way as before.

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

Platform artifacts have the same names as JVM one, but with additional suffix (e.g. `org.jetbrains.kotlinx:kotlinx-serialization-runtime-native`). For Native artifact, Gradle metadata is required (put the line `enableFeaturePreview('GRADLE_METADATA')` in your `gradle.properties`) and minimal supported version of Gradle is 5.3.

Typically, you need the following dependencies in your multiplatform project (don't forget to rename [source sets](https://kotlinlang.org/docs/reference/building-mpp-with-gradle.html#configuring-source-sets) according yo your setup):

```gradle
sourceSets {
    commonMain {
        dependencies {
            implementation kotlin('stdlib-common')
            implementation "org.jetbrains.kotlinx:kotlinx-serialization-runtime-common:$serialization_version"
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
            implementation "org.jetbrains.kotlinx:kotlinx-serialization-runtime:$serialization_version"
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
            implementation "org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:$serialization_version"
        }
    }
    jsTest {
        dependencies {
            implementation kotlin('test-js')
        }
    }
    nativeMain {
        dependencies {
            implementation "org.jetbrains.kotlinx:kotlinx-serialization-runtime-native:$serialization_version"
        }
    }
    nativeTest {}
}
```

JavaScript example is located at [`example-js`](examples/example-js) folder.
Multiplatform example is located at [`example-multiplatform`](examples/example-multiplatform) folder.

### Maven/JVM

Ensure the proper version of Kotlin and serialization version:

```xml
<properties>
    <kotlin.version>1.3.50</kotlin.version>
    <serialization.version>0.13.0</serialization.version>
</properties>
```

Include kotlinx bintray repository for library:

```xml
<repositories>
    <repository>
        <id>bintray-kotlin-kotlinx</id>
        <name>bintray</name>
        <url>https://kotlin.bintray.com/kotlinx</url>
    </repository>
</repositories>
```

You also can use JCenter.

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

All versions of library before `0.13.0` are using Gradle metadata v0.4 and therefore it is recommended to use Gradle 4.8-5.1 to build.

Library versions `0.11.0` and higher require Kotlin 1.3.30 and higher and incompatible with previous versions.

All versions of library before `0.10.0` are using Gradle metadata v0.3 and therefore require Gradle 4.7 for build.

Maven plugin coordinates before Kotlin 1.3.20 were `kotlinx-maven-serialization-plugin`.

For deprecated `kotlin-platform-native` plugin, you need to use `kotlinx-serialization-native` plugin (see [#2210](https://github.com/JetBrains/kotlin-native/issues/2210#issuecomment-429753168)).

## Troubleshooting IntelliJ IDEA

Serialization support should work out of the box, if you have 1.3.x Kotlin plugin installed and have imported the project from Maven or Gradle with serialization enabled in their buildscripts. If you have Kotlin 1.3.10 or lower, you have to delegate build to Gradle (`Settings - Build, Execution, Deployment - Build Tools - Gradle - Runner -` tick `Delegate IDE build/run actions to gradle`). Starting from 1.3.11, no delegation is required.
In case of problems, force project re-import from Gradle.
