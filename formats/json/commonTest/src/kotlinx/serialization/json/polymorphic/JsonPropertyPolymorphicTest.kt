/*
 * Copyright 2017-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.polymorphic

import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.JsonTestBase
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonPropertyPolymorphicTest : JsonTestBase() {

    @Test
    fun testPolymorphicProperties() = assertJsonFormAndRestored(
        InnerBox.serializer(),
        InnerBox(InnerImpl(42, "foo")),
        """{"base":{"type":"kotlinx.serialization.json.polymorphic.InnerImpl",""" +
                """"field":42,"str":"foo","nullable":null}}""",
        polymorphicRelaxedJson)

    @Test
    fun testFlatPolymorphic() = parametrizedTest { useStreaming ->
        val base: InnerBase = InnerImpl(42, "foo")
        val string = polymorphicRelaxedJson.encodeToString(PolymorphicSerializer(InnerBase::class), base, useStreaming)
        assertEquals("""{"type":"kotlinx.serialization.json.polymorphic.InnerImpl",""" +
                """"field":42,"str":"foo","nullable":null}""", string)
        assertEquals(base, polymorphicRelaxedJson.decodeFromString(PolymorphicSerializer(InnerBase::class), string, useStreaming))
    }

    @Test
    fun testNestedPolymorphicProperties() = assertJsonFormAndRestored(
        OuterBox.serializer(),
        OuterBox(OuterImpl(InnerImpl(42), InnerImpl2(42)), InnerImpl2(239)),
        """{"outerBase":{""" +
                """"type":"kotlinx.serialization.json.polymorphic.OuterImpl",""" +
                """"base":{"type":"kotlinx.serialization.json.polymorphic.InnerImpl","field":42,"str":"default","nullable":null},""" +
                """"base2":{"type":"kotlinx.serialization.json.polymorphic.InnerImpl2","field":42}},""" +
                """"innerBase":{"type":"kotlinx.serialization.json.polymorphic.InnerImpl2","field":239}}""",
        polymorphicRelaxedJson)

    @Test
    fun testPolymorphicNullableProperties() = assertJsonFormAndRestored(
        InnerNullableBox.serializer(),
        InnerNullableBox(InnerImpl(42, "foo")),
        """{"base":{"type":"kotlinx.serialization.json.polymorphic.InnerImpl",""" +
                """"field":42,"str":"foo","nullable":null}}""",
        polymorphicRelaxedJson)

    @Test
    fun testPolymorphicNullablePropertiesWithNull() =
        assertJsonFormAndRestored(InnerNullableBox.serializer(), InnerNullableBox(null), """{"base":null}""", polymorphicJson)

    @Test
    fun testNestedPolymorphicNullableProperties() = assertJsonFormAndRestored(
        OuterNullableBox.serializer(),
        OuterNullableBox(OuterNullableImpl(InnerImpl(42), null), InnerImpl2(239)),
        """{"outerBase":{""" +
                """"type":"kotlinx.serialization.json.polymorphic.OuterNullableImpl",""" +
                """"base":{"type":"kotlinx.serialization.json.polymorphic.InnerImpl","field":42,"str":"default","nullable":null},"base2":null},""" +
                """"innerBase":{"type":"kotlinx.serialization.json.polymorphic.InnerImpl2","field":239}}""",
        polymorphicRelaxedJson)
}
