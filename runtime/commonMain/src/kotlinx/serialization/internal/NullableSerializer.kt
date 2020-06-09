/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package kotlinx.serialization.internal

import kotlinx.serialization.*

@Deprecated(
    message = "Deprecated in the favor of extension",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("actualSerializer.nullable)")
) // TODO still used by the plugin
@InternalSerializationApi
public fun <T : Any> makeNullable(actualSerializer: KSerializer<T>): KSerializer<T?> {
    return NullableSerializer(actualSerializer)
}

/**
 * Use [KSerializer.nullable][nullable] instead.
 * @suppress internal API
 */
@InternalSerializationApi
public class NullableSerializer<T : Any>(private val serializer: KSerializer<T>) : KSerializer<T?> {

    override val descriptor: SerialDescriptor = SerialDescriptorForNullable(serializer.descriptor)

    override fun serialize(encoder: Encoder, value: T?) {
        if (value != null) {
            encoder.encodeNotNullMark()
            encoder.encodeSerializableValue(serializer, value)
        } else {
            encoder.encodeNull()
        }
    }

    override fun deserialize(decoder: Decoder): T? {
        return if (decoder.decodeNotNullMark()) decoder.decodeSerializableValue(serializer) else decoder.decodeNull()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as NullableSerializer<*>
        if (serializer != other.serializer) return false
        return true
    }

    override fun hashCode(): Int {
        return serializer.hashCode()
    }
}

internal class SerialDescriptorForNullable(internal val original: SerialDescriptor) : SerialDescriptor by original {
    override val serialName: String = original.serialName + "?"
    override val isNullable: Boolean
        get() = true

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SerialDescriptorForNullable) return false
        if (original != other.original) return false
        return true
    }

    override fun toString(): String {
        return "$original?"
    }

    override fun hashCode(): Int {
        return original.hashCode() * 31
    }
}
