# Serialization documentation and example cases

**Note**: Cases are presented here as a series of *unit-tests* using non-standard *unquoted* JSON for ease of presentation. Standards-compliant JSON is supported, too. Just replace `Json.unquoted` with plain `Json`.

## Supported properties

* Class constructor `val` and `var` properties. It is required for constructor to have only properties (no parameters).

    ```kotlin
    @Serializable
    data class Data(val a: Int, val b: Int)
    val data = Data(1, 2)

    // Serialize with internal serializer for Data class
    assertEquals("{a:1,b:2}", Json.unquoted.stringify(data))
    assertEquals(data, Json.parse<Data>("{a:1,b:2}"))

    // Serialize with external serializer for Data class
    @Serializer(forClass=Data::class)
    object ExtDataSerializer
    assertEquals("{a:1,b:2}", Json.unquoted.stringify(ExtDataSerializer, data))
    assertEquals(data, Json.parse(ExtDataSerializer, "{a:1,b:2}"))
    ```

 * In case of usage of **internal** serialization
 (`@Serializable` annotation on class), both body `val`s and `var`s are supported with any visibility levels.

    ```kotlin
    @Serializable
    class Data(val a: Int) {
        private val b: String = "42"

        override fun equals(other: Any?) = /*...*/
    }

    assertEquals("{a:1, b:42}", Json.unquoted.stringify(Data(1)))
    assertEquals(Data(1), Json.unquoted.parse<Data>("{a:1, b:42}"))
    ```

 * Property will be considered _optional_ if it has default value.

    ```kotlin
    @Serializable
    data class Data(val a: Int, val b: Int = 42)

    // Serialization and deserialization with internal serializer
    assertEquals("{a:0,b:42}",Json.unquoted.stringify(Data(0)))
    assertEquals(Json.unquoted.parse<Data>("{a:0,b:43}"),Data(b = 43))
    assertEquals(Json.unquoted.parse<Data>("{a:0,b:42}"),Data(0))
    assertEquals(Json.unquoted.parse<Data>("{a:0}"),Data(0))

    // This will throw SerializationException, because 'a' is missing.
    Json.unquoted.parse<Data>("{b:0}")
    ```

    > Tip: you can omit default values during serialization with
    `Json(encodeDefaults = false)` (see [here](runtime_usage#json)).


 * By default, only properties which have
 [backing fields](https://kotlinlang.org/docs/reference/properties.html#backing-fields)
 will be serialized and restored back.

    ```kotlin
    @Serializable
    data class Data(val a: Int) {
        private val b: String
            get() = "42"
    }

    // b is not in serialized form!
    assertEquals("{a:1}", Json.unquoted.stringify(Data(1)))
    ```

    You should be careful with this, especially when you have hierarchy of serializable classes with several overrides.

 * Moreover, if you have several properties with the same name and different backing fields
 (e.g. `open/override` pair), a compiler exception will be thrown. To resolve such conflicts, use `@SerialName` (see [below](#Annotations)).

 * Important note: In this case, body properties initializers and setters are not called. So, following approach would not work:

    ```kotlin
    @Serializable
    class Data(val a: String = "42") {
        val b: String = computeWithSideEffects()

        private fun computeWithSideEffects(): String {
            println("I'm a side effect")
            return "b"
        }
    }

    // prints nothing.
    val data = Json.unquoted.parse<Data>("{a: 100500, b: 10}")
    ```

* Initializers are called iff (if and only if) property is `@Transient` or optional and was not read (see below).

    ```kotlin
    @Serializable
    class Data(val a: String = "42") {
        val b: String = computeWithSideEffects()

        private fun computeWithSideEffects(): String {
            println("I'm a side effect")
            return "b"
        }
    }

    // prints "I'm a side effect" once.
    val data = Json.unquoted.parse<Data>("{a: 100500, b: 10}")
    val data = Json.unquoted.parse<Data>("{a: 100500}")
    ```

* *Common pattern*: Validation.

    Such classes are not serializable, because they have constructor parameters which are not properties:

    ```kotlin
    class Data(_a: Int) {
        val a: Int = if ( _a >= 0) _a else throw IllegalArgumentException()
    }
    ```

    They can be easily refactored to be used with `init` blocks. `init` blocks in internal deserialization, unlike initialization expressions, are always executed _after_ all variables have been set.

    ```kotlin
    @Serializable
    class Data(val a: Int) {
        init {
            check(a >= 0)
        }
    }
    ```

* **External** deserialization (annotation `@Serializer(forClass=...)`) has more limitations: it supports only primary constructor's vals/vars and class body `var` properties with visibility higher than protected.  Body `val`  properties and all private properties are unseen for external serializer/deserializer.
    It also invokes all setters on body `var`s and all initialization expressions with init blocks.

    It isn't supported yet in JavaScript.

    ```kotlin
    class Data {
        var a = 0
        var b = 0
        val unseen = 42
        override fun equals(other: Any?) = /*..*/
    }

    val data = Data().apply {
        a = 1
        b = 2
    }

    // Serialize with external serializer for Data class
    @Serializer(forClass=Data::class)
    object ExtDataSerializer

    assertEquals("{a:1,b:2}", Json.unquoted.stringify(ExtDataSerializer, data))
    assertEquals(data, Json.parse(ExtDataSerializer, "{a:1,b:2}"))
    ```

* Having both` @Serialiable class A` and `@Serializer(forClass=A::class)` is possible. In this case, object marked as serializer will try to deserialize class A internally, and some *strange effects* may happen. But it's not exactly.

## Annotations

* `@SerialName` annotation for overriding property name with custom name in formats with name support, like JSON.

    ```kotlin
    @Serializable
    data class Names(
            @SerialName("value1")
            val custom1: String,
            @SerialName("value2")
            val custom2: Int
    )

    assertEquals("{value1: a, value2: 42}", Json.unquoted.stringify(Names("a", 42)))
    ```

    > Starting from 0.6, `@SerialName` can be used on classes, too.

* `@Required` annotation for supported properties. It makes property with default value
still be mandatory and always present in serialized form.

    ```kotlin
    @Serializable
    class Data(@Required val a: Int = 0, val b: Int = 42) {
       var c = "Hello"

       override fun equals(other: Any?) = /*...*/
    }

    // Serialization and deserialization with internal serializer
    // External serializer also supported
    assertEquals("{a:0,b:42,c:Hello}",Json.unquoted.stringify(Data()))
    assertEquals(Json.unquoted.parse<Data>("{a:0,b:43,c:Hello}"),Data(b = 43))
    assertEquals(Json.unquoted.parse<Data>("{a:0,b:42,c:Hello}"),Data())
    assertEquals(Json.unquoted.parse<Data>("{a:0,c:Hello}"),Data())
    assertEquals(Json.unquoted.parse<Data>("{a:0}"),Data())

    // This will throw SerializationException, because 'a' is missing.
    Json.unquoted.parse<Data>("{b:0}")
    ```

* `@Transient` annotation for supported properties. This annotation excludes marked properties from process of serialization or deserialization. Requires default value. *Don't confuse with `kotlin.jvm.Transient`!*

    ```kotlin
    @Serializable
    class Data(val a: Int = 0, @Transient val b: Int = 42) {
        var c = "Hello"

        @Transient
        var d = "World"

        override fun equals(other: Any?) = /*...*/
    }

    // Serialization and deserialization with internal serializer
    // External serializer also supported
    assertEquals("{a:0,c:Hello}",Json.unquoted.stringify(Data()))
    assertEquals(Json.unquoted.parse<Data>("{a:0,c:Hello}"),Data())
    assertEquals(Json.unquoted.parse<Data>("{a:0}"),Data())


    // This will throw SerializationException, because
    // property 'b' is unknown to deserializer.
    Json.unquoted.parse<Data>("{a:0,b:100500,c:Hello}")
    ```

* Initializing `@Transient` or optional fields in init blocks is not supported.

    ```kotlin
    // This class is not serializable.
    class Data(val a: String = "42") {
        val b: String

        init {
            b = "b"
        }
    }
    ```

* Delegates are not supported and they're by default `@Transient` (since they do not have backing field), so this example works fine:

    ```kotlin
    @Serializable
    data class WithDelegates(val myMap: Map<String, String>) {

        // implicit @Transient
        val prop by myMap
    }

    assertEquals("value", Json.unquoted.parse<WithDelegates>("{myMap:{prop:value}}").prop)
    ```

## Nesting

* Nested values are recursively serialized, enums, primitive types, arrays, lists and maps are supported, plus other serializable classes.

    ```kotlin
    enum class TintEnum { LIGHT, DARK }

    @Serializable
    data class Data(
            val a: String,
            val b: List<Int>,
            val c: Map<String, TintEnum>
    )
    val data = Data("Str", listOf(1, 2), mapOf("lt" to TintEnum.LIGHT, "dk" to TintEnum.DARK))

    // Serialize with internal serializer for Data class
    assertEquals("{a:Str,b:[1,2],c:{lt:LIGHT,dk:DARK}}", Json.unquoted.stringify(data))
    assertEquals(data, Json.parse<Data>("{a:Str,b:[1,2],c:{lt:LIGHT,dk:DARK}}"))

    // Serialize with external serializer for Data class
    @Serializer(forClass=Data::class)
    object ExtDataSerializer
    assertEquals("{a:Str,b:[1,2],c:{lt:LIGHT,dk:DARK}}", Json.unquoted.stringify(ExtDataSerializer, data))
    assertEquals(data, Json.parse(ExtDataSerializer, "{a:Str,b:[1,2],c:{lt:LIGHT,dk:DARK}}"))
    ```

    To obtain serializers for root-level collections, you can use extension functions defined on serializers, like `.list` (see [this](https://github.com/Kotlin/kotlinx.serialization/issues/27) issue)

## User-defined serial annotations

In some cases, one may like to save additional format-specific information in the object itself. For example, protobuf field id.
For this purpose, you can define your own annotation class and annotate it with `@SerialInfo`:

```kotlin

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class ProtoId(val id: Int)

@Serializable
data class MyData(@ProtoId(2) val a: Int, @ProtoId(1) val b: String)
```
Note that it has to be explicitly targeted to property.

Inside a process of serialization/deserialization, they are available in `KSerialClassDesc` object:

```kotlin
override fun encodeElement(desc: SerialDescriptor, index: Int): Boolean {
            val id = desc.getElementAnnotations(index).filterIsInstance<ProtoId>().single().id
            ...
}
```

You can apply any number of annotations with any number of arguments.
**Limitations:** `@SerialInfo` annotation class properties must have one of the following types: primitive, String, enum, or primitive array (`IntArray`, `BooleanArray`, etc)

> Starting from 0.6, `@SerialInfo`-marked annotations can be used on classes, too. Use `.getEntityAnnotations()` method of `SerialDescriptor` to obtain them.
