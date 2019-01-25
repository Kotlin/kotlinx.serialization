/*
 * Copyright 2017-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.polymorphic

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.test.*

class JsonListPolymorphismTest : JsonTestBase() {

    @Serializable
    private data class ListWrapper(val list: List<@Polymorphic InnerBase>)

    @Test
    fun testPolymorphicValues() = parametrizedTest(
        ListWrapper(listOf(InnerImpl(1), InnerImpl2(2))),
        "{list:[" +
                "{type:kotlinx.serialization.json.polymorphic.InnerImpl,field:1,str:default,nullable:null}," +
                "{type:kotlinx.serialization.json.polymorphic.InnerImpl2,field:2}]}",
        polymorphicJson)

    @Serializable
    private data class ListNullableWrapper(val list: List<@Polymorphic InnerBase?>)

    @Test
    fun testPolymorphicNullableValues() = parametrizedTest(
        ListNullableWrapper(listOf(InnerImpl(1), null)),
        "{list:[" +
                "{type:kotlinx.serialization.json.polymorphic.InnerImpl,field:1,str:default,nullable:null}," +
                "null]}",
        polymorphicJson)

    @Test
    fun testPolymorphicNullableValuesWithNonNullSerializerFails() =
        parametrizedTest { useStreaming ->
            val wrapper = ListNullableWrapper(listOf(InnerImpl(1), null))
            val serialized = polymorphicJson.stringify(wrapper, useStreaming)
            assertFails { polymorphicJson.parse(ListWrapper.serializer(), serialized, useStreaming) }
        }
}
