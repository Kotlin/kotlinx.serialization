/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.descriptors

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.encoding.*

/**
 * Serial descriptor is an inherent property of [KSerializer] that describes the structure of the serializable type.
 * The structure of the serializable type is not only the characteristic of the type itself, but also of the serializer as well,
 * meaning that one type can have multiple descriptors that have completely different structures.
 *
 * For example, the class `class Color(val rgb: Int)` can have multiple serializable representations,
 * such as `{"rgb": 255}`, `"#0000FF"`, `[0, 0, 255]` and `{"red": 0, "green": 0, "blue": 255}`.
 * Representations are determined by serializers, and each such serializer has its own descriptor that identifies
 * each structure in a distinguishable and format-agnostic manner.
 *
 * ### Structure
 * Serial descriptor is identified by its [name][serialName] and consists of a kind, potentially empty set of
 * children elements, and additional metadata.
 *
 * * [serialName] uniquely identifies the descriptor (and the corresponding serializer) for non-generic types.
 *   For generic types, the actual type substitution is omitted from the string representation, and the name
 *   identifies the family of the serializers without type substitutions. However, type substitution is accounted for
 *   in [equals] and [hashCode] operations, meaning that descriptors of generic classes with the same name but different type
 *   arguments are not equal to each other.
 *   [serialName] is typically used to specify the type of the target class during serialization of polymorphic and sealed
 *   classes, for observability and diagnostics.
 * * [Kind][SerialKind] defines what this descriptor represents: primitive, enum, object, collection, etc.
 * * Children elements are represented as serial descriptors as well and define the structure of the type's elements.
 * * Metadata carries additional information, such as [nullability][nullable], [optionality][isElementOptional]
 *   and [serial annotations][getElementAnnotations].
 *
 * ### Usages
 * There are two general usages of the descriptors: THE serialization process and serialization introspection.
 *
 * #### Serialization
 * Serial descriptor is used as a bridge between decoders/encoders and serializers.
 * When asking for a next element, the serializer provides an expected descriptor to the decoder, and,
 * based on the descriptor content, the decoder decides how to parse its input.
 * In JSON, for example, when the encoder is asked to encode the next element and this element
 * is a subtype of [List], the encoder receives a descriptor with [StructureKind.LIST] and, based on that,
 * first writes an opening square bracket before writing the content of the list.
 *
 * Serial descriptor _encapsulates_ the structure of the data, so serializers can be free from
 * format-specific details. `ListSerializer` knows nothing about JSON and square brackets, providing
 * only the structure of the data and delegating encoding decision to the format itself.
 *
 * #### Introspection
 * Another usage of a serial descriptor is type introspection without its serialization.
 * Introspection can be used to check whether the given serializable class complies the
 * corresponding scheme and to generate JSON or ProtoBuf schema from the given class.
 *
 * ### Indices
 * Serial descriptor API operates with children indices.
 * For the fixed-size structures, such as regular classes, index is represented by a value in
 * the range from zero to [elementsCount] and represent and index of the property in this class.
 * Consequently, primitives do not have children and their element count is zero.
 *
 * For collections and maps indices do not have a fixed bound. Regular collections descriptors usually
 * have one element (`T`, maps have two, one for keys and one for values), but potentially unlimited
 * number of actual children values. Valid indices range is not known statically,
 * and implementations of such a descriptor should provide consistent and unbounded names and indices.
 *
 * In practice, for regular classes it is allowed to invoke `getElement*(index)` methods
 * with an index from `0` to [elementsCount] range and the element at the particular index corresponds to the
 * serializable property at the given position.
 * For collections and maps, index parameter for `getElement*(index)` methods is effectively bounded
 * by the maximal number of collection/map elements.
 *
 * ### Thread-safety and mutability
 * Serial descriptor implementation should be immutable and, thus, thread-safe.
 *
 * ### Equality and caching
 * Serial descriptor can be used as a unique identifier for format-specific data or schemas and
 * this implies the following restrictions on its `equals` and `hashCode`:
 *
 * An [equals] implementation should use both [serialName] and elements structure.
 * Comparing [elementDescriptors] directly is discouraged,
 * because it may cause a stack overflow error, e.g., if a serializable class `T` contains elements of type `T`.
 * To avoid it, a serial descriptor implementation should compare only descriptors
 * of class' type parameters, in a way that `serializer<Box<Int>>().descriptor != serializer<Box<String>>().descriptor`.
 * If type parameters are equal, descriptor structure should be compared by using children elements
 * descriptors' [serialName]s, which correspond to class names
 * (do not confuse with elements' own names, which correspond to properties' names); and/or other [SerialDescriptor]
 * properties, such as [kind].
 * An example of [equals] implementation:
 * ```
 * if (this === other) return true
 * if (other::class != this::class) return false
 * if (serialName != other.serialName) return false
 * if (!typeParametersAreEqual(other)) return false
 * if (this.elementDescriptors().map { it.serialName } != other.elementDescriptors().map { it.serialName }) return false
 * return true
 * ```
 *
 * [hashCode] implementation should use the same properties for computing the result.
 *
 * ### User-defined serial descriptors
 * The best way to define a custom descriptor is to use [buildClassSerialDescriptor] builder function, where
 * for each serializable property the corresponding element is declared.
 *
 * Example:
 * ```
 * // Class with custom serializer and custom serial descriptor
 * class Data(
 *     val intField: Int, // This field is ignored by custom serializer
 *     val longField: Long, // This field is written as long, but in serialized form is named as "_longField"
 *     val stringList: List<String> // This field is written as regular list of strings
 * )
 *
 * // Descriptor for such class:
 * buildClassSerialDescriptor("my.package.Data") {
 *     // intField is deliberately ignored by serializer -- not present in the descriptor as well
 *     element<Long>("_longField") // longField is named as _longField
 *     element("stringField", listSerialDescriptor<String>())
 * }
 *
 * // Example of 'serialize' function for such descriptor
 * override fun serialize(encoder: Encoder, value: Data) {
 *     encoder.encodeStructure(descriptor) {
 *         encodeLongElement(descriptor, 0, value.longField) // Will be written as "_longField" because descriptor's child at index 0 says so
 *         encodeSerializableElement(descriptor, 1, ListSerializer(String.serializer()), value.stringList)
 *     }
 * }
 * ```
 *
 * For classes that are represented as a single primitive value, [PrimitiveSerialDescriptor] builder function can be used instead.
 *
 * ### Consistency violations
 * An implementation of [SerialDescriptor] should be consistent with the implementation of the corresponding [KSerializer].
 * Yet it is not type-checked statically, thus making it possible to declare a non-consistent implementation of descriptor and serializer.
 * In such cases, the behavior of an underlying format is unspecified and may lead to both runtime errors and encoding of
 * corrupted data that is impossible to decode back.
 *
 * ### Not for implementation
 *
 * `SerialDescriptor` interface should not be implemented in 3rd party libraries, as new methods
 * might be added to this interface when kotlinx.serialization adds support for new Kotlin features.
 * This interface is safe to use and construct via [buildClassSerialDescriptor], [PrimitiveSerialDescriptor], and `SerialDescriptor` factory function.
 */
