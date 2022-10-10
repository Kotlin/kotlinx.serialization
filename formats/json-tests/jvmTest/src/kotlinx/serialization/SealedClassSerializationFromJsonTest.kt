package kotlinx.serialization

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SealedClassSerializationFromJsonTest {
    @Serializable
    sealed class BaseClass {
        abstract val firstProperty: Long
        abstract val secondProperty: String
    }

    @SerialName("FIRSTCHILD")
    @Serializable
    data class FirstChild(override val firstProperty: Long, override val secondProperty: String) : BaseClass()

    @SerialName("SECONDCHILD")
    @Serializable
    data class SecondChild(override val firstProperty: Long, override val secondProperty: String) : BaseClass()
    
    @Test
    fun testJsonSerialization() {
        val json = """{"type": "FIRSTCHILD", "firstProperty": 1, "secondProperty": "one"}"""

        val instance: BaseClass = Json.decodeFromString(json)

        assertIs<FirstChild>(instance)
        assertEquals(instance.firstProperty, 1)
        assertEquals(instance.secondProperty, "one")
    }
}