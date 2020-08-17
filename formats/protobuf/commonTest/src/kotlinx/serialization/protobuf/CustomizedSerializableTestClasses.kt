/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:UseContextualSerialization(B::class)
package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*

@Serializable
data class A(@ProtoNumber(1) val b: B)

data class B(@ProtoNumber(1) val value: Int)

object BSerializer : KSerializer<B> {
    override fun serialize(encoder: Encoder, value: B) {
        encoder.encodeInt(value.value)
    }

    override fun deserialize(decoder: Decoder): B {
        return B(decoder.decodeInt())
    }

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("B", PrimitiveKind.INT)
}

@Serializable
data class BList(@ProtoNumber(1) val bs: List<B>)

@Serializable
data class C(@ProtoNumber(1) val a: Int = 31, @ProtoNumber(2) val b: Int = 42) {
    @Serializer(forClass = C::class)
    companion object: KSerializer<C> {
        override fun serialize(encoder: Encoder, value: C) {
            val elemOutput = encoder.beginStructure(descriptor)
            elemOutput.encodeIntElement(descriptor, 1, value.b)
            if (value.a != 31) elemOutput.encodeIntElement(descriptor, 0, value.a)
            elemOutput.endStructure(descriptor)
        }
    }
}

@Serializable
data class CList1(@ProtoNumber(1) val c: List<C>)

@Serializable
data class CList2(@ProtoNumber(1) val d: Int = 5, @ProtoNumber(2) val c: List<C>) {
    @Serializer(forClass = CList2::class)
    companion object: KSerializer<CList2> {
        override fun serialize(encoder: Encoder, value: CList2) {
            val elemOutput = encoder.beginStructure(descriptor)
            elemOutput.encodeSerializableElement(descriptor, 1, ListSerializer(C), value.c)
            if (value.d != 5) elemOutput.encodeIntElement(descriptor, 0, value.d)
            elemOutput.endStructure(descriptor)
        }
    }
}

@Serializable
data class CList3(@ProtoNumber(1) val e: List<C> = emptyList(), @ProtoNumber(2) val f: Int) {
    @Serializer(forClass = CList3::class)
    companion object: KSerializer<CList3> {
        override fun serialize(encoder: Encoder, value: CList3) {
            val elemOutput = encoder.beginStructure(descriptor)
            if (value.e.isNotEmpty()) elemOutput.encodeSerializableElement(descriptor, 0, ListSerializer(C), value.e)
            elemOutput.encodeIntElement(descriptor, 1, value.f)
            elemOutput.endStructure(descriptor)
        }
    }
}

@Serializable
data class CList4(@ProtoNumber(1) val g: List<C> = emptyList(), @ProtoNumber(2) val h: Int) {
    @Serializer(forClass = CList4::class)
    companion object: KSerializer<CList4> {
        override fun serialize(encoder: Encoder, value: CList4) {
            val elemOutput = encoder.beginStructure(descriptor)
            elemOutput.encodeIntElement(descriptor, 1, value.h)
            if (value.g.isNotEmpty()) elemOutput.encodeSerializableElement(descriptor, 0, ListSerializer(C), value.g)
            elemOutput.endStructure(descriptor)
        }
    }
}

@Serializable
data class CList5(@ProtoNumber(1) val g: List<Int> = emptyList(), @ProtoNumber(2) val h: Int) {
    @Serializer(forClass = CList5::class)
    companion object: KSerializer<CList5> {
        override fun serialize(encoder: Encoder, value: CList5) {
            val elemOutput = encoder.beginStructure(descriptor)
            elemOutput.encodeIntElement(descriptor, 1, value.h)
            if (value.g.isNotEmpty()) elemOutput.encodeSerializableElement(
                descriptor, 0,
                ListSerializer(Int.serializer()),
                value.g
            )
            elemOutput.endStructure(descriptor)
        }
    }
}
