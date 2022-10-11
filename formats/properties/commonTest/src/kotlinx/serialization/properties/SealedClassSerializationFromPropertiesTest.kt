package kotlinx.serialization.properties

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SealedClassSerializationFromPropertiesTest {
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
    fun testPropertiesDeserialization() {
        val props = mapOf(
            "type" to "FIRSTCHILD",
            "firstProperty" to 1L,
            "secondProperty" to "one"
        )

        val instance: BaseClass = Properties.decodeFromMap(props)

        assertIs<FirstChild>(instance)
        assertEquals(instance.firstProperty, 1)
        assertEquals(instance.secondProperty, "one")
    }

    @Test
    fun testPropertiesSerialization() {
        val instance: BaseClass = FirstChild(
            firstProperty = 1L, secondProperty = "one"
        )

        val instanceProperties = Properties.encodeToMap(instance)

        assertEquals(1L, instanceProperties["firstProperty"])
        assertEquals("one", instanceProperties["secondProperty"])
    }
}