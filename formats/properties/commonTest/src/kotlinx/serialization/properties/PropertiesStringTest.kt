package kotlinx.serialization.properties

import kotlinx.serialization.Serializable
import kotlin.test.*

internal class PropertiesStringTest {

    @Serializable
    data class Data(val someProp: String? = "hello")

    // due to the problem of iteration order
    // we can only do asserts on single property objects
    private inline fun <reified T : Any> testRoundTrip(
        expected: String, obj: T
    ) {
        val propertyString = Properties.encodeToString(obj)
        assertEquals(expected, propertyString)

        val unmap = Properties.decodeFromString<T>(propertyString)
        assertEquals(obj, unmap)
    }

    @Test fun simpleDataWithDefaultValue() {
        testRoundTrip("someProp=hello", Data())
    }

    @Test fun simpleDataWithSpecialCharacters() {
        testRoundTrip("someProp=\\=some\\n", Data("=some\n"))
    }
}