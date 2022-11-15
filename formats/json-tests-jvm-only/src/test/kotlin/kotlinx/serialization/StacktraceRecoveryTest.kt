/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.coroutines.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlinx.serialization.json.internal.*
import kotlinx.serialization.modules.*
import kotlin.test.*

class StacktraceRecoveryTest {
    @Serializable
    private class Data(val s: String)

    private class BadDecoder : AbstractDecoder() {
        override val serializersModule: SerializersModule = EmptySerializersModule()
        override fun decodeElementIndex(descriptor: SerialDescriptor): Int = 42
    }

    @Test
    fun testJsonDecodingException() = checkRecovered("JsonDecodingException") {
        Json.decodeFromString<String>("42")
    }

    @Test
    fun testJsonEncodingException() = checkRecovered("JsonEncodingException") {
        Json.encodeToString(Double.NaN)
    }

    @Test
    // checks simple name because UFE is internal class
    fun testUnknownFieldException() = checkRecovered("UnknownFieldException") {
        val serializer = Data.serializer()
        serializer.deserialize(BadDecoder())
    }

    private fun checkRecovered(exceptionClassSimpleName: String, block: () -> Unit) = runBlocking {
        val result = runCatching {
            callBlockWithRecovery(block)
        }
        assertTrue(result.isFailure, "Block should have failed")
        val e = result.exceptionOrNull()!!
        assertEquals(exceptionClassSimpleName, e::class.simpleName!!)
        val cause = e.cause
        assertNotNull(cause, "Exception should have cause: $e")
        assertEquals(e.message, cause.message)
        assertEquals(exceptionClassSimpleName, e::class.simpleName!!)
    }

    // KLUDGE: A separate function with state-machine to ensure coroutine DebugMetadata is generated. See KT-41789
    private suspend fun callBlockWithRecovery(block: () -> Unit) {
        yield()
        // use withContext to perform switch between coroutines and thus trigger exception recovery machinery
        withContext(NonCancellable) {
            block()
        }
    }
}
