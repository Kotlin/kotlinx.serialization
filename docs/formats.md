<!--- TEST_NAME FormatsTest -->

# Alternative and custom formats (experimental)

This is the sixth chapter of the [Kotlin Serialization Guide](serialization-guide.md).
It goes beyond JSON, covering alternative and custom formats. Unlike JSON, which is
stable, these are currently experimental features of Kotlin serialization.

**Table of contents**

<!--- TOC -->

* [CBOR (experimental)](#cbor-experimental)
  * [Byte arrays and CBOR data types](#byte-arrays-and-cbor-data-types)
* [ProtoBuf (experimental)](#protobuf-experimental)
  * [Field numbers](#field-numbers)
  * [Integer types](#integer-types)
  * [Lists as repeated fields](#lists-as-repeated-fields)
* [Properties (experimental)](#properties-experimental)
* [Custom formats (experimental)](#custom-formats-experimental)
  * [Basic encoder](#basic-encoder)
  * [Basic decoder](#basic-decoder)
  * [Sequential decoding](#sequential-decoding)
  * [Adding collection support](#adding-collection-support)
  * [Adding null support](#adding-null-support)
  * [Efficient binary format](#efficient-binary-format)
  * [Format-specific types](#format-specific-types)

<!--- END -->

## CBOR (experimental) 

[CBOR][RFC 7049] is one of the standard compact binary
encodings for JSON, so it supports a subset of [JSON features](json.md) and
is generally very similar to JSON in use, but produces binary data.

> CBOR support is (experimentally) available in a separate 
> `org.jetbrains.kotlinx:kotlinx-serialization-cbor:<version>` module.

[Cbor] class has [Cbor.encodeToByteArray] and [Cbor.decodeFromByteArray] functions.
Let us take the basic example from the [JSON encoding](basic-serialization.md#json-encoding), 
but encode it using CBOR. 

<!--- INCLUDE
import kotlinx.serialization.*
import kotlinx.serialization.cbor.*

fun ByteArray.toAsciiHexString() = joinToString("") {
    if (it in 32..127) it.toChar().toString() else
        "{${it.toUByte().toString(16).padStart(2, '0').toUpperCase()}}"
}
-->

```kotlin    
@Serializable
data class Project(val name: String, val language: String)

fun main() {
    val data = Project("kotlinx.serialization", "Kotlin") 
    val bytes = Cbor.encodeToByteArray(data)   
    println(bytes.toAsciiHexString())
    val obj = Cbor.decodeFromByteArray<Project>(bytes)
    println(obj)
}
```                                  

> You can get the full code [here](../guide/example/example-formats-01.kt).

We print filtered ASCII representation of the output, writing non-ASCII data in hex, so we see how 
all the original strings are directly represented in CBOR, but the format delimiters itself are binary.

```text 
{BF}dnameukotlinx.serializationhlanguagefKotlin{FF}
Project(name=kotlinx.serialization, language=Kotlin)
```

<!--- TEST -->

In CBOR [hex notation](http://cbor.me/), the output is equivalent to the following:
```
BF                                      # map(*)
   64                                   # text(4)
      6E616D65                          # "name"
   75                                   # text(21)
      6B6F746C696E782E73657269616C697A6174696F6E # "kotlinx.serialization"
   68                                   # text(8)
      6C616E6775616765                  # "language"
   66                                   # text(6)
      4B6F746C696E                      # "Kotlin"
   FF                                   # primitive(*)
```

> Note, CBOR as a format, unlike JSON, supports maps with non-trivial keys 
> (see the [Allowing structured map keys](json.md#allowing-structured-map-keys) section for JSON workarounds),
> and Kotlin maps are serialized as CBOR maps, but some parsers (like `jackson-dataformat-cbor`) don't support this.


### Byte arrays and CBOR data types

Per the [RFC 7049 Major Types] section, CBOR supports the following data types:

- Major type 0: an unsigned integer
- Major type 1: a negative integer
- **Major type 2: a byte string**
- Major type 3: a text string
- **Major type 4: an array of data items**
- Major type 5: a map of pairs of data items
- Major type 6: optional semantic tagging of other major types
- Major type 7: floating-point numbers and simple data types that need no content, as well as the "break" stop code

By default, Kotlin `ByteArray` instances are encoded as **major type 4**.
When **major type 2** is desired, then the [`@ByteString`][ByteString] annotation can be used.

<!--- INCLUDE
import kotlinx.serialization.*
import kotlinx.serialization.cbor.*

fun ByteArray.toAsciiHexString() = joinToString("") {
    if (it in 32..127) it.toChar().toString() else
        "{${it.toUByte().toString(16).padStart(2, '0').toUpperCase()}}"
}
-->

```kotlin
@Serializable
data class Data(
    @ByteString
    val type2: ByteArray, // CBOR Major type 2
    val type4: ByteArray  // CBOR Major type 4
)        

fun main() {
    val data = Data(byteArrayOf(1, 2, 3, 4), byteArrayOf(5, 6, 7, 8)) 
    val bytes = Cbor.encodeToByteArray(data)   
    println(bytes.toAsciiHexString())
    val obj = Cbor.decodeFromByteArray<Data>(bytes)
    println(obj)
}
```

> You can get the full code [here](../guide/example/example-formats-02.kt).    

As we see, the CBOR byte that precedes the data is different for different type of encoding.

```text
{BF}etype2D{01}{02}{03}{04}etype4{9F}{05}{06}{07}{08}{FF}{FF}
Data(type2=[1, 2, 3, 4], type4=[5, 6, 7, 8])
```

<!--- TEST -->

In [CBOR dhex notation](http://cbor.me/), the output is equivalent to the following:
```
BF               # map(*)
   65            # text(5)
      7479706532 # "type2"
   44            # bytes(4)
      01020304   # "\x01\x02\x03\x04"
   65            # text(5)
      7479706534 # "type4"
   9F            # array(*)
      05         # unsigned(5)
      06         # unsigned(6)
      07         # unsigned(7)
      08         # unsigned(8)
      FF         # primitive(*)
   FF            # primitive(*)
```

## ProtoBuf (experimental)

(Protocol Buffers)[https://developers.google.com/protocol-buffers] is a language-neutral binary format that normally
relies on a separate ".proto" file that defines the protocol schema. It is more compact than CBOR, because it
assigns integer numbers to fields instead of names.

> Protocol buffers support is (experimentally) available in a separate 
> `org.jetbrains.kotlinx:kotlinx-serialization-protobuf:<version>` module.

Kotlin serialization is using proto2 semantics, where all fields are explicitly required or optional.
For a basic example we change our example to use 
[ProtoBuf] class with [ProtoBuf.encodeToByteArray] and [ProtoBuf.decodeFromByteArray] functions.

<!--- INCLUDE
import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*

fun ByteArray.toAsciiHexString() = joinToString("") {
    if (it in 32..127) it.toChar().toString() else
        "{${it.toUByte().toString(16).padStart(2, '0').toUpperCase()}}"
}
-->

```kotlin    
@Serializable
data class Project(val name: String, val language: String)

fun main() {
    val data = Project("kotlinx.serialization", "Kotlin") 
    val bytes = ProtoBuf.encodeToByteArray(data)   
    println(bytes.toAsciiHexString())
    val obj = ProtoBuf.decodeFromByteArray<Project>(bytes)
    println(obj)
}
```                                  

> You can get the full code [here](../guide/example/example-formats-03.kt).     

```text 
{0A}{15}kotlinx.serialization{12}{06}Kotlin
Project(name=kotlinx.serialization, language=Kotlin)
```

<!--- TEST -->

In [ProtoBuf hex notation](https://protogen.marcgravell.com/decode), the output is equivalent to the following:
```
Field #1: 0A String Length = 21, Hex = 15, UTF8 = "kotlinx.serialization"
Field #2: 12 String Length = 6, Hex = 06, UTF8 = "Kotlin"
```

### Field numbers

By default, field numbers in the Kotlin serialization [ProtoBuf] implementation are automatically assigned, 
which does not provide ability to define a stable data schema that evolves over time, which is normally achieved by 
writing a separate ".proto" file. However, with Kotlin serialization we can get this stability without a separate
schema file, using [ProtoNumber] annotation.

<!--- INCLUDE
import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*

fun ByteArray.toAsciiHexString() = joinToString("") {
    if (it in 32..127) it.toChar().toString() else
        "{${it.toUByte().toString(16).padStart(2, '0').toUpperCase()}}"
}
-->

```kotlin    
@Serializable
data class Project(
    @ProtoNumber(1)
    val name: String, 
    @ProtoNumber(3)
    val language: String
)

fun main() {
    val data = Project("kotlinx.serialization", "Kotlin") 
    val bytes = ProtoBuf.encodeToByteArray(data)   
    println(bytes.toAsciiHexString())
    val obj = ProtoBuf.decodeFromByteArray<Project>(bytes)
    println(obj)
}
```                                  

> You can get the full code [here](../guide/example/example-formats-04.kt).     

We see in the output that the number for the first property `name` did not change (as it is numbered from one by default),
but it did change for the `language` property. 

```text 
{0A}{15}kotlinx.serialization{1A}{06}Kotlin
Project(name=kotlinx.serialization, language=Kotlin)
```    

<!--- TEST -->
      
In [ProtoBuf hex notation](https://protogen.marcgravell.com/decode), the output is equivalent to the following:
```
Field #1: 0A String Length = 21, Hex = 15, UTF8 = "kotlinx.serialization" (total 21 chars)
Field #3: 1A String Length = 6, Hex = 06, UTF8 = "Kotlin"
```

### Integer types

Protocol buffers support various integer encodings, that are optimized for different ranges of integers.
They are specified using the [ProtoType] annotation and the [ProtoIntegerType] enum. 
The following example shows all three supported options.

<!--- INCLUDE
import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*

fun ByteArray.toAsciiHexString() = joinToString("") {
    if (it in 32..127) it.toChar().toString() else
        "{${it.toUByte().toString(16).padStart(2, '0').toUpperCase()}}"
}
-->

```kotlin    
@Serializable
class Data(
    @ProtoType(ProtoIntegerType.DEFAULT)
    val a: Int,
    @ProtoType(ProtoIntegerType.SIGNED)
    val b: Int,
    @ProtoType(ProtoIntegerType.FIXED)
    val c: Int
)

fun main() {
    val data = Data(1, -2, 3) 
    println(ProtoBuf.encodeToByteArray(data).toAsciiHexString())
}
```                   

> You can get the full code [here](../guide/example/example-formats-05.kt).           

* The [default][ProtoIntegerType.DEFAULT] is a varint encoding (`intXX`) that is optimized for 
  small non-negative numbers. The value of `1` is encoded in one byte `01`. 
* The [signed][ProtoIntegerType.SIGNED] is a signed ZigZag encoding (`sintXX`) that is optimized for 
  small signed integers. The value of `-2` is encoded in one byte `03`. 
* The [fixed][ProtoIntegerType.FIXED] encoding (`fixedXX`) always uses the fixed number of bytes.
  The value of `3` is encoded as four bytes `03 00 00 00`.
  
> `uintXX` and `sfixedXX` protocol buffer types are not supported.    

```text 
{08}{01}{10}{03}{1D}{03}{00}{00}{00}
```          

<!--- TEST -->

In [ProtoBuf hex notation](https://protogen.marcgravell.com/decode), the output is equivalent to the following:
```
Field #1: 08 Varint Value = 1, Hex = 01
Field #2: 10 Varint Value = 3, Hex = 03
Field #3: 1D Fixed32 Value = 3, Hex = 03-00-00-00
```

### Lists as repeated fields

Kotlin lists and other collections are representend as repeated fields. 
In the protocol buffers when the list is empty there are no elements in the 
stream with the corresponding number. For Kotlin serialization you must explicitly specify a default of `emptyList()`
for any property of a collection or a map type. Otherwise, you will not be able deserialize an empty
list, which is indistinguishable in protocol buffers from the missing field.  

<!--- INCLUDE
import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*

fun ByteArray.toAsciiHexString() = joinToString("") {
    if (it in 32..127) it.toChar().toString() else
        "{${it.toUByte().toString(16).padStart(2, '0').toUpperCase()}}"
}
-->

```kotlin    
@Serializable
data class Data(
    val a: List<Int> = emptyList(),
    val b: List<Int> = emptyList()
)

fun main() {
    val data = Data(listOf(1, 2, 3), listOf())
    val bytes = ProtoBuf.encodeToByteArray(data)
    println(bytes.toAsciiHexString())
    println(ProtoBuf.decodeFromByteArray<Data>(bytes))
}
```                   

> You can get the full code [here](../guide/example/example-formats-06.kt).    

```text 
{08}{01}{08}{02}{08}{03}
Data(a=[1, 2, 3], b=[])
```

<!--- TEST -->

> Packed repeated fields are not supported.
       
In [ProtoBuf diagnostic mode](https://protogen.marcgravell.com/decode), the output is equivalent to the following:
```
Field #1: 08 Varint Value = 1, Hex = 01
Field #1: 08 Varint Value = 2, Hex = 02
Field #1: 08 Varint Value = 3, Hex = 03
```

## Properties (experimental)

Kotlin serialization can serialize a class into a flat map with `String` keys via 
the [Properties][kotlinx.serialization.properties.Properties] format implementation.

> Properties support is (experimentally) available in a separate 
> `org.jetbrains.kotlinx:kotlinx-serialization-properties:<version>` module.

<!--- INCLUDE
import kotlinx.serialization.*
import kotlinx.serialization.properties.Properties // todo: remove when no longer needed
import kotlinx.serialization.properties.*
-->

```kotlin    
@Serializable
class Project(val name: String, val owner: User)

@Serializable
class User(val name: String)

fun main() {
    val data = Project("kotlinx.serialization",  User("kotlin"))
    val map = Properties.encodeToMap(data)
    map.forEach { (k, v) -> println("$k = $v") }
}
```      

> You can get the full code [here](../guide/example/example-formats-07.kt).

The resulting map has dot-separated keys representing keys of the nested objects.

```text 
name = kotlinx.serialization
owner.name = kotlin
```

<!--- TEST -->

## Custom formats (experimental)

A custom format for Kotlin serialization must provide implementation for [Encoder] and [Decoder] interfaces that
we saw used in the [Serializers](serializers.md) chapter.  
These are pretty large interfaces. For convenience, 
the [AbstractEncoder] and [AbstractDecoder] skeleton implementations are provided to simplify the task.
In [AbstractEncoder] most of the `encodeXxx` methods have default implementation that 
delegates to [`encodeValue(value: Any)`][AbstractEncoder.encodeValue] &mdash; the only method that must be
implemented to get a basic working format.

### Basic encoder

Let us starts with a trivial format implementation that encodes the data into a single list of primitive
constituent objects in the order they were written in the source code. To start, we implement a simple [Encoder] by
overriding `encodeValue` in [AbstractEncoder].

<!--- INCLUDE
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
-->

```kotlin
class ListEncoder : AbstractEncoder() {
    val list = mutableListOf<Any>()

    override fun encodeValue(value: Any) {
        list.add(value)
    }
}
```              

Now we write a convenience top-level function that creates encoder that encodes an object
and returns a list.

```kotlin
fun <T> encodeToList(serializer: SerializationStrategy<T>, value: T): List<Any> {
    val encoder = ListEncoder()
    encoder.encodeSerializableValue(serializer, value)
    return encoder.list
}
```                     

For even more convenience, to avoid the need to explicitly pass serializer, we write an `inline` overload of
`encodeToList` function with `reified` type parameter using the [serializer] function to retrieve
the appropriate [KSerializer] instance for the actually used type.

```kotlin 
inline fun <reified T> encodeToList(value: T) = encodeToList(serializer(), value)
```                  

Now we can test it.

```kotlin 
@Serializable
data class Project(val name: String, val owner: User, val votes: Int)

@Serializable
data class User(val name: String)

fun main() {
    val data = Project("kotlinx.serialization",  User("kotlin"), 9000)
    println(encodeToList(data))
}
```                                    

> You can get the full code [here](../guide/example/example-formats-08.kt).

As a result, we got all the primitives values in our object graph visited and put into a list
in a _serial_ order.

```text
[kotlinx.serialization, kotlin, 9000]
```    

<!--- TEST -->

> By itself, that's a useful feature if we need compute some kind of hashcode or digest for all the data
> that is contained in a serializable object tree.

### Basic decoder

<!--- INCLUDE 
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

class ListEncoder : AbstractEncoder() {
    val list = mutableListOf<Any>()

    override fun encodeValue(value: Any) {
        list.add(value)
    }
}

fun <T> encodeToList(serializer: SerializationStrategy<T>, value: T): List<Any> {
    val encoder = ListEncoder()
    encoder.encodeSerializableValue(serializer, value)
    return encoder.list
}

inline fun <reified T> encodeToList(value: T) = encodeToList(serializer(), value)
-->

A decoder needs to implements more substance.

* [decodeValue][AbstractDecoder.decodeValue] &mdash; returns the next value from the list.
* [decodeElementIndex][CompositeDecoder.decodeElementIndex] &mdash; returns the next index of a deserialized value.
  In this primitive format deserialization always happens in order, so we keep track of the index
  in the `elementIndex` variable. See 
  the [Hand-written composite serializer](serializers.md#hand-written-composite-serializer) section 
  on how it ends up being used.
* [beginStructure][Decoder.beginStructure] &must; returns a new instance of the `ListDecoder`, so that
  each structure that is being recursively decoded keeps track of its own `elementIndex` state separately.  

```kotlin
class ListDecoder(val list: ArrayDeque<Any>) : AbstractDecoder() {
    private var elementIndex = 0

    override fun decodeValue(): Any = list.removeFirst()
    
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (elementIndex == descriptor.elementsCount) return CompositeDecoder.DECODE_DONE
        return elementIndex++
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
        ListDecoder(list)
}
```

A couple of convenience functions for decoding.

```kotlin
fun <T> decodeFromList(list: List<Any>, deserializer: DeserializationStrategy<T>): T {
    val decoder = ListDecoder(ArrayDeque(list))
    return decoder.decodeSerializableValue(deserializer)
}

inline fun <reified T> decodeFromList(list: List<Any>): T = decodeFromList(list, serializer())
```

That is enough to start encoding and decoding basic serializable classes.

<!--- INCLUDE

@Serializable
data class Project(val name: String, val owner: User, val votes: Int)

@Serializable
data class User(val name: String)
-->

```kotlin    
fun main() {
    val data = Project("kotlinx.serialization",  User("kotlin"), 9000)
    val list = encodeToList(data)
    println(list)
    val obj = decodeFromList<Project>(list)
    println(obj)
}
```

> You can get the full code [here](../guide/example/example-formats-09.kt).

Now can convert a list of primitives back to an object tree.

```text
[kotlinx.serialization, kotlin, 9000]
Project(name=kotlinx.serialization, owner=User(name=kotlin), votes=9000)
```               

<!--- TEST -->

### Sequential decoding

The decoder we have implemented keeps track of the `elementIndex` in its state and implements
`decodeElementIndex`. This means that it is going to work with an arbitrary serializer, even the
simple one we wrote in 
the [Hand-written composite serializer](serializers.md#hand-written-composite-serializer) section.
However, this format always stores element in the order, so this book-keeping is not needed and
undermines decoding performance. All auto-generated serializers on JVM support 
the [Sequential decoding protocol (experimental)](serializers.md#sequential-decoding-protocol-experimental) and the decoder can indicate
its support by returning `true` from the [CompositeDecoder.decodeSequentially] function.

<!--- INCLUDE
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

class ListEncoder : AbstractEncoder() {
    val list = mutableListOf<Any>()

    override fun encodeValue(value: Any) {
        list.add(value)
    }
}

fun <T> encodeToList(serializer: SerializationStrategy<T>, value: T): List<Any> {
    val encoder = ListEncoder()
    encoder.encodeSerializableValue(serializer, value)
    return encoder.list
}

inline fun <reified T> encodeToList(value: T) = encodeToList(serializer(), value)
-->

```kotlin
class ListDecoder(val list: ArrayDeque<Any>) : AbstractDecoder() {
    private var elementIndex = 0

    override fun decodeValue(): Any = list.removeFirst()
    
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        if (elementIndex == descriptor.elementsCount) return CompositeDecoder.DECODE_DONE
        return elementIndex++
    }

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
        ListDecoder(list) 

    override fun decodeSequentially(): Boolean = true
}        
```

<!--- INCLUDE

fun <T> decodeFromList(list: List<Any>, deserializer: DeserializationStrategy<T>): T {
    val decoder = ListDecoder(ArrayDeque(list))
    return decoder.decodeSerializableValue(deserializer)
}

inline fun <reified T> decodeFromList(list: List<Any>): T = decodeFromList(list, serializer())

@Serializable
data class Project(val name: String, val owner: User, val votes: Int)

@Serializable
data class User(val name: String)

fun main() {
    val data = Project("kotlinx.serialization",  User("kotlin"), 9000)
    val list = encodeToList(data)
    println(list)
    val obj = decodeFromList<Project>(list)
    println(obj)
}
-->

> You can get the full code [here](../guide/example/example-formats-10.kt).

<!--- TEST 
[kotlinx.serialization, kotlin, 9000]
Project(name=kotlinx.serialization, owner=User(name=kotlin), votes=9000)
-->   
 
### Adding collection support

This basic format, so far, cannot property represent collections. In encodes them, but it does not keep
track on how many elements are there in the collection or where it ends, so it cannot properly decode them.
First, let us add propers support for collections to the encoder by implementing the 
[Encoder.beginCollection] function. The `beginCollection` function takes a collection size as a parameter, 
so we encode it to add it to the result. 
Our encoder implementation does not keep any state, so it just returns `this` from the `beginCollection` function.
 
<!--- INCLUDE 
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
-->

```kotlin
class ListEncoder : AbstractEncoder() {
    val list = mutableListOf<Any>()

    override fun encodeValue(value: Any) {
        list.add(value)
    }                               

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        encodeInt(collectionSize)
        return this
    }                                                
}
```

<!--- INCLUDE

fun <T> encodeToList(serializer: SerializationStrategy<T>, value: T): List<Any> {
    val encoder = ListEncoder()
    encoder.encodeSerializableValue(serializer, value)
    return encoder.list
}

inline fun <reified T> encodeToList(value: T) = encodeToList(serializer(), value)
-->

The decoder, for our case, needs to only implement the [CompositeDecoder.decodeCollectionSize] function
in addition the previous code.

> The formats that store collection size in advance have to return `true` from `decodeSequentially`.

```kotlin
class ListDecoder(val list: ArrayDeque<Any>, var elementsCount: Int = 0) : AbstractDecoder() {
    private var elementIndex = 0

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
}
```

<!--- INCLUDE

fun <T> decodeFromList(list: List<Any>, deserializer: DeserializationStrategy<T>): T {
    val decoder = ListDecoder(ArrayDeque(list))
    return decoder.decodeSerializableValue(deserializer)
}

inline fun <reified T> decodeFromList(list: List<Any>): T = decodeFromList(list, serializer())
-->

That is all that is needed to support collections and maps. 

```kotlin     
@Serializable
data class Project(val name: String, val owners: List<User>, val votes: Int)

@Serializable
data class User(val name: String)

fun main() {
    val data = Project("kotlinx.serialization",  listOf(User("kotlin"), User("jetbrains")), 9000)
    val list = encodeToList(data)
    println(list)
    val obj = decodeFromList<Project>(list)
    println(obj)
}
```

> You can get the full code [here](../guide/example/example-formats-11.kt).

We see the size of the list added to the result, letting decoder know where to stop. 

```text
[kotlinx.serialization, 2, kotlin, jetbrains, 9000]
Project(name=kotlinx.serialization, owners=[User(name=kotlin), User(name=jetbrains)], votes=9000)
```                      

<!--- TEST -->

### Adding null support

Our trivial format does not support `null` values so far. For nullable types we need to add some kind
of "null indicator", telling whether the upcoming value is null or is not null.

<!--- INCLUDE 
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

class ListEncoder : AbstractEncoder() {
    val list = mutableListOf<Any>()

    override fun encodeValue(value: Any) {
        list.add(value)
    }                               

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        encodeInt(collectionSize)
        return this
    }                                                
-->

In the encoder implementation we override [Encoder.encodeNull] and [Encoder.encodeNotNullMark].

```kotlin            
    override fun encodeNull() = encodeValue("NULL")
    override fun encodeNotNullMark() = encodeValue("!!")
```

<!--- INCLUDE
}

fun <T> encodeToList(serializer: SerializationStrategy<T>, value: T): List<Any> {
    val encoder = ListEncoder()
    encoder.encodeSerializableValue(serializer, value)
    return encoder.list
}

inline fun <reified T> encodeToList(value: T) = encodeToList(serializer(), value)

class ListDecoder(val list: ArrayDeque<Any>, var elementsCount: Int = 0) : AbstractDecoder() {
    private var elementIndex = 0

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
-->

In the decoder implementation we override [Decoder.decodeNotNullMark].

```kotlin 
    override fun decodeNotNullMark(): Boolean = decodeString() != "NULL"
```

<!--- INCLUDE
}

fun <T> decodeFromList(list: List<Any>, deserializer: DeserializationStrategy<T>): T {
    val decoder = ListDecoder(ArrayDeque(list))
    return decoder.decodeSerializableValue(deserializer)
}

inline fun <reified T> decodeFromList(list: List<Any>): T = decodeFromList(list, serializer())
-->

Let us test nullable properties both with not-null and null values. 

```kotlin     
@Serializable
data class Project(val name: String, val owner: User?, val votes: Int?)

@Serializable
data class User(val name: String)

fun main() {
    val data = Project("kotlinx.serialization",  User("kotlin") , null)
    val list = encodeToList(data)
    println(list)
    val obj = decodeFromList<Project>(list)
    println(obj)
}

```

> You can get the full code [here](../guide/example/example-formats-12.kt).

In the output we see how not-null`!!` and `NULL` marks are used.

```text
[kotlinx.serialization, !!, kotlin, NULL]
Project(name=kotlinx.serialization, owner=User(name=kotlin), votes=null)
```                      

<!--- TEST -->

### Efficient binary format

Now we are ready for an example of an efficient binary format. We are going to write data to the
[java.io.DataOutput] implementation. Instead of `encodeValue` we must override the individual 
`encodeXxx` functions for each of ten [primitives](builtin-classes.md#primitives) in the encoder.

<!--- INCLUDE 
import kotlinx.serialization.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import java.io.*
-->

```kotlin            
class DataOutputEncoder(val output: DataOutput) : AbstractEncoder() {
    override fun encodeBoolean(value: Boolean) = output.writeByte(if (value) 1 else 0)
    override fun encodeByte(value: Byte) = output.writeByte(value.toInt())
    override fun encodeShort(value: Short) = output.writeShort(value.toInt())
    override fun encodeInt(value: Int) = output.writeInt(value)
    override fun encodeLong(value: Long) = output.writeLong(value)
    override fun encodeFloat(value: Float) = output.writeFloat(value)
    override fun encodeDouble(value: Double) = output.writeDouble(value)
    override fun encodeChar(value: Char) = output.writeChar(value.toInt())
    override fun encodeString(value: String) = output.writeUTF(value)
    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) = output.writeInt(index)

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        encodeInt(collectionSize)
        return this
    }

    override fun encodeNull() = encodeBoolean(false)
    override fun encodeNotNullMark() = encodeBoolean(true)
}
```

<!--- INCLUDE

fun <T> encodeTo(output: DataOutput, serializer: SerializationStrategy<T>, value: T) {
    val encoder = DataOutputEncoder(output)
    encoder.encodeSerializableValue(serializer, value)
}

inline fun <reified T> encodeTo(output: DataOutput, value: T) = encodeTo(output, serializer(), value)
-->

The decoder implementation mirrors encoder's implementation overriding all the primitive `decodeXxx` functions.

```kotlin 
class DataInputDecoder(val input: DataInput, var elementsCount: Int = 0) : AbstractDecoder() {
    private var elementIndex = 0

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
```

<!--- INCLUDE

fun <T> decodeFrom(input: DataInput, deserializer: DeserializationStrategy<T>): T {
    val decoder = DataInputDecoder(input)
    return decoder.decodeSerializableValue(deserializer)
}

inline fun <reified T> decodeFrom(input: DataInput): T = decodeFrom(input, serializer())

fun ByteArray.toAsciiHexString() = joinToString("") {
    if (it in 32..127) it.toChar().toString() else
        "{${it.toUByte().toString(16).padStart(2, '0').toUpperCase()}}"
}
-->

We can now serialize and serialize arbitrary data. For example, the same classes as were
used in the [CBOR (experimental)](#cbor-experimental) and [ProtoBuf (experimental)](#protobuf-experimental) sections.   

```kotlin    
@Serializable
data class Project(val name: String, val language: String)

fun main() {
    val data = Project("kotlinx.serialization", "Kotlin")
    val output = ByteArrayOutputStream()
    encodeTo(DataOutputStream(output), data)
    val bytes = output.toByteArray()
    println(bytes.toAsciiHexString())
    val input = ByteArrayInputStream(bytes)
    val obj = decodeFrom<Project>(DataInputStream(input))
    println(obj)
}
```
              
> You can get the full code [here](../guide/example/example-formats-13.kt).

As we can see, the result is the dense binary format that only contains the data that is being serialized. 
It can be easily tweaked for any kind of domain-specific compact encoding.

```text
{00}{15}kotlinx.serialization{00}{06}Kotlin
Project(name=kotlinx.serialization, language=Kotlin)
```              

<!--- TEST -->                                                         

### Format-specific types

A format implementation might provide special support for data types that are not among the list of primitive
types in Kotlin serialization and do not have a corresponding `encodeXxx`/`decodeXxx` function. 
In the encoder this is achieved by overriding the 
[`encodeSerializableValue(serializer, value)`][Encoder.encodeSerializableValue] function. 

In our `DataOutput` format example we might want to provide a specialized efficient data path for serializing array
of bytes since [DataOutput][java.io.DataOutput] has a special method for this purpose.

Detection of the type is performed by looking at the `serializer`, not by checking the type of the `value`
being serialized, so we fetch the builtin [KSerializer] instance for `ByteArray` type.

> This an important difference. This way our format implementation properly supports 
> [Custom serializers](serializers.md#custom-serializers) that a user might specify for a type that just happens
> to be internally represented as a byte array, but need a different serial representation. 

<!--- INCLUDE 
import kotlinx.serialization.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import java.io.*
-->

```kotlin         
private val byteArraySerializer = serializer<ByteArray>()
```   

> Specifically for byte arrays, we could have also used the builtin 
> [ByteArraySerializer][kotlinx.serialization.builtins.ByteArraySerializer()] function. 

We add the corresponding code to the [Encoder] implementation of our 
[Efficient binary format](#efficient-binary-format). To make our `ByteArray` encoding even more efficient, 
we add a trivial implementation of `encodeCompactSize` function that uses only one byte to represent
a size of up to 254 bytes.  

<!--- INCLUDE
class DataOutputEncoder(val output: DataOutput) : AbstractEncoder() {
    override fun encodeBoolean(value: Boolean) = output.writeByte(if (value) 1 else 0)
    override fun encodeByte(value: Byte) = output.writeByte(value.toInt())
    override fun encodeShort(value: Short) = output.writeShort(value.toInt())
    override fun encodeInt(value: Int) = output.writeInt(value)
    override fun encodeLong(value: Long) = output.writeLong(value)
    override fun encodeFloat(value: Float) = output.writeFloat(value)
    override fun encodeDouble(value: Double) = output.writeDouble(value)
    override fun encodeChar(value: Char) = output.writeChar(value.toInt())
    override fun encodeString(value: String) = output.writeUTF(value)
    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) = output.writeInt(index)

    override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
        encodeInt(collectionSize)
        return this
    }

    override fun encodeNull() = encodeBoolean(false)
    override fun encodeNotNullMark() = encodeBoolean(true)
-->

```kotlin
    override fun <T> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
        if (serializer === byteArraySerializer)
            encodeByteArray(value as ByteArray)
        else
            super.encodeSerializableValue(serializer, value)
    }

    private fun encodeByteArray(bytes: ByteArray) {
        encodeCompactSize(bytes.size)
        output.write(bytes)
    }
    
    private fun encodeCompactSize(value: Int) {
        if (value < 0xff) {
            output.writeByte(value)
        } else {
            output.writeByte(0xff)
            output.writeInt(value)
        }
    }            
```

<!--- INCLUDE
}

fun <T> encodeTo(output: DataOutput, serializer: SerializationStrategy<T>, value: T) {
    val encoder = DataOutputEncoder(output)
    encoder.encodeSerializableValue(serializer, value)
}

inline fun <reified T> encodeTo(output: DataOutput, value: T) = encodeTo(output, serializer(), value)

class DataInputDecoder(val input: DataInput, var elementsCount: Int = 0) : AbstractDecoder() {
    private var elementIndex = 0

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
-->

A similar code is added to the [Decoder] implementation. Here we override
the [decodeSerializableValue][Decoder.decodeSerializableValue] function.

```kotlin 
    @Suppress("UNCHECKED_CAST")
    override fun <T> decodeSerializableValue(deserializer: DeserializationStrategy<T>, previousValue: T?): T =
        if (deserializer === byteArraySerializer)
            decodeByteArray() as T
        else
            super.decodeSerializableValue(deserializer, previousValue)

    private fun decodeByteArray(): ByteArray {
        val bytes = ByteArray(decodeCompactSize())
        input.readFully(bytes)
        return bytes
    }

    private fun decodeCompactSize(): Int {
        val byte = input.readByte().toInt() and 0xff
        if (byte < 0xff) return byte
        return input.readInt()
    }
```

<!--- INCLUDE
}

fun <T> decodeFrom(input: DataInput, deserializer: DeserializationStrategy<T>): T {
    val decoder = DataInputDecoder(input)
    return decoder.decodeSerializableValue(deserializer)
}

inline fun <reified T> decodeFrom(input: DataInput): T = decodeFrom(input, serializer())

fun ByteArray.toAsciiHexString() = joinToString("") {
    if (it in 32..127) it.toChar().toString() else
        "{${it.toUByte().toString(16).padStart(2, '0').toUpperCase()}}"
}
-->

Now everything is ready to check serialization of some byte arrays.

```kotlin    
@Serializable
data class Project(val name: String, val attachment: ByteArray)

fun main() {
    val data = Project("kotlinx.serialization", byteArrayOf(0x0A, 0x0B, 0x0C, 0x0D))
    val output = ByteArrayOutputStream()
    encodeTo(DataOutputStream(output), data)
    val bytes = output.toByteArray()
    println(bytes.toAsciiHexString())
    val input = ByteArrayInputStream(bytes)
    val obj = decodeFrom<Project>(DataInputStream(input))
    println(obj)
}
```
              
> You can get the full code [here](../guide/example/example-formats-14.kt).

As we can see, our custom byte array format is being used, with compact encoding of its size in one byte. 

```text
{00}{15}kotlinx.serialization{04}{0A}{0B}{0C}{0D}
Project(name=kotlinx.serialization, attachment=[10, 11, 12, 13])
```                   

<!--- TEST -->              

---

This chapter concludes [Kotlin Serialization Guide](serialization-guide.md). 
                                     

<!-- references -->
[RFC 7049]: https://tools.ietf.org/html/rfc7049
[RFC 7049 Major Types]: https://tools.ietf.org/html/rfc7049#section-2.1

<!-- Java references -->
[java.io.DataOutput]: https://docs.oracle.com/javase/8/docs/api/java/io/DataOutput.html

<!--- MODULE /kotlinx-serialization -->
<!--- INDEX kotlinx.serialization -->
[serializer]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization/serializer.html
[KSerializer]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization/-k-serializer/index.html
<!--- INDEX kotlinx.serialization.builtins -->
[kotlinx.serialization.builtins.ByteArraySerializer()]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.builtins/-byte-array-serializer.html
<!--- INDEX kotlinx.serialization.encoding -->
[Encoder]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.encoding/-encoder/index.html
[Decoder]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.encoding/-decoder/index.html
[AbstractEncoder]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.encoding/-abstract-encoder/index.html
[AbstractDecoder]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.encoding/-abstract-decoder/index.html
[AbstractEncoder.encodeValue]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.encoding/-abstract-encoder/encode-value.html
[AbstractDecoder.decodeValue]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.encoding/-abstract-decoder/decode-value.html
[CompositeDecoder.decodeElementIndex]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.encoding/-composite-decoder/decode-element-index.html
[Decoder.beginStructure]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.encoding/-decoder/begin-structure.html
[CompositeDecoder.decodeSequentially]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.encoding/-composite-decoder/decode-sequentially.html
[Encoder.beginCollection]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.encoding/-encoder/begin-collection.html
[CompositeDecoder.decodeCollectionSize]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.encoding/-composite-decoder/decode-collection-size.html
[Encoder.encodeNull]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.encoding/-encoder/encode-null.html
[Encoder.encodeNotNullMark]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.encoding/-encoder/encode-not-null-mark.html
[Decoder.decodeNotNullMark]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.encoding/-decoder/decode-not-null-mark.html
[Encoder.encodeSerializableValue]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.encoding/-encoder/encode-serializable-value.html
[Decoder.decodeSerializableValue]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.encoding/-decoder/decode-serializable-value.html
<!--- INDEX kotlinx.serialization.cbor -->
[Cbor]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.cbor/-cbor/index.html
[Cbor.encodeToByteArray]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.cbor/-cbor/encode-to-byte-array.html
[Cbor.decodeFromByteArray]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.cbor/-cbor/decode-from-byte-array.html
[ByteString]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.cbor/-byte-string/index.html
<!--- INDEX kotlinx.serialization.protobuf -->
[ProtoBuf]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.protobuf/-proto-buf/index.html
[ProtoBuf.encodeToByteArray]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.protobuf/-proto-buf/encode-to-byte-array.html
[ProtoBuf.decodeFromByteArray]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.protobuf/-proto-buf/decode-from-byte-array.html
[ProtoNumber]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.protobuf/-proto-number/index.html
[ProtoType]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.protobuf/-proto-type/index.html
[ProtoIntegerType]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.protobuf/-proto-integer-type/index.html
[ProtoIntegerType.DEFAULT]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.protobuf/-proto-integer-type/-d-e-f-a-u-l-t.html
[ProtoIntegerType.SIGNED]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.protobuf/-proto-integer-type/-s-i-g-n-e-d.html
[ProtoIntegerType.FIXED]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.protobuf/-proto-integer-type/-f-i-x-e-d.html
<!--- INDEX kotlinx.serialization.properties -->
[kotlinx.serialization.properties.Properties]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization/kotlinx.serialization.properties/-properties/index.html
<!--- END -->
   
