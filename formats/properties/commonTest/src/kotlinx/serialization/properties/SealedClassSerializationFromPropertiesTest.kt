package kotlinx.serialization.properties

import kotlin.reflect.KProperty1
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

    @Serializable
    data class CompositeClass(val item: BaseClass)

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

        assertEquals("FIRSTCHILD", instanceProperties["type"])
        assertEquals(1L, instanceProperties["firstProperty"])
        assertEquals("one", instanceProperties["secondProperty"])
    }

    @Test
    fun testWrappedPropertiesDeserialization() {
        val props = mapOf(
            "0.type" to "FIRSTCHILD",
            "0.firstProperty" to 1L,
            "0.secondProperty" to "one",
            "1.type" to "SECONDCHILD",
            "1.firstProperty" to 2L,
            "1.secondProperty" to "two"
        )

        val instances: List<BaseClass> = Properties.decodeFromMap(props)

        val expected = listOf(FirstChild(1, "one"), SecondChild(2, "two"))
        assertEquals(expected, instances)
    }

    @Test
    fun testWrappedPropertiesSerialization() {
        val instances: List<BaseClass> = listOf(
            FirstChild(firstProperty = 1L, secondProperty = "one"),
            SecondChild(firstProperty = 2L, secondProperty = "two")
        )

        val instanceProperties = Properties.encodeToMap(instances)

        assertEquals("FIRSTCHILD", instanceProperties["0.type"])
        assertEquals(1L, instanceProperties["0.firstProperty"])
        assertEquals("one", instanceProperties["0.secondProperty"])
        assertEquals("SECONDCHILD", instanceProperties["1.type"])
        assertEquals(2L, instanceProperties["1.firstProperty"])
        assertEquals("two", instanceProperties["1.secondProperty"])
    }

    @Test
    fun testCompositeClassPropertiesDeserialization() {
        val props = mapOf(
            "item.type" to "SECONDCHILD",
            "item.firstProperty" to 7L,
            "item.secondProperty" to "nothing"
        )

        val composite: CompositeClass = Properties.decodeFromMap(props)

        assertEquals(CompositeClass(SecondChild(7L, "nothing")), composite)
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