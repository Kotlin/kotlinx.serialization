/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:Suppress("EqualsOrHashCode")

package kotlinx.serialization.json

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.internal.*
import kotlin.test.*

class JsonTransientTest : JsonTestBase() {

    @Serializable
    class Data(val a: Int = 0, @Transient var b: Int = 42, val e: Boolean = false) {
        var c = "Hello"
        val d: String
            get() = "hello"

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

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
    fun testAll() = parametrizedTest { useStreaming ->
        assertEquals("""{"a":0,"e":false,"c":"Hello"}""",
            default.encodeToString(Data.serializer(), Data(), useStreaming))
    }

    @Test
    fun testMissingOptionals() = parametrizedTest { useStreaming ->
        assertEquals(default.decodeFromString(Data.serializer(), """{"a":0,"c":"Hello"}""", useStreaming), Data())
        assertEquals(default.decodeFromString(Data.serializer(), """{"a":0}""", useStreaming), Data())
    }

    @Test
    fun testThrowTransient() = parametrizedTest { useStreaming ->
        assertFailsWith(JsonDecodingException::class) {
            default.decodeFromString(Data.serializer(), """{"a":0,"b":100500,"c":"Hello"}""", useStreaming)
        }
    }
}
