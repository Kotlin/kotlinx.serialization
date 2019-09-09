/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package kotlinx.serialization.internal

import kotlinx.serialization.*


/**
 * Returns a nullable serializer for the given serializer of non-null type.
 */
public val <T : Any> KSerializer<T>.nullable: KSerializer<T?>
    get() {
        @Suppress("UNCHECKED_CAST")
        return if (descriptor.isNullable) (this as KSerializer<T?>) else NullableSerializer(this)
    }

@Deprecated(
    message = "Deprecated in the favor of extension",
    level = DeprecationLevel.WARNING,
    replaceWith = ReplaceWith("actualSerializer.nullable)")
)
fun <T : Any> makeNullable(actualSerializer: KSerializer<T>): KSerializer<T?> {
    return NullableSerializer(actualSerializer)
}

/**
 * Use [KSerializer.nullable][nullable] instead.
 * @suppress internal API
 */
@InternalSerializationApi
public class NullableSerializer<T : Any>(private val serializer: KSerializer<T>) : KSerializer<T?> {

    override val descriptor: SerialDescriptor = SerialDescriptorForNullable(serializer.descriptor)

    override fun serialize(encoder: Encoder, obj: T?) {
        if (obj != null) {
            encoder.encodeNotNullMark()
            encoder.encodeSerializableValue(serializer, obj)
        }
        else {
            encoder.encodeNull()
        }
    }

    override fun deserialize(decoder: Decoder): T? {
        return if (decoder.decodeNotNullMark()) decoder.decodeSerializableValue(serializer) else decoder.decodeNull()
    }

    override fun patch(decoder: Decoder, old: T?): T? {
        return when {
            old == null -> deserialize(decoder)
            decoder.decodeNotNullMark() -> decoder.updateSerializableValue(serializer, old)
            else -> decoder.decodeNull().let { old }
        }
    }

    private class SerialDescriptorForNullable(private val original: SerialDescriptor): SerialDescriptor by original {
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
}
