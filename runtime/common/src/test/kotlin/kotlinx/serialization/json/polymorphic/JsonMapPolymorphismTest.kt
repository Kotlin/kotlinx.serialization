/*
 * Copyright 2017-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.polymorphic

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.test.*

class JsonMapPolymorphismTest : JsonTestBase() {

    @Serializable
    private data class MapWrapper(val map: Map<String, @Polymorphic InnerBase>)

    @Test
    fun testPolymorphicValues() = parametrizedTest(
        MapWrapper(mapOf("k1" to InnerImpl(1), "k2" to InnerImpl2(2))),
        "{map:" +
                "{k1:{type:kotlinx.serialization.json.polymorphic.InnerImpl,field:1,str:default,nullable:null}," +
                "k2:{type:kotlinx.serialization.json.polymorphic.InnerImpl2,field:2}}}",
        polymorphicJson)

    @Serializable
    private data class MapNullableWrapper(val map: Map<String, @Polymorphic InnerBase?>)

    @Test
    fun testPolymorphicNullableValues() = parametrizedTest(
        MapNullableWrapper(mapOf("k1" to InnerImpl(1), "k2" to null)),
        "{map:" +
                "{k1:{type:kotlinx.serialization.json.polymorphic.InnerImpl,field:1,str:default,nullable:null}," +
                "k2:null}}",
        polymorphicJson)

//    @Test
//    fun testPolymorphicKeys() { // Actually I had no idea we support this kind of pattern. TODO
//        val wrapper = MapKeys(mapOf(InnerImpl(1) to "k2", InnerImpl2(2) to "k2"))
//        val serialized = unquoted.stringify(wrapper, true)
//        println(serialized)
//    }
}