/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.polymorphic

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.json.internal.*
import kotlinx.serialization.modules.*
import kotlinx.serialization.test.assertFailsWithSerial
import kotlin.test.*

class JsonPolymorphismExceptionTest : JsonTestBase() {

    @Serializable
    abstract class Base

    @Serializable
    @SerialName("derived")
    class Derived(val nested: Nested = Nested()) : Base()

    @Serializable
    class Nested

    @Test
    fun testDecodingException() = parametrizedTest { jsonTestingMode ->
        val serialModule = SerializersModule {
            polymorphic(Base::class) {
                subclass(Derived::class)
            }
        }

        val json = Json { serializersModule = serialModule }
        assertFailsWithSerial("JsonDecodingException") {
            json.decodeFromString(Base.serializer(), """{"type":"derived","nested":null}""", jsonTestingMode)
        }
    }

    @Test
    fun testMissingDiscriminator() = parametrizedTest { jsonTestingMode ->
        val serialModule = SerializersModule {
            polymorphic(Base::class) {
                subclass(Derived::class)
            }
        }

        val json = Json { serializersModule = serialModule }
        assertFailsWithSerial("JsonDecodingException") {
            json.decodeFromString(Base.serializer(), """{"nested":{}}""", jsonTestingMode)
        }
    }
}
