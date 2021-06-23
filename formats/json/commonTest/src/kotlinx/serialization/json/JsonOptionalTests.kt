/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.test.*
import kotlin.test.*

class JsonOptionalTests : JsonTestBase() {

    @Suppress("EqualsOrHashCode")
    @Serializable
    internal class Data(@Required val a: Int = 0, val b: Int = 42) {

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
    fun testAll() = parametrizedTest { jsonTestingMode ->
        assertEquals("""{"a":0,"b":42,"c":"Hello"}""",
            default.encodeToString(Data.serializer(), Data(), jsonTestingMode))
        assertEquals(lenient.decodeFromString(Data.serializer(), "{a:0,b:43,c:Hello}", jsonTestingMode), Data(b = 43))
        assertEquals(lenient.decodeFromString(Data.serializer(), "{a:0,b:42,c:Hello}", jsonTestingMode), Data())
    }

    @Test
    fun testMissingOptionals() = parametrizedTest { jsonTestingMode ->
        assertEquals(default.decodeFromString(Data.serializer(), """{"a":0,"c":"Hello"}""", jsonTestingMode), Data())
        assertEquals(default.decodeFromString(Data.serializer(), """{"a":0}""", jsonTestingMode), Data())
    }

    @Test
    fun testThrowMissingField() = parametrizedTest { jsonTestingMode ->
        assertFailsWithMissingField {
            lenient.decodeFromString(Data.serializer(), "{b:0}", jsonTestingMode)
        }
    }
}
