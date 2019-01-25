/*
 * Copyright 2017-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.polymorphic

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.test.*

class JsonListPolymorphismTest : JsonTestBase() {

    @BeforeTest
    fun setUp() {
        unquoted.install(polymorphicTestModule)
    }

    @Serializable
    private data class ListWrapper(val list: List<@Polymorphic InnerBase> )

    @Test
    fun testPolymorphicValues() = parametrizedTest { useStreaming ->
        val wrapper = ListWrapper(listOf(InnerImpl(1), InnerImpl2(2)))
        val serialized = unquoted.stringify(wrapper, useStreaming)
        assertEquals("{list:[" +
                "{\$type:kotlinx.serialization.json.polymorphic.InnerImpl,field:1,str:default,nullable:null}," +
                "{\$type:kotlinx.serialization.json.polymorphic.InnerImpl2,field:2}]}", serialized)
        assertEquals(wrapper, unquoted.parse(serialized, useStreaming))
    }

    @Serializable
    private data class ListNullableWrapper(val list: List<@Polymorphic InnerBase?>)

    @Test
    fun testPolymorphicNullableValues() = parametrizedTest { useStreaming ->
        val wrapper = ListNullableWrapper(listOf(InnerImpl(1), null))
        val serialized = unquoted.stringify(wrapper, useStreaming)
        assertEquals("{list:[" +
                "{\$type:kotlinx.serialization.json.polymorphic.InnerImpl,field:1,str:default,nullable:null}," +
                "null]}", serialized)
        assertEquals(wrapper, unquoted.parse(serialized, useStreaming))
    }
}
