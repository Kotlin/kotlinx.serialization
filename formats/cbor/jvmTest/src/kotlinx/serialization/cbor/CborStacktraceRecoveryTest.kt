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
        val result = kotlin.runCatching {
            // use withContext to perform switch between coroutines and thus trigger exception recovery machinery
            withContext(NonCancellable) {
                block()
            }
        }
        assertTrue(result.isFailure, "Block should have failed")
        val e = result.exceptionOrNull()!!
        assertEquals(E::class, e::class)
        val cause = e.cause
        assertNotNull(cause, "Exception should have cause: $e")
        assertEquals(e.message, cause.message)
        assertEquals(E::class, cause::class)
    }
}