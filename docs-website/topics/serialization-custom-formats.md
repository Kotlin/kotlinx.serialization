[//]: # (title: Custom serialization formats)
<primary-label ref="experimental-general"/>

JSON is currently the only stable format in Kotlin serialization.

Kotlin serialization also provides experimental support for serializing values to flat maps with the [`Properties`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-properties/kotlinx.serialization.properties/-properties/) class and [creating custom formats](#create-custom-formats).
You can use these options when JSON isn't the right fit for your use case.

For other experimental binary formats, see [CBOR format](serialization-cbor.md) and [ProtoBuf format](serialization-protobuf.md).

## Properties

Kotlin serialization can serialize a class into a flat map with `String` keys using
the [`Properties`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-properties/kotlinx.serialization.properties/-properties/) format implementation.

To use the `Properties` format in your project, add the properties serialization library dependency to your build file:

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

Here's an example that encodes a class into a flat map with dot-separated keys for nested properties:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.properties.Properties
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
}
```

## Create custom formats

To create a custom format in Kotlin serialization,
implement the [`Encoder`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-encoder/) and [`Decoder`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-decoder/) interfaces.

Serializers use these implementations to encode and decode values.
For structured values, such as classes and collections, serializers call the [`beginStructure()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-encoder/begin-structure.html) function.
This function returns a [`CompositeEncoder`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-composite-encoder/) or [`CompositeDecoder`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-composite-decoder/) interface with functions that encode or decode each property or element.

The `Encoder` and `Decoder` interfaces are extensive, but you can use the [`AbstractEncoder`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-abstract-encoder/) and [`AbstractDecoder`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-abstract-decoder/) classes to simplify the process of creating custom encoder and decoder implementations.
These classes implement the `Encoder`, `Decoder`, `CompositeEncoder`, and `CompositeDecoder` interfaces, so you can use the same class to handle primitive values and structured values in simple formats.

