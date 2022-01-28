package kotlinx.serialization.hocon

import kotlinx.serialization.*
import org.junit.Test
import kotlin.test.*

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
        val listNullable: List<Set<SimpleConfig?>?>,
    )

    @Test
    fun testEncodeConfigWithIterables() {
        val obj = ConfigWithIterables(
            array = booleanArrayOf(true, false),
            set = setOf(3, 1, 4),
            list = listOf("A", "B"),
            listNullable = listOf(null, setOf(SimpleConfig(42), null)),
        )
        val config = Hocon.encodeToConfig(obj)

        config.assertContains(
            """
                array = [true, false]
                set = [3, 1, 4]
                list = [A, B]
                listNullable = [null, [{ value: 42 }, null]]
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
            "three" to null,
            null to SimpleConfig(0),
        )
        val config = Hocon.encodeToConfig(objMap)

        config.assertContains(
            """
                one { value = 1 }
                two { value = 2 }
                three: null
                null { value = 0 }
            """
        )
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

    @Serializable
    data class PrimitiveKeysMaps(
        val number: Map<Int, String>,
        val boolean: Map<Boolean, String>,
        val nullable: Map<String?, String>,
        val enum: Map<RegularEnum, String>,
    )

    @Test
    fun testPrimitiveMapKeysEncoding() {
        val obj = PrimitiveKeysMaps(
            number = mapOf(42 to "these"),
            boolean = mapOf(true to "keys"),
            nullable = mapOf(null to "are"),
            enum = mapOf(RegularEnum.VALUE to "strings"),
        )
        val config = Hocon.encodeToConfig(obj)

        config.assertContains(
            """
                number { "42" = these }
                boolean { "true" = keys }
                nullable { "null" = are }
                enum { "VALUE" = strings }
            """
        )
    }

    @Test
    fun testEncodeMapWithUnsupportedKeys() {
        assertWrongMapKey("LIST", listOf(1, 1, 2, 3, 5))
        assertWrongMapKey("OBJECT", mapOf(1 to "one", 2 to "two"))
    }

    private fun assertWrongMapKey(type: String, key: Any?) {
        val message = "Value of type '$type' can't be used in HOCON as a key in the map. " +
                "It should have either primitive or enum kind."
        val obj = mapOf(key to "value")
        assertFailsWith<SerializationException>(message) { Hocon.encodeToConfig(obj) }
    }
}
