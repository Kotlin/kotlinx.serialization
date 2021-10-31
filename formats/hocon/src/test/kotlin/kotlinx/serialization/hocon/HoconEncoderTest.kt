package kotlinx.serialization.hocon

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Test

class HoconEncoderTest {

    @Serializable
    data class SimpleConfig(val value: Int)

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
    fun encodeSimpleConfig() {
        val obj = PrimitivesConfig(b = true, i = 42, d = 32.2, c = 'x', s = "string", n = null)
        val config = Hocon.encodeToConfig(obj)

        assertConfigEquals("b = true, i = 42, d = 32.2, c = x, s = string, n = null", config)
    }

    @Serializable
    data class ConfigWithEnum(val e: RegularEnum)

    @Serializable
    enum class RegularEnum { VALUE }

    @Test
    fun encodeConfigWithEnum() {
        val obj = ConfigWithEnum(RegularEnum.VALUE)
        val config = Hocon.encodeToConfig(obj)

        assertConfigEquals("e = VALUE", config)
    }

    @Serializable
    class ConfigWithIterables(
        val array: BooleanArray,
        val set: Set<Int>,
        val list: List<String>,
    )

    @Test
    fun encodeConfigWithIterables() {
        val obj = ConfigWithIterables(
            array = booleanArrayOf(true, false),
            set = setOf(3, 1, 4),
            list = listOf("A", "B"),
        )
        val config = Hocon.encodeToConfig(obj)

        assertConfigEquals(
            """
                array = [true, false]
                set = [3, 1, 4]
                list = [A, B]
            """.trimIndent(),
            config,
        )
    }

    @Serializable
    data class ConfigWithNested(
        val nested: SimpleConfig,
        val nestedList: List<SimpleConfig>,
    )

    @Test
    fun testNestedConfigEncoding() {
        val obj = ConfigWithNested(
            nested = SimpleConfig(1),
            nestedList = listOf(SimpleConfig(2)),
        )
        val config = Hocon.encodeToConfig(obj)

        assertConfigEquals("nested { value = 1 }, nestedList = [{ value: 2 }]", config)
    }

    @Test
    fun testMapEncoding() {
        val objMap = mapOf(
            "one" to SimpleConfig(1),
            "two" to SimpleConfig(2),
        )
        val config = Hocon.encodeToConfig(objMap)

        assertConfigEquals("one { value = 1 }, two { value = 2 }", config)
    }

    @Serializable
    data class ConfigWithDefaults(
        val defInt: Int = 0,
        val defString: String = "",
    )

    @Test
    fun testDefaultsNotEncodedByDefault() {
        val obj = ConfigWithDefaults(defInt = 42)
        val config = Hocon.encodeToConfig(obj)

        assertConfigEquals("defInt = 42", config)
    }

    @Test
    fun testDefaultsEncodedIfEnabled() {
        val hocon = Hocon { encodeDefaults = true }
        val obj = ConfigWithDefaults(defInt = 42)
        val config = hocon.encodeToConfig(obj)

        assertConfigEquals("defInt = 42, defString = \"\"", config)
    }
}

internal fun assertConfigEquals(expected: String, actual: Config) {
    assertEquals(ConfigFactory.parseString(expected), actual)
}
