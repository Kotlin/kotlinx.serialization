package kotlinx.serialization.json

import com.google.gson.*
import kotlinx.serialization.*
import org.junit.Test
import kotlin.test.*

class GsonCompatibilityTest {

    @Serializable
    data class Box(val d: Double, val f: Float)

    @Serializable
    data class Wr(val l: List<LE?>)

    @Serializable
    data class LE(val m: Map<String, Box>)

    @Test
    fun testNaN() {
        val wr = Wr(listOf(null, LE(mapOf()), LE(mapOf("k1" to Box(1.0, 1.0f), "k2" to Box(2.0, 2.0f)))))
        val s = """{"l":[null,{"m":{}},{"m":{"k1":{"d":1.0,"f":1.0},"k2":{"d":2.0,"f":2s}}}]}"""
        val e = assertFailsWith<SerializationException> {  Json.decodeFromString<Wr>(s) }
        assertTrue { e.message!!.contains("\$l[2].m.k1.f") }
    }

    @Test
    fun testInfinity() {
        checkCompatibility(Box(Double.POSITIVE_INFINITY, 1.0f))
        checkCompatibility(Box(1.0, Float.POSITIVE_INFINITY))
        checkCompatibility(Box(Double.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY))
    }

    @Test
    fun testNumber() {
        checkCompatibility(Box(23.9, 23.9f))
    }

    private fun checkCompatibility(box: Box) {
        checkCompatibility(box, Gson(), Json)
        checkCompatibility(box, GsonBuilder().serializeSpecialFloatingPointValues().create(), Json { allowSpecialFloatingPointValues = true })
    }

    private fun checkCompatibility(box: Box, gson: Gson, json: Json) {
        val jsonResult = resultOrNull { json.encodeToString(box) }
        val gsonResult = resultOrNull { gson.toJson(box) }
        assertEquals(gsonResult, jsonResult)

        if (jsonResult != null && gsonResult != null) {
            val jsonDeserialized: Box = json.decodeFromString(jsonResult)
            val gsonDeserialized: Box = gson.fromJson(gsonResult, Box::class.java)
            assertEquals(gsonDeserialized, jsonDeserialized)
        }
    }

    private fun resultOrNull(function: () -> String): String? {
        return try {
            function()
        } catch (t: Throwable) {
            null
        }
    }
}
