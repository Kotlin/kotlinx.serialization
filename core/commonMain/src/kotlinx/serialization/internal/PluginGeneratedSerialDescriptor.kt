/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE", "UNUSED")

package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.CompositeDecoder.Companion.UNKNOWN_NAME

/**
 * Implementation that plugin uses to implement descriptors for auto-generated serializers.
 */
@PublishedApi
@OptIn(ExperimentalSerializationApi::class)
internal open class PluginGeneratedSerialDescriptor(
    override val serialName: String,
    private val generatedSerializer: GeneratedSerializer<*>? = null,
    final override val elementsCount: Int
) : SerialDescriptor, CachedNames {
    override val kind: SerialKind get() = StructureKind.CLASS
    override val annotations: List<Annotation> get() = classAnnotations ?: emptyList()

    private var added = -1
    private val names = Array(elementsCount) { "[UNINITIALIZED]" }
    private val propertiesAnnotations = arrayOfNulls<MutableList<Annotation>?>(elementsCount)

    // Classes rarely have annotations, so we can save up a bit of allocations here
    private var classAnnotations: MutableList<Annotation>? = null
    private val elementsOptionality = BooleanArray(elementsCount)
    public override val serialNames: Set<String> get() = indices.keys

    // don't change lazy mode: KT-32871, KT-32872
    private val indices: Map<String, Int> by lazy { buildIndices() }
    // Cache child serializers, they are not cached by the implementation for nullable types
    private val childSerializers by lazy { generatedSerializer?.childSerializers() ?: emptyArray() }

    // Lazy because of JS specific initialization order (#789)
    internal val typeParameterDescriptors: Array<SerialDescriptor> by lazy {
        generatedSerializer?.typeParametersSerializers()?.map { it.descriptor }.compactArray()
    }

    // Can be without synchronization but Native will likely break due to freezing
    private val _hashCode: Int by lazy { hashCodeImpl(typeParameterDescriptors) }

    public fun addElement(name: String, isOptional: Boolean = false) {
        names[++added] = name
        elementsOptionality[added] = isOptional
        propertiesAnnotations[added] = null
    }

    public fun pushAnnotation(annotation: Annotation) {
        val list = propertiesAnnotations[added].let {
            if (it == null) {
                val result = ArrayList<Annotation>(1)
                propertiesAnnotations[added] = result
                result
            } else {
                it
            }
        }
        list.add(annotation)
    }

    public fun pushClassAnnotation(a: Annotation) {
        if (classAnnotations == null) {
            classAnnotations = ArrayList(1)
        }
        classAnnotations!!.add(a)
    }

    override fun getElementDescriptor(index: Int): SerialDescriptor {
        return childSerializers.getChecked(index).descriptor
    }

    override fun isElementOptional(index: Int): Boolean = elementsOptionality.getChecked(index)
    override fun getElementAnnotations(index: Int): List<Annotation> =
        propertiesAnnotations.getChecked(index) ?: emptyList()
    override fun getElementName(index: Int): String = names.getChecked(index)
    override fun getElementIndex(name: String): Int = indices[name] ?: UNKNOWN_NAME

    private fun buildIndices(): Map<String, Int> {
        val indices = HashMap<String, Int>()
        for (i in names.indices) {
            indices[names[i]] = i
        }
        return indices
    }

    override fun equals(other: Any?): Boolean = equalsImpl(other) { otherDescriptor ->
        typeParameterDescriptors.contentEquals(otherDescriptor.typeParameterDescriptors)
    }

    override fun hashCode(): Int = _hashCode

    override fun toString(): String {
        return indices.entries.joinToString(", ", "$serialName(", ")") {
            it.key + ": " + getElementDescriptor(it.value).serialName
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
internal inline fun <reified SD : SerialDescriptor> SD.equalsImpl(
    other: Any?,
    typeParamsAreEqual: (otherDescriptor: SD) -> Boolean
): Boolean {
    if (this === other) return true
    if (other !is SD) return false
    if (serialName != other.serialName) return false
    if (!typeParamsAreEqual(other)) return false
    if (this.elementsCount != other.elementsCount) return false
    for (index in 0 until elementsCount) {
        if (getElementDescriptor(index).serialName != other.getElementDescriptor(index).serialName) return false
        if (getElementDescriptor(index).kind != other.getElementDescriptor(index).kind) return false
    }
    return true
}

@OptIn(ExperimentalSerializationApi::class)
internal fun SerialDescriptor.hashCodeImpl(typeParams: Array<SerialDescriptor>): Int {
    var result = serialName.hashCode()
    result = 31 * result + typeParams.contentHashCode()
    val elementDescriptors = elementDescriptors
    val namesHash = elementDescriptors.elementsHashCodeBy { it.serialName }
    val kindHash = elementDescriptors.elementsHashCodeBy { it.kind }
    result = 31 * result + namesHash
    result = 31 * result + kindHash
    return result
}
