/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization


import kotlinx.serialization.json.*
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.test.*
import kotlin.test.*

class JsonElementPolymorphicErrorTest : JsonTestBase() {

    @Serializable
    abstract class Abstract

    @Serializable
    data class IntChild(val value: Int) : Abstract()

    @Serializable
    data class CollectionChild(val value: Int) : Abstract()

    @Serializable
    data class Holder(val value: Abstract)

    private val format = Json {
        prettyPrint = false
        serializersModule = SerializersModule {
            polymorphic(Abstract::class) {
                subclass(IntChild::class, IntChildSerializer)
                subclass(CollectionChild::class, CollectionChildSerializer)
            }
        }
    }

    object IntChildSerializer : JsonTransformingSerializer<IntChild>(serializer()) {
        override fun transformSerialize(element: JsonElement): JsonElement {
            return element.jsonObject.getValue("value")
        }
    }

    object CollectionChildSerializer : JsonTransformingSerializer<CollectionChild>(serializer()) {
        override fun transformSerialize(element: JsonElement): JsonElement {
            val value = element.jsonObject.getValue("value")
            return JsonArray(listOf(value))
        }
    }

    @Test
    fun test() = parametrizedTest { mode ->
        assertFailsWithMessage<SerializationException>("Json element JsonLiteral cannot be serialized polymorphous, for serial name 'kotlinx.serialization.JsonElementPolymorphicErrorTest.IntChild'") {
            format.encodeToString(
                Holder.serializer(),
                Holder(IntChild(42)),
                mode
            )
        }

        assertFailsWithMessage<SerializationException>("Json element JsonArray cannot be serialized polymorphous, for serial name 'kotlinx.serialization.JsonElementPolymorphicErrorTest.CollectionChild'") {
            format.encodeToString(
                Holder.serializer(),
                Holder(CollectionChild(42)),
                mode
            )
        }

    }

}
