package kotlinx.serialization.json

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import okio.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class TestTest {
    private val strLen = 1024 * 2 + 42

    @Serializable
    data class StringHolder(val data: String)

    @Test
    fun testParsesStringsLongerThanBuffer() {
        val str = "a".repeat(strLen)
        val input = """{"data":"$str"}"""
        assertEquals(input, Json.encodeViaOkio(StringHolder.serializer(), StringHolder(str)))
    }
    @Test
    fun test2() {
        val str = "a".repeat(strLen)

        val buffer = Buffer()

        Json.encodeToOkio(StringHolder.serializer(), StringHolder(str), buffer)

        val result = Json.decodeFromOkio(StringHolder.serializer(), buffer)

        assertEquals(StringHolder(str), result)
    }
    @Test
    fun test4() {
        val str = "a".repeat(strLen)

        val buffer = Buffer()

        Json.encodeToOkio(StringHolder(str), buffer)
        Json.decodeOkioToSequence(buffer, StringHolder.serializer())
    }
}

fun <T> Json.encodeViaOkio(serializer: KSerializer<T>, value: T): String {
    val limited = Buffer()

    encodeToOkio(serializer, value, limited)

    return limited.readUtf8()

}
