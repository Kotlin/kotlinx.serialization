/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.polymorphic

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.test.*

class JsonNullablePolymorphicTest : JsonTestBase() {
    @Serializable
    data class NullableHolder(@Polymorphic val a: Any?)

    @Serializable
    @SerialName("Box")
    data class Box(val i: Int)

    @Test
    fun testPolymorphicNulls() {
        val json = Json {
            serializersModule = SerializersModule {
                polymorphic(Any::class) {
                    subclass(Box::class)
                }
            }
        }

        assertJsonFormAndRestored(serializer(), NullableHolder(Box(42)), """{"a":{"type":"Box","i":42}}""", json)
        assertJsonFormAndRestored(serializer(), NullableHolder(null), """{"a":null}""", json)
    }
}
