package kotlinx.serialization

import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*
import kotlin.test.*

class JsonOverwriteKeyTest : JsonTestBase() {
    private val json = Json

    @Serializable
    data class Data(val a: Int)

    @Serializable
    data class Updatable(val d: Data)

    @Test
    fun testLatestValueWins() {
        val parsed: Updatable = default.decodeFromString("""{"d":{"a":"42"},"d":{"a":43}}""")
        assertEquals(Data(43), parsed.d)
    }

    @Serializable
    data class WrappedMap<T>(val mp: Map<String, T>)

    @Test
    fun testLatestKeyInMap() {
        val parsed = json.decodeFromString(WrappedMap.serializer(Int.serializer()), """{"mp": { "x" : 23, "x" : 42, "y": 4 }}""")
        assertEquals(WrappedMap(mapOf("x" to 42, "y" to 4)), parsed)
    }

    @Test
    fun testLastestListValueInMap() {
        val parsed = json.decodeFromString(WrappedMap.serializer(ListSerializer(Int.serializer())), """{"mp": { "x" : [23], "x" : [42], "y": [4] }}""")
        assertEquals(WrappedMap(mapOf("x" to listOf(42), "y" to listOf(4))), parsed)
    }
}
