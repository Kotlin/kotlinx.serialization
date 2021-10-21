package kotlinx.serialization.hocon

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Test

class HoconEncoderTest {

    @Serializable
    data class PrimitivesConfig(
        val b: Boolean,
        val i: Int,
        val d: Double,
        val c: Char,
        val s: String,
        val n: String?,
    )

    @Test
    fun `encode simple config`() {
        // Given
        val obj = PrimitivesConfig(b = true, i = 42, d = 32.2, c = 'x', s = "string", n = null)

        // When
        val config = Hocon.encodeToConfig(obj)

        // Then
        assertConfigEquals("b = true, i = 42, d = 32.2, c = x, s = string, n = null", config)
    }

    private fun assertConfigEquals(expected: String, actual: Config) {
        assertEquals(ConfigFactory.parseString(expected), actual)
    }
}
