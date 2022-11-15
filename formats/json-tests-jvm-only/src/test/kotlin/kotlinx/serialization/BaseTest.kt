package kotlinx.serialization

import junit.framework.TestCase.*
import kotlinx.serialization.json.*
import org.junit.*

@Serializable
class Data(val i: Int)

class BaseTest {
    @Test
    fun baseTest() {
        println(Data.serializer().descriptor)
        assertEquals("""{"i":42}""", Json.encodeToString(Data(42)))
    }
}
