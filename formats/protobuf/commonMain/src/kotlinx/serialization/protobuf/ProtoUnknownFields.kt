/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlinx.serialization.protobuf.internal.*
import kotlinx.serialization.protobuf.internal.ProtoWireType

/**
 * Mark a property as a holder for unknown fields in protobuf message.
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
@ExperimentalSerializationApi
public annotation class ProtoUnknownFields

@Serializable(with = ProtoMessageSerializer::class)
public class ProtoMessage internal constructor(
    public val fields: List<ProtoField>
) {
    public companion object {
        public val Empty: ProtoMessage = ProtoMessage(emptyList())
    }

    public val size: Int get() = fields.size
    public fun asByteArray(): ByteArray = fields.fold(ByteArray(0)) { acc, protoField -> acc + protoField.asWireContent() }

    public constructor(vararg fields: ProtoField) : this(fields.toList())

    public operator fun plus(other: ProtoMessage): ProtoMessage = merge(other)

    public fun merge(other: ProtoMessage): ProtoMessage {
        return ProtoMessage(fields + other.fields)
    }

    public fun merge(vararg field: ProtoField): ProtoMessage {
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

public fun ProtoMessage?.merge(other: ProtoMessage?): ProtoMessage {
    return when {
        this == null -> other ?: ProtoMessage.Empty
        other == null -> this
        else -> this + other
    }
}

public fun ProtoMessage?.merge(vararg fields: ProtoField): ProtoMessage {
    return when {
        this == null -> ProtoMessage(fields.toList())
        else -> this.merge(ProtoMessage(fields.toList()))
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable(with = ProtoFieldSerializer::class)
@KeepGeneratedSerializer
@ConsistentCopyVisibility
public data class ProtoField internal constructor(
    internal val id: Int,
    internal val wireType: ProtoWireType,
    internal val data: ProtoContentHolder
) {
    public companion object {
        public val Empty: ProtoField = ProtoField(0, ProtoWireType.INVALID, ProtoContentHolder.ByteArrayContent(ByteArray(0)))
    }

    public fun asWireContent(): ByteArray = byteArrayOf(((id shl 3) or wireType.typeId).toByte()) + data.byteArray

    public val contentLength: Int
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

internal sealed interface ProtoContentHolder {
    val byteArray: ByteArray

    data class ByteArrayContent(override val byteArray: ByteArray) : ProtoContentHolder {
        override fun equals(other: Any?): Boolean {
            return other is ProtoContentHolder && this.contentEquals(other)
        }

        override fun hashCode(): Int {
            return this.contentHashCode()
        }
    }

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

internal fun ProtoContentHolder(content: ByteArray): ProtoContentHolder = ProtoContentHolder.ByteArrayContent(content)

internal val ProtoContentHolder.contentLength: Int
    get() = byteArray.size

internal fun ProtoContentHolder.contentEquals(other: ProtoContentHolder): Boolean {
    return byteArray.contentEquals(other.byteArray)
}

internal fun ProtoContentHolder.contentHashCode(): Int {
    return byteArray.contentHashCode()
}

