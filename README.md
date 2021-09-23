# Kotlin multiplatform / multi-format reflectionless serialization

[![official JetBrains project](https://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)
[![TeamCity build](https://img.shields.io/teamcity/http/teamcity.jetbrains.com/s/KotlinTools_KotlinxSerialization_Ko.svg)](https://teamcity.jetbrains.com/viewType.html?buildTypeId=KotlinTools_KotlinxSerialization_Ko&guest=1)
[![Kotlin](https://img.shields.io/badge/kotlin-1.5.31-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Maven Central](https://img.shields.io/maven-central/v/org.jetbrains.kotlinx/kotlinx-serialization-core/1.3.0)](https://search.maven.org/artifact/org.jetbrains.kotlinx/kotlinx-serialization-core/1.3.0/pom)
[![KDoc link](https://img.shields.io/badge/API_reference-KDoc-blue)](https://kotlin.github.io/kotlinx.serialization/)
[![Slack channel](https://img.shields.io/badge/chat-slack-blue.svg?logo=slack)](https://kotlinlang.slack.com/messages/serialization/)

Kotlin serialization consists of a compiler plugin, that generates visitor code for serializable classes,
 runtime library with core serialization API and support libraries with various serialization formats.

* Supports Kotlin classes marked as `@Serializable` and standard collections.
* Provides [JSON](formats/README.md#JSON), [Protobuf](formats/README.md#ProtoBuf), [CBOR](formats/README.md#CBOR), [Hocon](formats/README.md#HOCON) and [Properties](formats/README.md#properties) formats.
* Complete multiplatform support: JVM, JS and Native.

## Table of contents

<!--- TOC -->

* [Introduction and references](#introduction-and-references)
* [Setup](#setup)
  * [Gradle](#gradle)
    * [Using the `plugins` block](#using-the-plugins-block)
    * [Using `apply plugin` (the old way)](#using-apply-plugin-the-old-way)
    * [Dependency on the JSON library](#dependency-on-the-json-library)
  * [Android](#android)
  * [Multiplatform (Common, JS, Native)](#multiplatform-common-js-native)
  * [Maven](#maven)

<!--- END -->

* **Additional links**
  * [Kotlin Serialization Guide](docs/serialization-guide.md)
  * [Full API reference](https://kotlin.github.io/kotlinx.serialization/)

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
    println(obj) // Project(name=kotlinx.serialization, language=Kotlin)
}
``` 

> You can get the full code [here](guide/example/example-readme-01.kt).

<!--- TEST_NAME ReadmeTest -->

<!--- TEST 
{"name":"kotlinx.serialization","language":"Kotlin"}
Project(name=kotlinx.serialization, language=Kotlin)
-->

**Read the [Kotlin Serialization Guide](docs/serialization-guide.md) for all details.**

You can find auto-generated documentation website on [GitHub Pages](https://kotlin.github.io/kotlinx.serialization/).

## Setup

Kotlin serialization plugin is shipped with the Kotlin compiler distribution, and the IDEA plugin is bundled into the Kotlin plugin.

Using Kotlin Serialization requires Kotlin compiler `1.4.0` or higher.
Make sure you have the corresponding Kotlin plugin installed in the IDE, no additional plugins for IDE are required.

### Gradle

#### Using the `plugins` block

You can set up the serialization plugin with the Kotlin plugin using 
[Gradle plugins DSL](https://docs.gradle.org/current/userguide/plugins.html#sec:plugins_block):

Kotlin DSL:

```kotlin
plugins {
    kotlin("jvm") version "1.5.31" // or kotlin("multiplatform") or any other kotlin plugin
    kotlin("plugin.serialization") version "1.5.31"
}
```       

Groovy DSL:

```gradle
plugins {
    id 'org.jetbrains.kotlin.multiplatform' version '1.5.31'
    id 'org.jetbrains.kotlin.plugin.serialization' version '1.5.31'
}
```

> Kotlin versions before 1.4.0 are not supported by the stable release of Kotlin serialization

#### Using `apply plugin` (the old way)

First, you have to add the serialization plugin to your classpath as the other [compiler plugins](https://kotlinlang.org/docs/reference/compiler-plugins.html):

Kotlin DSL:

```kotlin
buildscript {
    repositories { mavenCentral() }

    dependencies {
        val kotlinVersion = "1.5.31"
        classpath(kotlin("gradle-plugin", version = kotlinVersion))
        classpath(kotlin("serialization", version = kotlinVersion))
    }
}
```

Groovy DSL:

```gradle
buildscript {
    ext.kotlin_version = '1.5.31'
    repositories { mavenCentral() }

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

#### Dependency on the JSON library

After setting up the plugin one way or another, you have to add a dependency on the serialization library.
Note that while the plugin has version the same as the compiler one, runtime library has different coordinates, repository and versioning.

Kotlin DSL:

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.0")
}
```

Groovy DSL:

```gradle
repositories {
    mavenCentral()
}

dependencies {
    implementation "org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.0"
}
```

>We also provide `kotlinx-serialization-core` artifact that contains all serialization API but does not have bundled serialization format with it

### Android

Library should work on Android "as is". If you're using proguard, you need
to add this to your `proguard-rules.pro`:

```proguard
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt # core serialization annotations

# kotlinx-serialization-json specific. Add this if you have java.lang.NoClassDefFoundError kotlinx.serialization.json.JsonObjectSerializer
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Change here com.yourcompany.yourpackage
-keep,includedescriptorclasses class com.yourcompany.yourpackage.**$$serializer { *; } # <-- change package name to your app's
-keepclassmembers class com.yourcompany.yourpackage.** { # <-- change package name to your app's
    *** Companion;
}
-keepclasseswithmembers class com.yourcompany.yourpackage.** { # <-- change package name to your app's
    kotlinx.serialization.KSerializer serializer(...);
}
```

You may also want to keep all custom serializers you've defined.

### Multiplatform (Common, JS, Native)

Most of the modules are also available for Kotlin/JS and Kotlin/Native.
You can add dependency to the required module right to the common source set:
```gradle
commonMain {
    dependencies {
        // Works as common dependency as well as the platform one
        implementation "org.jetbrains.kotlinx:kotlinx-serialization-json:$serialization_version"
    }
}
```
The same artifact coordinates can be used to depend on platform-specific artifact in platform-specific source-set.

### Maven

Ensure the proper version of Kotlin and serialization version:

```xml
<properties>
    <kotlin.version>1.5.31</kotlin.version>
    <serialization.version>1.3.0</serialization.version>
</properties>
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
    <artifactId>kotlinx-serialization-json</artifactId>
    <version>${serialization.version}</version>
</dependency>
```
