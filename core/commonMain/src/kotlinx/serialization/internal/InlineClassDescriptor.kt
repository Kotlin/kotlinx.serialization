/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

@Suppress("Unused")
@PublishedApi
internal class InlineClassDescriptor(name: String, generatedSerializer: GeneratedSerializer<*>) :
    PluginGeneratedSerialDescriptor(name, generatedSerializer, 1) {

    override val isInline: Boolean = true

    override fun hashCode(): Int = _hashCode * 31

    override fun equals(other: Any?): Boolean = equalsImpl(other) { otherDescriptor ->
        otherDescriptor.isInline &&
                typeParameterDescriptors.contentEquals(otherDescriptor.typeParameterDescriptors)
    }
}

internal fun <T> InlinePrimitiveDescriptor(name: String, primitiveSerializer: KSerializer<T>): SerialDescriptor =
    InlineClassDescriptor(name, object : GeneratedSerializer<T> {
        // object needed only to pass childSerializers()
        override fun childSerializers(): Array<KSerializer<*>> = arrayOf(primitiveSerializer)

        override val descriptor: SerialDescriptor get() = error("unsupported")

        override fun serialize(encoder: Encoder, value: T) {
            error("unsupported")
        }

        override fun deserialize(decoder: Decoder): T {
            error("unsupported")
        }
    })