@SubclassOptInRequired(SealedSerializationApi::class)
public interface SerialDescriptor {
    /**
     * Serial name of the descriptor that identifies a pair of the associated serializer and target class.
     *
     * For generated and default serializers, the serial name is equal to the corresponding class's fully qualified name
     * or, if overridden, [SerialName].
     * Custom serializers should provide a unique serial name that identifies both the serializable class and
     * the serializer itself, ignoring type arguments if they are present, for example: `my.package.LongAsTrimmedString`.
     *
     * Do not confuse with [getElementName], which returns property name:
     *
     * ```
     * package my.app
     *
     * @Serializable
     * class User(val name: String)
     *
     * val userDescriptor = User.serializer().descriptor
     *
     * userDescriptor.serialName // Returns "my.app.User"
     * userDescriptor.getElementName(0) // Returns "name"
     * ```
     */
    public val serialName: String

    /**
     * The kind of the serialized form that determines **the shape** of the serialized data.
     * Formats use serial kind to add and parse serializer-agnostic metadata to the result.
     *
     * For example, JSON format wraps [classes][StructureKind.CLASS] and [StructureKind.MAP] into
     * brackets, while ProtoBuf just serialize these types in separate ways.
     *
     * Kind should be consistent with the implementation, for example, if it is a [primitive][PrimitiveKind],
     * then its element count should be zero and vice versa.
     *
     * Example of introspecting kinds:
     *
     * ```
     * @Serializable
     * class User(val name: String)
     *
     * val userDescriptor = User.serializer().descriptor
     *
     * userDescriptor.kind // Returns StructureKind.CLASS
     * userDescriptor.getElementDescriptor(0).kind // Returns PrimitiveKind.STRING
     * ```
     */
    public val kind: SerialKind

