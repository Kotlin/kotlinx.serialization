# Kotlin multiplatform / multi-format reflectionless serialization

[![official JetBrains project](https://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)
[![TeamCity build](https://img.shields.io/teamcity/http/teamcity.jetbrains.com/s/KotlinTools_KotlinxSerialization_Ko.svg)](https://teamcity.jetbrains.com/viewType.html?buildTypeId=KotlinTools_KotlinxSerialization_Ko&guest=1)
[![Download](https://api.bintray.com/packages/kotlin/kotlinx/kotlinx.serialization.runtime/images/download.svg?version=0.20.0) ](https://bintray.com/kotlin/kotlinx/kotlinx.serialization.runtime/0.20.0)

Kotlin serialization consists of a compiler plugin, that generates visitor code for serializable classes,
 runtime libraries with core serialization API and JSON format, and support libraries with ProtoBuf, CBOR and properties formats.

* Supports Kotlin classes marked as `@Serializable` and standard collections.
* Provides JSON (as a part of the core library), [Protobuf](formats/README.md#ProtoBuf), [CBOR](formats/README.md#CBOR), [Hocon](formats/README.md#HOCON) and [Properties](formats/README.md#properties) formats.
* Complete multiplatform support: JVM, JS and Native.

## Table of contents

<!--- TOC -->

* [Introduction and references](#introduction-and-references)
* [Setup](#setup)
  * [Gradle](#gradle)
    * [Using the `plugins` block](#using-the-plugins-block)
    * [Using `apply plugin` (the old way)](#using-apply-plugin-the-old-way)
    * [Dependency on the runtime library](#dependency-on-the-runtime-library)
  * [Android/JVM](#android/jvm)
  * [Multiplatform (common, JS, Native)](#multiplatform-common-js-native)
  * [Maven/JVM](#maven/jvm)

<!--- END -->

## Introduction and references

Here is a small example.

```kotlin
import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable 
data class Project(val name: String, val language: String)

fun main() {
    // Serializing objects
    val data = Project("kotlinx.serialization", "Kotlin")
    val string = Json.encodeToString(data)  
    println(string) // {"name":"kotlinx.serialization","language":"Kotlin"} 
    // Deserializing back into objects
    val obj = Json.decodeFromString<Project>(string)
    println(obj) // Project(name=kotlinx.serialization, langauge=Kotlin)
}
``` 

> You can get the full code [here](guide/example/example-readme-01.kt).

<!--- TEST_NAME ReadmeTest -->

<!--- TEST 
{"name":"kotlinx.serialization","language":"Kotlin"}
Project(name=kotlinx.serialization, language=Kotlin)
-->

**Read the [Kotlin Serialization Guide](docs/serialization-guide.md) for all details.**

## Setup

Kotlin serialization plugin is shipped with the Kotlin compiler distribution, and the IDEA plugin is bundled into the Kotlin plugin.

Using Kotlin Serialization requires Kotlin compiler `1.4.0` or higher.
Make sure you have the corresponding Kotlin plugin installed in the IDE, no additional plugins for IDE are required.
Example projects on JVM are available for [Gradle](examples/example-jvm/build.gradle) and [Maven](examples/example-jvm/pom.xml).

### Gradle

#### Using the `plugins` block

You can set up the serialization plugin with the Kotlin plugin using 
[Gradle plugins DSL](https://docs.gradle.org/current/userguide/plugins.html#sec:plugins_block):

Kotlin DSL:

```kotlin
plugins {
    kotlin("jvm") version "1.4.0" // or kotlin("multiplatform") or any other kotlin plugin
    kotlin("plugin.serialization") version "1.4.0"
}
```       

Groovy DSL:

```gradle
plugins {
    id 'org.jetbrains.kotlin.multiplatform' version '1.4.0'
    id 'org.jetbrains.kotlin.plugin.serialization' version '1.4.0'
}
```

> Kotlin versions before are not supported by the stable release of Kotlin serialization

#### Using `apply plugin` (the old way)

First, you have to add the serialization plugin to your classpath as the other [compiler plugins](https://kotlinlang.org/docs/reference/compiler-plugins.html):

Kotlin DSL:

```kotlin
buildscript {
    repositories { jcenter() }

    dependencies {
        val kotlinVersion = "1.4.0"
        classpath(kotlin("gradle-plugin", version = kotlinVersion))
        classpath(kotlin("serialization", version = kotlinVersion))
    }
}
```

Groovy DSL:

```gradle
buildscript {
    ext.kotlin_version = '1.4.0'
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
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.0.0-RC") // JVM dependency
}
```

Groovy DSL:

```gradle
repositories {
    jcenter()
}

dependencies {
    implementation "org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version" // or "kotlin-stdlib-jdk8"
    implementation "org.jetbrains.kotlinx:kotlinx-serialization-core:1.0.0-RC" // JVM dependency
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

### Maven/JVM

Ensure the proper version of Kotlin and serialization version:

```xml
<properties>
    <kotlin.version>1.4.0</kotlin.version>
    <serialization.version>1.0.0-RC</serialization.version>
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
    <artifactId>kotlinx-serialization-core</artifactId>
    <version>${serialization.version}</version>
</dependency>
```
