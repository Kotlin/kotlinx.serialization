/*
 * Copyright 2017-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlin.test.*

class JsonOptionalTests : JsonTestBase() {

    @Suppress("EqualsOrHashCode")
    @Serializable
    internal class Data(val a: Int = 0, @Optional val b: Int = 42) {
        @Optional
        var c = "Hello"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Data

            if (a != other.a) return false
            if (b != other.b) return false
            if (c != other.c) return false

            return true
        }

    }

    @Test
    fun testAll() = parametrizedTest { useStreaming ->
        assertEquals("{a:0,b:42,c:Hello}", unquoted.stringify(Data(), useStreaming))
        assertEquals(unquoted.parse("{a:0,b:43,c:Hello}", useStreaming), Data(b = 43))
        assertEquals(unquoted.parse("{a:0,b:42,c:Hello}", useStreaming), Data())
    }

    @Test
    fun testMissingOptionals() = parametrizedTest { useStreaming ->
        assertEquals(unquoted.parse("{a:0,c:Hello}", useStreaming), Data())
        assertEquals(unquoted.parse("{a:0}", useStreaming), Data())
    }

    @Test
    fun testThrowMissingField() = parametrizedTest { useStreaming ->
        assertFailsWith(MissingFieldException::class) {
            unquoted.parse<Data>("{b:0}", useStreaming)
        }
    }

}
