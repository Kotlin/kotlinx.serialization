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
@Serializable(with = ProtoUnknownFieldHolderSerializer::class)
public class ProtoUnknownFieldHolder internal constructor(
    internal val fields: List<ProtoField>
) {
    public companion object {
        /**
         * An empty [ProtoUnknownFieldHolder] instance.
         */
        public val Empty: ProtoUnknownFieldHolder = ProtoUnknownFieldHolder(emptyList())
    }

    internal constructor(vararg fields: ProtoField) : this(fields.toList())

    /**
     * Number of fields holding in the message.
     */
    public val size: Int get() = fields.size

    /**
     * Merges two [ProtoUnknownFieldHolder] instances.
     */
    public operator fun plus(other: ProtoUnknownFieldHolder): ProtoUnknownFieldHolder = merge(other)

    /**
     * Merges two [ProtoUnknownFieldHolder] instances.
     */
    public fun merge(other: ProtoUnknownFieldHolder): ProtoUnknownFieldHolder {
        return ProtoUnknownFieldHolder(fields + other.fields)
    }

    /**
     * Convenience method to merge multiple [ProtoField] with this message.
     */
    internal fun merge(vararg field: ProtoField): ProtoUnknownFieldHolder {
        return ProtoUnknownFieldHolder(fields + field)
    }

    override fun hashCode(): Int {
        return fields.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ProtoUnknownFieldHolder

        return fields == other.fields
    }
}

/**
 * Convenience method to merge two nullable [ProtoUnknownFieldHolder] instances.
 */
public fun ProtoUnknownFieldHolder?.merge(other: ProtoUnknownFieldHolder?): ProtoUnknownFieldHolder {
    return when {
        this == null -> other ?: ProtoUnknownFieldHolder.Empty
        other == null -> this
        else -> this + other
    }
}

/**
 * Convenience method to merge multiple [ProtoField] with a nullable [ProtoUnknownFieldHolder].
 */
internal fun ProtoUnknownFieldHolder?.merge(vararg fields: ProtoField): ProtoUnknownFieldHolder {
    return when {
        this == null -> ProtoUnknownFieldHolder(fields.toList())
        else -> this.merge(ProtoUnknownFieldHolder(fields.toList()))
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
    internal val data: ByteArray
) {
    companion object {
        val Empty: ProtoField = ProtoField(0, ProtoWireType.INVALID, ByteArray(0))
    }

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
