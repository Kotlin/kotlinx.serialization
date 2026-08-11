/*
 * Copyright 2017-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.test.checkDecodingException
import kotlinx.serialization.test.checkEncodingException
import kotlin.test.*

class PolymorphicErrorMessagesTest : JsonTestBase() {
    @Serializable
    class DummyData(@Polymorphic val a: Any)

    @Serializable
    class Holder(val d: DummyData)

    @Test
    fun testNotRegisteredMessage() = parametrizedTest { mode ->
        val input = """{"d":{"a":{"type":"my.Class", "value":42}}}"""
        checkDecodingException(mode, {
            default.decodeFromString<Holder>(input, mode)
        }) {
            message("Serializer for subclass 'my.Class' is not found in the polymorphic scope of 'Any'")
            if (mode != JsonTestingMode.TREE) path("$.d.a") else path("$.a") // #3170
            hint("Check if class with serial name 'my.Class' exists and serializer is registered in a corresponding SerializersModule.")
            // ReaderJsonLexer.peekLeadingMatchingValue is not implemented (#2626), so first-key optimization is not working for non-streaming yet.
            if (mode == JsonTestingMode.STREAMING) {
                offset(10)
                input(input)
            } else {
                input("{\"type\":\"my.Class\",\"value\":42}")
            }
        }
    }

    @Test
    fun testDiscriminatorMissingNoDefaultMessage() = parametrizedTest { mode ->
        val input = """{"d":{"a":{"value":42}}}"""
        checkDecodingException(mode, {
            default.decodeFromString<Holder>(input, mode)
        }) {
            // Always slow path when discriminator is missing, so no position
            message("Class discriminator was missing and no default serializers were registered in the polymorphic scope of 'Any'")
            if (mode != JsonTestingMode.TREE) path("$.d.a") else path("$.a") // #3170
            input("{\"value\":42}")
        }
    }

    @Test
    fun testClassDiscriminatorIsNull() = parametrizedTest { mode ->
        val input = """{"d":{"a":{"type":null, "value":42}}}"""
        checkDecodingException(mode, {
            default.decodeFromString<Holder>(input, mode)
        }) {
            // Always slow path when discriminator is missing, so no position
            message("Class discriminator was missing and no default serializers were registered in the polymorphic scope of 'Any'")
            if (mode != JsonTestingMode.TREE) path("$.d.a") else path("$.a") // #3170
            input("{\"type\":null,\"value\":42}")
        }
    }

    @Test
    fun testUnknownSubclassDuringEncoding() = parametrizedTest { mode ->
        val input = Holder(DummyData(StringData("x")))
        checkEncodingException(mode, {
            default.encodeToString(Holder.serializer(), input)
        }) {
            message("Serializer for subclass 'StringData' is not found in the polymorphic scope of 'Any'")
            hint("Check if class with serial name 'StringData' exists and serializer is registered in a corresponding SerializersModule.")
            serialName("StringData")
        }
    }
}
