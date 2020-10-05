package kotlinx.serialization.json

import kotlinx.serialization.Serializable
import kotlin.test.*

class JsonElementDecodingTest : JsonTestBase() {

    @Serializable
    data class A(val a: Int = 42)

    @Test
    fun testTopLevelClass() = assertSerializedForm(A(), """{}""".trimMargin())

    @Test
    fun testTopLevelNullableClass() {
        assertSerializedForm<A?>(A(), """{}""")
        assertSerializedForm<A?>(null, "null")
    }

    @Test
    fun testTopLevelPrimitive() = assertSerializedForm(42, """42""")

    @Test
    fun testTopLevelNullablePrimitive() {
        assertSerializedForm<Int?>(42, """42""")
        assertSerializedForm<Int?>(null, """null""")
    }

    @Test
    fun testTopLevelList() = assertSerializedForm(listOf(42), """[42]""")

    @Test
    fun testTopLevelNullableList() {
        assertSerializedForm<List<Int>?>(listOf(42), """[42]""")
        assertSerializedForm<List<Int>?>(null, """null""")
    }

    private inline fun <reified T> assertSerializedForm(value: T, expectedString: String) {
        val element = Json.encodeToJsonElement(value)
        assertEquals(expectedString, element.toString())
        assertEquals(value, Json.decodeFromJsonElement(element))
    }
}
