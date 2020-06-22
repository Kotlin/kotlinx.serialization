/*
 * Copyright 2017-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.polymorphic

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonTestBase
import kotlin.test.Test
import kotlin.test.assertFails

class JsonListPolymorphismTest : JsonTestBase() {

    @Serializable
    internal data class ListWrapper(val list: List<@Polymorphic InnerBase>)

    @Test
    fun testPolymorphicValues() = assertJsonFormAndRestored(
        ListWrapper.serializer(),
        ListWrapper(listOf(InnerImpl(1), InnerImpl2(2))),
        """{"list":[""" +
                """{"type":"kotlinx.serialization.json.polymorphic.InnerImpl","field":1,"str":"default","nullable":null},""" +
                """{"type":"kotlinx.serialization.json.polymorphic.InnerImpl2","field":2}]}""",
        polymorphicRelaxedJson)

    @Serializable
    internal data class ListNullableWrapper(val list: List<@Polymorphic InnerBase?>)

    @Test
    fun testPolymorphicNullableValues() = assertJsonFormAndRestored(
        ListNullableWrapper.serializer(),
        ListNullableWrapper(listOf(InnerImpl(1), null)),
        """{"list":[""" +
                """{"type":"kotlinx.serialization.json.polymorphic.InnerImpl","field":1,"str":"default","nullable":null},""" +
                "null]}",
        polymorphicRelaxedJson)

    @Test
    fun testPolymorphicNullableValuesWithNonNullSerializerFails() =
        parametrizedTest { useStreaming ->
            val wrapper = ListNullableWrapper(listOf(InnerImpl(1), null))
            val serialized = polymorphicRelaxedJson.encodeToString(ListNullableWrapper.serializer(), wrapper, useStreaming)
            assertFails { polymorphicRelaxedJson.decodeFromString(ListWrapper.serializer(), serialized, useStreaming) }
        }
}
