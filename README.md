# Kotlin multiplatform / multi-format reflectionless serialization

[![Kotlin Stable](https://kotl.in/badges/stable.svg)](https://kotlinlang.org/docs/components-stability.html)
[![JetBrains official project](https://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)
[![TeamCity build](https://img.shields.io/teamcity/http/teamcity.jetbrains.com/s/KotlinTools_KotlinxSerialization_Ko.svg)](https://teamcity.jetbrains.com/viewType.html?buildTypeId=KotlinTools_KotlinxSerialization_Ko&guest=1)
[![Kotlin](https://img.shields.io/badge/kotlin-1.7.20-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Maven Central](https://img.shields.io/maven-central/v/org.jetbrains.kotlinx/kotlinx-serialization-core/1.4.1)](https://search.maven.org/artifact/org.jetbrains.kotlinx/kotlinx-serialization-core/1.4.1/pom)
[![KDoc link](https://img.shields.io/badge/API_reference-KDoc-blue)](https://kotlinlang.org/api/kotlinx.serialization/)
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
  * [Full API reference](https://kotlinlang.org/api/kotlinx.serialization/)

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

You can find auto-generated documentation website on [kotlinlang.org](https://kotlinlang.org/api/kotlinx.serialization/).

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
    kotlin("jvm") version "1.7.20" // or kotlin("multiplatform") or any other kotlin plugin
    kotlin("plugin.serialization") version "1.7.20"
}
```       

Groovy DSL:

```gradle
plugins {
    id 'org.jetbrains.kotlin.multiplatform' version '1.7.20'
    id 'org.jetbrains.kotlin.plugin.serialization' version '1.7.20'
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
        val kotlinVersion = "1.7.20"
        classpath(kotlin("gradle-plugin", version = kotlinVersion))
        classpath(kotlin("serialization", version = kotlinVersion))
    }
}
```

Groovy DSL:

```gradle
buildscript {
    ext.kotlin_version = '1.7.20'
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
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
}
```

Groovy DSL:

```gradle
repositories {
    mavenCentral()
}

dependencies {
    implementation "org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1"
}
```

>We also provide `kotlinx-serialization-core` artifact that contains all serialization API but does not have bundled serialization format with it

### Android

By default, proguard rules are supplied with the library.
[These rules](proguard-rules.pro) keep serializers for _all_ serializable classes that are retained after shrinking.

**These rules do not affect serializable classes with named companion objects.**

If you want to serialize classes with named companion objects, you need to add and edit rule below to your `proguard-rules.pro` configuration. 


```proguard
# Serializer for classes with named companion objects are retrieved using `getDeclaredClasses`.
# If you have any, uncomment and replace classes with those containing named companion objects.
#-keepattributes InnerClasses # Needed for `getDeclaredClasses`.
#-if @kotlinx.serialization.Serializable class
#com.example.myapplication.HasNamedCompanion, # <-- List serializable classes with named companions.
#com.example.myapplication.HasNamedCompanion2
#{
#    static **$* *;
#}
#-keepnames class <1>$$serializer { # -keepnames suffices; class is kept when serializer() is kept.
#    static <1>$$serializer INSTANCE;
#}
```

In case you want to exclude serializable classes that are used, but never serialized at runtime,
you will need to write custom rules with narrower [class specifications](https://www.guardsquare.com/manual/configuration/usage).

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
    <kotlin.version>1.7.20</kotlin.version>
    <serialization.version>1.4.1</serialization.version>
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
