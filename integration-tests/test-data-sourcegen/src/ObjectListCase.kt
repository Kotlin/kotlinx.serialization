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
import kotlinx.serialization.Encoder
import kotlinx.serialization.KInput
import kotlinx.serialization.KSerializer
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.Optional
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.SerialId
import kotlinx.serialization.internal.ArrayListSerializer
import kotlinx.serialization.internal.SerialClassDescImplTagged

data class Data(@SerialId(1)
val a: Int, @SerialId(2)
val b: String) {
    @Suppress("NAME_SHADOWING")
    object serializer : KSerializer<Data> {
        override val serialClassDesc: SerialDescriptor =
                object : SerialClassDescImplTagged("Data") {
            init {
                addTaggedElement("a", 1)
                addTaggedElement("b", 2)
            }
        }

        override fun serialize(output: Encoder, obj: Data) {
            val output = output.beginStructure(serialClassDesc)
            output.encodeIntElementValue(serialClassDesc, 0, obj.a)
            output.encodeStringElementValue(serialClassDesc, 1, obj.b)
            output.endStructure(serialClassDesc)
        }

        override fun deserialize(input: KInput): Data {
            val input = input.readBegin(serialClassDesc)
            var local0: Int? = null
            var local1: String? = null
            var bitMask: Int = 0
            mainLoop@while (true) {
                val idx = input.readElement(serialClassDesc)
                when (idx) {
                    -1 -> {
                        break@mainLoop
                    }
                    0 -> {
                        local0 = input.readIntElementValue(serialClassDesc, 0)
                        bitMask = bitMask or 1
                    }
                    1 -> {
                        local1 = input.readStringElementValue(serialClassDesc, 1)
                        bitMask = bitMask or 2
                    }
                }
            }
            input.readEnd(serialClassDesc)
            if (bitMask and 1 == 0) {
                throw MissingFieldException("a")
            }
            if (bitMask and 2 == 0) {
                throw MissingFieldException("b")
            }
            return Data(local0!!, local1!!)
        }
    }
    companion object {
        fun serializer() = serializer
    }
}

data class DataList(@Optional
@SerialId(1)
val list: List<Data> = emptyList()) {
    @Suppress("NAME_SHADOWING")
    object serializer : KSerializer<DataList> {
        override val serialClassDesc: SerialDescriptor =
                object : SerialClassDescImplTagged("DataList") {
            init {
                addTaggedElement("list", 1)
            }
        }

        override fun serialize(output: Encoder, obj: DataList) {
            val output = output.beginStructure(serialClassDesc)
            output.encodeSerializableElementValue(serialClassDesc, 0, ArrayListSerializer(Data.serializer), obj.list)
            output.endStructure(serialClassDesc)
        }

        override fun deserialize(input: KInput): DataList {
            val input = input.readBegin(serialClassDesc)
            var local0: List<Data>? = null
            var bitMask: Int = 0
            mainLoop@while (true) {
                val idx = input.readElement(serialClassDesc)
                when (idx) {
                    -1 -> {
                        break@mainLoop
                    }
                    0 -> {
                        local0 = input.readSerializableElementValue(serialClassDesc, 0, ArrayListSerializer(Data.serializer))
                        bitMask = bitMask or 1
                    }
                }
            }
            input.readEnd(serialClassDesc)
            if (bitMask and 1 == 0) {
                local0 = emptyList()
            }
            return DataList(local0!!)
        }
    }
    companion object {
        fun serializer() = serializer
    }
}
