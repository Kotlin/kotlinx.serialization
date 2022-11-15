package kotlinx.serialization

import org.junit.Test
import kotlin.reflect.full.*
import kotlin.test.*

class RetentionTest {

    @Serializable
    class F(@SerialName("?") val a: Int, @Transient val b: Int = 42, @Required val c: Int)

    @Test
    fun testRetention() {
        assertEquals("?", F::a.findAnnotation<SerialName>()?.value)
        assertNotNull(F::b.findAnnotation<Transient>())
        assertNotNull(F::c.findAnnotation<Required>())
    }
}
