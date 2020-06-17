/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.PrimitiveKind.*
import kotlinx.serialization.StructureKind.*
import kotlinx.serialization.modules.*

/**
 * Serial kind is an intrinsic property of [SerialDescriptor] that indicates how
 * the corresponding type is structurally represented by its serializer.
 *
 * Kind is used by serialization formats to determine how exactly the given type
 * should be serialized. For example, JSON format detects the kind of the value and,
 * depending on that, may write it as a plain value for primitive kinds, open a
 * curly brace '{' for class-like structures and square bracket '[' for list- and array- like structures.
 *
 * Kinds are used both during serialization, to serialize a value properly and statically, and
 * to introspect the type structure or build serialization schema.
 *
 * Kind should match the structure of the serialized form, not the structure of the corresponding Kotlin class.
 * Meaning that if serializable class `class IntPair(val left: Int, val right: Int)` is represented by the serializer
 * as a single `Long` value, its descriptor should have [PrimitiveKind.LONG] without nested elements even though the class itself
 * represents a structure with two primitive fields.
 */
public sealed class SerialKind {
    override fun toString(): String {
        // KNPE should never happen, because SerialKind is sealed and all inheritors are non-anonymous
        return this::class.simpleName()!!
    }
}

/**
 * Values of primitive kinds usually are represented as a single value.
 * All default serializers for Kotlin [primitives types](https://kotlinlang.org/docs/tutorials/kotlin-for-py/primitive-data-types-and-their-limitations.html)
 * and [String] have primitive kind.
 *
 * ### Serializers interaction
 *
 * Serialization formats typically handle these kinds by calling a corresponding primitive method on encoder or decoder.
 * For example, if the following serializable class `class Color(val red: Byte, val green: Byte, val blue: Byte)` is represented by your serializer
 * as a single [Int] value, a typical serializer will serialize its value in the following manner:
 * ```
 * val intValue = color.rgbToInt()
 * encoder.encodeInt(intValue)
 * ```
 * and a corresponding [Decoder] counterpart.
 *
 * ### Implementation note
 *
 * Serial descriptors for primitive kinds are not expected to have any nested elements, thus its element count should be zero.
 * If a class is represented as a primitive value, its corresponding serial name *should not* be equal to the corresponding primitive type name.
 * For the `Color` example, represented as single [Int], its descriptor should have [INT] kind, zero elements and serial name **not equals**
 * to `kotlin.Int`: `PrimitiveDescriptor("my.package.ColorAsInt", PrimitiveKind.INT)`
 */
public sealed class PrimitiveKind : SerialKind() {
    /**
     * Primitive kind that represents a boolean `true`/`false` value.
     * Corresponding Kotlin primitive is [Boolean].
     * Corresponding encoder and decoder methods are [Encoder.encodeBoolean] and [Decoder.decodeBoolean].
     */
    public object BOOLEAN : PrimitiveKind()

    /**
     * Primitive kind that represents a single byte value.
     * Corresponding Kotlin primitive is [Byte].
     * Corresponding encoder and decoder methods are [Encoder.encodeByte] and [Decoder.decodeByte].
     */
    public object BYTE : PrimitiveKind()

    /**
     * Primitive kind that represents a 16-bit unicode character value.
     * Corresponding Kotlin primitive is [Char].
     * Corresponding encoder and decoder methods are [Encoder.encodeChar] and [Decoder.decodeChar].
     */
    public object CHAR : PrimitiveKind()

    /**
     * Primitive kind that represents a 16-bit short value.
     * Corresponding Kotlin primitive is [Short].
     * Corresponding encoder and decoder methods are [Encoder.encodeShort] and [Decoder.decodeShort].
     */
    public object SHORT : PrimitiveKind()

    /**
     * Primitive kind that represents a 32-bit int value.
     * Corresponding Kotlin primitive is [Int].
     * Corresponding encoder and decoder methods are [Encoder.encodeInt] and [Decoder.decodeInt].
     */
    public object INT : PrimitiveKind()

    /**
     * Primitive kind that represents a 64-bit long value.
     * Corresponding Kotlin primitive is [Long].
     * Corresponding encoder and decoder methods are [Encoder.encodeLong] and [Decoder.decodeLong].
     */
    public object LONG : PrimitiveKind()

    /**
     * Primitive kind that represents a 32-bit IEEE 754 floating point value.
     * Corresponding Kotlin primitive is [Float].
     * Corresponding encoder and decoder methods are [Encoder.encodeFloat] and [Decoder.decodeFloat].
     */
    public object FLOAT : PrimitiveKind()

    /**
     * Primitive kind that represents a 64-bit IEEE 754 floating point value.
     * Corresponding Kotlin primitive is [Double].
     * Corresponding encoder and decoder methods are [Encoder.encodeDouble] and [Decoder.decodeDouble].
     */
    public object DOUBLE : PrimitiveKind()

    /**
     * Primitive kind that represents a string value.
     * Corresponding Kotlin primitive is [String].
     * Corresponding encoder and decoder methods are [Encoder.encodeString] and [Decoder.decodeString].
     */
    public object STRING : PrimitiveKind()
}

