// This file was automatically generated from formats.md by Knit tool. Do not edit.
package example.exampleFormats09

import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*
import kotlinx.serialization.protobuf.schema.ProtoBufSchemaGenerator

@Serializable
data class SampleData(
    val amount: Long,
    val description: String?,
    val department: String = "QA"
)

@OptIn(ExperimentalSerializationApi::class)
fun main() {
  val descriptors = listOf(SampleData.serializer().descriptor)
  val schemas = ProtoBufSchemaGenerator.generateSchemaText(descriptors)
  println(schemas)
}
