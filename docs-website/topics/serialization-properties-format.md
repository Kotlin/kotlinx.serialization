[//]: # (title: Properties format)

You can use the [`Properties`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-properties/kotlinx.serialization.properties/-properties/) format to serialize a class into a flat map with `String` keys.

## Add dependencies for `Properties`

To use the `Properties` format in your project, add the `kotlinx-serialization-properties` serialization library dependency to your build file:

<tabs>

<tab id="gradle-properties" title="Gradle">

```kotlin
// build.gradle(.kts)

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-properties:%serializationVersion%")
}
```

</tab>

<tab id="maven-properties" title="Maven">

```xml
<!-- pom.xml -->

<dependencies>
    <dependency>
        <groupId>org.jetbrains.kotlinx</groupId>
        <artifactId>kotlinx-serialization-properties</artifactId>
        <version>%serializationVersion%</version>
    </dependency>
</dependencies>
```

</tab>
</tabs>

## Serialize objects to flat maps with `Properties`

Use the [`.encodeToMap()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-properties/kotlinx.serialization.properties/encode-to-map.html) and [`.decodeFromMap()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-properties/kotlinx.serialization.properties/decode-from-map.html) extension functions encode objects into flat maps and decode the maps back into objects.

Here's an example with dot-separated keys for nested properties:

```kotlin
import kotlinx.serialization.*
import kotlinx.serialization.properties.*

@Serializable
class Project(val name: String, val owner: User)

@Serializable
class User(val name: String)

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val data = Project("kotlinx.serialization", User("kotlin"))
    // Encodes the object into a flat map
    val map = Properties.encodeToMap(data)

    // Iterates through the map and prints the key-value pairs
    map.forEach { (k, v) -> println("$k = $v") }
    // name = kotlinx.serialization
    // owner.name = kotlin

    // Decodes the flat map back into an object
    val decodedData = Properties.decodeFromMap<Project>(map)
 
    println(decodedData.name)
    // kotlinx.serialization
    println(decodedData.owner.name)
    // kotlin
}
```