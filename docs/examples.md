# Serialization documentation and example cases

**Note**: Cases are presented here as a series of *unit-tests* using non-standard *unquoted* JSON for ease of presentation. Standards-compliant JSON is supported, too. Just replace `JSON.unquoted` with plain `JSON`.

## Supported properties

* Class constructor `val` and `var` properties. It is required for constructor to have only properties (no parameters).

    ```kotlin
    @Serializable
    data class Data(val a: Int, val b: Int)
    val data = Data(1, 2)
    
    // Serialize with internal serializer for Data class
    assertEquals("{a:1,b:2}", JSON.unquoted.stringify(data))
    assertEquals(data, JSON.parse<Data>("{a:1,b:2}"))
    
    // Serialize with external serializer for Data class
    @Serializer(forClass=Data::class)
    object ExtDataSerializer
    assertEquals("{a:1,b:2}", JSON.unquoted.stringify(ExtDataSerializer, data))
    assertEquals(data, JSON.parse(ExtDataSerializer, "{a:1,b:2}"))
    ```

 * In case of usage of **internal** serialization (`@Serializable` annotation on class), both body `val`s and `var`s are supported with any visibility levels.
    
    ```kotlin
    @Serializable
    class Data(val a: Int) {
        private val b: String = "42"
    
        override fun equals(other: Any?) = /*...*/
    }
    
    assertEquals("{a:1, b:42}", JSON.unquoted.stringify(Data(1)))
    assertEquals(Data(1), JSON.unquoted.parse<Data>("{a:1, b:42}"))
    ```

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
    val data = JSON.unquoted.parse<Data>("{a: 100500, b: 10}")
    ```

* Initializers are called iff property is `@Transient` or `@Optional` and was not read (see below).
    
    ```kotlin
    @Serializable
    class Data(val a: String = "42") {
        @Optional
        val b: String = computeWithSideEffects()
    
        private fun computeWithSideEffects(): String {
            println("I'm a side effect")
            return "b" 
        }
    }
    
    // prints "I'm a side effect" once.
    val data = JSON.unquoted.parse<Data>("{a: 100500, b: 10}")
    val data = JSON.unquoted.parse<Data>("{a: 100500}")
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
    
    assertEquals("{a:1,b:2}", JSON.unquoted.stringify(ExtDataSerializer, data))
    assertEquals(data, JSON.parse(ExtDataSerializer, "{a:1,b:2}"))
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

    assertEquals("{value1: a, value2: 42}", JSON.unquoted.stringify(Names("a", 42)))
    ```

* `@Optional` annotation for supported properties. Note: `@Optional` constructor parameters require default values, but properties with default values without annotation are treated as required.
    
    ```kotlin
    @Serializable
    class Data(val a: Int = 0, @Optional val b: Int = 42) {
       @Optional
       var c = "Hello"
    
       override fun equals(other: Any?) = /*...*/
    }
    
    // Serialization and deserialization with internal serializer
    // External serializer also supported
    assertEquals("{a:0,b:42,c:Hello}",JSON.unquoted.stringify(Data()))
    assertEquals(JSON.unquoted.parse<Data>("{a:0,b:43,c:Hello}"),Data(b = 43))
    assertEquals(JSON.unquoted.parse<Data>("{a:0,b:42,c:Hello}"),Data())
    assertEquals(JSON.unquoted.parse<Data>("{a:0,c:Hello}"),Data())
    assertEquals(JSON.unquoted.parse<Data>("{a:0}"),Data())
    
    // This will throw SerializationException, because 'a' is missing.
    JSON.unquoted.parse<Data>("{b:0}")
    ```

* `@Transient` annotation for supported properties. This annotation excludes marked properties from process of serialization or deserialization. Requires default value. *Don't confuse with `kotlin.jvm.Transient`!*
    
    ```kotlin
    @Serializable
    class Data(val a: Int = 0, @Transient val b: Int = 42) {
        @Optional
        var c = "Hello"
    
        @Transient
        var d = "World"
    
        override fun equals(other: Any?) = /*...*/
    }
    
    // Serialization and deserialization with internal serializer
    // External serializaer also supported
    assertEquals("{a:0,c:Hello}",JSON.unquoted.stringify(Data()))
    assertEquals(JSON.unquoted.parse<Data>("{a:0,c:Hello}"),Data())
    assertEquals(JSON.unquoted.parse<Data>("{a:0}"),Data())
    
    
    // This will throw SerializationException, because 
    // property 'b' is unknown to deserializer.
    JSON.unquoted.parse<Data>("{a:0,b:100500,c:Hello}")
    ```

* Initializing `@Transient` or `@Optional` fields in init blocks is not supported.
    
    ```kotlin
    // This class is not serializable.
    class Data(val a: String = "42") {
        @Optional
        val b: String 
    
        init {
            b = "b"
        }
    }
    ```

* Delegates are not supported. But you can mark them as `@Transient` and they would be instantiated as usual. So this code works fine:
    
    ```kotlin
    @Serializable
    data class WithDelegates(val myMap: Map<String, String>) {
    
        @Transient
        val prop by myMap
    }
    
    assertEquals("value", JSON.unquoted.parse<WithDelegates>("{myMap:{prop:value}}").prop)
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
    assertEquals("{a:Str,b:[1,2],c:{lt:LIGHT,dk:DARK}}", JSON.unquoted.stringify(data))
    assertEquals(data, JSON.parse<Data>("{a:Str,b:[1,2],c:{lt:LIGHT,dk:DARK}}"))
    
    // Serialize with external serializer for Data class
    @Serializer(forClass=Data::class)
    object ExtDataSerializer
    assertEquals("{a:Str,b:[1,2],c:{lt:LIGHT,dk:DARK}}", JSON.unquoted.stringify(ExtDataSerializer, data))
    assertEquals(data, JSON.parse(ExtDataSerializer, "{a:Str,b:[1,2],c:{lt:LIGHT,dk:DARK}}"))
    ```

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
override fun writeElement(desc: KSerialClassDesc, index: Int): Boolean {
            val id = desc.getAnnotationsForIndex(index).filterIsInstance<ProtoId>().single().id
            ...
}
```

You can apply any number of annotations with any number of arguments.
**Limitations:** `@SerialInfo` annotation class properties must have one of the following types: primitive, String, enum, or primitive array (`IntArray`, `BooleanArray`, etc)