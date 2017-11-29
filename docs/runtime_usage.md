# Runtime library contents and usage

## Obtaining serializers

Serializers are represented at runtime as `KSerializer<T>`, which in turn, implements interfaces `KSerialSaver<T>` and `KSerialLoader<T>`, where `T` is class you serialize. You don't need to call them by yourself; you just have to pass them properly to serialization format. You can write them on your own (see [custom serializers](custom_serializers.md)) or let the compiler plugin do the dirty work by marking class `@Serializable`. To retrieve the generated serializer, plugin emits special function on companion object called `.serializer()`.
If your class has generic type arguments, this function will have arguments for specifying serializers on type parameters, because it's impossible to serialize generic class statically in general case:

```kotlin
@Serializable
data class Data(val a: Int)

@Serializable
data class Box<T>(val boxed: T)

val dataSerial     : KSerializer<Data>      = Data.serializer()
val boxedDataSerial: KSerializer<Box<Data>> = Box.serializer(dataSerial)
```

Built-in types, like Int, and standard collections doesn't have that method. You can use corresponding serializers from `kotlinx.serialization.internal` package:

```kotlin
val i : KSerializer<Int>       = IntSerializer // object
val li: KSerializer<List<Int>> = ArrayListSerializer(IntSerializer) // generic, requires instantiation
```

For convenience, serializers have extension properties:

```kotlin
val li: KSerializer<List<Data>>       = Data.serializer().list
val mp: KSerializer<Map<String, Int>> = (StringSerializer to IntSerializer).map // extension on Pair of serializers
```

In following special case:
* Class explicitly marked `@Serializable`
* Class doesn't have generic type arguments

You can obtain serializer from KClass instance: `val d: KSerializer<MyData> = MyData::class.serializer()`. This approach is discouraged in general because of its implicitness, but maybe useful shorthand in some cases.

All external serializers (defined by user) must be [registered](custom_serializers.md#registering-and-context) and instantiated in a user-specific way.

## Serialization formats

Runtime library provides three ready-to use formats: JSON, CBOR and ProtoBuf.

### JSON

JSON format represented by `JSON` class from `kotlinx.serialization.json` package. It has constructor with four optional parameters:

* nonstrict - allow JSON parser skip fields which are not present in class. By default is false.
* unquoted - means that all field names and other objects (where it's possible) would not be wrapped in quotes. Useful for debugging.
* indented - classic pretty-printed multiline JSON.
* indent - size of indent, applicable if parameter above is true.

You can also use one of predefined instances, like `JSON.plain`, `JSON.indented`, `JSON.nonstrict` or `JSON.unquoted`. API is duplicated in companion object, so `JSON.parse(...)` equals to `JSON.plain.parse(...)`

JSON API:

```kotlin
fun <T> stringify(saver: KSerialSaver<T>, obj: T): String
inline fun <reified T : Any> stringify(obj: T): String = stringify(T::class.serializer(), obj)

fun <T> parse(loader: KSerialLoader<T>, str: String): T
inline fun <reified T : Any> parse(str: String): T = parse(T::class.serializer(), str)
```

`stringify` transforms object to string, `parse` parses. No surprises.

**Note**: because JSON doesn't support maps with keys other than
strings (and primitives), Kotlin maps with non-trivial key types are serialized as JSON lists.

**Caveat**: `T::class.serializer()` assumes that you use it on class defined as `@Serializable`,
so it wouldn't work with root-level collections or external serializers out of the box. It's always better to specify serializer [explicitly](#obtaining-serializers).

### CBOR

`CBOR` object doesn't support any tweaking and provides following functions:

```kotlin
fun <T : Any> dump(saver: KSerialSaver<T>, obj: T): ByteArray // saves object to bytes
inline fun <reified T : Any> dump(obj: T): ByteArray // same as above, resolves serializer by itself
inline fun <reified T : Any> dumps(obj: T): String // dump object and then pretty-print bytes to string

fun <T : Any> load(loader: KSerialLoader<T>, raw: ByteArray): T // load object from bytes
inline fun <reified T : Any> load(raw: ByteArray): T // save as above
inline fun <reified T : Any> loads(hex: String): T // inverse operation for dumps
```

**Note**: CBOR, unlike JSON, supports maps with non-trivial keys,
and Kotlin maps are serialized as CBOR maps, but some parsers (like `jackson-dataformat-cbor`) don't support this.

### Protobuf

Because protobuf relies on serial ids of fields, called 'tags', you have to provide this information, 
using serial annotation `@SerialId`:

```kotlin
@Serializable
data class KTestInt32(@SerialId(1) val a: Int)
```

This class is equivalent to the following proto definition:

```proto
message Int32 {
    required int32 a = 1;
}
```

Note that we are using proto2 semantics, where all fields are explicitly required or optional.

Number format is set via `@ProtoType` annotation. `ProtoNumberType.DEFAULT` is default varint encoding (`intXX`), `SIGNED`
is signed ZigZag representation (`sintXX`), and `FIXED` is `fixedXX` type. `uintXX` and `sfixedXX` are not supported yet.

Repeated fields represented as lists. Because format spec says that if the list is empty, there will be no elements in the stream with such tag,
you must explicitly mark any field of list type with `@Optional` annotation with default ` = emptyList()`. Same for maps.

Other known issues and limitations:

* Packed repeated fields are not supported
* If fields with list tag are going in the arbitrary order, they are not merged into one list, they get overwritten instead.

More examples of mappings from proto definitions to Koltin classes can be found in test data:
[here](../runtime/jvm/src/test/proto/test_data.proto) and [here](../runtime/jvm/src/test/kotlin/kotlinx/serialization/formats/RandomTests.kt#L47)

## Useful classes

### Mapper

`Mapper` allows you to serialize/deserialize object to/from map:

```kotlin
data class Data(val first: Int, val second: String)

val map: Map<String, Any> = Mapper.map(Data(42, "foo")) // mapOf("first" to 42, "second" to "foo")
```

To get your object back, use `unmap` function. To support objects with nullable values, use `mapNullable` and `unmapNullable`

### Dynamic object parser (JS only)

Allows you to convert JS objects of `dynamic` types into fair correct Kotlin objects.

```kotlin
@Serializable
data class Data(val a: Int)

@Serializable
data class DataWrapper(val s: String, val d: Data?)

val dyn = js("""{s:"foo", d:{a:42}}""")
val parsed = DynamicObjectParser().parse<DataWrapper>(dyn)
parsed == DataWrapper("foo", Data(42)) // true
```

> This feature is still in development and may have some bugs. Parser does not support kotlin maps with keys other than `String`.