The `AbstractEncoder` class provides default implementations for most of the encode functions, such as [`encodeString()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-abstract-encoder/encode-string.html),
which delegate to the [`encodeValue(value: Any)`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-abstract-encoder/encode-value.html) function.
This means that by overriding the `encodeValue()` function, you can create a basic, functional custom format with minimal effort.

The following sections use these APIs to create a custom format that encodes values into a list, decodes values from that list, and then adds sequential decoding, collection support, and null support.

### Create a basic encoder

To build a custom `Encoder` in Kotlin serialization, you can extend the `AbstractEncoder` class.
This allows you to control how each value is serialized during the encoding process.

To create a basic encoder:

1. Create a class that extends `AbstractEncoder` to define custom serialization logic:

    ```kotlin
    @ExperimentalSerializationApi
    class ListEncoder : AbstractEncoder() {
        val list = mutableListOf<Any>()
    
        override val serializersModule: SerializersModule = EmptySerializersModule()
    }
    ```

2. Override `encodeValue()` to define how the encoder handles each value.

    ```kotlin
        override fun encodeValue(value: Any) {
            list.add(value)
        }
    ```

3. Create a function that uses the encoder to serialize a value:

    ```kotlin
    @ExperimentalSerializationApi
    fun <T> encodeToList(serializer: SerializationStrategy<T>, value: T): List<Any> {
        val encoder = ListEncoder()
        encoder.encodeSerializableValue(serializer, value)
        return encoder.list
    }
    ```

4. Add an inline overload that uses `serializer()` with a reified type parameter, so callers don't need to pass the serializer explicitly:

    ```kotlin
    @ExperimentalSerializationApi
    inline fun <reified T> encodeToList(value: T) = encodeToList(serializer(), value)
    ```

> Since encoders are usually used by other parts of an application,
> propagate the `@ExperimentalSerializationApi` annotation instead of opting in only inside individual functions.
>
{style="note"}

Here's a minimal example that encodes the primitive values from an object graph into a flat list in serialization order:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*

// Creates a custom encoder that stores serialized values in a list
@ExperimentalSerializationApi
class ListEncoder : AbstractEncoder() {
    val list = mutableListOf<Any>()

    override val serializersModule: SerializersModule = EmptySerializersModule()

    // Stores each encoded value
    override fun encodeValue(value: Any) {
        list.add(value)
    }
}

// Encodes a value and returns the collected values as a list
@ExperimentalSerializationApi
fun <T> encodeToList(serializer: SerializationStrategy<T>, value: T): List<Any> {
    val encoder = ListEncoder()
    encoder.encodeSerializableValue(serializer, value)
    return encoder.list
}

// Provides a type-safe inline overload for convenience
@ExperimentalSerializationApi
inline fun <reified T> encodeToList(value: T) = encodeToList(serializer(), value)

@Serializable
data class Project(val name: String, val owner: User, val votes: Int)

@Serializable
data class User(val name: String)

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val data = Project("kotlinx.serialization", User("kotlin"), 9000)
    println(encodeToList(data))
    // [kotlinx.serialization, kotlin, 9000]
}
```
{kotlin-runnable="true"}

The output shows that the encoder collects all serialized values into a flat list in serialization order.
This can be useful when you need to process those values uniformly, for example to compute a hash or digest.

### Create a basic decoder

To build a custom `Decoder` in Kotlin serialization, you can extend the `AbstractDecoder` class.
This allows you to control how each value is deserialized during the decoding process.

To create a basic decoder:

1. Create a class that extends `AbstractDecoder` to define custom deserialization logic:

    ```kotlin
    @ExperimentalSerializationApi
    class ListDecoder(val list: ArrayDeque<Any>) : AbstractDecoder() {
        private var elementIndex = 0
    
        override val serializersModule: SerializersModule = EmptySerializersModule()
    }
    ```

2. Override the [`decodeValue()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-abstract-decoder/decode-value.html) function to define how the decoder deserializes values:

    ```kotlin
        override fun decodeValue(): Any = list.removeFirst()
    ```

3. Override the [`decodeElementIndex()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-composite-decoder/decode-element-index.html) function to report which element is decoded next and return `DECODE_DONE` when the structure is complete:

    ```kotlin
        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            if (elementIndex == descriptor.elementsCount) return CompositeDecoder.DECODE_DONE
            return elementIndex++
        }
    ```

   This format deserializes values in serialization order, so you can use a simple `elementIndex` counter to track progress.
   More complex formats often need additional logic to find the next decoded element.

   > To see how custom serializers use the element index from the `decodeElementIndex()` function, see [Create a custom composite serializer](serialization-create-and-use-serializers.md#create-a-custom-composite-serializer).
   >
   {style="tip"}

4. Override the [`beginStructure()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-abstract-decoder/begin-structure.html) function to create a new decoder for each nested structure, so each recursively decoded structure keeps its own `elementIndex` state:

    ```kotlin
        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
            ListDecoder(list)
    ```

5. Create a function that uses the decoder to deserialize a value:

    ```kotlin
    @ExperimentalSerializationApi
    fun <T> decodeFromList(list: List<Any>, deserializer: DeserializationStrategy<T>): T {
        val decoder = ListDecoder(ArrayDeque(list))
        return decoder.decodeSerializableValue(deserializer)
    }
    ```

6. Add an inline overload to make deserialization easier to call:

    ```kotlin
    @ExperimentalSerializationApi
    inline fun <reified T> decodeFromList(list: List<Any>): T = decodeFromList(list, serializer())
    ```

Here's a complete example that decodes a list of primitive values back into an object:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*

// Creates a custom encoder that stores serialized values in a list
@ExperimentalSerializationApi
class ListEncoder : AbstractEncoder() {
    val list = mutableListOf<Any>()

    override val serializersModule: SerializersModule = EmptySerializersModule()

    // Stores each encoded value
    override fun encodeValue(value: Any) {
       list.add(value)
    }
}

// Encodes a value and returns the collected values as a list
@ExperimentalSerializationApi
fun <T> encodeToList(serializer: SerializationStrategy<T>, value: T): List<Any> {
    val encoder = ListEncoder()
    encoder.encodeSerializableValue(serializer, value)
    return encoder.list
}

// Provides a type-safe, inline version of encodeToList for convenience
@ExperimentalSerializationApi
inline fun <reified T> encodeToList(value: T) = encodeToList(serializer(), value)

//sampleStart
// Creates a custom decoder that decodes serialized values from a list
@ExperimentalSerializationApi
class ListDecoder(val list: ArrayDeque<Any>) : AbstractDecoder() {
    private var elementIndex = 0

    override val serializersModule: SerializersModule = EmptySerializersModule()

    // Returns the next value from the list
    override fun decodeValue(): Any = list.removeFirst()
    
    // Reports the next element index or signals that decoding is complete
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (elementIndex == descriptor.elementsCount) return CompositeDecoder.DECODE_DONE
        return elementIndex++
    }

    // Creates a new decoder for each nested structure
    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
        ListDecoder(list)
}

// Deserializes an object from a list using the custom decoder
@ExperimentalSerializationApi
fun <T> decodeFromList(list: List<Any>, deserializer: DeserializationStrategy<T>): T {
    val decoder = ListDecoder(ArrayDeque(list))
    return decoder.decodeSerializableValue(deserializer)
}

// Provides a type-safe, inline version of decodeFromList for convenience
@ExperimentalSerializationApi
inline fun <reified T> decodeFromList(list: List<Any>): T = decodeFromList(list, serializer())

@Serializable
data class Project(val name: String, val owner: User, val votes: Int)

@Serializable
data class User(val name: String)

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val data = Project("kotlinx.serialization", User("kotlin"), 9000)
    val list = encodeToList(data)
    println(list)
    // [kotlinx.serialization, kotlin, 9000]

    val obj = decodeFromList<Project>(list)
    println(obj)
    // Project(name=kotlinx.serialization, owner=User(name=kotlin), votes=9000)
}
//sampleEnd
```
{kotlin-runnable="true"}

The output shows that the decoder reads the list back in serialization order and reconstructs the original object.

The `decodeElementIndex()` function reports which property comes next, and the `beginStructure()` function creates a new decoder state for nested objects.

### Optimize with sequential decoding

If your format always stores values in declaration order and doesn't support [skipping optional elements](serialization-create-and-use-serializers.md#encode-default-values-in-custom-serializers), you can optimize decoding with the [`decodeSequentially()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-composite-decoder/decode-sequentially.html) function.

When this function returns `true`, serializers that support sequential decoding can read values in order without repeatedly requesting the next element index.
This can improve performance when deserializing data stored in sequential formats. Serializers generated by the `kotlinx.serialization` plugin use this optimization.

> Returning `true` from the `decodeSequentially()` function doesn't guarantee that serializers using your decoder will use sequential decoding.
> Therefore, make sure your decoder also supports regular decoding with the `decodeElementIndex()` function.
>
{style="note"}

> To see how a serializer uses the `decodeSequentially()` function during deserialization, see [Optimize deserialization with sequential decoding](serialization-create-and-use-serializers.md#optimize-deserialization-with-sequential-decoding).
>
{style="tip"}

Here's how to apply this optimization to the custom `ListDecoder`:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*

@ExperimentalSerializationApi
class ListEncoder : AbstractEncoder() {
    val list = mutableListOf<Any>()

    override val serializersModule: SerializersModule = EmptySerializersModule()

    override fun encodeValue(value: Any) {
        list.add(value)
    }
}

@ExperimentalSerializationApi
fun <T> encodeToList(serializer: SerializationStrategy<T>, value: T): List<Any> {
    val encoder = ListEncoder()
    encoder.encodeSerializableValue(serializer, value)
    return encoder.list
}

@ExperimentalSerializationApi
inline fun <reified T> encodeToList(value: T) = encodeToList(serializer(), value)

//sampleStart
@ExperimentalSerializationApi
class ListDecoder(val list: ArrayDeque<Any>) : AbstractDecoder() {
    private var elementIndex = 0

    override val serializersModule: SerializersModule = EmptySerializersModule()

    override fun decodeValue(): Any = list.removeFirst()

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (elementIndex == descriptor.elementsCount) return CompositeDecoder.DECODE_DONE
        return elementIndex++
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
        ListDecoder(list)

    // Enables sequential decoding for serializers that support it
    override fun decodeSequentially(): Boolean = true
}

//sampleEnd
@ExperimentalSerializationApi
fun <T> decodeFromList(list: List<Any>, deserializer: DeserializationStrategy<T>): T {
    val decoder = ListDecoder(ArrayDeque(list))
    return decoder.decodeSerializableValue(deserializer)
}

@ExperimentalSerializationApi
inline fun <reified T> decodeFromList(list: List<Any>): T = decodeFromList(list, serializer())

@Serializable
data class Project(val name: String, val owner: User, val votes: Int)

@Serializable
data class User(val name: String)

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val data = Project("kotlinx.serialization", User("kotlin"), 9000)
    val list = encodeToList(data)
    println(list)
    // [kotlinx.serialization, kotlin, 9000]

    val obj = decodeFromList<Project>(list)
    println(obj)
    // Project(name=kotlinx.serialization, owner=User(name=kotlin), votes=9000)
}
```
{kotlin-runnable="true"}

In this example, `ListDecoder` overrides the `decodeSequentially()` function to return `true`.
This lets supported serializers read values directly in declaration order instead of querying each element index one by one.

### Add collection support

A basic custom format can encode collection elements one by one, but decoding also needs to know how many elements belong to the collection.

To support collections in a custom format, encode the collection size along with its elements so the decoder knows where the collection ends:

1. Implement the [`beginCollection()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-encoder/begin-collection.html) function in the encoder to handle the collection size.
2. Return the encoder instance from the `beginCollection()` function if the encoder doesn't need extra collection-specific state.
3. Implement the [`decodeCollectionSize()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-composite-decoder/decode-collection-size.html) function in the decoder to decode and store the collection size during deserialization.
4. Return `true` from the `decodeSequentially()` function if the format stores collection size in advance and values are read in order.

Here's a complete example that adds collection support to `ListEncoder`:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*

@ExperimentalSerializationApi
fun <T> encodeToList(serializer: SerializationStrategy<T>, value: T): List<Any> {
   val encoder = ListEncoder()
   encoder.encodeSerializableValue(serializer, value)
   return encoder.list
}

@ExperimentalSerializationApi
inline fun <reified T> encodeToList(value: T) = encodeToList(serializer(), value)

@ExperimentalSerializationApi
fun <T> decodeFromList(list: List<Any>, deserializer: DeserializationStrategy<T>): T {
   val decoder = ListDecoder(ArrayDeque(list))
   return decoder.decodeSerializableValue(deserializer)
}

@ExperimentalSerializationApi
inline fun <reified T> decodeFromList(list: List<Any>): T = decodeFromList(list, serializer())

@Serializable
data class Project(val name: String, val owners: List<User>, val votes: Int)

@Serializable
data class User(val name: String)

//sampleStart
@ExperimentalSerializationApi
class ListEncoder : AbstractEncoder() {
    val list = mutableListOf<Any>()

    override val serializersModule: SerializersModule = EmptySerializersModule()

    override fun encodeValue(value: Any) {
        list.add(value)
    }

    // Stores the collection size before its elements
    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        encodeInt(collectionSize)
        // Returns the same encoder because no additional collection-specific state is required
        return this
    }
}

@ExperimentalSerializationApi
class ListDecoder(val list: ArrayDeque<Any>, var elementsCount: Int = 0) : AbstractDecoder() {
    private var elementIndex = 0

    override val serializersModule: SerializersModule = EmptySerializersModule()

    override fun decodeValue(): Any = list.removeFirst()

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (elementIndex == elementsCount) return CompositeDecoder.DECODE_DONE
        return elementIndex++
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
        ListDecoder(list, descriptor.elementsCount)

    // Signals that values are decoded in order because the collection size is known in advance
    override fun decodeSequentially(): Boolean = true

    // Decodes and stores the collection size before reading the collection elements
    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int =
        decodeInt().also { elementsCount = it }
}

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val data = Project("kotlinx.serialization", listOf(User("kotlin"), User("jetbrains")), 9000)
    val list = encodeToList(data)
    println(list)
    // [kotlinx.serialization, 2, kotlin, jetbrains, 9000]

    val obj = decodeFromList<Project>(list)
    println(obj)
    // Project(name=kotlinx.serialization, owners=[User(name=kotlin), User(name=jetbrains)], votes=9000)
}
//sampleEnd
```
{kotlin-runnable="true"}

In this example, the encoded list includes the collection size before the collection elements, so the decoder can correctly decode collections.

### Add null support

To support null values in a custom format, you need a way to distinguish `null` from a regular value.

This typically involves adding a "null indicator" before each nullable value that distinguishes between `null` values and actual data.

To add support for null values in a custom format:

1. Override [`encodeNull()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-abstract-encoder/encode-null.html) in the encoder to specify how `null` values are marked.
2. Override [`encodeNotNullMark()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-encoder/encode-not-null-mark.html) in the encoder to mark non-null values.
3. Override [`decodeNotNullMark()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-decoder/decode-not-null-mark.html) in the decoder to check that marker before decoding the value.

Here's an example that adds `null` support to the `ListEncoder` and `ListDecoder` implementations:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*

@ExperimentalSerializationApi
fun <T> encodeToList(serializer: SerializationStrategy<T>, value: T): List<Any> {
   val encoder = ListEncoder()
   encoder.encodeSerializableValue(serializer, value)
   return encoder.list
}

@ExperimentalSerializationApi
inline fun <reified T> encodeToList(value: T) = encodeToList(serializer(), value)

@ExperimentalSerializationApi
fun <T> decodeFromList(list: List<Any>, deserializer: DeserializationStrategy<T>): T {
   val decoder = ListDecoder(ArrayDeque(list))
   return decoder.decodeSerializableValue(deserializer)
}

@ExperimentalSerializationApi
inline fun <reified T> decodeFromList(list: List<Any>): T = decodeFromList(list, serializer())

@Serializable
data class Project(val name: String, val owner: User?, val votes: Int?)

@Serializable
data class User(val name: String)

//sampleStart
@ExperimentalSerializationApi
class ListEncoder : AbstractEncoder() {
    val list = mutableListOf<Any>()

    override val serializersModule: SerializersModule = EmptySerializersModule()

    override fun encodeValue(value: Any) {
        list.add(value)
    }

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        encodeInt(collectionSize)
        return this
    }

    // Represents null values as NULL
    override fun encodeNull() = encodeValue("NULL")

    // Represents non-null values with "!!"
    override fun encodeNotNullMark() = encodeValue("!!")
}

@ExperimentalSerializationApi
class ListDecoder(val list: ArrayDeque<Any>, var elementsCount: Int = 0) : AbstractDecoder() {
    private var elementIndex = 0

    override val serializersModule: SerializersModule = EmptySerializersModule()

    override fun decodeValue(): Any = list.removeFirst()

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (elementIndex == elementsCount) return CompositeDecoder.DECODE_DONE
        return elementIndex++
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
        ListDecoder(list, descriptor.elementsCount)

    override fun decodeSequentially(): Boolean = true

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int =
        decodeInt().also { elementsCount = it }

    // Checks if the next value is "NULL" to determine whether the next nullable value is null
    override fun decodeNotNullMark(): Boolean = decodeString() != "NULL"
}

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    val data = Project("kotlinx.serialization", User("kotlin"), null)
    val list = encodeToList(data)
    println(list)
    // [kotlinx.serialization, !!, kotlin, NULL]

    val obj = decodeFromList<Project>(list)
    println(obj)
    // Project(name=kotlinx.serialization, owner=User(name=kotlin), votes=null)
}
//sampleEnd
```
{kotlin-runnable="true"}

In this example, the encoder writes `!!` before a non-null value and writes `NULL` for a `null` value.
The decoder checks these markers to decide whether to decode a value or return `null`.

### Create a compact binary format

Binary formats are often used for their compact representation of data, making them ideal for scenarios where minimizing storage size or transmission bandwidth is important.

Custom binary formats allow you to control how data is serialized and deserialized at a low level,
providing flexibility to optimize performance and compatibility with other systems.

You can create a custom binary format with Kotlin serialization by implementing the
[`java.io.DataOutput`](https://docs.oracle.com/javase/8/docs/api/java/io/DataOutput.html) and the [`java.io.DataInput`](https://docs.oracle.com/javase/8/docs/api/java/io/DataInput.html) interfaces.

Let's look at an example of how to turn the `ListEncoder` and `ListDecoder` implementations into a compact binary format using `DataOutput` and `DataInput`:

1. Override the encode functions for each primitive type, such as `encodeInt()` for integers or `encodeString()` for strings.
   These type-specific encode functions [avoid boxing](numbers.md#boxing-and-caching-numbers-on-the-jvm) and let you define the binary representation for each [primitive type](serialization-serialize-builtin-types.md#basic-types).
   In this example, the values are encoded directly to the `DataOutput` stream:

    ```kotlin
        override fun encodeBoolean(value: Boolean) = output.writeByte(if (value) 1 else 0)
        override fun encodeByte(value: Byte) = output.writeByte(value.toInt())
        override fun encodeShort(value: Short) = output.writeShort(value.toInt())
        override fun encodeInt(value: Int) = output.writeInt(value)
        override fun encodeLong(value: Long) = output.writeLong(value)
        override fun encodeFloat(value: Float) = output.writeFloat(value)
        override fun encodeDouble(value: Double) = output.writeDouble(value)
        override fun encodeChar(value: Char) = output.writeChar(value.code)
        override fun encodeString(value: String) = output.writeUTF(value)
        override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) = output.writeInt(index)
    ```

2. Implement the decode functions for each primitive type, such as `decodeInt()` or `decodeString()`.
   This lets the decoder decode values directly from the `DataInput` stream and reconstruct the original data structure:

    ```kotlin
        override fun decodeBoolean(): Boolean = input.readByte().toInt() != 0
        override fun decodeByte(): Byte = input.readByte()
        override fun decodeShort(): Short = input.readShort()
        override fun decodeInt(): Int = input.readInt()
        override fun decodeLong(): Long = input.readLong()
        override fun decodeFloat(): Float = input.readFloat()
        override fun decodeDouble(): Double = input.readDouble()
        override fun decodeChar(): Char = input.readChar()
        override fun decodeString(): String = input.readUTF()
        override fun decodeEnum(enumDescriptor: SerialDescriptor): Int = input.readInt()
    ```

3. Use these classes to serialize and deserialize Kotlin objects in a binary format:

    ```kotlin
    // Imports declarations from the serialization library
    import kotlinx.serialization.*
    import kotlinx.serialization.Serializable
    import kotlinx.serialization.descriptors.*
    import kotlinx.serialization.encoding.*
    import kotlinx.serialization.modules.*
    import java.io.*

    @ExperimentalSerializationApi
    class DataOutputEncoder(val output: DataOutput) : AbstractEncoder() {
        override val serializersModule: SerializersModule = EmptySerializersModule()

        // Encodes primitive values directly in binary form
        override fun encodeBoolean(value: Boolean) = output.writeByte(if (value) 1 else 0)
        override fun encodeByte(value: Byte) = output.writeByte(value.toInt())
        override fun encodeShort(value: Short) = output.writeShort(value.toInt())
        override fun encodeInt(value: Int) = output.writeInt(value)
        override fun encodeLong(value: Long) = output.writeLong(value)
        override fun encodeFloat(value: Float) = output.writeFloat(value)
        override fun encodeDouble(value: Double) = output.writeDouble(value)
        override fun encodeChar(value: Char) = output.writeChar(value.code)
        override fun encodeString(value: String) = output.writeUTF(value)
        override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) = output.writeInt(index)

        override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
            encodeInt(collectionSize)
            return this
        }

        override fun encodeNull() = encodeBoolean(false)
        override fun encodeNotNullMark() = encodeBoolean(true)
    }
   
    @ExperimentalSerializationApi
    fun <T> encodeTo(output: DataOutput, serializer: SerializationStrategy<T>, value: T) {
        val encoder = DataOutputEncoder(output)
        encoder.encodeSerializableValue(serializer, value)
    }

    @ExperimentalSerializationApi
    inline fun <reified T> encodeTo(output: DataOutput, value: T) = encodeTo(output, serializer(), value)

    @ExperimentalSerializationApi
    class DataInputDecoder(val input: DataInput, var elementsCount: Int = 0) : AbstractDecoder() {
        private var elementIndex = 0
        override val serializersModule: SerializersModule = EmptySerializersModule()

        // Decodes primitive values directly from binary form
        override fun decodeBoolean(): Boolean = input.readByte().toInt() != 0
        override fun decodeByte(): Byte = input.readByte()
        override fun decodeShort(): Short = input.readShort()
        override fun decodeInt(): Int = input.readInt()
        override fun decodeLong(): Long = input.readLong()
        override fun decodeFloat(): Float = input.readFloat()
        override fun decodeDouble(): Double = input.readDouble()
        override fun decodeChar(): Char = input.readChar()
        override fun decodeString(): String = input.readUTF()
        override fun decodeEnum(enumDescriptor: SerialDescriptor): Int = input.readInt()

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            if (elementIndex == elementsCount) return CompositeDecoder.DECODE_DONE
            return elementIndex++
        }

        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
            DataInputDecoder(input, descriptor.elementsCount)
 
        override fun decodeSequentially(): Boolean = true
  
        override fun decodeCollectionSize(descriptor: SerialDescriptor): Int =
            decodeInt().also { elementsCount = it }
 
        override fun decodeNotNullMark(): Boolean = decodeBoolean()
    }

    @ExperimentalSerializationApi
    fun <T> decodeFrom(input: DataInput, deserializer: DeserializationStrategy<T>): T {
        val decoder = DataInputDecoder(input)
        return decoder.decodeSerializableValue(deserializer)
    }

    @ExperimentalSerializationApi
    inline fun <reified T> decodeFrom(input: DataInput): T = decodeFrom(input, serializer())

    fun ByteArray.toAsciiHexString() = joinToString("") {
        if (it in 32..127) it.toInt().toChar().toString() else
            "{${it.toUByte().toString(16).padStart(2, '0').uppercase()}}"
    }
    //sampleStart
    @Serializable
    data class Project(val name: String, val language: String)

    @OptIn(ExperimentalSerializationApi::class)
    fun main() {
        val data = Project("kotlinx.serialization", "Kotlin")
        // Encodes the object to the custom binary format
        val output = ByteArrayOutputStream()
        encodeTo(DataOutputStream(output), data)

        val bytes = output.toByteArray()
        println(bytes.toAsciiHexString())
        // {00}{15}kotlinx.serialization{00}{06}Kotlin

        // Decodes the object from the custom binary format
        val input = ByteArrayInputStream(bytes)
        val obj = decodeFrom<Project>(DataInputStream(input))
        println(obj)
        // Project(name=kotlinx.serialization, language=Kotlin)
    }
    //sampleEnd
    ```
    {kotlin-runnable="true"}

In this example, the custom format encodes only the serialized values without keys or property names in binary form and decodes them back into a `Project` object.
This makes it easier to adapt the format for cases where you need a compact representation and precise control over the binary encoding.

### Add support for format-specific types

A custom format can provide support for types that don't map directly to the standard primitive encoding functions.

To add this support, override the `encodeSerializableValue()` function in the encoder and the `decodeSerializableValue()` function in the decoder.
This lets you [define a custom serialization](create-custom-serializers.md) logic for format-specific types,
while maintaining efficient handling and flexibility for non-standard data representations.

To detect a type correctly, compare the `serializer.descriptor` property with the descriptor of the serializer for that type instead of checking the runtime type of the value.
This preserves the declared serialized form, even when a type uses a custom serializer or shares the same underlying representation as another type.

Let's look at an example of how to extend the [compact binary format example](#create-a-compact-binary-format) with specialized support for the type `ByteArray`:

1. Obtain a serializer for the format-specific type, so the encoder can detect it by descriptor:

    ```kotlin
    private val byteArraySerializer = serializer<ByteArray>()
    ```   

    > You can also use the built-in [`ByteArraySerializer()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.builtins/-byte-array-serializer.html) function for a similar result.
    >
    {style="tip"}

2. Override the `encodeSerializableValue()` function in the encoder to detect `ByteArray` values by descriptor and use a specialized encoding path.
   Then define helper functions to encode the byte array:

   ```kotlin
   override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
       if (serializer.descriptor == byteArraySerializer.descriptor)
           encodeByteArray(value as ByteArray)
       else
           super.encodeSerializableValue(serializer, value)
   }

   private fun encodeByteArray(bytes: ByteArray) {
       encodeCompactSize(bytes.size)
       output.write(bytes)
   }

   // Encodes sizes up to 254 in a single byte
   private fun encodeCompactSize(value: Int) {
       if (value < 0xff) {
           output.writeByte(value)
       } else {
           output.writeByte(0xff)
           output.writeInt(value)
       }
   }
   ```

3. Override the [`decodeSerializableValue()`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-decoder/decode-serializable-value.html) function in the decoder to detect `ByteArray` values by descriptor and deserialize them with the matching compact size format:

   ```kotlin
       @Suppress("UNCHECKED_CAST")
       override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>, previousValue: T?): T =
           if (deserializer.descriptor == byteArraySerializer.descriptor)
               decodeByteArray() as T
           else
               super.decodeSerializableValue(deserializer, previousValue)
   
       // Decodes ByteArray data
       private fun decodeByteArray(): ByteArray {
           val bytes = ByteArray(decodeCompactSize())
           input.readFully(bytes)
           return bytes
       }
   
       // Decodes size efficiently using a compact format
       private fun decodeCompactSize(): Int {
           val byte = input.readByte().toInt() and 0xff
           if (byte < 0xff) return byte
           return input.readInt()
       }
   ```
   {collapsible="true" default-state="expanded" collapsed-title="override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>, previousValue: T?)"}

4. Serialize and deserialize objects with the embedded `ByteArray` data:

   ```kotlin
   @Serializable
   data class Project(val name: String, val attachment: ByteArray)
   
   @OptIn(ExperimentalSerializationApi::class)
   fun main() {
       val data = Project("kotlinx.serialization", byteArrayOf(0x0A, 0x0B, 0x0C, 0x0D))
       val output = ByteArrayOutputStream()
       encodeTo(DataOutputStream(output), data)
       val bytes = output.toByteArray()
       println(bytes.toAsciiHexString())
       // {00}{15}kotlinx.serialization{04}{0A}{0B}{0C}{0D}

       val input = ByteArrayInputStream(bytes)
       val obj = decodeFrom<Project>(DataInputStream(input))
       println(obj)
       // Project(name=kotlinx.serialization, attachment=[10, 11, 12, 13])
   }
   ```
   {collapsible="true" default-state="expanded" collapsed-title="data class Project(val name: String, val attachment: ByteArray)"}

Here's the complete example that serializes and deserializes a class with a `ByteArray` property using the specialized encoding and decoding path:

```kotlin
// Imports declarations from the serialization library
import kotlinx.serialization.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.modules.*
import kotlinx.serialization.encoding.*
import java.io.*

// Declares the serializer used to detect ByteArray by descriptor
private val byteArraySerializer = serializer<ByteArray>()

@ExperimentalSerializationApi
class DataOutputEncoder(val output: DataOutput) : AbstractEncoder() {
    override val serializersModule: SerializersModule = EmptySerializersModule()

    // Encodes the primitive types
    override fun encodeBoolean(value: Boolean) = output.writeByte(if (value) 1 else 0)
    override fun encodeByte(value: Byte) = output.writeByte(value.toInt())
    override fun encodeShort(value: Short) = output.writeShort(value.toInt())
    override fun encodeInt(value: Int) = output.writeInt(value)
    override fun encodeLong(value: Long) = output.writeLong(value)
    override fun encodeFloat(value: Float) = output.writeFloat(value)
    override fun encodeDouble(value: Double) = output.writeDouble(value)
    override fun encodeChar(value: Char) = output.writeChar(value.code)
    override fun encodeString(value: String) = output.writeUTF(value)
    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) = output.writeInt(index)

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        encodeInt(collectionSize)
        return this
    }

    override fun encodeNull() = encodeBoolean(false)
    override fun encodeNotNullMark() = encodeBoolean(true)

    // Detects ByteArray by descriptor and uses a specialized encoding path
    override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
        if (serializer.descriptor == byteArraySerializer.descriptor)
            encodeByteArray(value as ByteArray)
        else
            super.encodeSerializableValue(serializer, value)
    }

    // Encodes a ByteArray with a compact size representation
    private fun encodeByteArray(bytes: ByteArray) {
        encodeCompactSize(bytes.size)
        output.write(bytes)
    }

    // Encodes sizes up to 254 in a single byte
    private fun encodeCompactSize(value: Int) {
        if (value < 0xff) {
            output.writeByte(value)
        } else {
            output.writeByte(0xff)
            output.writeInt(value)
        }
    }
}

@ExperimentalSerializationApi
fun <T> encodeTo(output: DataOutput, serializer: SerializationStrategy<T>, value: T) {
    val encoder = DataOutputEncoder(output)
    encoder.encodeSerializableValue(serializer, value)
}

@ExperimentalSerializationApi
inline fun <reified T> encodeTo(output: DataOutput, value: T) = encodeTo(output, serializer(), value)

@ExperimentalSerializationApi
class DataInputDecoder(val input: DataInput, var elementsCount: Int = 0) : AbstractDecoder() {
    private var elementIndex = 0
    override val serializersModule: SerializersModule = EmptySerializersModule()
    // Decodes the primitive types
    override fun decodeBoolean(): Boolean = input.readByte().toInt() != 0
    override fun decodeByte(): Byte = input.readByte()
    override fun decodeShort(): Short = input.readShort()
    override fun decodeInt(): Int = input.readInt()
    override fun decodeLong(): Long = input.readLong()
    override fun decodeFloat(): Float = input.readFloat()
    override fun decodeDouble(): Double = input.readDouble()
    override fun decodeChar(): Char = input.readChar()
    override fun decodeString(): String = input.readUTF()
    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int = input.readInt()

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (elementIndex == elementsCount) return CompositeDecoder.DECODE_DONE
        return elementIndex++
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
        DataInputDecoder(input, descriptor.elementsCount)

    override fun decodeSequentially(): Boolean = true

    override fun decodeCollectionSize(descriptor: SerialDescriptor): Int =
        decodeInt().also { elementsCount = it }

    override fun decodeNotNullMark(): Boolean = decodeBoolean()

    // Detects ByteArray by descriptor and uses a specialized decoding path
    @Suppress("UNCHECKED_CAST")
    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>, previousValue: T?): T =
        if (deserializer.descriptor == byteArraySerializer.descriptor)
            decodeByteArray() as T
        else
            super.decodeSerializableValue(deserializer, previousValue)

    // Decodes the array size and then the byte array contents
    private fun decodeByteArray(): ByteArray {
        val bytes = ByteArray(decodeCompactSize())
        input.readFully(bytes)
        return bytes
    }

    // Decodes sizes up to 254 from a single byte
    private fun decodeCompactSize(): Int {
        val byte = input.readByte().toInt() and 0xff
        if (byte < 0xff) return byte
        return input.readInt()
    }
}

@ExperimentalSerializationApi
fun <T> decodeFrom(input: DataInput, deserializer: DeserializationStrategy<T>): T {
    val decoder = DataInputDecoder(input)
    return decoder.decodeSerializableValue(deserializer)
}

@ExperimentalSerializationApi
inline fun <reified T> decodeFrom(input: DataInput): T = decodeFrom(input, serializer())

fun ByteArray.toAsciiHexString() = joinToString("") {
    if (it in 32..127) it.toInt().toChar().toString() else
        "{${it.toUByte().toString(16).padStart(2, '0').uppercase()}}"
}

@Serializable
data class Project(val name: String, val attachment: ByteArray)

@OptIn(ExperimentalSerializationApi::class)

//sampleStart
fun main() {
    val data = Project("kotlinx.serialization", byteArrayOf(0x0A, 0x0B, 0x0C, 0x0D))
    val output = ByteArrayOutputStream()
    encodeTo(DataOutputStream(output), data)
    val bytes = output.toByteArray()
    println(bytes.toAsciiHexString())
    // {00}{15}kotlinx.serialization{04}{0A}{0B}{0C}{0D}

    val input = ByteArrayInputStream(bytes)
    val obj = decodeFrom<Project>(DataInputStream(input))
    println(obj)
    // Project(name=kotlinx.serialization, attachment=[10, 11, 12, 13])
}
//sampleEnd
```
{kotlin-runnable="true"}

### Define a format-specific `@Serializable` annotation

You can use the [`@MetaSerializable`](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-meta-serializable/) annotation to define a format-specific `@Serializable` annotation.
Add the format-specific annotation to a class instead of `@Serializable` to make the class serializable and include the annotation data in the generated serial descriptor.

Here's an example that defines a `@BinarySerializable` annotation:

```kotlin
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MetaSerializable

//sampleStart
@OptIn(ExperimentalSerializationApi::class)
// Defines a format-specific annotation that makes annotated classes serializable
@MetaSerializable
@Target(AnnotationTarget.CLASS)
annotation class BinarySerializable(val typeId: Int)

@BinarySerializable(typeId = 1)
data class Project(val name: String, val language: String)

@OptIn(ExperimentalSerializationApi::class)
fun main() {
    // The serialization plugin generates a serializer for Project
    // in the same way as for a class annotated with @Serializable
    val descriptor = Project.serializer().descriptor

    // Retrieves the class identifier from the annotation in the serial descriptor
    val typeId = descriptor.annotations
        .filterIsInstance<BinarySerializable>()
        .first()
        .typeId

    println(typeId)
    // 1
}
//sampleEnd
```
{kotlin-runnable="true"}

## What's next

* Learn how to serialize data in [CBOR format](serialization-cbor.md).
* Explore Protocol Buffers serialization in [ProtoBuf format](serialization-protobuf.md).
