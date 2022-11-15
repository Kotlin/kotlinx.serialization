package kotlinx.serialization

import junit.framework.TestCase.*
import kotlinx.serialization.json.*
import org.junit.*

@Serializable
class Basic(val i: Int)

class BaseTest {
    @Test
    fun baseTest() {
        println(Basic.serializer().descriptor)
        assertEquals("""{"i":42}""", Json.encodeToString(Basic(42)))
    }
}
