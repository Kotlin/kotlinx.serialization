import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObjectSerializer
import kotlinx.serialization.schema.*

@Serializable
data class Data(val a: Int, @Optional val b: String = "42")

fun main(args: Array<String>) {
    println(">> Sample of Json schema: ")
    println(Json(indented = true).stringify(JsonObjectSerializer, JsonSchema(Data.serializer().descriptor)))
    println(">> Sample of Protobuf schema: ")
    println(ProtoSchema(Data.serializer().descriptor))
}
