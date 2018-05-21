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

// Auto-generated file, do not modify!
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.Optional
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.Transient
import kotlinx.serialization.internal.ArrayListSerializer
import kotlinx.serialization.internal.IntSerializer
import kotlinx.serialization.internal.NullableSerializer
import kotlinx.serialization.internal.SerialClassDescImplTagged

data class MyData(
        val x: Int?,
        @Optional
        val y: String = "foo",
        val intList: List<Int> = listOf(1,2,3),
        @Transient
        val trans: Int = 42
) {
    @Suppress("NAME_SHADOWING")
    object serializer : KSerializer<MyData> {
        override val serialClassDesc: SerialDescriptor =
                object : SerialClassDescImplTagged("MyData") {
            init {
                addElement("x")
                addElement("y")
                addElement("intList")
            }
        }

        override fun serialize(output: Encoder, obj: MyData) {
            val output = output.beginStructure(serialClassDesc)
            output.encodeNullableSerializableElementValue(serialClassDesc, 0, NullableSerializer(IntSerializer), obj.x)
            output.encodeStringElementValue(serialClassDesc, 1, obj.y)
            output.encodeSerializableElementValue(serialClassDesc, 2, ArrayListSerializer(IntSerializer), obj.intList)
            output.endStructure(serialClassDesc)
        }

        override fun deserialize(input: Decoder): MyData {
            val input = input.beginStructure(serialClassDesc)
            var local0: Int? = null
            var local1: String? = null
            var local2: List<Int>? = null
            var bitMask: Int = 0
            mainLoop@while (true) {
                val idx = input.decodeElement(serialClassDesc)
                when (idx) {
                    -1 -> {
                        break@mainLoop
                    }
                    0 -> {
                        local0 = input.decodeNullableSerializableElementValue(serialClassDesc, 0, NullableSerializer(IntSerializer))
                        bitMask = bitMask or 1
                    }
                    1 -> {
                        local1 = input.decodeStringElementValue(serialClassDesc, 1)
                        bitMask = bitMask or 2
                    }
                    2 -> {
                        local2 = input.decodeSerializableElementValue(serialClassDesc, 2, ArrayListSerializer(IntSerializer))
                        bitMask = bitMask or 4
                    }
                }
            }
            input.endStructure(serialClassDesc)
            if (bitMask and 1 == 0) {
                throw MissingFieldException("x")
            }
            if (bitMask and 2 == 0) {
                local1 = "foo"
            }
            if (bitMask and 4 == 0) {
                throw MissingFieldException("intList")
            }
            return MyData(local0, local1!!, local2!!, 42)
        }
    }
    companion object {
        fun serializer() = serializer
    }
}
