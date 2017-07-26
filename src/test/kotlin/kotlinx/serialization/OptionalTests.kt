package kotlinx.serialization

import org.junit.Assert.assertEquals
import org.junit.Test

class OptionalTests {

    @Serializable
    class Data(val a: Int = 0, @Optional val b: Int = 42) {
        @Optional
        var c = "Hello"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other?.javaClass != javaClass) return false

            other as Data

            if (a != other.a) return false
            if (b != other.b) return false
            if (c != other.c) return false

            return true
        }
    }

    @Test
    fun test() {
        assertEquals("{a:0,b:42,c:Hello}",JSON.unquoted.stringify(Data()))
        assertEquals(JSON.unquoted.parse<Data>("{a:0,b:43,c:Hello}"),Data(b = 43))
        assertEquals(JSON.unquoted.parse<Data>("{a:0,b:42,c:Hello}"),Data())
        assertEquals(JSON.unquoted.parse<Data>("{a:0,c:Hello}"),Data())
        assertEquals(JSON.unquoted.parse<Data>("{a:0}"),Data())
    }

    @Test(expected = SerializationException::class)
    fun testThrow() {
        JSON.unquoted.parse<Data>("{b:0}")
    }

}
