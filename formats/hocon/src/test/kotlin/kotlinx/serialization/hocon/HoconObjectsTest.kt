/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.hocon

import com.typesafe.config.*
import kotlinx.serialization.*
import org.junit.*
import org.junit.Assert.*

internal inline fun <reified T> deserializeConfig(
    configString: String,
    deserializer: DeserializationStrategy<T>,
    useNamingConvention: Boolean = false
): T {
    val ucnc = useNamingConvention
    return Hocon { useConfigNamingConvention = ucnc }
        .decodeFromConfig(deserializer, ConfigFactory.parseString(configString))
}

class ConfigParserObjectsTest {

    @Serializable
    data class Simple(val a: Int)

    @Serializable
    data class ConfigObjectInner(val e: String, val f: Float = 1.1f)

    @Serializable
    data class ConfigObject(val a: Int, val b: ConfigObjectInner)

    @Serializable
    data class ConfWithList(val a: Int, val b: List<Int>)

    @Serializable
    data class ConfWithMap(val x: Map<String, Int>)

    @Serializable
    data class NestedObj(val x: List<Simple>)

    @Serializable
    data class ComplexConfig(
        val i: Int,
        val s: String,
        val iList: List<Int>,
        val inner: List<Simple>,
        val ll: List<List<String>>,
        val m: Map<String, ConfigObjectInner>
    )

    @Serializable
    data class VeryComplexConfig(
        val l: List<Map<String, List<Simple?>>?>,
        val m: Map<String, NestedObj?>?
    )

    private val configString = """
        i = 42
        s = "foo"
        iList: [1,2,3]
        inner = [{ a: 100500 }]
        ll = [[a, b],[x,z]]
        m : {
            kek: {e: foo, f: 5.6 }
            bar: {e: baz }
        }
"""

    private val complexConfigString = """
        l = [ { x = [ { a:42 }, null] }
        null
        ]
        m = {
            x: null
            y: { x = [ {a=43} ] }
        }
"""

    @Test
    fun `complex config`() {
        val obj = deserializeConfig(configString, ComplexConfig.serializer())
        with(obj) {
            assertEquals(42, i)
            assertEquals("foo", s)
            assertEquals(listOf(1, 2, 3), iList)
            assertEquals(listOf(Simple(100500)), inner)
            assertEquals(listOf(listOf("a", "b"), listOf("x", "z")), ll)
            assertEquals(mapOf("kek" to ConfigObjectInner("foo", f = 5.6f), "bar" to ConfigObjectInner("baz")), m)
        }
    }

    @Test
    fun `very complex config`() {
        val obj = deserializeConfig(complexConfigString, VeryComplexConfig.serializer())
        with(obj) {
            assertEquals(
                listOf(
                    mapOf(
                        "x" to listOf(Simple(42), null)
                    ),
                    null
                ), l
            )
            assertEquals(
                mapOf(
                    "x" to null,
                    "y" to NestedObj(listOf(Simple(43)))
                ), m
            )
        }
    }

    @Test
    fun `simple config`() {
        val conf = ConfigFactory.parseString("a: 42")
        assertEquals(42, conf.getInt("a"))
        val simple = Hocon.decodeFromConfig(Simple.serializer(), conf)
        assertEquals(Simple(42), simple)
    }

    @Test
    fun `config with object`() {
        val conf = ConfigFactory.parseString("a: 42, b: {e = foo}")
        assertEquals(42, conf.getInt("a"))
        assertEquals("foo", conf.getString("b.e"))
        val obj = Hocon.decodeFromConfig(ConfigObject.serializer(), conf)
        assertEquals(42, obj.a)
        assertEquals("foo", obj.b.e)
        assertEquals(1.1f, obj.b.f)
    }

    @Test
    fun `config with list`() {
        val obj = deserializeConfig("a: 42, b: [1,2,3,]", ConfWithList.serializer())
        assertEquals(42, obj.a)
        assertEquals(listOf(1, 2, 3), obj.b)
    }

    @Test
    fun `config with nested object`() {
        val obj = deserializeConfig("x: [{a: 42}, {a: 43}, {a: 44}]", NestedObj.serializer())
        assertEquals(listOf(42, 43, 44).map { Simple(it) }, obj.x)
    }

    @Test
    fun `config with map`() {
        val obj = deserializeConfig("x: { a = 42, b = 43, c = 44 }", ConfWithMap.serializer())
        assertEquals(mapOf("a" to 42, "b" to 43, "c" to 44), obj.x)
    }
}
