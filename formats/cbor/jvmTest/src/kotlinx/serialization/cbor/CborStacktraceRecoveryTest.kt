/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.cbor

import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.cbor.internal.*
import kotlin.test.*

class CborStacktraceRecoveryTest {
    @Test
    fun testCborDecodingException() = checkRecovered<CborDecodingException> {
        Cbor.decodeFromByteArray<String>(byteArrayOf(0xFF.toByte()))
    }

    private inline fun <reified E : Exception> checkRecovered(noinline block: () -> Unit) = runBlocking {
        val result = runCatching {
            callBlockWithRecovery(block)
        }
        assertTrue(result.isFailure, "Block should have failed")
        val e = result.exceptionOrNull()!!
        assertEquals(E::class, e::class)
        val cause = e.cause
        assertNotNull(cause, "Exception should have cause: $e")
        assertEquals(e.message, cause.message)
        assertEquals(E::class, cause::class)
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
