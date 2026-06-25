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
 *
 * @property fields The list of unknown fields held by this holder,
 * each item presents for one top level field with its tagged raw byte array.
 */
@Serializable(with = ProtoUnknownFieldHolderSerializer::class)
public class ProtoUnknownFieldHolder internal constructor(
    public val fields: List<ByteArray>
) {
    public companion object {
        /**
         * An empty [ProtoUnknownFieldHolder] instance.
         */
        public val Empty: ProtoUnknownFieldHolder = ProtoUnknownFieldHolder(emptyList())
    }

    public constructor(vararg fields: ByteArray) : this(listOf(*fields))

    /**
     * Number of overall bytes holding in this holder.
     */
    public val contentSize: Int get() = fields.sumOf { it.size }

    /**
     * Number of unknown fields holding in this holder.
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

    override fun toString(): String {
        return "ProtoUnknownFieldHolder(contentSize=$contentSize, size=$size)"
    }
}

