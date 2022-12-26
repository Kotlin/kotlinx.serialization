@file:UseSerializers(ConfigValueSerializer::class)
package kotlinx.serialization.hocon

import com.typesafe.config.*
import kotlinx.serialization.*
import kotlinx.serialization.hocon.serializers.ConfigValueSerializer
import org.junit.Assert.*
import org.junit.Test
import kotlin.test.assertFailsWith

class HoconConfigValueTest {

    @Serializable
    data class Simple(val d: ConfigValue)

    @Serializable
    data class Nullable(val d: ConfigValue?)

    @Serializable
    data class ConfigList(val ld: List<ConfigValue>)

    @Serializable
    data class ConfigMap(val mp: Map<String, ConfigValue>)

    @Serializable
    data class Complex(
        val i: Int,
        val s: Simple,
        val n: Nullable,
        val l: List<Simple>,
        val ln: List<Nullable>,
        val f: Boolean,
        val ld: List<ConfigValue>,
        val mp: Map<String, ConfigValue>,
    )

    private val configValue = ConfigValueFactory.fromMap(mapOf("value" to "test"))

    @Test
    fun testSerialize() {
        Hocon.encodeToConfig(Simple(configValue)).assertContains("d: { value = \"test\" }")
    }

    @Test
    fun testSerializeNullable() {
        Hocon.encodeToConfig(Nullable(null)).assertContains("d = null")
        Hocon.encodeToConfig(Nullable(configValue)).assertContains("d: { value = \"test\" }")
    }

    @Test
    fun testSerializeList() {
        Hocon.encodeToConfig(ConfigList(List(2) { configValue }))
            .assertContains("ld: [{ value = \"test\" }, { value = \"test\" }]")
    }

    @Test
    fun testSerializeMap() {
        Hocon.encodeToConfig(ConfigMap(mapOf("test" to configValue)))
            .assertContains("mp: { test = { value = \"test\" } }")
    }

    @Test
    fun testSerializeComplex() {
        val obj = Complex(
            i = 6,
            s = Simple(configValue),
            n = Nullable(null),
            l = listOf(Simple(configValue), Simple(configValue)),
            ln = listOf(Nullable(null), Nullable(configValue)),
            f = true,
            ld = listOf(configValue, configValue),
            mp = mapOf("test" to configValue),
        )
        Hocon.encodeToConfig(obj)
            .assertContains("""
                i = 6
                s: { d = { value = "test" } }
                n: { d = null }
                l: [ { d = { value = "test" } }, { d = { value = "test" } } ]
                ln: [ { d = null }, { d = { value = "test" } } ]
                f = true
                ld: [ { value = "test" }, { value = "test" } ]
                mp: { test = { value = "test" } }
            """.trimIndent())
    }

    @Test
    fun testDeserialize() {
        val obj = deserializeConfig("d: { value = \"test\" }", Simple.serializer())
        assertEquals(configValue, obj.d)
    }

    @Test
    fun testDeserializeNullable() {
        var obj = deserializeConfig("d: null", Nullable.serializer())
        assertNull(obj.d)
        obj = deserializeConfig("d: { value = \"test\" }", Nullable.serializer())
        assertEquals(configValue, obj.d)
    }

    @Test
    fun testDeserializeList() {
        val obj = deserializeConfig("ld: [{ value = \"test\" }, { value = \"test\" }]", ConfigList.serializer())
        assertEquals(List(2){ configValue }, obj.ld)
    }

    @Test
    fun testDeserializeMap() {
        val obj = deserializeConfig("""
             mp: { test = { value = "test" } }
        """.trimIndent(), ConfigMap.serializer())
        assertEquals(mapOf("test" to configValue), obj.mp)
    }

    @Test
    fun testDeserializeComplex() {
        val obj = deserializeConfig("""
            i = 6
            s: { d = { value = "test" } }
            n: { d = null }
            l: [ { d = { value = "test" } }, { d = { value = "test" } } ]
            ln: [ { d = null }, { d = { value = "test" } } ]
            f = true
            ld: [ { value = "test" }, { value = "test" } ]
            mp: { test = { value = "test" } }
        """.trimIndent(), Complex.serializer())
        assertEquals(configValue, obj.s.d)
        assertNull(obj.n.d)
        assertEquals(listOf(Simple(configValue), Simple(configValue)), obj.l)
        assertEquals(listOf(Nullable(null), Nullable(configValue)), obj.ln)
        assertEquals(6, obj.i)
        assertTrue(obj.f)
        assertEquals(List(2){ configValue }, obj.ld)
        assertEquals(mapOf("test" to configValue), obj.mp)
    }

    @Test
    fun testThrowsWhenInvalidConfig() {
        val message = "Value at d cannot be read as ConfigValue because it is not a valid HOCON config value"
        assertFailsWith<SerializationException>(message) {
            deserializeConfig("d = null", Simple.serializer())
        }
    }
}
