package kotlinx.serialization

import kotlinx.serialization.json.*
import kotlin.test.*

class JsonAstTest {
    @Test
    fun parseWithoutExceptions() {
        val input = """{"a": "foo",              "b": 10, "c": ["foo", 100500, {"bar": "baz"}]}"""
        val elem = JsonAstReader(input).readFully()
        println(elem)
    }

    @Test
    fun jsonValue() {
        val v = JsonValue("foo")
        assertEquals(v, JsonAstReader("foo").readFully())
    }

    @Test
    fun jsonObject() {
        val inp = """{"a": "foo",              "b": 10}"""
        val elem = JsonAstReader(inp).readFully()
        assertTrue(elem is JsonObject)
        elem as JsonObject
        assertEquals(setOf("a", "b"), elem.keys)
        assertEquals(JsonValue("foo"), elem["a"])
        val ten = elem.getValue("b") as JsonValue
        assertEquals(10, ten.asInt)
    }
}
