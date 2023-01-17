/*
 * Copyright 2017-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(ExperimentalSerializationApi::class)

package kotlinx.serialization.internal

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind

internal object NothingSerialDescriptor : SerialDescriptor {
    public override val kind: SerialKind = StructureKind.OBJECT

    public override val serialName: String = "kotlin.Nothing"

    override val elementsCount: Int get() = 0
    override fun getElementName(index: Int): String = error()
    override fun getElementIndex(name: String): Int = error()
    override fun isElementOptional(index: Int): Boolean = error()
    override fun getElementDescriptor(index: Int): SerialDescriptor = error()
    override fun getElementAnnotations(index: Int): List<Annotation> = error()
    override fun toString(): String = "NothingSerialDescriptor"
    override fun equals(other: Any?): Boolean {
        return this === other
    }

    override fun hashCode(): Int = serialName.hashCode() + 31 * kind.hashCode()
    private fun error(): Nothing =
        throw IllegalStateException("Descriptor for type `kotlin.Nothing` does not have elements")
}
