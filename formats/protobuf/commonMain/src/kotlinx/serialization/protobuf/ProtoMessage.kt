/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlinx.serialization.protobuf.internal.*

/**
 * Represents a protobuf message.
 *
 * Especially used as a holder of unknown proto fields in an arbitrary protobuf message.
 */
@Serializable(with = ProtoMessageSerializer::class)
public class ProtoMessage internal constructor(
    internal val fields: List<ProtoField>
) {
    public companion object {
        /**
         * An empty [ProtoMessage] instance.
         *
         * Useful as a default value for [ProtoUnknownFields] properties.
         */
        public val Empty: ProtoMessage = ProtoMessage(emptyList())
    }

    /**
     * Number of fields holding in the message.
     */
    public val size: Int get() = fields.size

    /**
     * Returns a byte array representing of the message.
     */
    public fun asByteArray(): ByteArray =
        fields.fold(ByteArray(0)) { acc, protoField -> acc + protoField.asWireContent() }

    internal constructor(vararg fields: ProtoField) : this(fields.toList())

    /**
     * Merges two [ProtoMessage] instances.
     */
    public operator fun plus(other: ProtoMessage): ProtoMessage = merge(other)

    /**
     * Merges two [ProtoMessage] instances.
     */
    public fun merge(other: ProtoMessage): ProtoMessage {
        return ProtoMessage(fields + other.fields)
    }

    /**
     * Convenience method to merge multiple [ProtoField] with this message.
     */
    internal fun merge(vararg field: ProtoField): ProtoMessage {
        return ProtoMessage(fields + field)
    }

    override fun hashCode(): Int {
        return fields.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ProtoMessage

        return fields == other.fields
    }
}

/**
 * Convenience method to merge two nullable [ProtoMessage] instances.
 */
public fun ProtoMessage?.merge(other: ProtoMessage?): ProtoMessage {
    return when {
        this == null -> other ?: ProtoMessage.Empty
        other == null -> this
        else -> this + other
    }
}

/**
 * Convenience method to merge multiple [ProtoField] with a nullable [ProtoMessage].
 */
internal fun ProtoMessage?.merge(vararg fields: ProtoField): ProtoMessage {
    return when {
        this == null -> ProtoMessage(fields.toList())
        else -> this.merge(ProtoMessage(fields.toList()))
    }
}

/**
 * Represents a single field in a protobuf message.
 */
@Serializable(with = ProtoFieldSerializer::class)
@ConsistentCopyVisibility
internal data class ProtoField internal constructor(
    internal val id: Int,
    internal val wireType: ProtoWireType,
    internal val data: ProtoContentHolder
) {
    companion object {
        val Empty: ProtoField = ProtoField(0, ProtoWireType.INVALID, ProtoContentHolder.ByteArrayContent(ByteArray(0)))
    }

    fun asWireContent(): ByteArray = byteArrayOf(((id shl 3) or wireType.typeId).toByte()) + data.byteArray

    val contentLength: Int
        get() = asWireContent().size

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ProtoField

        if (id != other.id) return false
        if (wireType != other.wireType) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + wireType.hashCode()
        result = 31 * result + data.contentHashCode()
        return result
    }
}

/**
 * A data representation of a protobuf field in [ProtoField.data], without the field number and wire type.
 */
internal sealed interface ProtoContentHolder {

    /**
     * Returns a byte array representation of the content.
     */
    val byteArray: ByteArray

    /**
     * Represents the content in raw byte array.
     */
    data class ByteArrayContent(override val byteArray: ByteArray) : ProtoContentHolder {
        override fun equals(other: Any?): Boolean {
            return other is ProtoContentHolder && this.contentEquals(other)
        }

        override fun hashCode(): Int {
            return this.contentHashCode()
        }
    }

    /**
     * Represents the content with a nested [ProtoMessage].
     */
    data class MessageContent(val content: ProtoMessage) : ProtoContentHolder {
        override val byteArray: ByteArray
            get() = content.asByteArray()

        override fun equals(other: Any?): Boolean {
            return other is ProtoContentHolder && this.contentEquals(other)
        }

        override fun hashCode(): Int {
            return this.contentHashCode()
        }
    }
}

/**
 * Creates a [ProtoContentHolder] instance with a byte array content.
 */
internal fun ProtoContentHolder(content: ByteArray): ProtoContentHolder = ProtoContentHolder.ByteArrayContent(content)

/**
 * Get the length in bytes of the content in the [ProtoContentHolder].
 */
internal val ProtoContentHolder.contentLength: Int
    get() = byteArray.size

/**
 * Checks if the content of two [ProtoContentHolder] instances are equal.
 */
internal fun ProtoContentHolder.contentEquals(other: ProtoContentHolder): Boolean {
    return byteArray.contentEquals(other.byteArray)
}

/**
 * Calculates the hash code of the content in the [ProtoContentHolder].
 */
internal fun ProtoContentHolder.contentHashCode(): Int {
    return byteArray.contentHashCode()
}

