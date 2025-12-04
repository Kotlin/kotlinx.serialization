[//]: # (title: Serialize JSON with I/O sources)
<primary-label ref="experimental-general"/>

The Kotlin serialization library provides APIs for working with JVM streams and [`kotlinx-io`](https://github.com/Kotlin/kotlinx-io) or [Okio](https://square.github.io/okio/) sources and sinks.
You can use these APIs to serialize and deserialize JSON directly from I/O sources without creating intermediate strings.

> When working with I/O resources, it's important to close them properly to prevent resource leaks.  
> You can do this with the `.use()` function, which closes the resource automatically when the operation completes.
>
{style="note"}

## Serialize JSON to JVM output streams

Use the [`.encodeToStream()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/encode-to-stream.html) extension function to serialize JSON directly to a JVM [`OutputStream`](https://docs.oracle.com/javase/8/docs/api/java/io/OutputStream.html):

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.io.FileOutputStream

@Serializable
data class Project(val name: String, val stars: Int)

fun main() {
    val project = Project("kotlinx.serialization", 9000)
    
    // Creates an OutputStream for the project.json file
    FileOutputStream("project.json").use { output ->
        
        // Serializes the project instance into the OutputStream
        Json.encodeToStream(project, output)
    }
}
```

In this example, the JSON representation of `Project` is serialized into the `project.json` file.

## Deserialize JSON from JVM input streams

To deserialize JSON directly from a JVM [`InputStream`](https://docs.oracle.com/javase/8/docs/api/java/io/InputStream.html), use the [`.decodeFromStream()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/decode-from-stream.html) extension function:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.io.FileInputStream

@Serializable
data class Project(val name: String, val stars: Int)

fun main() {
    // Opens an InputStream
    FileInputStream("project.json").use { input ->

        // Deserializes the JSON contents of the InputStream into a Project instance
        val project = Json.decodeFromStream<Project>(input)

        // Prints the deserialized Project instance
        println(project)
    }
}
```

In this example, the JSON contents of the input stream are deserialized into a single `Project` instance.

If your input contains multiple JSON objects in a top-level JSON array, you can use [`.decodeToSequence()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/decode-to-sequence.html) to process the elements lazily.
This lets you handle each value as it is parsed.

Here's an example:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import java.io.FileInputStream

@Serializable
data class Project(val name: String, val stars: Int)

fun main() {
    // Opens an InputStream for the projects.json file containing a JSON array of Project objects
    FileInputStream("projects.json").use { input ->

        // Lazily deserializes each Project from the InputStream
        val projects = Json.decodeToSequence<Project>(input)

        // Processes elements one by one
        for (project in projects) {
            println(project)
        }
    }
}
```

> You can iterate through sequences returned by `.decodeToSequence()` only once
> because they are tied to the underlying stream.
> 
{style="note"}

## JSON serialization with kotlinx-io and Okio

In addition to JVM streams, you can work with JSON using I/O types, such as `Source` and `Sink` from [`kotlinx-io`](https://github.com/Kotlin/kotlinx-io), and `BufferedSource`
and `BufferedSink` from [Okio](https://square.github.io/okio/).

You can use the following `Json` extension functions to read and write JSON directly through these I/O types:

* [`.encodeToSink()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json-io/kotlinx.serialization.json.io/encode-to-sink.html) and [`.encodeToBufferedSink()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json-okio/kotlinx.serialization.json.okio/encode-to-buffered-sink.html) to write JSON to a `Sink` or `BufferedSink`.
* [`.decodeFromSource()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json-io/kotlinx.serialization.json.io/decode-from-source.html) and [`.decodeFromBufferedSource()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json-okio/kotlinx.serialization.json.okio/decode-from-buffered-source.html) to read a single JSON value from a `Source` or `BufferedSource`.
* [`.decodeSourceToSequence()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json-io/kotlinx.serialization.json.io/decode-source-to-sequence.html) and [`.decodeBufferedSourceToSequence()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json-okio/kotlinx.serialization.json.okio/decode-buffered-source-to-sequence.html) to lazily decode multiple JSON values as a `Sequence<T>`.

> The JSON I/O extension functions use UTF-8 encoding and throw `SerializationException` for invalid JSON data and `IOException` for I/O failures.
> 
{style="tip"}

To use these extension functions with `kotlinx-io` or Okio types, add the following dependencies to your project:

<tabs>
<tab id="kotlin" title="Gradle Kotlin">

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-io:%serializationVersion%")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-okio:%serializationVersion%")

    implementation("org.jetbrains.kotlinx:kotlinx-io-core:%kotlinxIoVersion%")
    implementation("com.squareup.okio:okio:%okioVersion%")
}

```

</tab>
<tab id="groovy" title="Gradle Groovy">

```groovy
// build.gradle
dependencies {
    implementation "org.jetbrains.kotlinx:kotlinx-serialization-json-io:%serializationVersion%"
    implementation "org.jetbrains.kotlinx:kotlinx-serialization-json-okio:%serializationVersion%"

    implementation "org.jetbrains.kotlinx:kotlinx-io-core:%kotlinxIoVersion%"
    implementation "com.squareup.okio:okio:%okioVersion%"
}
```

</tab>
<tab id="maven" title="Maven">

```xml
<!-- pom.xml -->
<dependencies>
    <dependency>
        <groupId>org.jetbrains.kotlinx</groupId>
        <artifactId>kotlinx-serialization-json-io</artifactId>
        <version>%serializationVersion%</version>
    </dependency>
    <dependency>
        <groupId>org.jetbrains.kotlinx</groupId>
        <artifactId>kotlinx-serialization-json-okio</artifactId>
        <version>%serializationVersion%</version>
    </dependency>
    <dependency>
        <groupId>org.jetbrains.kotlinx</groupId>
        <artifactId>kotlinx-io-core</artifactId>
        <version>%kotlinxIoVersion%</version>
    </dependency>
    <dependency>
        <groupId>com.squareup.okio</groupId>
        <artifactId>okio</artifactId>
        <version>%okioVersion%</version>
    </dependency>
</dependencies>
```
</tab>
</tabs>

The next sections cover examples using `kotlinx-io` types with these APIs.  
You can use Okio types similarly with their corresponding `BufferedSource` and `BufferedSink`.


### Serialize JSON to Sinks

Use the `.encodeToSink()` function to serialize JSON to a `Sink`:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*

// Imports declarations for kotlinx-io types and JSON I/O support
import kotlinx.serialization.json.io.*
import kotlinx.io.*
import kotlinx.io.files.*

@Serializable
data class Project(val name: String, val stars: Int)

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val project = Project("kotlinx.serialization", 9000)

    // Creates a Sink for the project.json file
    val path = Path("project.json")
    SystemFileSystem.sink(path).buffered().use { sink: Sink ->

        // Serializes the Project instance directly into a Sink
        Json.encodeToSink(project, sink)
    }
}
```

### Deserialize JSON from Sources

To deserialize JSON from a `Source`, use the `.decodeFromSource()` function:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*

// Imports declarations for kotlinx-io types and JSON I/O support
import kotlinx.serialization.json.io.*
import kotlinx.io.*
import kotlinx.io.files.*

@Serializable
data class Project(val name: String, val stars: Int)

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    // Opens a Source for the project.json file
    val path = Path("project.json")
    SystemFileSystem.source(path).buffered().use { source: Source ->

        // Deserializes a Project instance directly from a Source
        val project = Json.decodeFromSource<Project>(source)

        println(project)
    }
}
```

If your input contains a large JSON array or multiple top-level JSON objects,
you can turn a `Source` into a lazily decoded `Sequence<T>` with the
`.decodeSourceToSequence()` function.

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.json.*

// Imports declarations for kotlinx-io types and JSON I/O support
import kotlinx.serialization.json.io.*
import kotlinx.io.*
import kotlinx.io.files.*

@Serializable
data class Project(val name: String, val stars: Int)

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    // Opens a Source for the projects.json file containing multiple JSON objects
    val path = Path("projects.json")
    SystemFileSystem.source(path).buffered().use { source: Source ->

        // Lazily deserializes each Project as it is read from the Source
        val projects: Sequence<Project> = Json.decodeSourceToSequence(source)

        for (project in projects) {
            println(project)
        }
    }
}
```

> You can iterate through sequences returned by `.decodeSourceToSequence()` only once
> because they are tied to the underlying `Source`.
> 
{style="note"}
