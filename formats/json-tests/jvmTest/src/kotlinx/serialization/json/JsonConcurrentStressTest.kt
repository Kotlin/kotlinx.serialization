package kotlinx.serialization.json

import kotlinx.coroutines.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.random.*
import kotlin.test.*

// Stresses out that JSON decoded in parallel does not interfere (mostly via caching of various buffers)
class JsonConcurrentStressTest : JsonTestBase() {
    private val charset = "ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz0123456789"

    @Test
    fun testDecodeInParallelSimpleList() = doTest(100) { mode ->
        val value = (1..10000).map { Random.nextDouble() }
        val string = Json.encodeToString(ListSerializer(Double.serializer()), value, mode)
        assertEquals(value, Json.decodeFromString(ListSerializer(Double.serializer()), string, mode))
    }

    @Serializable
    data class Foo(val s: String, val f: Foo?)

    @Test
    fun testDecodeInParallelListOfPojo() = doTest(1_000) { mode ->
        val value = (1..100).map {
            val randomString = getRandomString()
            val nestedFoo = Foo("null抢\u000E鋽윝䑜厼\uF70A紲ᢨ䣠null⛾䉻嘖緝ᯧnull쎶\u0005null" + randomString, null)
            Foo(getRandomString(), nestedFoo)
        }
        val string = Json.encodeToString(ListSerializer(Foo.serializer()), value, mode)
        assertEquals(value, Json.decodeFromString(ListSerializer(Foo.serializer()), string, mode))
    }

    @Test
    fun testDecodeInParallelPojo() = doTest(100_000) { mode ->
        val randomString = getRandomString()
        val nestedFoo = Foo("null抢\u000E鋽윝䑜厼\uF70A紲ᢨ䣠null⛾䉻嘖緝ᯧnull쎶\u0005null" + randomString, null)
        val randomFoo = Foo(getRandomString(), nestedFoo)
        val string = Json.encodeToString(Foo.serializer(), randomFoo, mode)
        assertEquals(randomFoo, Json.decodeFromString(Foo.serializer(), string, mode))
    }

    @Test
    fun testDecodeInParallelSequencePojo() =  runBlocking<Unit> {
        for (i in 1 until 1_000) {
            launch(Dispatchers.Default) {
                val values = (1..100).map {
                    val randomString = getRandomString()
                    val nestedFoo = Foo("null抢\u000E鋽윝䑜厼\uF70A紲ᢨ䣠null⛾䉻嘖緝ᯧnull쎶\u0005null" + randomString, null)
                    Foo(getRandomString(), nestedFoo)
                }
                val baos = ByteArrayOutputStream()
                for (value in values) {
                    Json.encodeToStream(Foo.serializer(), value, baos)
                }
                val bais = ByteArrayInputStream(baos.toByteArray())
                assertEquals(values, Json.decodeToSequence(bais, Foo.serializer()).toList())
            }
        }
    }

    private fun getRandomString() = (1..Random.nextInt(0, charset.length)).map { charset[it] }.joinToString(separator = "")

    private fun doTest(iterations: Int, block: (JsonTestingMode) -> Unit) {
        runBlocking<Unit> {
            for (i in 1 until iterations) {
                launch(Dispatchers.Default) {
                    parametrizedTest {
                        block(it)
                    }
                }
            }
        }
    }
}
