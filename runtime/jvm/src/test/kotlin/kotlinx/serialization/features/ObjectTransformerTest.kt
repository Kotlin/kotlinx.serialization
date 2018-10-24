package kotlinx.serialization.features

import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.Serializable
import kotlinx.serialization.ValueTransformer
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

object LowercaseTransformer : ValueTransformer() {
    override fun transformStringValue(desc: SerialDescriptor, index: Int, value: String): String =
        value.toLowerCase()
}

private val originalString = "oRiGinaL StRIng"
private val expectedString = originalString.toLowerCase()

@Serializable
data class SimpleString(val str: String = originalString)

@Serializable
data class NestedStrings(val str: String = originalString, val nested: SimpleString = SimpleString())

@Serializable
data class StringsInList(val str: String = originalString, val l: List<String> = listOf(originalString, originalString))

class ObjectTransformerTest {


    @Test
    fun testSimple() {
        with(LowercaseTransformer.transform(SimpleString())) {
            assertEquals(expectedString, str)
        }
    }

    @Test
    fun testNested() {
        with(LowercaseTransformer.transform(NestedStrings())) {
            assertEquals(expectedString, str)
            assertEquals(expectedString, nested.str)
        }
    }

    @Test
    fun testList() {
        with(LowercaseTransformer.transform(StringsInList())) {
            assertEquals(expectedString, str)
            assertTrue(l.all { it == expectedString })
        }
    }
}
