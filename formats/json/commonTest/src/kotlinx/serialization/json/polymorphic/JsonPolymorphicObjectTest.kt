/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.polymorphic

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.test.*

class JsonPolymorphicObjectTest : JsonTestBase() {

    @Serializable
    data class Holder(@Polymorphic val a: Any)

    @Serializable
    @SerialName("MyObject")
    object MyObject {
        @Suppress("unused")
        val unused = 42
    }

    val json = Json {
        serializersModule = SerializersModule {
            polymorphic(Any::class) {
                subclass(MyObject::class, MyObject.serializer()) // JS bug workaround
            }
        }
    }

    @Test
    fun testRegularPolymorphism() {
        assertJsonFormAndRestored(Holder.serializer(), Holder(MyObject), """{"a":{"type":"MyObject"}}""", json)
    }

    @Test
    fun testArrayPolymorphism() {
        val json = Json(from = json) {
            useArrayPolymorphism = true
        }
        assertJsonFormAndRestored(Holder.serializer(), Holder(MyObject), """{"a":["MyObject",{}]}""", json)
    }
}
