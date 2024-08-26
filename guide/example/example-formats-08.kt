// This file was automatically generated from formats.md by Knit tool. Do not edit.
package example.exampleFormats08

import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*

// The outer class
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class Data(
    @ProtoNumber(1) val name: String,
    @ProtoOneOf val phone: IPhoneType?,
)

// The oneof interface
@Serializable sealed interface IPhoneType

// Message holder for home_phone
@OptIn(ExperimentalSerializationApi::class)
@Serializable @JvmInline value class HomePhone(@ProtoNumber(2) val number: String): IPhoneType

// Message holder for work_phone. Can also be a value class, but we leave it as `data` to demonstrate that both variants can be used.
@OptIn(ExperimentalSerializationApi::class)
@Serializable data class WorkPhone(@ProtoNumber(3) val number: String): IPhoneType

@OptIn(ExperimentalSerializationApi::class)
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
