/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.descriptors

import kotlinx.serialization.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.internal.*
import kotlin.reflect.*

/**
 * Builder for [SerialDescriptor].
 * The resulting descriptor will be uniquely identified by the given [serialName], [typeParameters] and
 * elements structure described in [builderAction] function.
 *
 * Example:
 * ```
 * // Class with custom serializer and custom serial descriptor
 * class Data(
 *     val intField: Int, // This field is ignored by custom serializer
 *     val longField: Long, // This field is written as long, but in serialized form is named as "_longField"
 *     val stringList: List<String> // This field is written as regular list of strings
 *     val nullableInt: Int?
 * )
 * // Descriptor for such class:
 * SerialDescriptor("my.package.Data") {
 *     // intField is deliberately ignored by serializer -- not present in the descriptor as well
 *     element<Long>("_longField") // longField is named as _longField
 *     element("stringField", listSerialDescriptor<String>())
 *     element("nullableInt", serialDescriptor<Int>().nullable)
 * }
 * ```
 *
 * Example for generic classes:
 * ```
 * @Serializable(CustomSerializer::class)
 * class BoxedList<T>(val list: List<T>)
 *
 * class CustomSerializer<T>(tSerializer: KSerializer<T>): KSerializer<BoxedList<T>> {
 *   // here we use tSerializer.descriptor because it represents T
 *   override val descriptor = SerialDescriptor("pkg.BoxedList", CLASS, tSerializer.descriptor) {
 *     // here we have to wrap it with List first, because property has type List<T>
 *     element("list", ListSerializer(tSerializer).descriptor) // or listSerialDescriptor(tSerializer.descriptor)
 *   }
 * }
 * ```
 */
@Suppress("FunctionName")
@OptIn(ExperimentalSerializationApi::class)
public fun buildClassSerialDescriptor(
    serialName: String,
    vararg typeParameters: SerialDescriptor,
    builderAction: ClassSerialDescriptorBuilder.() -> Unit = {}
): SerialDescriptor {
    require(serialName.isNotBlank()) { "Blank serial names are prohibited" }
    val sdBuilder = ClassSerialDescriptorBuilder(serialName)
    sdBuilder.builderAction()
    return SerialDescriptorImpl(
        serialName,
        StructureKind.CLASS,
        sdBuilder.elementNames.size,
        typeParameters.toList(),
        sdBuilder
    )
}

/**
 * Factory to create a trivial primitive descriptors.
 * Primitive descriptors should be used when the serialized form of the data has a primitive form, for example:
 * ```
 * object LongAsStringSerializer : KSerializer<Long> {
 *     override val descriptor: SerialDescriptor =
 *         PrimitiveDescriptor("kotlinx.serialization.LongAsStringSerializer", PrimitiveKind.STRING)
 *
 *     override fun serialize(encoder: Encoder, value: Long) {
 *         encoder.encodeString(value.toString())
 *     }
 *
 *     override fun deserialize(decoder: Decoder): Long {
 *         return decoder.decodeString().toLong()
 *     }
 * }
 * ```
 */
public fun PrimitiveSerialDescriptor(serialName: String, kind: PrimitiveKind): SerialDescriptor {
    require(serialName.isNotBlank()) { "Blank serial names are prohibited" }
    return PrimitiveDescriptorSafe(serialName, kind)
}

/**
 * Factory to create a new descriptor that is identical to [original] except that the name is equal to [serialName].
 * Should be used when you want to serialize a type as another non-primitive type.
 * Don't use this if you want to serialize a type as a primitive value, use [PrimitiveSerialDescriptor] instead.
 * 
 * Example:
 * ```
 * @Serializable(CustomSerializer::class)
 * class CustomType(val a: Int, val b: Int, val c: Int)
 *
 * class CustomSerializer: KSerializer<CustomType> {
 *     override val descriptor = SerialDescriptor("CustomType", IntArraySerializer().descriptor)
 *
 *     override fun serialize(encoder: Encoder, value: CustomType) {
 *         encoder.encodeSerializableValue(IntArraySerializer(), intArrayOf(value.a, value.b, value.c))
 *     }
 *
 *     override fun deserialize(decoder: Decoder): CustomType {
 *         val array = decoder.decodeSerializableValue(IntArraySerializer())
 *         return CustomType(array[0], array[1], array[2])
 *     }
 * }
 * ```
 */
