/*
 * Copyright 2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE", "UNUSED")
package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlinx.serialization.CompositeDecoder.Companion.UNKNOWN_NAME
import kotlin.LazyThreadSafetyMode.*
import kotlin.jvm.JvmOverloads

open class SerialClassDescImpl @JvmOverloads constructor(
    override val name: String,
    private val generatedSerializer: GeneratedSerializer<*>? = null
) : SerialDescriptor {
    /*
     * Unused methods are invoked by auto-generated plugin code
     */
    override val kind: SerialKind get() = StructureKind.CLASS
    override val elementsCount: Int get() = annotations.size

    private val names: MutableList<String> = ArrayList()
    private val annotations: MutableList<MutableList<Annotation>> = mutableListOf()
    private val classAnnotations: MutableList<Annotation> = mutableListOf()
    private var flags = BooleanArray(4)

    private val descriptors: MutableList<SerialDescriptor> = mutableListOf()

    // don't change lazy mode: KT-32871, KT-32872
    private val indices: Map<String, Int> by lazy { buildIndices() }

    @JvmOverloads
    public fun addElement(name: String, isOptional: Boolean = false) {
        names.add(name)
        val idx = names.size - 1
        ensureFlagsCapacity(idx)
        flags[idx] = isOptional
        annotations.add(mutableListOf())
    }

    public fun pushAnnotation(a: Annotation) {
        annotations.last().add(a)
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

    override fun getEntityAnnotations(): List<Annotation> = classAnnotations
    override fun getElementAnnotations(index: Int): List<Annotation> = annotations[index]
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
        if (name != other.name) return false
        if (elementDescriptors() != other.elementDescriptors()) return false
        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + elementDescriptors().hashCode()
        return result
    }

    override fun toString(): String {
        return indices.entries.joinToString(", ", "$name(", ")") { it.key + ": " + getElementDescriptor(it.value).name }
    }

    private class MissingDescriptorException(index: Int, origin: SerialDescriptor) :
        SerializationException("Element descriptor at index $index has not been found in $origin")
}
