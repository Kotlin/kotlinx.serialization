/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.internal.*

/**
 * Builder for [SerialDescriptor].
 * The resulting descriptor will be uniquely identified by the given [serialName],
 * with the corresponding [kind] and structure described in [builder] function.
 * The count of descriptor elements should be known in advance to make API less error-prone,
 * and this builder will throw [IllegalStateException] if count of added elements will be
 * lesser or greater than [elementsCount].
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
 * SerialDescriptor("my.package.Data", 3) {
 *     // intField is deliberately ignored by serializer -- not present in the descriptor as well
 *     element<Long>("_longField") // longField is named as _longField
 *     element("stringField", listDescriptor<String>())
 * }
 * ```
 */
public fun SerialDescriptor(
    serialName: String,
    kind: SerialKind = StructureKind.CLASS,
    builder: SerialDescriptorBuilder.() -> Unit
): SerialDescriptor {
    val sdBuilder = SerialDescriptorBuilder(serialName)
    sdBuilder.builder()
    return SerialDescriptorImpl(serialName, kind, sdBuilder.elementNames.size, sdBuilder)
}

/**
 * Returns new serial descriptor for the same type with [isNullable][SerialDescriptor.isNullable]
 * property set to `true`.
 */
public val SerialDescriptor.nullable: SerialDescriptor
    get() {
        if (this.isNullable) return this
        return SerialDescriptorForNullable(this)
    }

/**
 * Builder for [SerialDescriptor].
 * Both explicit builder functions and implicit (using `typeOf`) are present and
 * are equivalent.
 * For example, `element<Int?>("nullableIntField")` is indistinguishable from
 * `element("nullableIntField", IntSerializer.descriptor.nullable)` and
 * from `element("nullableIntField", descriptor<Int?>)`.
 *
 * Please refer to [SerialDescriptor] builder function for a complete example.
 */
public class SerialDescriptorBuilder internal constructor(
    public val serialName: String
) {
    /**
     * Whether the resulting descriptor represents [nullable][SerialDescriptor.isNullable] type
     */
    public var isNullable: Boolean = false

    /**
     * [Serial][SerialInfo] annotations on a target type.
     */
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
     *     @ProtoId(1) val longField: Long
     * )
     *
     * // Corresponding descriptor
     * SerialDescriptor("package.Data", 2) {
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
        if (!uniqueNames.add(elementName)) {
            error("Element with name '$elementName' is already registered")
        }
        elementNames += elementName
        elementDescriptors += descriptor
        elementAnnotations += annotations
        elementOptionality += isOptional
    }

    /**
     * Reified version of [element] function that
     * extract descriptor using `serializer<T>().descriptor` call with the all the restrictions of `serializer<T>().descriptor`.
     */
    @ImplicitReflectionSerializer
    public inline fun <reified T> element(
        elementName: String,
        annotations: List<Annotation> = emptyList(),
        isOptional: Boolean = false
    ) {
        val descriptor = serializer<T>().descriptor
        element(elementName, descriptor, annotations, isOptional)
    }
}

// All these methods are extensions to avoid top-level scope pollution

/**
 * Retrieves descriptor of type [T] using reified [serializer] function.
 */
@ImplicitReflectionSerializer
public inline fun <reified T> SerialDescriptorBuilder.descriptor(): SerialDescriptor = serializer<T>().descriptor

/**
 * Creates descriptor for the type `List<T>` where `T` is the type associated with [typeDescriptor].
 */
public fun SerialDescriptorBuilder.listDescriptor(typeDescriptor: SerialDescriptor): SerialDescriptor {
    return ArrayListClassDesc(typeDescriptor)
}
/**
 * Creates descriptor for the type `List<T>`.
 */
@ImplicitReflectionSerializer
public inline fun <reified T> SerialDescriptorBuilder.listDescriptor(): SerialDescriptor {
    return ArrayListClassDesc(serializer<T>().descriptor)
}

/**
 * Creates descriptor for the type `Map<K, V>` where `K` and `V` are types
 * associated with [keyDescriptor] and [valueDescriptor] respectively.
 */
public fun SerialDescriptorBuilder.mapDescriptor(
    keyDescriptor: SerialDescriptor,
    valueDescriptor: SerialDescriptor
): SerialDescriptor {
    return HashMapClassDesc(keyDescriptor, valueDescriptor)
}

/**
 * Creates descriptor for the type `Map<K, V>`.
 */
@ImplicitReflectionSerializer
public inline fun <reified K, reified V> SerialDescriptorBuilder.mapDescriptor(): SerialDescriptor {
    return HashMapClassDesc(serializer<K>().descriptor, serializer<V>().descriptor)
}

/**
 * Creates descriptor for the type `Set<T>` where `T` is the type associated with [typeDescriptor].
 */
public fun SerialDescriptorBuilder.setDescriptor(typeDescriptor: SerialDescriptor): SerialDescriptor {
    return HashSetClassDesc(typeDescriptor)
}

/**
 * Creates descriptor for the type `Set<T>`.
 */
@ImplicitReflectionSerializer
public inline fun <reified T> SerialDescriptorBuilder.setDescriptor(): SerialDescriptor {
    return HashSetClassDesc(serializer<T>().descriptor)
}

private class SerialDescriptorImpl(
    override val serialName: String,
    override val kind: SerialKind,
    override val elementsCount: Int,
    builder: SerialDescriptorBuilder
) : SerialDescriptor {

    public override val isNullable: Boolean = builder.isNullable
    public override val annotations: List<Annotation> = builder.annotations

    private val elementNames: Array<String> = builder.elementNames.toTypedArray()
    private val elementDescriptors: Array<SerialDescriptor> = builder.elementDescriptors.toTypedArray()
    private val elementAnnotations: Array<List<Annotation>> = builder.elementAnnotations.toTypedArray()
    private val elementOptionality: BooleanArray = builder.elementOptionality.toBooleanArray()
    private val name2Index: Map<String, Int> = elementNames.withIndex().map { it.value to it.index }.toMap()

    override fun getElementName(index: Int): String = elementNames.getChecked(index)
    override fun getElementIndex(name: String): Int = name2Index[name] ?: CompositeDecoder.UNKNOWN_NAME
    override fun getElementAnnotations(index: Int): List<Annotation> = elementAnnotations.getChecked(index)
    override fun getElementDescriptor(index: Int): SerialDescriptor = elementDescriptors.getChecked(index)
    override fun isElementOptional(index: Int): Boolean = elementOptionality.getChecked(index)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as SerialDescriptorImpl
        if (serialName != other.serialName) return false
        return true
    }

    override fun hashCode(): Int {
        return serialName.hashCode()
    }

    override fun toString(): String {
        return (0 until elementsCount).joinToString(", ", prefix = "$serialName(", postfix = ")") {
            getElementName(it) + ": " + getElementDescriptor(it).serialName
        }
    }
}
