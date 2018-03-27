// Auto-generated file, do not modify!
import kotlin.Int
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlinx.serialization.KInput
import kotlinx.serialization.KOutput
import kotlinx.serialization.KSerialClassDesc
import kotlinx.serialization.KSerializer
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.Optional
import kotlinx.serialization.SerialId
import kotlinx.serialization.internal.ArrayListSerializer
import kotlinx.serialization.internal.SerialClassDescImplTagged

data class Data(@SerialId(1)
val a: Int, @SerialId(2)
val b: String) {
    @Suppress("NAME_SHADOWING")
    object serializer : KSerializer<Data> {
        override val serialClassDesc: KSerialClassDesc =
                object : SerialClassDescImplTagged("Data") {
            init {
                addTaggedElement("a", 1)
                addTaggedElement("b", 2)
            }
        }

        override fun save(output: KOutput, obj: Data) {
            val output = output.writeBegin(serialClassDesc)
            output.writeIntElementValue(serialClassDesc, 0, obj.a)
            output.writeStringElementValue(serialClassDesc, 1, obj.b)
            output.writeEnd(serialClassDesc)
        }

        override fun load(input: KInput): Data {
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
        override val serialClassDesc: KSerialClassDesc =
                object : SerialClassDescImplTagged("DataList") {
            init {
                addTaggedElement("list", 1)
            }
        }

        override fun save(output: KOutput, obj: DataList) {
            val output = output.writeBegin(serialClassDesc)
            output.writeSerializableElementValue(serialClassDesc, 0, ArrayListSerializer(Data.serializer), obj.list)
            output.writeEnd(serialClassDesc)
        }

        override fun load(input: KInput): DataList {
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
