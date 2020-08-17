/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*
import kotlin.test.*

class CheckedData<T : Any>(val data: T, val checkSum: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as CheckedData<*>

        if (data != other.data) return false
        if (!checkSum.contentEquals(other.checkSum)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.hashCode()
        result = 31 * result + checkSum.contentHashCode()
        return result
    }
}

@Serializer(forClass = CheckedData::class)
class CheckedDataSerializer<T : Any>(private val dataSerializer: KSerializer<T>) : KSerializer<CheckedData<T>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("CheckedDataSerializer") {
        val dataDescriptor = dataSerializer.descriptor
        element("data", dataDescriptor)
        element("checkSum", ByteArraySerializer().descriptor)
    }

    override fun serialize(encoder: Encoder, value: CheckedData<T>) {
        val out = encoder.beginStructure(descriptor)
        out.encodeSerializableElement(descriptor, 0, dataSerializer, value.data)
        out.encodeStringElement(descriptor, 1, InternalHexConverter.printHexBinary(value.checkSum))
        out.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): CheckedData<T> {
        val inp = decoder.beginStructure(descriptor)
        lateinit var data: T
        lateinit var sum: ByteArray
        loop@ while (true) {
            when (val i = inp.decodeElementIndex(descriptor)) {
                CompositeDecoder.DECODE_DONE -> break@loop
                0 -> data = inp.decodeSerializableElement(descriptor, i, dataSerializer)
                1 -> sum = InternalHexConverter.parseHexBinary(inp.decodeStringElement(descriptor, i))
                else -> throw SerializationException("Unknown index $i")
            }
        }
        inp.endStructure(descriptor)
        return CheckedData(data, sum)
    }
}

@Serializable
data class DataWithString(@Serializable(with = CheckedDataSerializer::class) val data: CheckedData<String>)

@Serializable
data class DataWithInt(@Serializable(with = CheckedDataSerializer::class) val data: CheckedData<Int>)


class GenericCustomSerializerTest {
    @Test
    fun testStringData() {
        val original = DataWithString(CheckedData("my data", byteArrayOf(42, 32)))
        val s = Json.encodeToString(DataWithString.serializer(), original)
        assertEquals("""{"data":{"data":"my data","checkSum":"2A20"}}""", s)
        val restored = Json.decodeFromString(DataWithString.serializer(), s)
        assertEquals(original, restored)
    }

    @Test
    fun testIntData() {
        val original = DataWithInt(CheckedData(42, byteArrayOf(42)))
        val s = Json.encodeToString(DataWithInt.serializer(), original)
        assertEquals("""{"data":{"data":42,"checkSum":"2A"}}""", s)
        val restored = Json.decodeFromString(DataWithInt.serializer(), s)
        assertEquals(original, restored)
    }
}
