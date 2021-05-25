/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlinx.serialization.test.noLegacyJs
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
    abstract class AbstractMessage {
        @Serializable
        @SerialName("Message.StringMessage")
        data class StringMessage(val description: String, val message: String) : AbstractMessage()

        @Serializable
        @SerialName("Message.IntMessage")
        data class IntMessage(val description: String, val message: Int) : AbstractMessage()
    }


    @Test
    fun testSealedClassesHaveCustomDiscriminator() = noLegacyJs {
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
    fun testAbstractClassesHaveCustomDiscriminator() = noLegacyJs {
        val messages = listOf(
            AbstractMessage.StringMessage("string message", "foo"),
            AbstractMessage.IntMessage("int message", 42),
        )
        val module = SerializersModule {
            polymorphic(AbstractMessage::class) {
                subclass(AbstractMessage.StringMessage.serializer())
                subclass(AbstractMessage.IntMessage.serializer())
            }
        }
        val json = Json { serializersModule = module }
        val expected =
            """[{"abstractType":"Message.StringMessage","description":"string message","message":"foo"},{"abstractType":"Message.IntMessage","description":"int message","message":42}]"""
        assertJsonFormAndRestored(ListSerializer(AbstractMessage.serializer()), messages, expected, json)
    }

    @Serializable
    @JsonClassDiscriminator("message_type")
    abstract class Base

    @Serializable
    abstract class ErrorClass : Base()

    @Serializable
    data class Message(val message: Base, val error: ErrorClass?)

    @Serializable
    @SerialName("my.app.BaseMessage")
    data class BaseMessage(val message: String) : Base()

    @Serializable
    @SerialName("my.app.GenericError")
    data class GenericError(@SerialName("error_code") val errorCode: Int) : ErrorClass()


    @Test
    fun testDocumentationInheritanceSample() = noLegacyJs {
        val module = SerializersModule {
            polymorphic(Base::class) {
                subclass(BaseMessage.serializer())
            }
            polymorphic(ErrorClass::class) {
                subclass(GenericError.serializer())
            }
        }
        val json = Json { serializersModule = module }
        assertJsonFormAndRestored(
            Message.serializer(),
            Message(BaseMessage("not found"), GenericError(404)),
            """{"message":{"message_type":"my.app.BaseMessage","message":"not found"},"error":{"message_type":"my.app.GenericError","error_code":404}}""",
            json
        )
    }
}
