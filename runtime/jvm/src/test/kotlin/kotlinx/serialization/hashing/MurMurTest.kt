package kotlinx.serialization.hashing

import kotlinx.serialization.Serializable
import org.junit.*
import org.junit.Test
import kotlin.test.*

class MurMurTest {

    @Serializable
    data class Person(val id: Int, val name: String, val surname: String)

    @Test
    fun testLongHash() {
        val person = Person(1, "name", "surname")
        val hasher = MurMur3_128Hasher()
        assertEquals(5334069520134281793L, hasher.longHash(person))
    }
}
