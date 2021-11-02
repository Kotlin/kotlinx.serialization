/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.test.*

class SealedInterfacesJsonSerializationTest {
    @Serializable
    sealed interface I

    @Serializable
    sealed class Response: I {
        @Serializable
        data class ResponseInt(val i: Int): Response()

        @Serializable
        data class ResponseString(val s: String): Response()
    }

    @Serializable
    object NoResponse: I

    @Test
    fun testSealedInterfaceJson() {
        val messages = listOf<I>(Response.ResponseInt(10), NoResponse, Response.ResponseString("foo"))
        println(Json.encodeToString(messages))
    }
}