/**
 * Structure kind represents values with composite structure of nested elements of depth and arbitrary number.
 * We acknowledge following structured kinds:
 *
 * ### Regular classes
 * The most common case for serialization, that represents an arbitrary structure with fixed count of elements.
 * When the regular Kotlin class is marked as [Serializable], its descriptor kind will be [CLASS].
 *
 * ### Lists
 * [LIST] represent a structure with potentially unknown in advance number of elements of the same type.
 * All standard serializable [List] implementors and arrays are represented as [LIST] kind of the same type.
 *
 * ### Maps
 * [MAP] represent a structure with potentially unknown in advance number of key-value pairs of the same type.
 * All standard serializable [Map] implementors are represented as [Map] kind of the same type.
 *
 * ### Kotlin objects
 * A singleton object defined with `object` keyword with an [OBJECT] kind.
 * By default, objects are serialized as empty structures without any states and their identity is preserved
 * across serialization within the same process, so you always have the same instance of the object.
 *
 * ### Serializers interaction
 * Serialization formats typically handle these kinds by marking structure start and end.
 * E.g. the following serializable class `class IntHolder(myValue: Int)` of structure kind [CLASS] is handled by
 * serializer as the following call sequence:
 * ```
 * val composite = encoder.beginStructure(descriptor) // Denotes the start of the structure
 * composite.encodeIntElement(descriptor, index = 0, holder.myValue)
 * composite.endStructure(descriptor) // Denotes the end of the structure
 * ```
 * and its corresponding [Decoder] counterpart.
 *
 * ### Serial descriptor implementors note
 * These kinds can be used not only for collection and regular classes.
 * For example, provided serializer for [Map.Entry] represents it as [Map] type, so it is serialized
 * as `{"actualKey": "actualValue"}` map directly instead of `{"key": "actualKey", "value": "actualValue"}`
 */
public sealed class StructureKind : SerialKind() {

    /**
     * Structure kind for regular classes with an arbitrary, but known statically, structure.
     * Serializers typically encode classes with calls to [Encoder.beginStructure] and [CompositeEncoder.endStructure],
     * writing the elements of the class between these calls.
     */
    public object CLASS : StructureKind()

    /**
     * Structure kind for lists and arrays of an arbitrary length.
     * Serializers typically encode classes with calls to [Encoder.beginCollection] and [CompositeEncoder.endStructure],
     * writing the elements of the list between these calls.
     * Built-in list serializers treat elements as homogeneous, though application-specific serializers may impose
     * application-specific restrictions on specific [LIST] types.
     *
     * Example of such application-specific serialization may be class `class ListOfThreeElements() : List<Any>`,
     * for which an author of the serializer knows that while it is `List<Any>`, in fact, is always has three elements
     * of a known type (e.g. the first is always a string, the second one is always an int etc.)
     */
    public object LIST : StructureKind()

    /**
     * Structure kind for maps of an arbitrary length.
     * Serializers typically encode classes with calls to [Encoder.beginCollection] and [CompositeEncoder.endStructure],
     * writing the elements of the map between these calls.
     *
     * Built-in map serializers treat elements as homogeneous, though application-specific serializers may impose
     * application-specific restrictions on specific [MAP] types.
     */
    public object MAP : StructureKind()

    /**
     * Structure kind for singleton objects defined with `object` keyword.
     * By default, objects are serialized as empty structures without any state and their identity is preserved
     * across serialization within the same process, so you always have the same instance of the object.
     *
     * Empty structure is represented as a call to [Encoder.beginStructure] with the following [CompositeEncoder.endStructure]
     * without any intermediate encodings.
     */
    public object OBJECT : StructureKind()
}

/**
 * Union structure kind represents a [tagged union][https://en.wikipedia.org/wiki/Tagged_union] structure,
 * meaning that the type is represent by one of a multiple possible values (potentially unknown).
 * An example of such union kind can be enum or its derivatives, such as "one of known strings".
 */
public sealed class UnionKind : SerialKind() {

    /**
     * Represents a Kotlin [Enum] with statically known values.
     * All enum values should be enumerated in descriptor elements.
     * Each element descriptor of a [Enum] kind represents an instance of a particular enum
     * and has an [StructureKind.OBJECT] kind.
     * Each [positional name][SerialDescriptor.getElementName] contains a corresponding enum element [name][Enum.name].
     *
     * Corresponding encoder and decoder methods are [Encoder.encodeEnum] and [Decoder.decodeEnum].
     */
    public object ENUM_KIND : UnionKind() // https://github.com/JetBrains/kotlin-native/issues/1447

    /**
     * Represents an "unknown" type that will be known only at the moment of the serialization.
     * Effectively it defers the choice of the serializer to a moment of the serialization, and can
     * be used for [contextual][ContextualSerialization] serialization.
     *
     * To introspect descriptor of this kind, an instance of [SerialModule] is required.
     * See [capturedKClass] extension property for more details.
     * However, if possible options are known statically (e.g. for sealed classes), they can be
     * enumerated in child descriptors similarly to [ENUM_KIND].
     */
    public object CONTEXTUAL : UnionKind()
}

/**
 * Polymorphic kind represents a (bounded) polymorphic value, that is referred
 * by some base class or interface, but its structure is defined by one of the possible implementations.
 * Polymorphic kind is, by its definition, a union kind and is extracted to its own subtype to emphasize
 * bounded and sealed polymorphism common property: not knowing the actual type statically and requiring
 * formats to additionally encode it.
 */
public sealed class PolymorphicKind : SerialKind() {
    /**
     * Sealed kind represents Kotlin sealed classes, where all subclasses are known statically at the moment of declaration.
     * [SealedClassSerializer] can be used as an example of sealed serialization.
     */
    public object SEALED : PolymorphicKind()

    /**
     * Open polymorphic kind represents statically unknown type that is hidden behind a given base class or interface.
     * [PolymorphicSerializer] can be used as an example of polymorphic serialization.
     *
     * Due to security concerns and typical mistakes that arises from polymorphic serialization, by default
     * `kotlinx.serialization` provides only bounded polymorphic serialization, forcing users to register all possible
     * serializers for a given base class or interface.
     *
     * To introspect descriptor of this kind (e.g. list possible subclasses), an instance of [SerialModule] is required.
     * See [capturedKClass] extension property for more details.
     */
    public object OPEN : PolymorphicKind()
}

