// This file was automatically generated from formats.md by Knit tool. Do not edit.
package example.exampleFormats09

import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Data(
    @ProtoNumber(1) val name: String,
    @ProtoUnknownFields val unknownFields: ProtoMessage = ProtoMessage.Empty
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class NewData(
    @ProtoNumber(1) val name: String,
    @ProtoNumber(2) val age: Int,
)

@OptIn(ExperimentalSerializationApi::class)
fun main() {
  val dataFromNewBinary = NewData("Tom", 25)
  val hexString = ProtoBuf.encodeToHexString(dataFromNewBinary)
  val dataInOldBinary = ProtoBuf.decodeFromHexString<Data>(hexString)
  val hexOfOldData = ProtoBuf.encodeToHexString(dataInOldBinary)
  println(hexOfOldData)
  println(hexString)
  assert(hexOfOldData == hexString)
}