    /**
     * Whether the descriptor describes a nullable type.
     * Returns `true` if associated serializer can serialize/deserialize nullable elements of the described type.
     *
     * Example:
     *
     * ```
     * @Serializable
     * class User(val name: String, val alias: String?)
     *
     * val userDescriptor = User.serializer().descriptor
     *
     * userDescriptor.isNullable // Returns false
     * userDescriptor.getElementDescriptor(0).isNullable // Returns false
     * userDescriptor.getElementDescriptor(1).isNullable // Returns true
     * ```
     */
    public val isNullable: Boolean get() = false

    /**
     * Returns `true` if this descriptor describes a serializable value class which underlying value
     * is serialized directly.
     *
     * This property is true for serializable `@JvmInline value` classes:
     * ```
     * @Serializable
     * class User(val name: Name)
     *
     * @Serializable
     * @JvmInline
     * value class Name(val value: String)
     *
     * User.serializer().descriptor.isInline // false
     * User.serializer().descriptor.getElementDescriptor(0).isInline // true
     * Name.serializer().descriptor.isInline // true
     * ```
     */
    public val isInline: Boolean get() = false

    /**
     * The number of elements this descriptor describes, besides from the class itself.
     * [elementsCount] describes the number of **semantic** elements, not the number
     * of actual fields/properties in the serialized form, even though they frequently match.
     *
     * For example, for the following class
     * `class Complex(val real: Long, val imaginary: Long)` the corresponding descriptor
     * and the serialized form both have two elements, while for `List<Int>`
     * the corresponding descriptor has a single element (`IntDescriptor`, the type of list element),
     * but from zero up to `Int.MAX_VALUE` values in the serialized form:
     *
     * ```
     * @Serializable
     * class Complex(val real: Long, val imaginary: Long)
     *
     * Complex.serializer().descriptor.elementsCount // Returns 2
     *
     * @Serializable
     * class OuterList(val list: List<Int>)
     *
     * OuterList.serializer().descriptor.getElementDescriptor(0).elementsCount // Returns 1
     * ```
     */
    public val elementsCount: Int

    /**
     * Returns serial annotations of the associated class.
     * Serial annotations can be used to specify additional metadata that may be used during serialization.
     * Only annotations marked with [SerialInfo] are added to the resulting list.
     *
     * Do not confuse with [getElementAnnotations]:
     * ```
     * @Serializable
     * @OnClassSerialAnnotation
     * class Nested(...)
     *
     * @Serializable
     * class Outer(@OnPropertySerialAnnotation val nested: Nested)
     *
     * val outerDescriptor = Outer.serializer().descriptor
     *
     * outerDescriptor.getElementAnnotations(0) // Returns [@OnPropertySerialAnnotation]
     * outerDescriptor.getElementDescriptor(0).annotations // Returns [@OnClassSerialAnnotation]
     * ```
     */
    public val annotations: List<Annotation> get() = emptyList()

    /**
     * Returns a positional name of the child at the given [index].
     * Positional name represents a corresponding property name in the class, associated with
     * the current descriptor.
     *
     * Do not confuse with [serialName], which returns class name:
     *
     * ```
     * package my.app
     *
     * @Serializable
     * class User(val name: String)
     *
     * val userDescriptor = User.serializer().descriptor
     *
     * userDescriptor.serialName // Returns "my.app.User"
     * userDescriptor.getElementName(0) // Returns "name"
     * ```
     *
     * @throws IndexOutOfBoundsException for an illegal [index] values.
     * @throws IllegalStateException if the current descriptor does not support children elements (e.g. is a primitive)
     */
    public fun getElementName(index: Int): String

    /**
     * Returns an index in the children list of the given element by its name or [CompositeDecoder.UNKNOWN_NAME]
     * if there is no such element.
     * The resulting index, if it is not [CompositeDecoder.UNKNOWN_NAME], is guaranteed to be usable with [getElementName].
     *
     * Example:
     *
     * ```
     * @Serializable
     * class User(val name: String, val alias: String?)
     *
     * val userDescriptor = User.serializer().descriptor
     *
     * userDescriptor.getElementIndex("name") // Returns 0
     * userDescriptor.getElementIndex("alias") // Returns 1
     * userDescriptor.getElementIndex("lastName") // Returns CompositeDecoder.UNKNOWN_NAME = -3
     * ```
     */
    public fun getElementIndex(name: String): Int

