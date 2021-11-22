package kotlinx.serialization.hocon

import kotlinx.serialization.*
import org.junit.*

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
    fun testEncodeSimpleConfig() {
        val obj = PrimitivesConfig(b = true, i = 42, d = 32.2, c = 'x', s = "string", n = null)
        val config = Hocon.encodeToConfig(obj)

        config.assertContains("b = true, i = 42, d = 32.2, c = x, s = string, n = null")
    }

    @Serializable
    data class ConfigWithEnum(val e: RegularEnum)

    @Serializable
    enum class RegularEnum { VALUE }

    @Test
    fun testEncodeConfigWithEnum() {
        val obj = ConfigWithEnum(RegularEnum.VALUE)
        val config = Hocon.encodeToConfig(obj)

        config.assertContains("e = VALUE")
    }

    @Serializable
    class ConfigWithIterables(
        val array: BooleanArray,
        val set: Set<Int>,
        val list: List<String>,
    )

    @Test
    fun testEncodeConfigWithIterables() {
        val obj = ConfigWithIterables(
            array = booleanArrayOf(true, false),
            set = setOf(3, 1, 4),
            list = listOf("A", "B"),
        )
        val config = Hocon.encodeToConfig(obj)

        config.assertContains(
            """
                array = [true, false]
                set = [3, 1, 4]
                list = [A, B]
            """
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

        config.assertContains("nested { value = 1 }, nestedList = [{ value: 2 }]")
    }

    @Test
    fun testMapEncoding() {
        val objMap = mapOf(
            "one" to SimpleConfig(1),
            "two" to SimpleConfig(2),
        )
        val config = Hocon.encodeToConfig(objMap)

        config.assertContains("one { value = 1 }, two { value = 2 }")
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

        config.assertContains("defInt = 42")
    }

    @Test
    fun testDefaultsEncodedIfEnabled() {
        val hocon = Hocon { encodeDefaults = true }
        val obj = ConfigWithDefaults(defInt = 42)
        val config = hocon.encodeToConfig(obj)

        config.assertContains("defInt = 42, defString = \"\"")
    }
}
