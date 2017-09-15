import kotlinx.serialization.Optional
import kotlinx.serialization.SerialId
import kotlinx.serialization.Serializable
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
    val jsonInput = document.getElementById("json") as HTMLTextAreaElement
    val kotlinLabel = document.getElementById("kotlin") as HTMLTextAreaElement
    val protoLabel = document.getElementById("proto") as HTMLTextAreaElement
    val cborLabel = document.getElementById("cbor") as HTMLTextAreaElement

    fun convert() {
        val txt: String = jsonInput.value
        try {

            val kotl = JSON.parse<DataList>(txt)
            val proto = ProtoBuf.dumps(kotl)
            val cbor = CBOR.dumps(kotl)

            kotlinLabel.value = kotl.toString()
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