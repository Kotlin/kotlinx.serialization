# Serialization and inline classes (experimental, IR-specific)

This appendix describes how inline classes are handled by kotlinx.serialization.

> Features described in this document are currently [experimental](https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/compatibility.md#experimental-api)
> and are available only with IR compilers. Native targets use IR compiler by default;
> see documentation for [JS](https://kotlinlang.org/docs/reference/js-ir-compiler.html) and [JVM](https://kotlinlang.org/docs/reference/whatsnew14.html#new-jvm-ir-backend) to learn how to enable IR compilers.
> Inline classes themselves are an [Alpha](https://kotlinlang.org/docs/reference/inline-classes.html#alpha-status-of-inline-classes) Kotlin feature.

**Table of contents**

<!--- TOC -->

* [Serializable inline classes](#serializable-inline-classes)
* [Unsigned types support (JSON only)](#unsigned-types-support-json-only)
* [Using inline classes in your custom serializers](#using-inline-classes-in-your-custom-serializers)

<!--- END -->

## Serializable inline classes

We can mark inline class as serializable:

```kotlin
@Serializable
inline class Color(val rgb: Int)
```

Inline class in Kotlin is stored as its underlying type when possible (i.e. no boxing is required). 
Serialization framework makes does not impose any additional restriction and uses the underlying type where possible as well.

```kotlin
@Serializable
data class NamedColor(val color: Color, val name: String)

fun main() {
  println(Json.encodeToString(NamedColor(Color(0), "black")))
}
```

In this example, `NamedColor` is serialized as two primitives: `color: Int` and `name: String` without an allocation 
of `Color` class. When we run the example, encoding data with JSON format, we get the following
output:

```text
{"color": 0, "name": "black"}
```

As we see, `Color` class is not included during the encoding, only its underlying data. This invariant holds even if the actual inline class
is [allocated](https://kotlinlang.org/docs/reference/inline-classes.html#representation) — for example, when inline
class is used as a generic type argument:

```kotlin
@Serializable
class Palette(val colors: List<Color>)

fun main() {
  println(Json.encodeToString(Palette(listOf(Color(0), Color(255), Color(128)))))
}
```

The snippet produces the following output:

```text
{"colors":[0, 255, 128]}
```

## Unsigned types support (JSON only)

Kotlin standard library provides ready-to-use unsigned arithmetics, leveraging inline classes
to represent unsigned types: `UByte`, `UShort`, `UInt` and `ULong`.
[Json] format has built-in support for them: these types are serialized as theirs string
representations in unsigned form.
These types are handled as regular serializable types by the compiler plugin and can be freely used in serializable classes:

```kotlin
@Serializable
class Counter(val counted: UByte, val description: String)

fun main() {
    val counted = 239.toUByte()
    println(Json.encodeToString(Counter(counted, "tries")))
}
```

The output is following:

```text
{"counted":239,"description":"tries"}
```

> Unsigned types are currently unsupported in Protobuf and CBOR, but we plan to add them later.

## Using inline classes in your custom serializers

Let's return to our `NamedColor` example and try to write a custom serializer for it. Normally, as shown
in [Hand-written composite serializer](serializers.md#hand-written-composite-serializer), we would write the following code
in `serialize` method:

```kotlin
override fun serialize(encoder: Encoder, value: NamedColor) {
  encoder.beginStructure(descriptor) {
    encodeSerializableElement(descriptor, 0, Color.serializer(), value.color)
    encodeStringElement(descriptor, 1, value.name)
  }
}
```

However, since `Color` is used as a type argument in [encodeSerializableElement][CompositeEncoder.encodeSerializableElement] function, `value.color` will be boxed
to `Color` wrapper before passing it to the function, preventing the inline class optimization. To avoid this, we can use
special [encodeInlineElement][CompositeEncoder.encodeInlineElement] function instead. It uses [serial descriptor][SerialDescriptor] of `Color` ([retrieved][SerialDescriptor.getElementDescriptor] from serial descriptor of `NamedColor`) instead of [KSerializer],
does not have type parameters and does not accept any values. Instead, it returns [Encoder]. Using it, we can encode
unboxed value:

```kotlin
override fun serialize(encoder: Encoder, value: NamedColor) {
  encoder.beginStructure(descriptor) {
    encodeInlineElement(descriptor, 0).encodeInt(value.color)
    encodeStringElement(descriptor, 1, value.name)
  }
}
```

The same principle goes also with [CompositeDecoder]: it has [decodeInlineElement][CompositeDecoder.decodeInlineElement] function that returns [Decoder].

If your class should be represented as a primitive (as shown in [Primitive serializer](serializers.md#primitive-serializer) section),
and you cannot use [beginStructure][Encoder.beginStructure] function, there is a complementary function in [Encoder] called [encodeInline][Encoder.encodeInline].
We will use it to show an example how one can represent a class as an unsigned integer.

Let's start with a UID class:

```kotlin
@Serializable(UIDSerializer::class)
class UID(val uid: Int)
```

`uid` type is `Int`, but suppose we want it to be an unsigned integer in JSON. We can start writing the
following custom serializer:

```kotlin
object UIDSerializer: KSerializer<UID> {
  override val descriptor = UInt.serializer().descriptor
}
```

Note that we are using here descriptor from `UInt.serializer()` — it means that the class' representation looks like a
UInt's one.

Then the `serialize` method:

```kotlin
override fun serialize(encoder: Encoder, value: UID) {
  encoder.encodeInline(descriptor).encodeInt(value.uid)
}
```

That's where the magic happens — despite we called a regular [encodeInt][Encoder.encodeInt] with a `uid: Int` argument, the output will contain
an unsigned int because of the special encoder from `encodeInline` function. Since JSON format supports unsigned integers, it
recognizes theirs descriptors when they're passed into `encodeInline` and handles consecutive calls as for unsigned integers.

The `deserialize` method looks symmetrically:

```kotlin
override fun deserialize(decoder: Decoder): UID {
  return UID(decoder.decodeInline(descriptor).decodeInt())
}
```

> Disclaimer: You can also write such a serializer for inline class itself (imagine UID being the inline class — there's no need to change anything in the serializer).
> However, do not use anything in custom serializers for inline classes besides `encodeInline`. As we discussed, calls to inline class serializer may be
> optimized and replaced with a `encodeInlineElement` calls.
> `encodeInline` and `encodeInlineElement` calls with the same descriptor are considered equivalent and can be replaced with each other — formats should return the same `Encoder`.
> If you embed custom logic in custom inline class serializer, you may get different results depending on whether this serializer was called at all
> (and this, in turn, depends on whether inline class was boxed or not).

---

<!--- MODULE /kotlinx-serialization-core -->
<!--- INDEX kotlinx-serialization-core/kotlinx.serialization -->

[KSerializer]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization/-k-serializer/index.html

<!--- INDEX kotlinx-serialization-core/kotlinx.serialization.encoding -->

[CompositeEncoder.encodeSerializableElement]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-composite-encoder/encode-serializable-element.html
[CompositeEncoder.encodeInlineElement]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-composite-encoder/encode-inline-element.html
[Encoder]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-encoder/index.html
[CompositeDecoder]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-composite-decoder/index.html
[CompositeDecoder.decodeInlineElement]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-composite-decoder/decode-inline-element.html
[Decoder]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-decoder/index.html
[Encoder.beginStructure]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-encoder/begin-structure.html
[Encoder.encodeInline]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-encoder/encode-inline.html
[Encoder.encodeInt]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.encoding/-encoder/encode-int.html

<!--- INDEX kotlinx-serialization-core/kotlinx.serialization.descriptors -->

[SerialDescriptor]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.descriptors/-serial-descriptor/index.html
[SerialDescriptor.getElementDescriptor]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization-core/kotlinx.serialization.descriptors/-serial-descriptor/get-element-descriptor.html

<!--- MODULE /kotlinx-serialization-json -->
<!--- INDEX kotlinx-serialization-json/kotlinx.serialization.json -->

[Json]: https://kotlin.github.io/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json/index.html

<!--- END -->
