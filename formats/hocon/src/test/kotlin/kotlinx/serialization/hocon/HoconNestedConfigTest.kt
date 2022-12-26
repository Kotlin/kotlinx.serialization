@file:UseSerializers(NestedConfigSerializer::class)
package kotlinx.serialization.hocon

import com.typesafe.config.*
import kotlinx.serialization.*
import kotlinx.serialization.hocon.serializers.NestedConfigSerializer
import org.junit.Assert.*
import org.junit.Test
import kotlin.test.assertFailsWith

class HoconNestedConfigTest {

    @Serializable
    data class Simple(val d: Config)

    @Serializable
    data class Nullable(val d: Config?)

    @Serializable
    data class ConfigList(val ld: List<Config>)

    @Serializable
    data class ConfigMap(val mp: Map<String, Config>)

    @Serializable
    data class Complex(
        val i: Int,
        val s: Simple,
        val n: Nullable,
        val l: List<Simple>,
        val ln: List<Nullable>,
        val f: Boolean,
        val ld: List<Config>,
        val mp: Map<String, Config>,
    )

    private val nestedConfig = ConfigFactory.parseString("nested { value = \"test\" }")

    @Test
    fun testSerialize() {
        Hocon.encodeToConfig(Simple(nestedConfig)).assertContains("d: { nested: { value = \"test\" } }")
    }

    @Test
    fun testSerializeNullable() {
        Hocon.encodeToConfig(Nullable(null)).assertContains("d = null")
        Hocon.encodeToConfig(Nullable(nestedConfig)).assertContains("d: { nested: { value = \"test\" } }")
    }

    @Test
    fun testSerializeList() {
        Hocon.encodeToConfig(ConfigList(List(2){nestedConfig}))
            .assertContains("ld: [{ nested: { value = \"test\" } }, { nested: { value = \"test\" } }]")
    }

    @Test
    fun testSerializeMap() {
        Hocon.encodeToConfig(ConfigMap(mapOf("test" to nestedConfig)))
            .assertContains("mp: { test = { nested: { value = \"test\" } } }")
    }

    @Test
    fun testSerializeComplex() {
        val obj = Complex(
            i = 6,
            s = Simple(nestedConfig),
            n = Nullable(null),
            l = listOf(Simple(nestedConfig), Simple(nestedConfig)),
            ln = listOf(Nullable(null), Nullable(nestedConfig)),
            f = true,
            ld = listOf(nestedConfig, nestedConfig),
            mp = mapOf("test" to nestedConfig),
        )
        Hocon.encodeToConfig(obj)
            .assertContains("""
                i = 6
                s: { d = { nested: { value = "test" } } }
                n: { d = null }
                l: [ { d = { nested: { value = "test" } } }, { d = { nested: { value = "test" } } } ]
                ln: [ { d = null }, { d = { nested: { value = "test" } } } ]
                f = true
                ld: [ { nested: { value = "test" } }, { nested: { value = "test" } } ]
                mp: { test = { nested: { value = "test" } } }
            """.trimIndent())
    }

    @Test
    fun testDeserialize() {
        val obj = deserializeConfig("d: { nested: { value = \"test\" } }", Simple.serializer())
        assertEquals(nestedConfig, obj.d)
    }

    @Test
    fun testDeserializeNullable() {
        var obj = deserializeConfig("d: null", Nullable.serializer())
        assertNull(obj.d)
        obj = deserializeConfig("d: { nested: { value = \"test\" } }", Nullable.serializer())
        assertEquals(nestedConfig, obj.d)
    }

    @Test
    fun testDeserializeList() {
        val obj = deserializeConfig(
            "ld: [{ nested: { value = \"test\" } }, { nested: { value = \"test\" } }]",
            ConfigList.serializer()
        )
        assertEquals(List(2){ nestedConfig }, obj.ld)
    }

    @Test
    fun testDeserializeMap() {
        val obj = deserializeConfig("""
             mp: { test = { nested: { value = "test" } } }
        """.trimIndent(), ConfigMap.serializer())
        assertEquals(mapOf("test" to nestedConfig), obj.mp)
    }

    @Test
    fun testDeserializeComplex() {
        val obj = deserializeConfig("""
            i = 6
            s: { d = { nested: { value = "test" } } }
            n: { d = null }
            l: [ { d = { nested: { value = "test" } } }, { d = { nested: { value = "test" } } } ]
            ln: [ { d = null }, { d = { nested: { value = "test" } } } ]
            f = true
            ld: [ { nested: { value = "test" } }, { nested: { value = "test" } } ]
            mp: { test = { nested: { value = "test" } } }
        """.trimIndent(), Complex.serializer())
        assertEquals(nestedConfig, obj.s.d)
        assertNull(obj.n.d)
        assertEquals(listOf(Simple(nestedConfig), Simple(nestedConfig)), obj.l)
        assertEquals(listOf(Nullable(null), Nullable(nestedConfig)), obj.ln)
        assertEquals(6, obj.i)
        assertTrue(obj.f)
        assertEquals(List(2){ nestedConfig }, obj.ld)
        assertEquals(mapOf("test" to nestedConfig), obj.mp)
    }

    @Test
    fun testThrowsWhenInvalidConfig() {
        val message = "Value at d cannot be read as Config because it is not a valid HOCON config value"
        assertFailsWith<SerializationException>(message) {
            deserializeConfig("d = unknown", Simple.serializer())
        }
    }
}
