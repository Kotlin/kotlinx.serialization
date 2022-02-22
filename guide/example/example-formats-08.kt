// This file was automatically generated from formats.md by Knit tool. Do not edit.
package example.exampleFormats08

@Serializable
data class SampleData(
    val amount: Long,
    val description: String?,
    val department: String = "QA"
)

val descriptors = listOf(SampleData.serializer().descriptor)
val schemas = ProtoBufSchemaGenerator.generateSchemaText(descriptors)
println(schemas)
