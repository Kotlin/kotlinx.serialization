/*
 * Copyright 2017-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.test.*

class PolymorphicDeserializationErrorMessagesTest : JsonTestBase() {
    @Serializable
    class DummyData(@Polymorphic val a: Any)

    @Serializable
    class Holder(val d: DummyData)

    // TODO: remove this after #2480 is merged
    private fun checkSerializationException(action: () -> Unit, assertions: SerializationException.(String) -> Unit) {
        val e = assertFailsWith(SerializationException::class, action)
        assertNotNull(e.message)
        e.assertions(e.message!!)
    }

    @Test
    fun testNotRegisteredMessage() = parametrizedTest { mode ->
        val input = """{"d":{"a":{"type":"my.Class", "value":42}}}"""
        checkSerializationException({
            default.decodeFromString<Holder>(input, mode)
        }, { message ->
            // ReaderJsonLexer.peekLeadingMatchingValue is not implemented, so first-key optimization is not working for streaming yet.
            if (mode == JsonTestingMode.STREAMING)
                assertContains(message, "Unexpected JSON token at offset 10: Serializer for subclass 'my.Class' is not found in the polymorphic scope of 'Any' at path: \$.d.a")
            else
                assertContains(message, "Serializer for subclass 'my.Class' is not found in the polymorphic scope of 'Any'")
        })
    }

    @Test
    fun testDiscriminatorMissingNoDefaultMessage() = parametrizedTest { mode ->
        val input = """{"d":{"a":{"value":42}}}"""
        checkSerializationException({
            default.decodeFromString<Holder>(input, mode)
        }, { message ->
            // Always slow path when discriminator is missing, so no position and path
            assertContains(message, "Class discriminator was missing and no default serializers were registered in the polymorphic scope of 'Any'")
        })
    }

    @Test
    fun testClassDiscriminatorIsNull() = parametrizedTest { mode ->
        val input = """{"d":{"a":{"type":null, "value":42}}}"""
        checkSerializationException({
            default.decodeFromString<Holder>(input, mode)
        }, { message ->
            // Always slow path when discriminator is missing, so no position and path
            assertContains(message, "Class discriminator was missing and no default serializers were registered in the polymorphic scope of 'Any'")
        })
    }
}
