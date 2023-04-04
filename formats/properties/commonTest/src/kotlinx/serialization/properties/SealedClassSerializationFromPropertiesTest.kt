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
        assertEquals(instance.firstProperty, 1L)
        assertEquals(instance.secondProperty, "one")
    }

    @Test
    fun testPropertiesSerialization() {
        val instance: BaseClass = FirstChild(
            firstProperty = 1L,
            secondProperty = "one"
        )

        val instanceProperties = Properties.encodeToMap(instance)

        assertEquals("FIRSTCHILD", instanceProperties["type"])
        assertEquals(1L, instanceProperties["firstProperty"])
        assertEquals("one", instanceProperties["secondProperty"])
    }

    @Serializable
    data class CompositeClass(val item: BaseClass)

    @Test
    fun testCompositeClassPropertiesDeserialization() {
        val props = mapOf(
            "item.type" to "SECONDCHILD",
            "item.firstProperty" to 7L,
            "item.secondProperty" to "nothing"
        )

        val composite: CompositeClass = Properties.decodeFromMap(props)
        val instance = composite.item

        assertIs<SecondChild>(instance)
        assertEquals(instance.firstProperty, 7L)
        assertEquals(instance.secondProperty, "nothing")
    }

    @Test
    fun testCompositeClassPropertiesSerialization() {
        val composite = CompositeClass(
            item = FirstChild(
                firstProperty = 5L,
                secondProperty = "something"
            )
        )

        val compositeProperties = Properties.encodeToMap(composite)

        assertEquals("FIRSTCHILD", compositeProperties["item.type"])
        assertEquals(5L, compositeProperties["item.firstProperty"])
        assertEquals("something", compositeProperties["item.secondProperty"])
    }
}