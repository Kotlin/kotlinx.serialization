// This file was automatically generated from formats.md by Knit tool. Do not edit.
package example.exampleFormats08

import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*

@Serializable
data class Data(
    @ProtoNumber(1) val name: String,
    @ProtoOneOf val phone: IPhoneType,
)
@Serializable sealed interface IPhoneType
@Serializable @ProtoNumber(2) @JvmInline value class HomePhone(val number: String): IPhoneType
@Serializable @ProtoNumber(3) data class WorkPhone(val number: String): IPhoneType

fun main() {
  val dataTom = Data("Tom", HomePhone("123"))
  val stringTom = ProtoBuf.encodeToHexString(dataTom)
  val dataJerry = Data("Jerry", WorkPhone("789"))
  val stringJerry = ProtoBuf.encodeToHexString(dataJerry)
  println(stringTom)
  println(stringJerry)
  println(ProtoBuf.decodeFromHexString<Data>(stringTom))
  println(ProtoBuf.decodeFromHexString<Data>(stringJerry))
}
