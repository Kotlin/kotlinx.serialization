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

package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlinx.serialization.CompositeDecoder.Companion.UNKNOWN_NAME
import kotlin.jvm.JvmOverloads

open class SerialClassDescImpl(override val name: String) : SerialDescriptor {
    override val kind: SerialKind get() = StructureKind.CLASS

    private val names: MutableList<String> = ArrayList()
    private val annotations: MutableList<MutableList<Annotation>> = mutableListOf()
    private val classAnnotations: MutableList<Annotation> = mutableListOf()

    private var flags = BooleanArray(4)

    private val descriptors: MutableList<SerialDescriptor> = mutableListOf()

    private var _indices: Map<String, Int>? = null
    private val indices: Map<String, Int> by lazy { buildIndices() }

    @JvmOverloads
    fun addElement(name: String, isOptional: Boolean = false) {
        names.add(name)
        val idx = names.size - 1
        ensureFlagsCapacity(idx)
        flags[idx] = isOptional
        annotations.add(mutableListOf())
    }

    fun pushAnnotation(a: Annotation) {
        annotations.last().add(a)
    }

    fun pushDescriptor(desc: SerialDescriptor) {
        descriptors.add(desc)
    }

    override fun getElementDescriptor(index: Int): SerialDescriptor {
        return descriptors[index]
    }

    override fun isElementOptional(index: Int): Boolean {
        return flags[index]
    }

    fun pushClassAnnotation(a: Annotation) {
        classAnnotations.add(a)
    }

    override fun getEntityAnnotations(): List<Annotation> = classAnnotations
    override fun getElementAnnotations(index: Int): List<Annotation> = annotations[index]
    override val elementsCount: Int
        get() = annotations.size

    override fun getElementName(index: Int): String = names[index]
    override fun getElementIndex(name: String): Int = indices[name] ?: UNKNOWN_NAME

    private fun ensureFlagsCapacity(i: Int) {
        if (flags.size <= i)
            flags = flags.copyOf(flags.size * 2)
    }

    private fun buildIndices(): Map<String, Int> {
        val indices = HashMap<String, Int>()
        for (i in 0..names.size - 1)
            indices.put(names[i], i)
        return indices
    }

    override fun toString() = "$name$names"
}
