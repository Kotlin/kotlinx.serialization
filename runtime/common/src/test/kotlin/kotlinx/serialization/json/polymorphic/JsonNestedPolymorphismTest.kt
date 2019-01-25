/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.polymorphic

import kotlinx.serialization.*
import kotlinx.serialization.context.*
import kotlinx.serialization.json.*
import kotlin.test.*

class JsonNestedPolymorphismTest : JsonTestBase() {

    @BeforeTest
    fun setUp() {
        unquoted.install(object : SerialModule {
            override fun registerIn(context: MutableSerialContext) {
                context.registerPolymorphicSerializer(Any::class, InnerImpl::class, InnerImpl.serializer())
                context.registerPolymorphicSerializer(Any::class, InnerImpl2::class, InnerImpl2.serializer())
                context.registerPolymorphicSerializer(InnerBase::class, InnerImpl::class, InnerImpl.serializer())
                context.registerPolymorphicSerializer(InnerBase::class, InnerImpl2::class, InnerImpl2.serializer())
                context.registerPolymorphicSerializer(Any::class, OuterImpl::class, OuterImpl.serializer())
            }
        })
    }

    @Serializable
    private data class NestedGenericsList(val list: List<List<@Polymorphic Any>>)

    @Test
    fun testAnyList() = parametrizedTest { useStreaming ->
        val wrapper = NestedGenericsList(listOf(listOf(InnerImpl(1)), listOf(InnerImpl(2))))
        val serialized = unquoted.stringify(wrapper, useStreaming)
        assertEquals("{list:[[" +
                "{\$type:kotlinx.serialization.json.polymorphic.InnerImpl,field:1,str:default,nullable:null}],[" +
                "{\$type:kotlinx.serialization.json.polymorphic.InnerImpl,field:2,str:default,nullable:null}]]}",
            serialized)

        assertEquals(wrapper, unquoted.parse(serialized, useStreaming))
    }

    @Serializable
    private data class NestedGenericsMap(val list: Map<String, Map<String, @Polymorphic Any>>)

    @Test
    fun testAnyMap() = parametrizedTest { useStreaming ->
        val wrapper = NestedGenericsMap(mapOf("k1" to mapOf("k1" to InnerImpl(1))))
        val serialized = unquoted.stringify(wrapper, useStreaming)
        assertEquals("{list:{k1:{k1:{\$type:kotlinx.serialization.json.polymorphic.InnerImpl,field:1,str:default,nullable:null}}}}", serialized)
        assertEquals(wrapper, unquoted.parse(serialized, useStreaming))
    }

    @Serializable
    private data class AnyWrapper(@Polymorphic val any: Any)

    @Test
    fun testAny() = parametrizedTest { useStreaming ->
        val wrapper = AnyWrapper(OuterImpl(InnerImpl2(1), InnerImpl(2)))
        val serialized = unquoted.stringify(wrapper, useStreaming)
        assertEquals("{any:" +
                "{\$type:kotlinx.serialization.json.polymorphic.OuterImpl,base:{\$type:kotlinx.serialization.json.polymorphic.InnerImpl2,field:1}," +
                "base2:{\$type:kotlinx.serialization.json.polymorphic.InnerImpl,field:2,str:default,nullable:null}}}",
            serialized)
        assertEquals(wrapper, unquoted.parse(serialized, useStreaming))
    }
}
