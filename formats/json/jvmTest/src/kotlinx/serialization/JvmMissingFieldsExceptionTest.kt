package kotlinx.serialization

import org.junit.Test
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.junit.Ignore
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@Ignore("Test of future compiler optimization, remove annotation in release 1.1!")
class JvmMissingFieldsExceptionTest {
    @Serializable
    data class Generic<out T1, out T2, out T3>(val f1: T1, val f2: T2, val f3: T3)

    @Serializable
    sealed class Parent(val p1: Int, val p2: Int = 2, val p3: Int) {
        @Serializable
        @SerialName("child")
        data class Child(val c1: Int = 1, val c2: Int, val c3: Int = 3) : Parent(c1 + 1, 2, 3)
    }

    @Serializable
    open class ShortPlaneClass(val f1: Int, val f2: Int, val f3: Int = 3, val f4: Int)

    @Serializable
    class WithTransient(val f1: Int, @Transient val f2: Int = 2, val f3: Int, val f4: Int)

    @Serializable
    abstract class SimpleAbstract(val p1: Int, val p2: Int)

    @Serializable
    @SerialName("a")
    data class ChildA(val c1: Int, val c2: Int = 2, val c3: Int) : SimpleAbstract(0, 10)

    @Serializable
    data class PolymorphicWrapper(@Polymorphic val nested: SimpleAbstract)

    @Serializable
    class BigPlaneClass(
            val f0: Int,
            val f5: Int = 5,
            val f6: Int,
            val f7: Int = 7,
            val f8: Int,
            val f9: Int,
            val f10: Int,
            val f11: Int,
            val f12: Int,
            val f13: Int,
            val f14: Int,
            val f15: Int,
            val f16: Int,
            val f17: Int,
            val f18: Int,
            val f19: Int,
            val f20: Int,
            val f21: Int,
            val f22: Int,
            val f23: Int,
            val f24: Int,
            val f25: Int,
            val f26: Int,
            val f27: Int,
            val f28: Int,
            val f29: Int,
            val f30: Int,
            val f31: Int,
            val f32: Int,
            val f33: Int,
            val f34: Int,
            val f35: Int,
    ) : ShortPlaneClass(1, 2, 3, 4)

    @Test
    fun testShortPlaneClass() {
        assertFailsWithMessages(listOf("f2", "f4")) {
            Json.decodeFromString<ShortPlaneClass>("""{"f1":1}""")
        }
    }

    @Test
    fun testBigPlaneClass() {
        val missedFields = MutableList(35) { "f$it" }
        val definedInJsonFields = arrayOf("f1", "f15", "f34")
        val optionalFields = arrayOf("f3", "f5", "f7")
        missedFields.removeAll(definedInJsonFields)
        missedFields.removeAll(optionalFields)
        assertFailsWithMessages(missedFields) {
            Json.decodeFromString<BigPlaneClass>("""{"f1":1, "f15": 15, "f34": 34}""")
        }
    }

    @Test
    fun testAnnotatedPolymorphic() {
        val module = SerializersModule {
            polymorphic(SimpleAbstract::class, null) {
                subclass(ChildA::class)
            }
        }

        assertFailsWithMessages(listOf("p2", "c3")) {
            Json {
                serializersModule = module
                useArrayPolymorphism = false
            }.decodeFromString<PolymorphicWrapper>("""{"nested": {"type": "a", "p1": 1, "c1": 11}}""")
        }
    }


    @Test
    fun testSealed() {
        assertFailsWithMessages(listOf("p3", "c2")) {
            Json { useArrayPolymorphism = false }
                    .decodeFromString<Parent>("""{"type": "child", "p1":1, "c1": 11}""")
        }
    }

    @Test
    fun testTransient() {
        assertFailsWithMessages(listOf("f3", "f4")) {
            Json { useArrayPolymorphism = false }
                    .decodeFromString<WithTransient>("""{"f1":1}""")
        }
    }

    @Test
    fun testGeneric() {
        assertFailsWithMessages(listOf("f2", "f3")) {
            Json.decodeFromString<Generic<Int, Int, Int>>("""{"f1":1}""")
        }
    }


    private inline fun assertFailsWithMessages(messages: List<String>, block: () -> Unit) {
        val exception = assertFailsWith(SerializationException::class, null, block)
        assertEquals("kotlinx.serialization.MissingFieldException", exception::class.qualifiedName)
        val missedMessages = messages.filter { !exception.message!!.contains(it) }
        assertTrue(missedMessages.isEmpty(), "Expected message '${exception.message}' to contain substrings $missedMessages")
    }
}
