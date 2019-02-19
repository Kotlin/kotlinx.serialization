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
    fun testPolymorphicProperties() = parametrizedTest(
        InnerBox(InnerImpl(42, "foo")),
        "{base:{type:kotlinx.serialization.json.polymorphic.InnerImpl,field:42,str:foo,nullable:null}}"
    )

    @Test
    fun testFlatPolymorphic(){//} = parametrizedTest { useStreaming -> // TODO issue #287
        val base: InnerBase = InnerImpl(42, "foo")
        val string = unquoted.stringify(PolymorphicSerializer(InnerBase::class), base, true)
        assertEquals("{type:kotlinx.serialization.json.polymorphic.InnerImpl,field:42,str:foo,nullable:null}", string)
        assertEquals(base, unquoted.parse(PolymorphicSerializer(InnerBase::class), string, true))
    }

    @Test
    fun testNestedPolymorphicProperties() = parametrizedTest(
        OuterBox(OuterImpl(InnerImpl(42), InnerImpl2(42)), InnerImpl2(239)),
        "{outerBase:{" +
                "type:kotlinx.serialization.json.polymorphic.OuterImpl," +
                "base:{type:kotlinx.serialization.json.polymorphic.InnerImpl,field:42,str:default,nullable:null}," +
                "base2:{type:kotlinx.serialization.json.polymorphic.InnerImpl2,field:42}}," +
                "innerBase:{type:kotlinx.serialization.json.polymorphic.InnerImpl2,field:239}}"
    )

    @Test
    fun testPolymorphicNullableProperties() = parametrizedTest(
        InnerNullableBox(InnerImpl(42, "foo")),
        "{base:{type:kotlinx.serialization.json.polymorphic.InnerImpl,field:42,str:foo,nullable:null}}"
    )

    @Test
    fun testPolymorphicNullablePropertiesWithNull() = parametrizedTest(InnerNullableBox(null), "{base:null}")

    @Test
    fun testNestedPolymorphicNullableProperties() = parametrizedTest(
        OuterNullableBox(OuterNullableImpl(InnerImpl(42), null), InnerImpl2(239)),
        "{outerBase:{" +
                "type:kotlinx.serialization.json.polymorphic.OuterNullableImpl," +
                "base:{type:kotlinx.serialization.json.polymorphic.InnerImpl,field:42,str:default,nullable:null},base2:null}," +
                "innerBase:{type:kotlinx.serialization.json.polymorphic.InnerImpl2,field:239}}"
    )
}