    /**
     * Returns serial annotations of the child element at the given [index].
     * This method differs from `getElementDescriptor(index).annotations` by reporting only
     * element-specific annotations:
     * ```
     * @Serializable
     * @OnClassSerialAnnotation
     * class Nested(...)
     *
     * @Serializable
     * class Outer(@OnPropertySerialAnnotation val nested: Nested)
     *
     * val outerDescriptor = Outer.serializer().descriptor
     *
     * outerDescriptor.getElementAnnotations(0) // Returns [@OnPropertySerialAnnotation]
     * outerDescriptor.getElementDescriptor(0).annotations // Returns [@OnClassSerialAnnotation]
     * ```
     * Only annotations marked with [SerialInfo] are added to the resulting list.
     *
     * @throws IndexOutOfBoundsException for an illegal [index] values.
     * @throws IllegalStateException if the current descriptor does not support children elements (e.g. is a primitive).
     */
    public fun getElementAnnotations(index: Int): List<Annotation>

    /**
     * Retrieves the descriptor of the child element for the given [index].
     * For the property of type `T` on the position `i`, `getElementDescriptor(i)` yields the same result
     * as for `T.serializer().descriptor`, if the serializer for this property is not explicitly overridden
     * with `@Serializable(with = ...`)`, [Polymorphic] or [Contextual].
     * This method can be used to completely introspect the type that the current descriptor describes.
     *
     * Example:
     * ```
     * @Serializable
     * @OnClassSerialAnnotation
     * class Nested(...)
     *
     * @Serializable
     * class Outer(val nested: Nested)
     *
     * val outerDescriptor = Outer.serializer().descriptor
     *
     * outerDescriptor.getElementDescriptor(0).serialName // Returns "Nested"
     * outerDescriptor.getElementDescriptor(0).annotations // Returns [@OnClassSerialAnnotation]
     * ```
     *
     * @throws IndexOutOfBoundsException for illegal [index] values.
     * @throws IllegalStateException if the current descriptor does not support children elements (e.g. is a primitive).
     */
    public fun getElementDescriptor(index: Int): SerialDescriptor

    /**
     * Whether the element at the given [index] is optional (can be absent in serialized form).
     * For generated descriptors, all elements that have a corresponding default parameter value are
     * marked as optional. Custom serializers can treat optional values in a serialization-specific manner
     * without a default parameters constraint.
     *
     * Example of optionality:
     * ```
     * @Serializable
     * class Holder(
     *     val a: Int, // isElementOptional(0) == false
     *     val b: Int?, // isElementOptional(1) == false
     *     val c: Int? = null, // isElementOptional(2) == true
     *     val d: List<Int>, // isElementOptional(3) == false
     *     val e: List<Int> = listOf(1), // isElementOptional(4) == true
     * )
     * ```
     * Returns `false` for valid indices of collections, maps, and enums.
     *
     * @throws IndexOutOfBoundsException for an illegal [index] values.
     * @throws IllegalStateException if the current descriptor does not support children elements (e.g. is a primitive).
     */
    public fun isElementOptional(index: Int): Boolean
}

/**
 * Returns an iterable of all descriptor [elements][SerialDescriptor.getElementDescriptor].
 *
 * Example:
 *
 * ```
 * @Serializable
 * class User(val name: String, val alias: String?)
 *
 * User.serializer().descriptor.elementDescriptors.toList() // Returns [PrimitiveDescriptor(kotlin.String), PrimitiveDescriptor(kotlin.String)?]
 * ```
 */
public val SerialDescriptor.elementDescriptors: Iterable<SerialDescriptor>
    get() = Iterable {
        object : Iterator<SerialDescriptor> {
            private var elementsLeft = elementsCount
            override fun hasNext(): Boolean = elementsLeft > 0

            override fun next(): SerialDescriptor {
                return getElementDescriptor(elementsCount - (elementsLeft--))
            }
        }
    }

/**
 * Returns an iterable of all descriptor [element names][SerialDescriptor.getElementName].
 *
 * Example:
 *
 * ```
 * @Serializable
 * class User(val name: String, val alias: String?)
 *
 * User.serializer().descriptor.elementNames.toList() // Returns ["name", "alias"]
 * ```
 */
public val SerialDescriptor.elementNames: Iterable<String>
    get() = Iterable {
        object : Iterator<String> {
            private var elementsLeft = elementsCount
            override fun hasNext(): Boolean = elementsLeft > 0

            override fun next(): String {
                return getElementName(elementsCount - (elementsLeft--))
            }
        }
    }
