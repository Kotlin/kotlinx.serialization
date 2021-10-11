package kotlinx.serialization.json

import kotlinx.coroutines.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.test.*
import org.junit.*
import kotlin.concurrent.*
import kotlin.random.*

class ParallelJsonStressTest : JsonTestBase() {
    private val iterations = 1_000_000

    @Test
    fun testDecodeInParallel() = runBlocking<Unit> {
        for (i in 1..1000) {
            launch(Dispatchers.Default) {
                val value = (1..10000).map { Random.nextDouble() }
                assertSerializedAndRestored(value, ListSerializer(Double.serializer()))
            }
        }
    }
}
