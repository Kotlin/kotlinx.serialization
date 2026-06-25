/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlinx.serialization.protobuf.internal.*

/**
 * Represents a thin wrapper over fields in a protobuf message.
 *
 * Used to store unknown proto fields in a concrete class,
 * but do not deserialize byte array to this type directly.
 */
@Serializable(with = ProtoUnknownFieldHolderSerializer::class)
public class ProtoUnknownFieldHolder internal constructor(
    internal val fields: ByteArray
) {
    public companion object {
        /**
         * An empty [ProtoUnknownFieldHolder] instance.
         */
        public val Empty: ProtoUnknownFieldHolder = ProtoUnknownFieldHolder(ByteArray(0))
    }

    /**
     * Number of bytes holding in the message.
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

    override fun hashCode(): Int {
        return fields.contentHashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ProtoUnknownFieldHolder

        return fields.contentEquals(other.fields)
    }

    override fun toString(): String {
        return "ProtoUnknownFieldHolder(size=$size)"
    }
}

