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

## Serialize an object to JSON

Serialization is the process of converting an object into a format that can be easily stored or transmitted, such as JSON.
In Kotlin, you can serialize objects to JSON using the `kotlinx.serialization` library.

To make a class serializable, you need to use the [`@Serializable`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-serializable/) annotation.
This annotation indicates to the compiler to generate the necessary code for serializing and deserializing instances of the class.
For more information, see [The @Serialization annotation](serialization.md#the-serializable-annotation) section.

When marking a class with the `@Serializable` annotation, consider the following:

* Only properties that store their values directly, known as backing fields are serialized. Properties that compute their values dynamically using custom getters are not serialized.
* All parameters in the primary constructor must be properties of the object.
* If data validation is needed before serialization, you can use the init block to validate the properties.

Let's look at an example:

1. Import the necessary serialization libraries:

    ```kotlin
    import kotlinx.serialization.*
    import kotlinx.serialization.json.*
    ```

2. Make a class serializable by annotating it with `@Serializable`.

    ```kotlin
    @Serializable
    data class Data(val a: Int, val b: String)
    ```

   > The `@Serialization` annotation enables default serialization of all properties in the primary constructor.
   > You can adjust this behavior using various techniques. These include serialization of backing fields, defining
   > custom constructors, specifying optional properties, marking properties as required and more. 
   > These techniques allow for precise control over which properties are serialized and how the serialization process is managed.
   > For more information, see [Serialization customization options](serialization-customization-options.md).
   >
   {type="note"}

3. Serialize an instance of this class by calling `Json.encodeToString()`.

    ```kotlin
    // Imports the necessary libraries
    import kotlinx.serialization.Serializable
    import kotlinx.serialization.json.Json
    import kotlinx.serialization.encodeToString
    
    @Serializable
    data class Data(val a: Int, val b: String)
    
    fun main() {
       val json = Json.encodeToString(Data(42, "str"))
    }
    ```

   As a result, you get a string containing the state of this object in the JSON format: `{"a": 42, "b": "str"}`

   > You can serialize object collections, such as lists, in a single call:
   >
   > ```kotlin
    > val dataList = listOf(Data(42, "str"), Data(12, "test"))
    > val jsonList = Json.encodeToString(dataList)
    > ```
   >
    {type="note"}

4. You can also use the `.serializer()` function to retrieve and use the automatically generated serializer:

   ```kotlin
    // Imports the necessary libraries for serialization
    import kotlinx.serialization.Serializable
    import kotlinx.serialization.json.Json
    import kotlinx.serialization.encodeToString
    // Imports the KSerializer interface
    import kotlinx.serialization.KSerializer
   
    // Marks the Data class as serializable
    @Serializable
    data class Data(val a: Int, val b: String)
   
    fun main() {
        // Retrieves the automatically generated serializer for the Data class
        val serializer: KSerializer<Data> = Data.serializer()
   
        // Serializes an instance of Data using the retrieved serializer
        val json = Json.encodeToString(serializer, Data(42, "str"))
        println(json)
        // {"a":42,"b":"str"}
    }
   ```
   The `.serializer()` function allows you to explicitly obtain the serializer instance created by the `kotlinx.serialization` library for your `@Serializable` class,
   allowing you to interact with the serialization process directly.
   For more information, see the [Create custom serializers](create-custom-serializers.md) page.

## Deserialize an object from JSON

Deserialization reconstructs an object from its serialized form.

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

## What's next?

* Discover various techniques for adjusting serialization behavior in [Serialization customization options](serialization-customization-options.md).
* Learn how to create custom serializers in [Create custom serializers](create-custom-serializers.md).
* 