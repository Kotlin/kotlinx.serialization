package kotlinx.serialization

import kotlin.test.Test
import kotlin.test.assertEquals

class JsTest {
    @Serializable
    data class Data(val a: Int)

    @Test
    fun jsTest() {
        val parsed = kotlinx.serialization.json.JSON.unquoted.parse<Data>("{a: 42}")
        assertEquals(parsed, Data(42))
    }
}