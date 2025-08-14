[//]: # (title: Get started with Kotlin serialization)

[Serialization](serialization.md) converts objects into a format that can be stored or transmitted and later reconstructed.

This tutorial shows you how to add the necessary plugins and dependencies for Kotlin serialization and how to serialize and deserialize objects in JSON format.

## Add plugins and dependencies for Kotlin serialization

To include the `kotlinx.serialization` library in your project, add the corresponding plugin and dependency configuration based on your build tool.

> To set up the Kotlin compiler plugin for Bazel, see the example provided in the [rules_kotlin repository](https://github.com/bazelbuild/rules_kotlin/tree/master/examples/plugin/src/serialization).
>
{style="tip"}

### Gradle

Add the following dependency to your `build.gradle(.kts)` file:

<tabs>
<tab id="kotlin" title="Kotlin">

```kotlin
// build.gradle.kts
plugins {
    kotlin("plugin.serialization") version "%kotlinVersion%"
}

dependencies { 
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:%serializationVersion%")
}
```

</tab>

<tab id="groovy" title="Groovy">

```groovy
// build.gradle
plugins {
   id 'org.jetbrains.kotlin.plugin.serialization' version '%kotlinVersion%'  
}

dependencies {
    implementation 'org.jetbrains.kotlinx:kotlinx-serialization-json:%serializationVersion%'
}
```

</tab>

</tabs>

### Maven

Add the serialization plugin and library to your `pom.xml` file:

1. Specify the Kotlin and serialization version in the `<properties>` section:

    ```xml
    <properties>
        <kotlin.version>%kotlinVersion%</kotlin.version>
        <serialization.version>%serializationVersion%</serialization.version>
    </properties>
   ```

2. Add the Kotlin serialization Maven plugin to the `<build>` section:

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

3. Add the JSON serialization library dependency to the `<dependencies>` section:

   ```xml
   <dependencies>
       <dependency>
           <groupId>org.jetbrains.kotlinx</groupId>
           <artifactId>kotlinx-serialization-json</artifactId>
           <version>${serialization.version}</version>
       </dependency>
   </dependencies>
   ```

### Add the Kotlin serialization library to a multiplatform project

To use Kotlin serialization in multiplatform projects, add the JSON serialization library dependency to your common source set:

```kotlin
commonMain {
   dependencies {
      implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:%serializationVersion%")
   }
}
```

## Serialize objects to JSON

In Kotlin, you can serialize objects to JSON using the `kotlinx.serialization` library.

To make a class serializable, you need to mark it with the [`@Serializable`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-serializable/) annotation.
This annotation instructs the compiler to generate the code required for serializing and deserializing instances of the class.
For more information, see [The @Serialization annotation](serialization-customization-options.md#the-serializable-annotation).

Let's look at an example:

1. Import declarations from the necessary serialization libraries:

    ```kotlin
    import kotlinx.serialization.*
    import kotlinx.serialization.json.*
    ```

2. Make a class serializable by annotating it with `@Serializable`:

    ```kotlin
    @Serializable
    data class Data(val a: Int, val b: String)
    ```

   > The `@Serializable` annotation enables default serialization of all properties with backing fields.
   > You can customize serialization behavior with property-level annotations, optional properties, and more.
   > 
   > For more information, see [Serialize classes](serialization-customization-options.md).
   >
   {style="note"}

3. Use the [`Json.encodeToString()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json/encode-to-string.html) function to serialize an instance of this class:

    ```kotlin
    // Imports declarations from the serialization and JSON handling libraries
    import kotlinx.serialization.*
    import kotlinx.serialization.json.*

    // Marks the Data class as serializable
    @Serializable
    data class Data(val a: Int, val b: String)

    fun main() {
        // Serializes an instance of the Data class into a JSON string
        val json = Json.encodeToString(Data(42, "str"))
        println(json)
        // {"a":42,"b":"str"}
    }
    ```
   {kotlin-runnable="true"}

   As a result, you get a string containing the state of this object in JSON format: `{"a":42,"b":"str"}`

   > You can also serialize a collection of objects in a single call:
   >
   > ```kotlin
    > val dataList = listOf(Data(42, "str"), Data(12, "test"))
    > val jsonList = Json.encodeToString(dataList)
    > ```
   >
   {style="tip"}

## Deserialize objects from JSON

Deserialization converts a JSON string back into an object.

To deserialize an object from JSON in Kotlin:

1. Import declarations from the necessary serialization libraries:

    ```kotlin
    import kotlinx.serialization.*
    import kotlinx.serialization.json.*
    ```

2. Make a class serializable by annotating it with `@Serializable`:

    ```kotlin
    @Serializable
    data class Data(val a: Int, val b: String)
    ```

3. Use the [`Json.decodeFromString()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json/decode-from-string.html) function to deserialize an object from JSON:

    ```kotlin
    // Imports declarations from the serialization and JSON handling libraries
    import kotlinx.serialization.*
    import kotlinx.serialization.json.*

    // Marks the Data class as serializable
    @Serializable
    data class Data(val a: Int, val b: String)

    fun main() {
        // Deserializes a JSON string into an instance of the Data class
        val obj = Json.decodeFromString<Data>("""{"a":42, "b": "str"}""")
        println(obj)
        // Data(a=42, b=str)
    }
    ```
   {kotlin-runnable="true"}

Congratulations! You have successfully serialized an object to JSON and deserialized it back into an object in Kotlin!

## What's next

* Learn how to serialize standard types, including built-in types like numbers and strings, in [Serialize built-in types](serialization-serialize-builtin-types.md).
* Discover how to customize class serialization and adjust the default behavior of the `@Serializable` annotation in the [Serialize classes](serialization-customization-options.md) section.
* Dive deeper into handling JSON data and configuring JSON serialization in the [JSON serialization overview](configure-json-serialization.md).
