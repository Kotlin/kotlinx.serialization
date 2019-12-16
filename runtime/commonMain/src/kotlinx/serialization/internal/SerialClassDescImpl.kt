/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE", "UNUSED")
package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlinx.serialization.CompositeDecoder.Companion.UNKNOWN_NAME
import kotlin.jvm.JvmOverloads

open class SerialClassDescImpl @JvmOverloads constructor(
    override val serialName: String,
    private val generatedSerializer: GeneratedSerializer<*>? = null
) : SerialDescriptor {
    /*
     * Unused methods are invoked by auto-generated plugin code
     */
    override val kind: SerialKind get() = StructureKind.CLASS
    override val elementsCount: Int get() = _annotations.size

    private val names: MutableList<String> = ArrayList()
    private val _annotations: MutableList<MutableList<Annotation>> = mutableListOf()
    override val annotations: List<Annotation>
        get() = classAnnotations
    private val classAnnotations: MutableList<Annotation> = mutableListOf()
    private var flags = BooleanArray(4)

    private val descriptors: MutableList<SerialDescriptor> = mutableListOf()

    // don't change lazy mode: KT-32871, KT-32872
    private val indices: Map<String, Int> by lazy { buildIndices() }

    @JvmOverloads // TODO protected
    public fun addElement(name: String, isOptional: Boolean = false) {
        names.add(name)
        val idx = names.size - 1
        ensureFlagsCapacity(idx)
        flags[idx] = isOptional
        _annotations.add(mutableListOf())
    }

    public fun pushAnnotation(a: Annotation) {
        _annotations.last().add(a)
    }

    public fun pushClassAnnotation(a: Annotation) {
        classAnnotations.add(a)
    }

    public fun pushDescriptor(desc: SerialDescriptor) {
        descriptors.add(desc)
    }

    override fun getElementDescriptor(index: Int): SerialDescriptor {
        // todo: cache
        return generatedSerializer?.childSerializers()?.getOrNull(index)?.descriptor
                ?: descriptors.getOrNull(index)
                ?: throw MissingDescriptorException(index, this)
    }

    override fun isElementOptional(index: Int): Boolean {
        return flags[index]
    }

    override fun getElementAnnotations(index: Int): List<Annotation> = _annotations[index]
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

    private class MissingDescriptorException(index: Int, origin: SerialDescriptor) :
        SerializationException("Element descriptor at index $index has not been found in ${origin.serialName}")
}
