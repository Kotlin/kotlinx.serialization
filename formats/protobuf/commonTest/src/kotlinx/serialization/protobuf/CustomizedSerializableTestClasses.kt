/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:ContextualSerialization(B::class)
package kotlinx.serialization.protobuf

import kotlinx.serialization.*

@Serializable
data class A(@SerialId(1) val b: B)

data class B(@SerialId(1) val value: Int)

object BSerializer : KSerializer<B> {
    override fun serialize(encoder: Encoder, obj: B) {
        encoder.encodeInt(obj.value)
    }

    override fun deserialize(decoder: Decoder): B {
        return B(decoder.decodeInt())
    }

    override val descriptor: SerialDescriptor = PrimitiveDescriptor("B", PrimitiveKind.INT)
}

@Serializable
data class BList(@SerialId(1) val bs: List<B>)

@Serializable
data class C(@SerialId(1) val a: Int = 31, @SerialId(2) val b: Int = 42) {
    @Serializer(forClass = C::class)
    companion object: KSerializer<C> {
        override fun serialize(encoder: Encoder, obj: C) {
            val elemOutput = encoder.beginStructure(descriptor)
            elemOutput.encodeIntElement(descriptor, 1, obj.b)
            if (obj.a != 31) elemOutput.encodeIntElement(descriptor, 0, obj.a)
            elemOutput.endStructure(descriptor)
        }
    }
}

@Serializable
data class CList1(@SerialId(1) val c: List<C>)

@Serializable
data class CList2(@SerialId(1) val d: Int = 5, @SerialId(2) val c: List<C>) {
    @Serializer(forClass = CList2::class)
    companion object: KSerializer<CList2> {
        override fun serialize(encoder: Encoder, obj: CList2) {
            val elemOutput = encoder.beginStructure(descriptor)
            elemOutput.encodeSerializableElement(descriptor, 1, C.list, obj.c)
            if (obj.d != 5) elemOutput.encodeIntElement(descriptor, 0, obj.d)
            elemOutput.endStructure(descriptor)
        }
    }
}

@Serializable
data class CList3(@SerialId(1) val e: List<C> = emptyList(), @SerialId(2) val f: Int) {
    @Serializer(forClass = CList3::class)
    companion object: KSerializer<CList3> {
        override fun serialize(encoder: Encoder, obj: CList3) {
            val elemOutput = encoder.beginStructure(descriptor)
            if (obj.e.isNotEmpty()) elemOutput.encodeSerializableElement(descriptor, 0, C.list, obj.e)
            elemOutput.encodeIntElement(descriptor, 1, obj.f)
            elemOutput.endStructure(descriptor)
        }
    }
}

@Serializable
data class CList4(@SerialId(1) val g: List<C> = emptyList(), @SerialId(2) val h: Int) {
    @Serializer(forClass = CList4::class)
    companion object: KSerializer<CList4> {
        override fun serialize(encoder: Encoder, obj: CList4) {
            val elemOutput = encoder.beginStructure(descriptor)
            elemOutput.encodeIntElement(descriptor, 1, obj.h)
            if (obj.g.isNotEmpty()) elemOutput.encodeSerializableElement(descriptor, 0, C.list, obj.g)
            elemOutput.endStructure(descriptor)
        }
    }
}

@Serializable
data class CList5(@SerialId(1) val g: List<Int> = emptyList(), @SerialId(2) val h: Int) {
    @Serializer(forClass = CList5::class)
    companion object: KSerializer<CList5> {
        override fun serialize(encoder: Encoder, obj: CList5) {
            val elemOutput = encoder.beginStructure(descriptor)
            elemOutput.encodeIntElement(descriptor, 1, obj.h)
            if (obj.g.isNotEmpty()) elemOutput.encodeSerializableElement(descriptor, 0, IntSerializer.list,
                obj.g)
            elemOutput.endStructure(descriptor)
        }
    }
}
