
# Runtime library contents and usage

* [Retrieving serializers](#retrieving-serializers)
  + [Reified API](#reified-api)
  + [Special serializers](#special-serializers)
* [Serialization formats](#serialization-formats)
  + [JSON](#json)
  + [CBOR](#cbor)
  + [Protobuf](#protobuf)
* [Useful classes](#useful-classes)
  + [Mapper](#mapper)
  + [Dynamic object parser (JS only)](#dynamic-object-parser-js-only)

## Retrieving serializers

Serializers are represented at runtime as `KSerializer<T>`, which in turn, implements interfaces `SerializationStrategy<T>` and `DeserializationStrategy<T>`, where `T` is class you serialize.
You don't need to call them by yourself; you just have to pass them properly to serialization format. You can write them on your own (see [custom serializers](custom_serializers.md)) or let the compiler plugin do the dirty work by marking class `@Serializable`.
To retrieve the generated serializer, plugin emits special function on companion object called `.serializer()`.
If your class has generic type arguments, this function will have arguments for specifying serializers on type parameters, because it's impossible to serialize generic class statically in general case:

```kotlin
@Serializable
data class Data(val a: Int)

@Serializable
data class Box<T>(val boxed: T)

val dataSerializer: KSerializer<Data> = Data.serializer()
val boxedDataSerializer: KSerializer<Box<Data>> = Box.serializer(dataSerializer)
```

Built-in types, like `Int`, and standard collections doesn't have that method and instead are available via 
factories and companion extensions:
```kotlin
val intSerializer: KSerializer<Int>  = Int.serializer()
val intListSerializer: KSerializer<List<Int>> = ListSerializer(Int.serializer()) // generic, requires instantiation
```

For convenience, serializers have extension properties:
```kotlin
val dataListSerializer: KSerializer<List<Data>> = Data.serializer().list
val mapSerializer: KSerializer<Map<String, Int>> = MapSerializer(String.serializer(), Int.serializer())
```

To convert from serializer for type `T` to serializer for nullable type `T?`, you can use extension factory method `nullable`:

```kotlin
val nullableIntSerializer: KSerializer<Int?> = IntSerializer.nullable
```

All external serializers (defined by user) are instantiated in a user-specific way. To learn how to write them, see [docs](custom_serializers.md).

## Reified API

Most of the API provided by `kotlinx.serialization` can be used without providing a serializer instance, but a corresponding type arguments.

For example, the recommended way to serialize class `Data` to JSON is the following:
```kotlin
val json: Json = Json.Default
val string = json.stringify(Data(...)) // infers as json.stringify<Data>(Data(...)) by the compiler
val deserializedInstance = json.parse<Data>(string)
```

Same principle applies to serial module builders, serial descriptors and serializers:
```kotlin
val intSerializer = serializer<Int>() // 

val module = SerialModule {
    polymorphic<Any> {
        subclass<MyClass>()
    }
}
```

### Special serializers

There are two special serializers which are turned on using corresponding annotations:
`@Contextual` for `ContextSerializer` and `@Polymorphic` for `PolymorphicSerializer`.

The former allows to switch to the run-time resolving of serializers instead of compile-time.
This can be useful when you want to use some custom external serializer
or to define different serializers for different formats.
The latter allows polymorphic serialization and deserialization using runtime class information
and recorded name of a class.
Consult theirs documentation for details. Polymorphic serialization is explained in details [here](polymorphism.md).

Both use serial modules system, which is explained [here](custom_serializers.md#registering-and-context).

## Serialization formats

Runtime library provides three ready-to use formats: JSON, CBOR and ProtoBuf.

### JSON

JSON format represented by `Json` class from `kotlinx.serialization.json` package.
It is configurable via `JsonConfiguration` class, which has following parameters:

* encodeDefaults - set this to false to omit writing optional properties if they are equal to theirs default values.
* strictMode - Prohibits unknown keys when parsing JSON. Prohibits NaN and Infinity float values when serializing JSON. Enabled by default.
* unquoted - means that all field names and other objects (where it's possible) would not be wrapped in quotes. Useful for debugging.
* prettyPrint - classic pretty-printed multiline JSON.
* indent - size of indent, applicable if parameter above is true.
* useArrayPolymorphism – switches to writing polymorphic values in `[className, object]` format. Disabled by default.
* classDiscriminator – name of the class descriptor property in polymorphic serialization

It also has two pre-defined sets of parameters: `Default` and `Stable`.
`Default` provides recommended and sane configuration, however, due to a library evolution,
it can be tweaked and changed between library releases.
`Stable` provides configuration which is guaranteed to be unchanged between library releases.
Since `JsonConfiguration` is a data class, you can `copy` any configuration you like to tweak it.

All unstable constructors and configurations are annotated with [experimental annotation](https://kotlinlang.org/docs/reference/experimental.html#using-experimental-apis) `kotlinx.serialization.UnstableDefault`.

You can also specify desired behaviour for duplicating keys.
By default it is `UpdateMode.OVERWRITE`.
You can use `UpdateMode.UPDATE`, and by doing that you'll be able to merge two lists or maps with same key into one; but be aware that serializers for non-collection types are throwing `UpdateNotSupportedException` by default.
To prohibit duplicated keys, you can use `UpdateMode.BANNED`.

JSON API:

```kotlin
fun <T> stringify(serializer: SerializationStrategy<T>, obj: T): String
inline fun <reified T : Any> stringify(obj: T): String = stringify(serializer<T>(), obj)

fun <T> parse(loader: DeserializationStrategy<T>, str: String): T
inline fun <reified T : Any> parse(str: String): T = parse(serializer<T>(), str)
```

`stringify` transforms object to string, `parse` parses. No surprises.

Besides this, functions `toJson` and `fromJson` allow converting @Serializable Kotlin object to and from [abstract JSON syntax tree](../runtime/commonMain/src/kotlinx/serialization/json/JsonElement.kt#L23). To build JSON AST from String, use `parseJson`.

You can also use one of predefined instances, like `Json.plain`, `Json.indented`, `Json.nonstrict` or `Json.unquoted`. API is duplicated in companion object, so `Json.parse(...)` equals to `Json.plain.parse(...)`.

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
[here](../runtime/testProto/test_data.proto) and [here](../runtime/jvmTest/src/kotlinx/serialization/formats/RandomTests.kt)

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

 ### Dynamic object serializer 
 
 Allows you to convert kotlin data structures into their dynamic JS representation.

```kotlin

@Serializable
data class Data(val a: Int)

@Serializable
open class DataWrapper(open val s: String, val d: Data?)

val wrapper = DataWrapper("foo", Data(42))
JSON.stringify(wrapper) // {"s_dsrefg$_0":"foo","d":{"a":42}}
val plainJS: dynamic = DynamicObjectSerializer().serialize(DataWrapper.serializer(), wrapper)
plainJS.s == wrapper.s // true
JSON.stringify(plainJS) // {"s":"foo","d":{"a":42}}
```