@ExperimentalSerializationApi
public fun SerialDescriptor(serialName: String, original: SerialDescriptor): SerialDescriptor {
    require(serialName.isNotBlank()) { "Blank serial names are prohibited" }
    require(original.kind !is PrimitiveKind) { "For primitive descriptors please use 'PrimitiveSerialDescriptor' instead" }
    require(serialName != original.serialName) { "The name of the wrapped descriptor ($serialName) cannot be the same as the name of the original descriptor (${original.serialName})" }
    
    return WrappedSerialDescriptor(serialName, original)
}

@OptIn(ExperimentalSerializationApi::class)
internal class WrappedSerialDescriptor(override val serialName: String, original: SerialDescriptor) : SerialDescriptor by original

/**
 * An unsafe alternative to [buildClassSerialDescriptor] that supports an arbitrary [SerialKind].
 * This function is left public only for migration of pre-release users and is not intended to be used
 * as generally-safe and stable mechanism. Beware that it can produce inconsistent or non spec-compliant instances.
 *
 * If you end up using this builder, please file an issue with your use-case in kotlinx.serialization
 */
@InternalSerializationApi
@OptIn(ExperimentalSerializationApi::class)
public fun buildSerialDescriptor(
    serialName: String,
    kind: SerialKind,
    vararg typeParameters: SerialDescriptor,
    builder: ClassSerialDescriptorBuilder.() -> Unit = {}
): SerialDescriptor {
    require(serialName.isNotBlank()) { "Blank serial names are prohibited" }
    require(kind != StructureKind.CLASS) { "For StructureKind.CLASS please use 'buildClassSerialDescriptor' instead" }
    val sdBuilder = ClassSerialDescriptorBuilder(serialName)
    sdBuilder.builder()
    return SerialDescriptorImpl(serialName, kind, sdBuilder.elementNames.size, typeParameters.toList(), sdBuilder)
}


/**
 * Retrieves descriptor of type [T] using reified [serializer] function.
 */
public inline fun <reified T> serialDescriptor(): SerialDescriptor = serializer<T>().descriptor

/**
 * Retrieves descriptor of type associated with the given [KType][type]
 */
public fun serialDescriptor(type: KType): SerialDescriptor = serializer(type).descriptor

/**
 * Creates a descriptor for the type `List<T>` where `T` is the type associated with [elementDescriptor].
 */
@ExperimentalSerializationApi
public fun listSerialDescriptor(elementDescriptor: SerialDescriptor): SerialDescriptor {
    return ArrayListClassDesc(elementDescriptor)
}

/**
 * Creates a descriptor for the type `List<T>`.
 */
@ExperimentalSerializationApi
public inline fun <reified T> listSerialDescriptor(): SerialDescriptor {
    return listSerialDescriptor(serializer<T>().descriptor)
}

/**
 * Creates a descriptor for the type `Map<K, V>` where `K` and `V` are types
 * associated with [keyDescriptor] and [valueDescriptor] respectively.
 */
@ExperimentalSerializationApi
public fun mapSerialDescriptor(
    keyDescriptor: SerialDescriptor,
    valueDescriptor: SerialDescriptor
): SerialDescriptor {
    return HashMapClassDesc(keyDescriptor, valueDescriptor)
}

/**
 * Creates a descriptor for the type `Map<K, V>`.
 */
@ExperimentalSerializationApi
public inline fun <reified K, reified V> mapSerialDescriptor(): SerialDescriptor {
    return mapSerialDescriptor(serializer<K>().descriptor, serializer<V>().descriptor)
}

/**
 * Creates a descriptor for the type `Set<T>` where `T` is the type associated with [elementDescriptor].
 */
@ExperimentalSerializationApi
public fun setSerialDescriptor(elementDescriptor: SerialDescriptor): SerialDescriptor {
    return HashSetClassDesc(elementDescriptor)
}

/**
 * Creates a descriptor for the type `Set<T>`.
 */
@ExperimentalSerializationApi
public inline fun <reified T> setSerialDescriptor(): SerialDescriptor {
    return setSerialDescriptor(serializer<T>().descriptor)
}

/**
 * Returns new serial descriptor for the same type with [isNullable][SerialDescriptor.isNullable]
 * property set to `true`.
 */
@OptIn(ExperimentalSerializationApi::class)
public val SerialDescriptor.nullable: SerialDescriptor
    get() {
        if (this.isNullable) return this
        return SerialDescriptorForNullable(this)
    }

/**
 * Builder for [SerialDescriptor] for user-defined serializers.
 *
 * Both explicit builder functions and implicit (using reified type-parameters) are present and are equivalent.
 * For example, `element<Int?>("nullableIntField")` is indistinguishable from
 * `element("nullableIntField", IntSerializer.descriptor.nullable)` and
 * from `element("nullableIntField", descriptor<Int?>)`.
 *
 * Please refer to [SerialDescriptor] builder function for a complete example.
 */
