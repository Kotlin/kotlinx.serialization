package kotlinx.serialization

import org.junit.Assert.assertEquals
import org.junit.Test


class TransientTests {
    @Serializable
    class Data(val a: Int = 0, @Transient val b: Int = 42, @Optional val e: Boolean = false) {
        @Optional
        var c = "Hello"

        @Transient
        var d = "World"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other?.javaClass != javaClass) return false

            other as Data

            if (a != other.a) return false
            if (b != other.b) return false
            if (c != other.c) return false
            if (d != other.d) return false

            return true
        }

        override fun toString(): String {
            return "Data(a=$a, b=$b, e=$e, c='$c', d='$d')"
        }


    }

    @Test
    fun test() {
        assertEquals("{a:0,e:false,c:Hello}",JSON.unquoted.stringify(Data()))
        assertEquals(JSON.unquoted.parse<Data>("{a:0,c:Hello}"),Data())
        assertEquals(JSON.unquoted.parse<Data>("{a:0}"),Data())
    }

    @Test(expected = SerializationException::class)
    fun testThrow() {
        JSON.unquoted.parse<Data>("{a:0,b:100500,c:Hello}")
        println("Kek")
    }

}
