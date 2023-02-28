package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.json.Json.Default.decodeFromString
import org.junit.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.*

class MissingFieldExceptionWithPathTest {

    @Test // Repro for #2212
    fun testMfeIsNotReappliedMultipleTimes() {
        val inputMalformed = """{"title": "...","cast": [{}]"""
        try {
            Json.decodeFromString<Movie>(inputMalformed)
            fail("Unreacheable state")
        } catch (e: MissingFieldException) {
            val fullStackTrace = e.stackTraceToString()
            val i1 = fullStackTrace.toString().indexOf("at path")
            val i2 = fullStackTrace.toString().lastIndexOf("at path")
            assertEquals(i1, i2)
            assertTrue(i1 != -1)
        }
    }

    @Serializable
    data class Movie(
        val title: String,
        val cast: List<Cast>,
    )

    @Serializable
    data class Cast(
        val name: String
    )
}
