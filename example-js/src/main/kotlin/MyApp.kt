import kotlinx.serialization.*
import kotlinx.serialization.cbor.CBOR
import kotlinx.serialization.json.JSON
import kotlinx.serialization.protobuf.ProtoBuf
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLTextAreaElement
import kotlin.browser.document

@Serializable
data class Data(@SerialId(1) val a: Int, @SerialId(2) val b: String)

@Serializable
data class DataList(@SerialId(1) @Optional val list: List<Data> = emptyList())

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

            val serial = DataList.serializer()
            val kotl = JSON.parse(serial, txt)
            val json = JSON.indented.stringify(serial, kotl)
            val proto = ProtoBuf.dumps(serial, kotl)
            val cbor = CBOR.dumps(serial, kotl)

            kotlinLabel.value = kotl.toString()
            jsonLabel.value = json
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
