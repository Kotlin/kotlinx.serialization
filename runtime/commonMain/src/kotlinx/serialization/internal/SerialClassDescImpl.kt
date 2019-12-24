/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE", "UNUSED")
package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlinx.serialization.CompositeDecoder.Companion.UNKNOWN_NAME
import kotlin.jvm.*

/**
 * Implementation that plugin uses to implement descriptors
 * for auto-generated serializers.
 *
 * Unused methods are invoked by auto-generated plugin code
 */
@InternalSerializationApi
public open class SerialClassDescImpl @JvmOverloads constructor(
    override val serialName: String,
    private val generatedSerializer: GeneratedSerializer<*>? = null
) : SerialDescriptor {
    override val kind: SerialKind get() = StructureKind.CLASS
    override val elementsCount: Int get() = propertiesAnnotations.size
    override val annotations: List<Annotation> get() = classAnnotations

    private val names: MutableList<String> = ArrayList()
    private val propertiesAnnotations: MutableList<MutableList<Annotation>?> = mutableListOf()
    private val classAnnotations: MutableList<Annotation> = mutableListOf()
    private val descriptors: MutableList<SerialDescriptor> = mutableListOf()
    // Properly guess size for generated serializer
    private var flags = BooleanArray(4)

    // don't change lazy mode: KT-32871, KT-32872
    private val indices: Map<String, Int> by lazy { buildIndices() }

    @JvmOverloads
    public fun addElement(name: String, isOptional: Boolean = false) {
        names.add(name)
        val idx = names.size - 1
        ensureFlagsCapacity(idx)
        flags[idx] = isOptional
        propertiesAnnotations.add(null)
    }

    // TODO rename
    public fun pushAnnotation(annotation: Annotation) {
        val list = propertiesAnnotations.last().let {
            if (it == null) {
                val result = ArrayList<Annotation>(1)
                propertiesAnnotations[propertiesAnnotations.lastIndex] = result
                result
            } else {
                it
            }
        }
        list.add(annotation)
    }

    // TODO rename
    public fun pushClassAnnotation(a: Annotation) {
        classAnnotations.add(a)
    }

    public fun pushDescriptor(desc: SerialDescriptor) {
        descriptors.add(desc)
    }

    override fun getElementDescriptor(index: Int): SerialDescriptor {
        return generatedSerializer?.childSerializers()?.get(index)?.descriptor ?: descriptors[index]
    }

    override fun isElementOptional(index: Int): Boolean {
        if (index !in flags.indices) throw IndexOutOfBoundsException("Index $index out of bounds ${flags.indices}")
        return flags[index]
    }

    override fun getElementAnnotations(index: Int): List<Annotation> = propertiesAnnotations[index] ?: emptyList()
    override fun getElementName(index: Int): String = names[index]
    override fun getElementIndex(name: String): Int = indices[name] ?: UNKNOWN_NAME

    private fun ensureFlagsCapacity(i: Int) {
        if (flags.size <= i)
            flags = flags.copyOf(flags.size * 2)
    }

    private fun buildIndices(): Map<String, Int> {
        val indices = HashMap<String, Int>()
        for (i in 0 until names.size) {
            indices[names[i]] = i
        }
        return indices
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SerialClassDescImpl) return false
        if (serialName != other.serialName) return false
        if (elementDescriptors() != other.elementDescriptors()) return false
        return true
    }

    override fun hashCode(): Int {
        var result = serialName.hashCode()
        result = 31 * result + elementDescriptors().hashCode()
        return result
    }

    override fun toString(): String {
        return indices.entries.joinToString(", ", "$serialName(", ")") { it.key + ": " + getElementDescriptor(it.value).serialName }
    }
}

internal fun NamedDescriptor(name: String, kind: SerialKind): SerialDescriptor {
    return object : SerialClassDescImpl(name) {
        override val kind: SerialKind
            get() = kind
    }
}
