/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.cbor

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

@Serializable
data class Simple(val a: String)

@Serializable
data class TypesUmbrella(
    val str: String,
    val i: Int,
    val nullable: Double?,
    val list: List<String>,
    val map: Map<Int, Boolean>,
    val inner: Simple,
    val innersList: List<Simple>,
    @ByteString val byteString: ByteArray,
    val byteArray: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as TypesUmbrella

        if (str != other.str) return false
        if (i != other.i) return false
        if (nullable != other.nullable) return false
        if (list != other.list) return false
        if (map != other.map) return false
        if (inner != other.inner) return false
        if (innersList != other.innersList) return false
        if (!byteString.contentEquals(other.byteString)) return false
        if (!byteArray.contentEquals(other.byteArray)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = str.hashCode()
        result = 31 * result + i
        result = 31 * result + (nullable?.hashCode() ?: 0)
        result = 31 * result + list.hashCode()
        result = 31 * result + map.hashCode()
        result = 31 * result + inner.hashCode()
        result = 31 * result + innersList.hashCode()
        result = 31 * result + byteString.contentHashCode()
        result = 31 * result + byteArray.contentHashCode()
        return result
    }
}

@Serializable
data class NumberTypesUmbrella(
    val int: Int,
    val long: Long,
    val float: Float,
    val double: Double,
    val boolean: Boolean,
    val char: Char
)

@Serializable
data class NullableByteString(
    @ByteString val byteString: ByteArray?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as NullableByteString

        if (byteString != null) {
            if (other.byteString == null) return false
            if (!byteString.contentEquals(other.byteString)) return false
        } else if (other.byteString != null) return false

        return true
    }

    override fun hashCode(): Int {
        return byteString?.contentHashCode() ?: 0
    }
}

@Serializable(with = CustomByteStringSerializer::class)
data class CustomByteString(val a: Byte, val b: Byte, val c: Byte)

class CustomByteStringSerializer : KSerializer<CustomByteString> {
    override val descriptor = SerialDescriptor("CustomByteString", ByteArraySerializer().descriptor)

    override fun serialize(encoder: Encoder, value: CustomByteString) {
        encoder.encodeSerializableValue(ByteArraySerializer(), byteArrayOf(value.a, value.b, value.c))
    }

    override fun deserialize(decoder: Decoder): CustomByteString {
        val array = decoder.decodeSerializableValue(ByteArraySerializer())
        return CustomByteString(array[0], array[1], array[2])
    }
}

@Serializable
data class TypeWithCustomByteString(@ByteString val x: CustomByteString)

@Serializable
data class TypeWithNullableCustomByteString(@ByteString val x: CustomByteString?)

@Serializable
data class WithTags(
    @Tagged(12uL) val a: ULong,
    val b: Int,
    @ByteString val c: ByteArray,
    @Tagged(90uL, 12uL) val d: String
)