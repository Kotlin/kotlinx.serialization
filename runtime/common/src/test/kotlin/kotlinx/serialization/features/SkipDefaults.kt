package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.json.Json
import kotlin.test.*

@Serializable
data class Data(val bar: String, @Optional val foo: Int = 42) {
    @Optional
    var list: List<Int> = emptyList()

    @Optional
    val listWithSomething: List<Int> = listOf(1, 2, 3)
}

class SkipDefaultsTest {
    private val json = Json(encodeDefaults = false)
    private val cbor = Cbor(encodeDefaults = false)

    @Test
    fun serializeCorrectlyDefaults() {
        val d = Data("bar")
        assertEquals("""{"bar":"bar","foo":42,"list":[],"listWithSomething":[1,2,3]}""", Json.stringify(Data.serializer(), d))
    }

    @Test
    fun serializeCorrectly() {
        val d = Data("bar", 100).apply { list = listOf(1, 2, 3) }
        assertEquals("""{"bar":"bar","foo":100,"list":[1,2,3]}""", json.stringify(Data.serializer(), d))
    }

    @Test
    fun serializeCorrectlyAndDropBody() {
        val d = Data("bar", 43)
        assertEquals("""{"bar":"bar","foo":43}""", json.stringify(Data.serializer(), d))
    }

    @Test
    fun serializeCorrectlyAndDropAll() {
        val d = Data("bar")
        assertEquals("""{"bar":"bar"}""", json.stringify(Data.serializer(), d))
    }

    @Test
    fun cborDropsDefaults() {
        val d = Data("bar")
        assertEquals("bf6362617263626172ff", cbor.dumps(Data.serializer(), d))
    }
}
