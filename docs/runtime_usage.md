# Runtime library contents and usage

## Obtaining serializers

Serializers are represented at runtime as `KSerializer<T>`, which in turn, implements interfaces `SerializationStrategy<T>` and `DeserializationStrategy<T>`, where `T` is class you serialize.
You don't need to call them by yourself; you just have to pass them properly to serialization format. You can write them on your own (see [custom serializers](custom_serializers.md)) or let the compiler plugin do the dirty work by marking class `@Serializable`.
To retrieve the generated serializer, plugin emits special function on companion object called `.serializer()`.
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

All external serializers (defined by user) are instantiated in a user-specific way. To learn how to write them, see [docs](custom_serializers.md).

## Serialization formats

Runtime library provides three ready-to use formats: JSON, CBOR and ProtoBuf.

### JSON

JSON format represented by `Json` class from `kotlinx.serialization.json` package. It has following constructor parameters:

* strict - Prohibits unknown keys when parsing JSON. Prohibits NaN and Infinity float values when serializing JSON. Enabled by default.
* unquoted - means that all field names and other objects (where it's possible) would not be wrapped in quotes. Useful for debugging.
* indented - classic pretty-printed multiline JSON.
* indent - size of indent, applicable if parameter above is true.
* encodeDefaults - set this to false to omit writing optional properties if they are equal to theirs default values.

You can also use one of predefined instances, like `Json.plain`, `Json.indented`, `Json.nonstrict` or `Json.unquoted`. API is duplicated in companion object, so `Json.parse(...)` equals to `Json.plain.parse(...)`

You can also specify desired behaviour for duplicating keys. By default it is `UpdateMode.OVERWRITE`. You can use `UpdateMode.UPDATE`, and by doing that you'll be able to merge two lists or maps with same key into one; but be aware that serializers for non-collection types are throwing `UpdateNotSupportedException` by default. To prohibit duplicated keys, you can use `UpdateMode.BANNED`.

JSON API:

```kotlin
fun <T> stringify(serializer: SerializationStrategy<T>, obj: T): String
inline fun <reified T : Any> stringify(obj: T): String = stringify(T::class.serializer(), obj)

fun <T> parse(loader: DeserializationStrategy<T>, str: String): T
inline fun <reified T : Any> parse(str: String): T = parse(T::class.serializer(), str)
```

`stringify` transforms object to string, `parse` parses. No surprises.

Besides this, functions `toJson` and `fromJson` allow converting @Serializable Kotlin object to and from [abstract JSON syntax tree](https://github.com/Kotlin/kotlinx.serialization/blob/master/runtime/common/src/main/kotlin/kotlinx/serialization/json/JsonElement.kt#L28). To build JSON AST from String, use `parseJson`.

**Note**: because JSON doesn't support maps with keys other than
strings (and primitives), Kotlin maps with non-trivial key types are serialized as JSON lists.

**Caveat**: `T::class.serializer()` assumes that you use it on class defined as `@Serializable`,
so it wouldn't work with root-level collections or external serializers out of the box. It's always better to specify serializer [explicitly](#obtaining-serializers).

### CBOR

`Cbor` class provides following functions:

```kotlin
fun <T : Any> dump(serializer: SerializationStrategy<T>, obj: T): ByteArray // saves object to bytes
inline fun <reified T : Any> dump(obj: T): ByteArray // same as above, resolves serializer by itself
inline fun <reified T : Any> dumps(obj: T): String // dump object and then pretty-print bytes to string

fun <T : Any> load(loader: DeserializationStrategy<T>, raw: ByteArray): T // load object from bytes
inline fun <reified T : Any> load(raw: ByteArray): T // save as above
inline fun <reified T : Any> loads(hex: String): T // inverse operation for dumps
```

It has `UpdateMode.BANNED` by default. As Json, Cbor supports omitting default values.

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
you must explicitly mark any field of list type with default ` = emptyList()`. Same for maps. Update mode for Protobuf is set to `UPDATE` and can't be changed, thus allowing merging several scattered lists into one.

Other known issues and limitations:

* Packed repeated fields are not supported

More examples of mappings from proto definitions to Koltin classes can be found in test data:
[here](../runtime/jvm/src/test/proto/test_data.proto) and [here](../runtime/jvm/src/test/kotlin/kotlinx/serialization/formats/RandomTests.kt#L47)

## Useful classes

### Mapper

`Mapper` allows you to serialize/deserialize object to/from map:

```kotlin
@Serializable
data class Data(val first: Int, val second: String)

val map: Map<String, Any> = Mapper.map(Data(42, "foo")) // mapOf("first" to 42, "second" to "foo")
```

To get your object back, use `unmap` function. To support objects with nullable values, use `mapNullable` and `unmapNullable`.

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

> Parser does not support kotlin maps with keys other than `String`.

