[//]: # (title: Get started with Kotlin serialization)

[Serialization](serialization.md) converts objects into a format you can store or transmit and later reconstruct.

Kotlin serialization supports multiple formats. 
This tutorial shows you how to add the necessary plugins and dependencies for Kotlin serialization, and how to serialize and deserialize objects in JSON format.

## Add plugins and dependencies

To include the `kotlinx.serialization` library in your project, add the corresponding plugin and dependency configuration based on your build tool:

<tabs>
<tab id="kotlin" title="Gradle Kotlin">

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
<tab id="groovy" title="Gradle Groovy">

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
<tab id="maven" title="Maven">

```xml
<!-- pom.xml -->
<properties>
    <kotlin.version>%kotlinVersion%</kotlin.version>
    <serialization.version>%serializationVersion%</serialization.version>
</properties>

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

<dependencies>
    <dependency>
        <groupId>org.jetbrains.kotlinx</groupId>
        <artifactId>kotlinx-serialization-json</artifactId>
        <version>${serialization.version}</version>
    </dependency>
</dependencies>
```

</tab>
</tabs>

> To set up the Kotlin compiler plugin for Bazel, follow the example from the [rules_kotlin repository](https://github.com/bazelbuild/rules_kotlin/tree/master/examples/plugin/src/serialization).
> Bazel isn't officially supported by the Kotlin team, and this repository is maintained independently.
>
{style="tip"}

### Add the library to a multiplatform project

To use Kotlin serialization for JSON in multiplatform projects, add the JSON serialization library dependency to your common source set:

```kotlin
commonMain {
   dependencies {
      implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:%serializationVersion%")
   }
}
```

This dependency automatically includes the core serialization library as well.

### Configure R8 for Kotlin serialization in Android projects {initial-collapse-state="collapsed" collapsible="true"}

The Kotlin serialization library includes default [ProGuard rules](https://github.com/Kotlin/kotlinx.serialization/blob/master/rules/common.pro), so you don't need additional setup to keep serializers for all serializable classes after [shrinking](https://developer.android.com/topic/performance/app-optimization/enable-app-optimization).
However, these rules don't apply to classes with named companion objects.

To retain serializers for classes with named companion objects, add rules based on the [compatibility mode](https://r8.googlesource.com/r8/+/refs/heads/master/compatibility-faq.md) you use to your `proguard-rules.pro` file:

<tabs>
<tab id="compatibility" title="R8 compatibility mode">

```bash
# Serializer for classes with named companion objects are retrieved using getDeclaredClasses
# If you have any such classes, replace the examples below with your own
-keepattributes InnerClasses # Required for getDeclaredClasses

-if @kotlinx.serialization.Serializable class
com.example.myapplication.HasNamedCompanion, # <-- List serializable classes with named companions
com.example.myapplication.HasNamedCompanion2
{
    static **$* *;
}
-keepnames class <1>$$serializer { # Using -keepnames is enough for the serializer() call to reference the class correctly
    static <1>$$serializer INSTANCE;
}
```

</tab>

<tab id="full" title="R8 full mode">

```bash
# Serializer for classes with named companion objects are retrieved using getDeclaredClasses
# If you have any such classes, replace the examples below with your own
-keepattributes InnerClasses # Required for getDeclaredClasses

-if @kotlinx.serialization.Serializable class
com.example.myapplication.HasNamedCompanion, # <-- List serializable classes with named companions
com.example.myapplication.HasNamedCompanion2
{
    static **$* *;
}
-keepnames class <1>$$serializer { # Using -keepnames is enough for the serializer() call to reference the class correctly
    static <1>$$serializer INSTANCE;
}

# Keep both serializer and serializable classes to save the attribute InnerClasses
-keepclasseswithmembers, allowshrinking, allowobfuscation, allowaccessmodification class
com.example.myapplication.HasNamedCompanion, # <-- List serializable classes with named companions
com.example.myapplication.HasNamedCompanion2
{
    *;
}
```

</tab>
</tabs>

> You can exclude serializable classes that are never serialized at runtime by using custom ProGuard rules with narrower [class specifications](https://www.guardsquare.com/manual/configuration/usage).
> 
{style="tip"}

## Serialize objects to JSON

In Kotlin, you can serialize objects to JSON using the `kotlinx.serialization` library.

To make a class serializable, you need to mark it with the [`@Serializable`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-serializable/) annotation.
This annotation instructs the compiler to generate the code required for serializing and deserializing instances of the class.
For more information, see [The `@Serializable` annotation](serialization-customization-options.md#the-serializable-annotation).

Let's look at an example:

1. Import declarations from the necessary serialization libraries:

    ```kotlin
    import kotlinx.serialization.*
    import kotlinx.serialization.json.*
    ```

2. Make a class serializable by annotating it with `@Serializable`:

    ```kotlin
    @Serializable
    data class Book(val yearPublished: Int, val title: String)
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

    // Marks the Book class as serializable
    @Serializable
    data class Book(val yearPublished: Int, val title: String)

    fun main() {
        // Serializes an instance of the Book class into a JSON string
        val json = Json.encodeToString(Book(1937, "The Hobbit"))
        println(json)
        // {"yearPublished":1937,"title":"The Hobbit"}
    }
    ```
   {kotlin-runnable="true" id="serialize-get-started"}

   As a result, you get a string containing the state of this object in JSON format: `{"yearPublished":1937,"title":"The Hobbit"}`

   > You can also serialize a collection of objects in a single call:
   >
   > ```kotlin
    > val bookList = listOf(Book(1937, "The Hobbit"), Book(1867, "War and Peace"))
    > val jsonList = Json.encodeToString(bookList)
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
    data class Book(val yearPublished: Int, val title: String)
    ```

3. Use the [`Json.decodeFromString()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json/decode-from-string.html) function to deserialize an object from JSON:

    ```kotlin
    // Imports declarations from the serialization and JSON handling libraries
    import kotlinx.serialization.*
    import kotlinx.serialization.json.*

    // Marks the Book class as serializable
    @Serializable
    data class Book(val yearPublished: Int, val title: String)

    fun main() {
        // Deserializes a JSON string into an instance of the Book class
        val obj = Json.decodeFromString<Book>("""{"yearPublished":1937, "title": "The Hobbit"}""")
        println(obj)
        // Book(yearPublished=1937, title=The Hobbit)
    }
    ```
   {kotlin-runnable="true" id="deserialize-get-started"}

Congratulations! You have successfully serialized an object to JSON and deserialized it back into an object in Kotlin.

## What's next

* Learn how to serialize basic types such as primitives and strings, as well as certain standard library classes, in [Serialize built-in types](serialization-serialize-builtin-types.md).
* Discover how to customize class serialization and adjust the default behavior of the `@Serializable` annotation in [Serialize classes](serialization-customization-options.md).
* Dive deeper into handling JSON data and configuring JSON serialization in the [JSON serialization overview](configure-json-serialization.md).
