/*
 * Copyright 2017-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.polymorphic

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.test.*

class JsonPropertyPolymorphicTest : JsonTestBase() {

    @BeforeTest
    fun setUp() {
        unquoted.install(polymorphicTestModule)
    }

    @Test
    fun testPolymorphicProperties() = parametrizedTest { useStreaming ->
        val box = InnerBox(InnerImpl(42, "foo"))
        val string = unquoted.stringify(InnerBox.serializer(), box, useStreaming)
        assertEquals("{base:{\$type:kotlinx.serialization.json.polymorphic.InnerImpl,field:42,str:foo,nullable:null}}", string)
        assertEquals(box, unquoted.parse(InnerBox.serializer(), string, useStreaming))
    }

    @Test
    fun testFlatPolymorphic(){//} = parametrizedTest { useStreaming -> // TODO issue #287
        val base: InnerBase = InnerImpl(42, "foo")
        val string = unquoted.stringify(PolymorphicSerializer(InnerBase::class), base, true)
        assertEquals("{\$type:kotlinx.serialization.json.polymorphic.InnerImpl,field:42,str:foo,nullable:null}", string)
        assertEquals(base, unquoted.parse(PolymorphicSerializer(InnerBase::class), string, true))
    }

    @Test
    fun testNestedPolymorphicProperties() = parametrizedTest { useStreaming ->
        val box = OuterBox(OuterImpl(InnerImpl(42), InnerImpl2(42)), InnerImpl2(239))
        val string = unquoted.stringify(OuterBox.serializer(), box, useStreaming)
        assertEquals("{outerBase:{" +
                "\$type:kotlinx.serialization.json.polymorphic.OuterImpl," +
                "base:{\$type:kotlinx.serialization.json.polymorphic.InnerImpl,field:42,str:default,nullable:null}," +
                "base2:{\$type:kotlinx.serialization.json.polymorphic.InnerImpl2,field:42}}," +
                "innerBase:{\$type:kotlinx.serialization.json.polymorphic.InnerImpl2,field:239}}", string)
        assertEquals(box, unquoted.parse(OuterBox.serializer(), string, useStreaming))
    }

    @Test
    fun testPolymorphicNullableProperties() = parametrizedTest { useStreaming ->
        val box = InnerNullableBox(InnerImpl(42, "foo"))
        val string = unquoted.stringify(InnerNullableBox.serializer(), box, useStreaming)
        assertEquals("{base:{\$type:kotlinx.serialization.json.polymorphic.InnerImpl,field:42,str:foo,nullable:null}}", string)
        assertEquals(box, unquoted.parse(InnerNullableBox.serializer(), string, useStreaming))
    }

    @Test
    fun testPolymorphicNullablePropertiesWithNull() = parametrizedTest { useStreaming ->
        val box = InnerNullableBox(null)
        val string = unquoted.stringify(InnerNullableBox.serializer(), box, useStreaming)
        assertEquals("{base:null}", string)
        assertEquals(box, unquoted.parse(InnerNullableBox.serializer(), string, useStreaming))
    }

    @Test
    fun testNestedPolymorphicNullableProperties() = parametrizedTest { useStreaming ->
        val box = OuterNullableBox(OuterNullableImpl(InnerImpl(42), null), InnerImpl2(239))
        val string = unquoted.stringify(OuterNullableBox.serializer(), box, useStreaming)
        assertEquals("{outerBase:{" +
                "\$type:kotlinx.serialization.json.polymorphic.OuterNullableImpl," +
                "base:{\$type:kotlinx.serialization.json.polymorphic.InnerImpl,field:42,str:default,nullable:null},base2:null}," +
                "innerBase:{\$type:kotlinx.serialization.json.polymorphic.InnerImpl2,field:239}}", string)
        assertEquals(box, unquoted.parse(OuterNullableBox.serializer(), string, useStreaming))
    }
}
