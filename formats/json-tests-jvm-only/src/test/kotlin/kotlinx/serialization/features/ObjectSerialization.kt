/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlinx.serialization.test.*
import kotlin.test.*

class ObjectSerializationTest : JsonTestBase() {

    sealed class ApiResponse {
        @Serializable
        @SerialName("ApiError")
        object Error : ApiResponse()

        @Serializable
        @SerialName("ApiResponse")
        data class Response(val message: String) : ApiResponse()
    }

    @Serializable
    data class ApiCarrier(@Polymorphic val response: ApiResponse)

    val module = SerializersModule {
        polymorphic(ApiResponse::class) {
            subclass(ApiResponse.Error.serializer())
            subclass(ApiResponse.Response.serializer())
        }
    }

    val json = Json { serializersModule = module }

    @Test
    fun testSealedClassSerialization() {
        val carrier1 = ApiCarrier(ApiResponse.Error)
        val carrier2 = ApiCarrier(ApiResponse.Response("OK"))
        assertJsonFormAndRestored(ApiCarrier.serializer(), carrier1, """{"response":{"type":"ApiError"}}""", json)
        assertJsonFormAndRestored(
            ApiCarrier.serializer(),
            carrier2,
            """{"response":{"type":"ApiResponse","message":"OK"}}""",
            json
        )
    }

    @Test
    fun testUnknownKeys() {
        val string = """{"metadata":"foo"}"""
        assertFailsWithMessage<SerializationException>("ignoreUnknownKeys") {
            Json.decodeFromString(
                ApiResponse.Error.serializer(),
                string
            )
        }
        val json = Json { ignoreUnknownKeys = true }
        assertEquals(ApiResponse.Error, json.decodeFromString(ApiResponse.Error.serializer(), string))
    }
}
