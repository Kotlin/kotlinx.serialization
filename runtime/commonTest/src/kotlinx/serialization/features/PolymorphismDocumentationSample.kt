/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.*
import kotlinx.serialization.test.assertStringFormAndRestored
import kotlin.test.Test

class PolymorphismDocumentationSample {

    interface Message

    @Serializable
    data class StringMessage(val message: String) : Message

    @Serializable
    @SerialName("msg_number")
    data class IntMessage(val number: Int) : Message

    val messageModule = SerializersModule {
        polymorphic(Message::class) {
            subclass(StringMessage.serializer())
            subclass(IntMessage.serializer())
        }
    }

    val json = Json { serializersModule = messageModule }

    val jsonWithArrays = Json {
        useArrayPolymorphism = true
        serializersModule = messageModule
    }

    @Serializable
    data class MessageWrapper(val m: Message)

    @Test
    fun testStringMessage() = assertStringFormAndRestored(
        """{"m":{"type":"kotlinx.serialization.features.PolymorphismDocumentationSample.StringMessage","message":"string"}}""",
        MessageWrapper(StringMessage("string")),
        MessageWrapper.serializer(),
        json
    )

    @Test
    fun testIntMessage() = assertStringFormAndRestored(
        """{"m":["msg_number",{"number":121}]}""",
        MessageWrapper(IntMessage(121)),
        MessageWrapper.serializer(),
        jsonWithArrays
    )
}