public class ClassSerialDescriptorBuilder internal constructor(
    public val serialName: String
) {

    /**
     * Indicates that serializer associated with the current serial descriptor
     * support nullable types, meaning that it should declare nullable type
     * in its [KSerializer] type parameter and handle nulls during encoding and decoding.
     */
    @ExperimentalSerializationApi
    public var isNullable: Boolean = false

    /**
     * [Serial][SerialInfo] annotations on a target type.
     */
    @ExperimentalSerializationApi
    public var annotations: List<Annotation> = emptyList()

    internal val elementNames: MutableList<String> = ArrayList()
    private val uniqueNames: MutableSet<String> = HashSet()
    internal val elementDescriptors: MutableList<SerialDescriptor> = ArrayList()
    internal val elementAnnotations: MutableList<List<Annotation>> = ArrayList()
    internal val elementOptionality: MutableList<Boolean> = ArrayList()

    /**
     * Add an element with a given [name][elementName], [descriptor],
     * type annotations and optionality the resulting descriptor.
     *
     * Example of usage:
     * ```
     * class Data(
     *     val intField: Int? = null, // Optional, has default value
     *     @ProtoNumber(1) val longField: Long
     * )
     *
     * // Corresponding descriptor
     * SerialDescriptor("package.Data") {
     *     element<Int?>("intField", isOptional = true)
     *     element<Long>("longField", annotations = listOf(protoIdAnnotationInstance))
     * }
     * ```
     */
    public fun element(
        elementName: String,
        descriptor: SerialDescriptor,
        annotations: List<Annotation> = emptyList(),
        isOptional: Boolean = false
    ) {
        require(uniqueNames.add(elementName)) { "Element with name '$elementName' is already registered" }
        elementNames += elementName
        elementDescriptors += descriptor
        elementAnnotations += annotations
        elementOptionality += isOptional
    }
}

/**
 * A reified version of [element] function that
 * extract descriptor using `serializer<T>().descriptor` call with all the restrictions of `serializer<T>().descriptor`.
 */
public inline fun <reified T> ClassSerialDescriptorBuilder.element(
    elementName: String,
    annotations: List<Annotation> = emptyList(),
    isOptional: Boolean = false
) {
    val descriptor = serializer<T>().descriptor
    element(elementName, descriptor, annotations, isOptional)
}

@OptIn(ExperimentalSerializationApi::class)
internal class SerialDescriptorImpl(
    override val serialName: String,
    override val kind: SerialKind,
    override val elementsCount: Int,
    typeParameters: List<SerialDescriptor>,
    builder: ClassSerialDescriptorBuilder
) : SerialDescriptor, CachedNames {

    override val annotations: List<Annotation> = builder.annotations
    override val serialNames: Set<String> = builder.elementNames.toHashSet()

    private val elementNames: Array<String> = builder.elementNames.toTypedArray()
    private val elementDescriptors: Array<SerialDescriptor> = builder.elementDescriptors.compactArray()
    private val elementAnnotations: Array<List<Annotation>> = builder.elementAnnotations.toTypedArray()
    private val elementOptionality: BooleanArray = builder.elementOptionality.toBooleanArray()
    private val name2Index: Map<String, Int> = elementNames.withIndex().map { it.value to it.index }.toMap()
    private val typeParametersDescriptors: Array<SerialDescriptor> = typeParameters.compactArray()
    private val _hashCode: Int by lazy { hashCodeImpl(typeParametersDescriptors) }

    override fun getElementName(index: Int): String = elementNames.getChecked(index)
    override fun getElementIndex(name: String): Int = name2Index[name] ?: CompositeDecoder.UNKNOWN_NAME
    override fun getElementAnnotations(index: Int): List<Annotation> = elementAnnotations.getChecked(index)
    override fun getElementDescriptor(index: Int): SerialDescriptor = elementDescriptors.getChecked(index)
    override fun isElementOptional(index: Int): Boolean = elementOptionality.getChecked(index)

    override fun equals(other: Any?): Boolean =
        equalsImpl(other) { otherDescriptor: SerialDescriptorImpl ->
            typeParametersDescriptors.contentEquals(
                otherDescriptor.typeParametersDescriptors
            )
        }

    override fun hashCode(): Int = _hashCode

    override fun toString(): String {
        return (0 until elementsCount).joinToString(", ", prefix = "$serialName(", postfix = ")") {
            getElementName(it) + ": " + getElementDescriptor(it).serialName
        }
    }
}

