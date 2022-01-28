/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.hocon

import com.typesafe.config.*
import kotlinx.serialization.*
import org.junit.Ignore
import org.junit.Test
import kotlin.test.*

class HoconRootMapTest {
    private val configForRootMap = """
        key1 {
            a = "text1"
            b = 11
            }
        
        key2 {
            a = "text2"
            b = 12
        }
        
        key3 {
            a = "text3"
            b = 13
        }
    """
    private val configWithEmptyObject = "{}"
    private val configWithRootList = "[foo, bar]"
    private val emptyConfig = ""

    @Serializable
    data class CompositeValue(
        val a: String,
        val b: Int
    )

    @Test
    fun testConfigWithRootMap() {
        val config = ConfigFactory.parseString(configForRootMap)
        val obj = Hocon.decodeFromConfig<Map<String, CompositeValue>>(config)

        assertEquals(CompositeValue("text1", 11), obj["key1"])
        assertEquals(CompositeValue("text2", 12), obj["key2"])
        assertEquals(CompositeValue("text3", 13), obj["key3"])
    }

    @Test
    fun testEmptyObjectDecode() {
        val config = ConfigFactory.parseString(configWithEmptyObject)
        // non-null map decoded from empty object as empty map
        val map = Hocon.decodeFromConfig<Map<String, String>>(config)
        assertTrue(map.isEmpty())

        // nullable map decoded from empty object as null - not obvious
        assertNull(Hocon.decodeFromConfig<Map<String, String>?>(config))

        // root-level list in config not supported but nullable list can be decoded from empty object
        assertNull(Hocon.decodeFromConfig<List<String>?>(config))
    }

    @Test
    fun testUnsupportedRootObjectsEncode() {
        assertWrongRootValue("LIST", listOf(1, 1, 2, 3, 5))
        assertWrongRootValue("NUMBER", 42)
        assertWrongRootValue("BOOLEAN", false)
        assertWrongRootValue("NULL", null)
        assertWrongRootValue("STRING", "string")
    }

    private fun assertWrongRootValue(type: String, rootValue: Any?) {
        val message = "Value of type '$type' can't be used at the root of HOCON Config. " +
                "It should be either object or map."
        assertFailsWith<SerializationException>(message) { Hocon.encodeToConfig(rootValue) }
    }

    @Ignore
    @Test
    fun testErrors() {
        // because com.typesafe:config lib not support list in root we can't decode non-null list
        val config = ConfigFactory.parseString(configWithEmptyObject)
        assertFailsWith<NoSuchElementException> {
            Hocon.decodeFromConfig<List<String>>(config)
        }

        // because com.typesafe:config lib not support list in root it fails while parsing
        assertFailsWith<Exception> {
            ConfigFactory.parseString(configWithRootList)
        }

        // com.typesafe:config lib parse empty config as empty object
        ConfigFactory.parseString(emptyConfig)
    }
}
