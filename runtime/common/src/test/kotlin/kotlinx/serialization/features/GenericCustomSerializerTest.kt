/*
 * Copyright 2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.internal.HexConverter
import kotlinx.serialization.internal.SerialClassDescImpl
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

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
class CheckedDataSerializer<T : Any>(val dataSerializer: KSerializer<T>) : KSerializer<CheckedData<T>> {
    override val descriptor: SerialDescriptor = object : SerialClassDescImpl("CheckedDataSerializer") {
        init {
            addElement("data")
            addElement("checkSum")
        }
    }

    override fun serialize(encoder: Encoder, obj: CheckedData<T>) {
        val out = encoder.beginStructure(descriptor)
        out.encodeSerializableElement(descriptor, 0, dataSerializer, obj.data)
        out.encodeStringElement(descriptor, 1, HexConverter.printHexBinary(obj.checkSum))
        out.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): CheckedData<T> {
        val inp = decoder.beginStructure(descriptor)
        lateinit var data: T
        lateinit var sum: ByteArray
        loop@ while (true) {
            when (val i = inp.decodeElementIndex(descriptor)) {
                CompositeDecoder.READ_DONE -> break@loop
                0 -> data = inp.decodeSerializableElement(descriptor, i, dataSerializer)
                1 -> sum = HexConverter.parseHexBinary(inp.decodeStringElement(descriptor, i))
                else -> throw SerializationException("Unknown index $i")
            }
        }
        inp.endStructure(descriptor)
        return CheckedData(data, sum)
    }
}

@Serializable
data class StringData(@Serializable(with = CheckedDataSerializer::class) val data: CheckedData<String>)
@Serializable
data class IntData(@Serializable(with = CheckedDataSerializer::class) val data: CheckedData<Int>)


class GenericCustomSerializerTest {
    @Test
    fun testStringData() {
        val original = StringData(CheckedData("my data", byteArrayOf(42, 32)))
        val s = Json.stringify(StringData.serializer(), original)
        assertEquals("""{"data":{"data":"my data","checkSum":"2A20"}}""", s)
        val restored = Json.parse(StringData.serializer(), s)
        assertEquals(original, restored)
    }

    @Test
    fun testIntData() {
        val original = IntData(CheckedData(42, byteArrayOf(42)))
        val s = Json.stringify(IntData.serializer(), original)
        assertEquals("""{"data":{"data":42,"checkSum":"2A"}}""", s)
        val restored = Json.parse(IntData.serializer(), s)
        assertEquals(original, restored)
    }
}
