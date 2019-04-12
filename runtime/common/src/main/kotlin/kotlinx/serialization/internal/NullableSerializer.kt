/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlin.reflect.KClass

fun <T : Any> makeNullable(actualSerializer: KSerializer<T>): KSerializer<T?> = NullableSerializer(actualSerializer)

class NullableSerializer<T : Any>(public val actualSerializer: KSerializer<T>) : KSerializer<T?> {
    private class SerialDescriptorForNullable(val original: SerialDescriptor): SerialDescriptor by original {
        override val isNullable: Boolean
            get() = true

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SerialDescriptorForNullable) return false

            if (original != other.original) return false

            return true
        }

        override fun hashCode(): Int {
            return original.hashCode() * 31
        }
    }

    override val descriptor: SerialDescriptor = SerialDescriptorForNullable(actualSerializer.descriptor)

    override fun serialize(encoder: Encoder, obj: T?) {
        if (obj != null) {
            encoder.encodeNotNullMark()
            encoder.encodeSerializableValue(actualSerializer, obj)
        }
        else {
            encoder.encodeNull()
        }
    }

    override fun deserialize(decoder: Decoder): T? {
        return if (decoder.decodeNotNullMark()) decoder.decodeSerializableValue(actualSerializer) else decoder.decodeNull()
    }

    override fun patch(decoder: Decoder, old: T?): T? {
        return when {
            old == null -> deserialize(decoder)
            decoder.decodeNotNullMark() -> decoder.updateSerializableValue(actualSerializer, old)
            else -> decoder.decodeNull().let { old }
        }
    }
}
