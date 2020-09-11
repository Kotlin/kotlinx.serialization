/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.polymorphic

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.test.*

class JsonMapPolymorphismTest : JsonTestBase() {

    @Serializable
    internal data class MapWrapper(val map: Map<String, @Polymorphic InnerBase>)

    @Test
    fun testPolymorphicValues() = assertJsonFormAndRestored(
        MapWrapper.serializer(),
        MapWrapper(mapOf("k1" to InnerImpl(1), "k2" to InnerImpl2(2))),
        """{"map":{"k1":{"type":"kotlinx.serialization.json.polymorphic.InnerImpl","field":1,"str":"default","nullable":null},"k2":{"type":"kotlinx.serialization.json.polymorphic.InnerImpl2","field":2}}}""".trimMargin(),
        polymorphicJson
    )

    @Serializable
    internal data class MapNullableWrapper(val map: Map<String, @Polymorphic InnerBase?>)

    @Serializable
    internal data class MapKeys(val map: Map<@Polymorphic InnerBase, String>)

    @Test
    fun testPolymorphicNullableValues() = assertJsonFormAndRestored(
        MapNullableWrapper.serializer(),
        MapNullableWrapper(mapOf("k1" to InnerImpl(1), "k2" to null)),
        """{"map":{"k1":{"type":"kotlinx.serialization.json.polymorphic.InnerImpl","field":1,"str":"default","nullable":null},"k2":null}}""",
        polymorphicJson
    )

    @Test
    fun testPolymorphicKeys() {
        val json = Json {
            allowStructuredMapKeys = true
            serializersModule = polymorphicTestModule
        }
        assertJsonFormAndRestored(
            MapKeys.serializer(),
            MapKeys(mapOf(InnerImpl(1) to "k2", InnerImpl2(2) to "k2")),
            """{"map":[{"type":"kotlinx.serialization.json.polymorphic.InnerImpl","field":1,"str":"default","nullable":null},"k2",{"type":"kotlinx.serialization.json.polymorphic.InnerImpl2","field":2},"k2"]}""",
            json
        )
    }

    @Test
    fun testPolymorphicKeysInArray() {
        val json = Json {
            allowStructuredMapKeys = true
            useArrayPolymorphism = true
            serializersModule = polymorphicTestModule
        }
        assertJsonFormAndRestored(
            MapKeys.serializer(),
            MapKeys(mapOf(InnerImpl(1) to "k2", InnerImpl2(2) to "k2")),
            """{"map":[["kotlinx.serialization.json.polymorphic.InnerImpl",{"field":1,"str":"default","nullable":null}],"k2",["kotlinx.serialization.json.polymorphic.InnerImpl2",{"field":2}],"k2"]}""",
            json
        )
    }

    @Serializable
    abstract class Base

    @Serializable
    data class Derived(val myMap: Map<StringData, String>) : Base()

    @Test
    fun testIssue480() {
        val json = Json {
            allowStructuredMapKeys = true
            serializersModule = SerializersModule {
                polymorphic(Base::class) {
                    subclass(Derived.serializer())
                }
            }
        }

        assertJsonFormAndRestored(
            Base.serializer(),
            Derived(mapOf(StringData("hi") to "hello")),
            """{"type":"kotlinx.serialization.json.polymorphic.JsonMapPolymorphismTest.Derived","myMap":[{"data":"hi"},"hello"]}""",
            json
        )
    }
}
