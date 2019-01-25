/*
 * Copyright 2017-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.polymorphic

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.test.*

class JsonMapPolymorphismTest : JsonTestBase() {

    @BeforeTest
    fun setUp() {
        unquoted.install(polymorphicTestModule)
    }

    @Serializable
    private data class MapWrapper(val map: Map<String, @Polymorphic InnerBase>)

    @Test
    fun testPolymorphicValues() = parametrizedTest { useStreaming ->
        val wrapper = MapWrapper(mapOf("k1" to InnerImpl(1), "k2" to InnerImpl2(2)))
        val serialized = unquoted.stringify(wrapper, useStreaming)
        assertEquals("{map:" +
                "{k1:{\$type:kotlinx.serialization.json.polymorphic.InnerImpl,field:1,str:default,nullable:null}," +
                "k2:{\$type:kotlinx.serialization.json.polymorphic.InnerImpl2,field:2}}}", serialized)
        assertEquals(wrapper, unquoted.parse(serialized, useStreaming))
    }

    @Serializable
    private data class MapNullableWrapper(val map: Map<String, @Polymorphic InnerBase?>)

    @Test
    fun testPolymorphicNullableValues() = parametrizedTest { useStreaming ->
        val wrapper = MapNullableWrapper(mapOf("k1" to InnerImpl(1), "k2" to null))
        val serialized = unquoted.stringify(wrapper, useStreaming)
        assertEquals("{map:" +
                "{k1:{\$type:kotlinx.serialization.json.polymorphic.InnerImpl,field:1,str:default,nullable:null}," +
                "k2:null}}", serialized)
        assertEquals(wrapper, unquoted.parse(serialized, useStreaming))
    }

//    @Test
//    fun testPolymorphicKeys() { // Actually I had no idea we support this kind of pattern. TODO
//        val wrapper = MapKeys(mapOf(InnerImpl(1) to "k2", InnerImpl2(2) to "k2"))
//        val serialized = unquoted.stringify(wrapper, true)
//        println(serialized)
//    }
}