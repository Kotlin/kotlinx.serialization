package kotlinx.serialization.formats.json

import com.google.gson.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.junit.Test
import kotlin.test.*

class GsonCompatibilityTest {

    @Serializable
    data class Box(val d: Double, val f: Float)

    @Test
    fun testNaN() {
        checkCompatibility(Box(Double.NaN, 1.0f))
        checkCompatibility(Box(1.0, Float.NaN))
        checkCompatibility(Box(Double.NaN, Float.NaN))
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
