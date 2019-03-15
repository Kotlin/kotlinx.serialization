/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.test.assertStringFormAndRestored
import kotlin.test.Test
import kotlin.test.assertEquals

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

class ObjectSerializationTest {

    val module = SerializersModule {
        polymorphic(ApiResponse::class) {
            ApiResponse.Error::class with ApiResponse.Error.serializer()
            ApiResponse.Response::class with ApiResponse.Response.serializer()
        }
    }

    val json = Json(context = module)

    @Test
    fun canBeSerialized() {
        val carrier1 = ApiCarrier(ApiResponse.Error)
        val carrier2 = ApiCarrier(ApiResponse.Response("OK"))
        assertStringFormAndRestored("""{"response":{"type":"ApiError"}}""", carrier1, ApiCarrier.serializer(), json)
        assertStringFormAndRestored(
            """{"response":{"type":"ApiResponse","message":"OK"}}""",
            carrier2,
            ApiCarrier.serializer(),
            json
        )
    }

    @Test
    fun correctDescriptor() {
        val serialDesc: SerialDescriptor = ApiResponse.Error.serializer().descriptor
        assertEquals(UnionKind.OBJECT, serialDesc.kind)
        assertEquals(1, serialDesc.elementsCount)
        assertEquals("ApiError", serialDesc.name)
    }
}
