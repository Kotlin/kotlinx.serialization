[//]: # (title: Get started with Kotlin serialization)

[Serialization](serialization.md) in Kotlin involves converting objects to a format that can be easily stored or transmitted and later reconstructed.

In this guide, we will walk you through the process of configuring your project for Kotlin serialization,
as well as how to serialize and deserialize objects to and from JSON format.

## Add plugins and dependencies for Kotlin serialization

Before starting, you must configure your build script to use Kotlin serialization tools in your project.
This guide covers how to apply the Kotlin serialization plugin and add the necessary dependencies using Gradle
(with Kotlin DSL and Groovy DSL) and Maven.

<tabs>
<tab id="kotlin" title="Kotlin DSL">

1. Add the Kotlin serialization Gradle plugin `kotlin("plugin.serialization")` to your `build.gradle.kts` file:

    ```kotlin
    plugins {
        kotlin("jvm") version "%kotlinVersion%"
        kotlin("plugin.serialization") version "%kotlinVersion%"
    }
    ```

2. Add the JSON serialization library dependency `org.jetbrains.kotlinx:kotlinx-serialization-json:%serializationVersion%` to your `build.gradle.kts` file:

    ```kotlin
    dependencies { 
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:%serializationVersion%")
    } 
    ```

</tab>

<tab id="groovy" title="Groovy DSL">

1. Add the Kotlin serialization Gradle plugin `org.jetbrains.kotlin.plugin.serialization` to your `build.gradle` file:

    ```groovy
    plugins {
        id 'org.jetbrains.kotlin.jvm' version '%kotlinVersion%'
        id 'org.jetbrains.kotlin.plugin.serialization' version '%kotlinVersion%'  
    }
    ```

2. Add the JSON serialization library dependency `org.jetbrains.kotlinx:kotlinx-serialization-json:%serializationVersion%` to your `build.gradle` file:

    ```groovy
    dependencies {
        implementation 'org.jetbrains.kotlinx:kotlinx-serialization-json:%serializationVersion%'
    } 
    ```

</tab>

<tab id="maven" title="Maven">

1. Specify the Kotlin and serialization versions by adding the following properties to your `pom.xml` file:

    ```xml
    <properties>
    <kotlin.version>%kotlinVersion%</kotlin.version>
    <serialization.version>%serializationVersion%</serialization.version>
    </properties>
   ```

2. Add the Kotlin serialization Maven plugin to the `<build>` section of your `pom.xml` file:

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

3. Add the JSON serialization library dependency to the `<dependencies>` section of your `pom.xml` file:

   ```xml
   <dependencies>
       <dependency>
           <groupId>org.jetbrains.kotlinx</groupId>
           <artifactId>kotlinx-serialization-json</artifactId>
           <version>${serialization.version}</version>
       </dependency>
   </dependencies>
   ```

</tab>

<tab id="bazel" title="Bazel">

To set up the Kotlin compiler plugin for Bazel, see the example provided in the [rules_kotlin repository](https://github.com/bazelbuild/rules_kotlin/tree/master/examples/plugin/src/serialization).

</tab>

</tabs>

### Add Kotlin serialization dependencies for multiplatform projects

You can use Kotlin serialization in multiplatform projects, including Kotlin/JS and Kotlin/Native.
To do so, you must add the JSON serialization library dependency to your common source set:

```kotlin
commonMain {
   dependencies {
      implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:%serializationVersion%")
   }
}
```

## Serialize objects to JSON

Serialization is the process of converting an object into a format that can be easily stored or transmitted, such as JSON.
In Kotlin, you can serialize objects to JSON using the `kotlinx.serialization` library.

To make a class serializable, you need to mark it with the [`@Serializable`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-serializable/) annotation.
This annotation indicates to the compiler to generate the necessary code for serializing and deserializing instances of the class.
For more information, see [The @Serialization annotation](serialization-customization-options.md#the-serializable-annotation) section.

Let's look at an example:

1. Import the necessary serialization libraries:

    ```kotlin
    import kotlinx.serialization.*
    import kotlinx.serialization.json.*
    ```

2. Make a class serializable by annotating it with `@Serializable`:

    ```kotlin
    @Serializable
    data class Data(val a: Int, val b: String)
    ```

   > The `@Serializable` annotation enables default serialization of all properties in the primary constructor.
   > You can customize serialization behavior using various techniques like custom constructors, optional properties, and more.
   > For more information, see [Serialize classes](serialization-customization-options.md).
   >
   {style="note"}

3. Serialize an instance of this class by calling the `Json.encodeToString()` function:

    ```kotlin
    // Imports the necessary libraries for serialization and JSON handling
    import kotlinx.serialization.Serializable
    import kotlinx.serialization.json.Json
    import kotlinx.serialization.encodeToString
    
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

   As a result, you get a string containing the state of this object in JSON format: `{"a": 42, "b": "str"}`

    > You can also serialize a collection of objects in a single call:
    >
    > ```kotlin
    > val dataList = listOf(Data(42, "str"), Data(12, "test"))
    > val jsonList = Json.encodeToString(dataList)
    > ```
    >
    {style="note"}

## Deserialize objects from JSON

Deserialization converts a JSON string back into an object.

To deserialize an object from JSON in Kotlin:

1. Import the necessary serialization libraries:

    ```kotlin
    import kotlinx.serialization.*
    import kotlinx.serialization.json.*
    ```

2. Make a class serializable by annotating it with `@Serializable`:

    ```kotlin
    @Serializable
    data class Data(val a: Int, val b: String)
    ```

3. Use the `Json.decodeFromString()` function to deserialize an object from JSON:

    ```kotlin
    // Imports the necessary libraries for serialization and JSON handling
    import kotlinx.serialization.*
    import kotlinx.serialization.json.*
    
    // Marks the Data class as serializable
    @Serializable
    data class Data(val a: Int, val b: String)
    
    fun main() {
        // Deserializes a JSON string into an instance of the Data class
        val obj = Json.decodeFromString<Data>("""{"a":42, "b": "str"}""")
        println(obj)
        // Data(a=42, b="str")
    }
    ```
    {kotlin-runnable="true"}

Congratulations! You have successfully serialized an object to JSON and deserialized it back into an object in Kotlin!

## What's next?

* Learn how to serialize standard types, including built-in types like numbers and strings, in [Serialize built-in types](serialization-serialize-builtin-types.md).
* Discover how to customize class serialization and adjust the default behavior of the `@Serializable` annotation in the [Serialize classes](serialization-customization-options.md) section.
* Dive deeper into handling JSON data and configuring JSON serialization in the [JSON serialization overview](configure-json-serialization.md).
