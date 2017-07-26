package kotlinx.serialization

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Created by user on 7/14/17.
 */


class InternalInheritanceTest {
    @Serializable
    open class A(val parent: Int) {
        @Optional
        private val rootOptional = "rootOptional"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is A) return false

            if (parent != other.parent) return false
            if (rootOptional != other.rootOptional) return false

            return true
        }

    }

    @Serializable
    open class B(val parent2: Int, @Transient val transientDerived: String = "X", val derived: String) : A(parent2) {
        protected val bodyDerived = "body"
    }

    @Serializable
    class C(val parent3: Int) : B(parent3, derived = "derived") {
        @Optional
        val lastDerived = "optional"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other?.javaClass != javaClass) return false

            other as C

            if (!super.equals(other)) return false
            if (parent3 != other.parent3) return false
            if (lastDerived != other.lastDerived) return false
            if (parent2 != other.parent2) return false
            if (transientDerived != other.transientDerived) return false
            if (derived != other.derived) return false
            if (bodyDerived != other.bodyDerived) return false

            return true
        }


    }


    @Test
    fun test() {
        assertEquals(
                "{parent:42,rootOptional:rootOptional,parent2:42,derived:derived,bodyDerived:body,parent3:42,lastDerived:optional}",
                JSON.unquoted.stringify(C(42))
        )
        assertEquals(
                "{parent:13,rootOptional:rootOptional,parent2:13,derived:bbb,bodyDerived:body}",
                JSON.unquoted.stringify(B(13, derived = "bbb"))
        )
        assertEquals(
                C(42),
                JSON.unquoted.parse<C>("{parent:42,rootOptional:rootOptional,parent2:42,derived:derived,bodyDerived:body,parent3:42,lastDerived:optional}")
        )
        assertEquals(
                C(43),
                JSON.unquoted.parse<C>("{parent:43,rootOptional:rootOptional,parent2:43,derived:derived,bodyDerived:body,parent3:43,lastDerived:optional}")
        )
        assertEquals(
                B(100, derived = "wowstring"),
                JSON.unquoted.parse<B>("{parent:100,rootOptional:rootOptional,parent2:100,derived:wowstring,bodyDerived:body}")
        )
        assertEquals(
                C(44),
                JSON.unquoted.parse<C>("{parent:44, parent2:44,derived:derived,bodyDerived:body,parent3:44}")
        )
        assertEquals(
                B(101, derived = "wowstring"),
                JSON.unquoted.parse<B>("{parent:101,parent2:101,derived:wowstring,bodyDerived:body}")
        )
        assertEquals(
                A(77),
                JSON.unquoted.parse<A>("{parent:77,rootOptional:rootOptional}")
        )
        assertEquals(
                A(78),
                JSON.unquoted.parse<A>("{parent:78}")
        )
    }

    @Test(expected = SerializationException::class)
    fun testThrow1() {
        JSON.unquoted.parse<B>("{parent:100,rootOptional:rootOptional,transientDerived: X, parent2:100,derived:wowstring,bodyDerived:body}")
    }

    @Test(expected = SerializationException::class)
    fun testThrow2() {
        JSON.unquoted.parse<C>("{parent:42,rootOptional:rootOptional,derived:derived,bodyDerived:body,parent3:42,lastDerived:optional}")
    }
}
