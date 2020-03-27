/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE", "UNUSED")
package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlinx.serialization.CompositeDecoder.Companion.UNKNOWN_NAME

/**
 * Implementation that plugin uses to implement descriptors for auto-generated serializers.
 */
// TODO get rid of the rest of the usages and make it hidden
@InternalSerializationApi
@Deprecated(level = DeprecationLevel.ERROR, message = "Should not be used in general code")
public open class PluginGeneratedSerialDescriptor(
    override val serialName: String,
    private val generatedSerializer: GeneratedSerializer<*>? = null,
    final override val elementsCount: Int
) : SerialDescriptor {
    override val kind: SerialKind get() = StructureKind.CLASS
    override val annotations: List<Annotation> get() = classAnnotations ?: emptyList()

    private var added = -1
    private val names = Array(elementsCount) { "[UNINITIALIZED]" }
    private val propertiesAnnotations = arrayOfNulls<MutableList<Annotation>?>(elementsCount)
    // Classes rarely have annotations, so we can save up a bit of allocations here
    private var classAnnotations: MutableList<Annotation>? = null
    private var elementsOptionality = BooleanArray(elementsCount)
    internal val namesSet: Set<String> get() = indices.keys
    // don't change lazy mode: KT-32871, KT-32872
    private val indices: Map<String, Int> by lazy { buildIndices() }

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
        return generatedSerializer?.childSerializers()?.get(index)?.descriptor
            ?: throw IndexOutOfBoundsException("$serialName descriptor has only $elementsCount elements, index: $index")
    }

    override fun isElementOptional(index: Int): Boolean = elementsOptionality.getChecked(index)
    override fun getElementAnnotations(index: Int): List<Annotation> = propertiesAnnotations.getChecked(index) ?: emptyList()
    override fun getElementName(index: Int): String = names.getChecked(index)
    override fun getElementIndex(name: String): Int = indices[name] ?: UNKNOWN_NAME

    private fun buildIndices(): Map<String, Int> {
        val indices = HashMap<String, Int>()
        for (i in names.indices) {
            indices[names[i]] = i
        }
        return indices
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        @Suppress("DEPRECATION_ERROR")
        if (other !is SerialDescriptor) return false
        if (serialName != other.serialName) return false
        // TODO compare only serial names
        if (elementDescriptors() != other.elementDescriptors()) return false
        return true
    }

    override fun hashCode(): Int {
        var result = serialName.hashCode()
        // TODO hashcode only for serial name
        result = 31 * result + elementDescriptors().hashCode()
        return result
    }

    override fun toString(): String {
        return indices.entries.joinToString(", ", "$serialName(", ")") { it.key + ": " + getElementDescriptor(it.value).serialName }
    }
}
