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

    @Serializable
    data class ConfigWithEnum(val e: RegularEnum)

    @Serializable
    enum class RegularEnum { VALUE }

    @Test
    fun `encode config with enum`() {
        // Given
        val obj = ConfigWithEnum(RegularEnum.VALUE)

        // When
        val config = Hocon.encodeToConfig(obj)

        // Then
        assertConfigEquals("e = VALUE", config)
    }

    @Serializable
    class ConfigWithIterables(
        val array: BooleanArray,
        val set: Set<Int>,
        val list: List<String>,
    )

    @Test
    fun `encode config with iterables`() {
        // Given
        val obj = ConfigWithIterables(
            array = booleanArrayOf(true, false),
            set = setOf(3, 1, 4),
            list = listOf("A", "B"),
        )

        // When
        val config = Hocon.encodeToConfig(obj)

        // Then
        assertConfigEquals(
            """
                array = [true, false]
                set = [3, 1, 4]
                list = [A, B]
            """.trimIndent(),
            config,
        )
    }

    private fun assertConfigEquals(expected: String, actual: Config) {
        assertEquals(ConfigFactory.parseString(expected), actual)
    }
}
