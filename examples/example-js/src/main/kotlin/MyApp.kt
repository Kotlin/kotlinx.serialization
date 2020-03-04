import kotlinx.serialization.*
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoId
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLTextAreaElement
import kotlin.browser.document

@Serializable
data class Data(@ProtoId(1) val a: Int, @ProtoId(2) val b: String)

@Serializable
data class DataList(@ProtoId(1) val list: List<Data> = emptyList())

fun main() {

    val btn = document.getElementById("submit") as HTMLButtonElement
    val txtInput = document.getElementById("txt") as HTMLTextAreaElement
    val kotlinLabel = document.getElementById("kotlin") as HTMLTextAreaElement
    val jsonLabel = document.getElementById("json") as HTMLTextAreaElement
    val protoLabel = document.getElementById("proto") as HTMLTextAreaElement
    val cborLabel = document.getElementById("cbor") as HTMLTextAreaElement

    fun convert() {
        val txt: String = txtInput.value
        try {
            val json = Json { prettyPrint = true }
            val serial = DataList.serializer()
            val kotl = json.parse(serial, txt)
            val jsonString = json.stringify(serial, kotl)
            val proto = ProtoBuf.dumps(serial, kotl)
            val cbor = Cbor.dumps(serial, kotl)

            kotlinLabel.value = kotl.toString()
            jsonLabel.value = jsonString
            protoLabel.value = proto
            cborLabel.value = cbor
        } catch (e: Exception) {
            kotlinLabel.value = e.message ?: "Unknown error"
            protoLabel.value = ""
            cborLabel.value = ""
        }
    }

    btn.onclick = { convert() }

    convert()
}
