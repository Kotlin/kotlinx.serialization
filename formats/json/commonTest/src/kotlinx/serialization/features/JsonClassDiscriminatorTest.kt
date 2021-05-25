/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.test.*

class JsonClassDiscriminatorTest : JsonTestBase() {
    @Serializable
    @JsonClassDiscriminator("sealedType")
    sealed class SealedMessage {
        @Serializable
        @SerialName("SealedMessage.StringMessage")
        data class StringMessage(val description: String, val message: String) : SealedMessage()

        @SerialName("EOF")
        @Serializable
        object EOF : SealedMessage()
    }

    @Serializable
    @JsonClassDiscriminator("abstractType")
    abstract class Message {
        @Serializable
        @SerialName("Message.StringMessage")
        data class StringMessage(val description: String, val message: String) : Message()

        @Serializable
        @SerialName("Message.IntMessage")
        data class IntMessage(val description: String, val message: Int) : Message()
    }

    @Test
    fun testSealedClassesHaveCustomDiscriminator() {
        val messages = listOf(
            SealedMessage.StringMessage("string message", "foo"),
            SealedMessage.EOF
        )
        val expected =
            """[{"sealedType":"SealedMessage.StringMessage","description":"string message","message":"foo"},{"sealedType":"EOF"}]"""
        assertJsonFormAndRestored(
            ListSerializer(SealedMessage.serializer()),
            messages,
            expected,
        )
    }

    @Test
    fun testAbstractClassesHaveCustomDiscriminator() {
        val messages = listOf(
            Message.StringMessage("string message", "foo"),
            Message.IntMessage("int message", 42),
        )
        val module = SerializersModule {
            polymorphic(Message::class) {
                subclass(Message.StringMessage.serializer())
                subclass(Message.IntMessage.serializer())
            }
        }
        val json = Json { serializersModule = module }
        val expected =
            """[{"abstractType":"Message.StringMessage","description":"string message","message":"foo"},{"abstractType":"Message.IntMessage","description":"int message","message":42}]"""
        assertJsonFormAndRestored(ListSerializer(Message.serializer()), messages, expected, json)
    }
}
