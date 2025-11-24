[//]: # (title: Serialize JSON with I/O sources)
<primary-label ref="experimental-general"/>

The Kotlin serialization library provides APIs for working with JVM streams and Okio's buffered sources and sinks.
You can use these APIs to serialize and deserialize JSON directly from I/O sources without creating intermediate strings.

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
    // Opens an InputStream for the "projects.json" file containing a JSON array of Project objects
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

## Serialize JSON with Okio

[Intro about Okio]
Introduce main use case and functions. (link to [Okio](https://square.github.io/okio/))

[Example]