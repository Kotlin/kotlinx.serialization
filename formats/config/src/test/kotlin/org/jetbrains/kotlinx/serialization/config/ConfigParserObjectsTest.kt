/*
 *  Copyright 2018 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.jetbrains.kotlinx.serialization.config

import com.typesafe.config.ConfigFactory
import kotlinx.serialization.KSerialLoader
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Test

internal inline fun <reified T> deserializeConfig(configString: String, loader: KSerialLoader<T>): T =
    ConfigParser.parse(ConfigFactory.parseString(configString), loader)

class ConfigParserObjectsTest() {

    @Serializable
    data class Simple(val a: Int)

    @Serializable
    data class ConfigObjectInner(val e: String)

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
            kek: {e: foo }
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
            assertEquals(mapOf("kek" to ConfigObjectInner("foo"), "bar" to ConfigObjectInner("baz")), m)
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
        val simpl = ConfigParser.parse(conf, Simple.serializer())
        assertEquals(Simple(42), simpl)
    }

    @Test
    fun `config with object`() {
        val conf = ConfigFactory.parseString("a: 42, b: {e = foo}")
        assertEquals(42, conf.getInt("a"))
        assertEquals("foo", conf.getString("b.e"))
        val obj = ConfigParser.parse(conf, ConfigObject.serializer())
        assertEquals(42, obj.a)
        assertEquals("foo", obj.b.e)
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
        assertEquals(listOf(42,43,44).map { Simple(it) }, obj.x)
    }

    @Test
    fun `config with map`() {
        val obj = deserializeConfig("x: { a = 42, b = 43, c = 44 }", ConfWithMap.serializer())
        assertEquals(mapOf("a" to 42, "b" to 43, "c" to 44), obj.x)
    }
}
