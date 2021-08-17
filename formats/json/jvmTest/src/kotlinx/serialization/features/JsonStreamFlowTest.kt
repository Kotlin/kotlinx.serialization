package kotlinx.serialization.features

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.StringData
import kotlinx.serialization.json.*
import org.junit.Ignore
import org.junit.Test
import java.io.*
import kotlin.test.assertEquals

class JsonStreamFlowTest {
    val json = Json {}

    suspend inline fun <reified T> Flow<T>.writeToStream(os: OutputStream) {
        collect {
            json.encodeToStream(it, os)
        }
    }

    suspend inline fun <reified T> InputStream.readToFlow(): Flow<T> {
        return flow<T> {
            while(available() != 0) {
                emit(json.decodeFromStream(this@readToFlow))
            }
        }
    }

    val inputString = """{"data":"a"}{"data":"b"}{"data":"c"}"""
    val inputList = listOf(StringData("a"), StringData("b"), StringData("c"))

    @Test
    fun testEncodeSeveralItems() {
        val items = inputList
        val os = ByteArrayOutputStream()
        runBlocking {
            val f = flow<StringData> { items.forEach { emit(it) } }
            f.writeToStream(os)
        }

        assertEquals(inputString, os.toString(Charsets.UTF_8.name()))
    }

    @Test
    @Ignore // todo: InputStream is consumed fully to buffer, looks like mechanism for multiple reading should be embedded in the framework itself
    fun testDecodeSeveralItems() {
        val ins = ByteArrayInputStream(inputString.encodeToByteArray())
        val ml = mutableListOf<StringData>()
        runBlocking {
            ins.readToFlow<StringData>().toCollection(ml)
        }
        assertEquals(inputList, ml)
    }


}
