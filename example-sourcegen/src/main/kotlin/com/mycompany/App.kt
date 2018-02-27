package com.mycompany

import com.mycompany.model.MyData
import kotlinx.serialization.internal.HexConverter
import kotlinx.serialization.json.JSON
import kotlinx.serialization.protobuf.ProtoBuf

fun main(args: Array<String>) {
    val data = MyData(42)
    println("Serialized data in JSON: ${JSON.stringify(MyData.serializer, data)}")
    println("Serialized data in Protobuf: ${HexConverter.printHexBinary(ProtoBuf.dump(MyData.serializer, data))}")
}
