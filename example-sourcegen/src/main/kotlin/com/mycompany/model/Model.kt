// Auto-generated file, do not modify!
package com.mycompany.model

import kotlin.Int
import kotlin.String
import kotlin.collections.List
import kotlinx.serialization.KInput
import kotlinx.serialization.KOutput
import kotlinx.serialization.KSerialClassDesc
import kotlinx.serialization.KSerializer
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.Optional
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
    object serializer : KSerializer<MyData> {
        override val serialClassDesc: KSerialClassDesc =
                object : SerialClassDescImplTagged("com.mycompany.model.MyData") {
            init {
                addElement("x")
                addElement("y")
                addElement("intList")
            }
        }

        override fun save(output: KOutput, obj: MyData) {
            val output = output.writeBegin(serialClassDesc)
            output.writeNullableSerializableElementValue(serialClassDesc, 0, NullableSerializer(IntSerializer), obj.x)
            output.writeStringElementValue(serialClassDesc, 1, obj.y)
            output.writeSerializableElementValue(serialClassDesc, 2, ArrayListSerializer(IntSerializer), obj.intList)
            output.writeEnd(serialClassDesc)
        }

        override fun load(input: KInput): MyData {
            val input = input.readBegin(serialClassDesc)
            var local0: Int? = null
            var local1: String? = null
            var local2: List<Int>? = null
            var bitMask: Int = 0
            mainLoop@while (true) {
                val idx = input.readElement(serialClassDesc)
                when (idx) {
                    -1 -> {
                        break@mainLoop
                    }
                    0 -> {
                        local0 = input.readNullableSerializableElementValue(serialClassDesc, 0, NullableSerializer(IntSerializer))
                        bitMask = bitMask or 1
                    }
                    1 -> {
                        local1 = input.readStringElementValue(serialClassDesc, 1)
                        bitMask = bitMask or 2
                    }
                    2 -> {
                        local2 = input.readSerializableElementValue(serialClassDesc, 2, ArrayListSerializer(IntSerializer))
                        bitMask = bitMask or 4
                    }
                }
            }
            input.readEnd(serialClassDesc)
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
}
