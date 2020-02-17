/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlin.test.*

class JsonGenericTest : JsonTestBase() {

    @Serializable
    class Array2DBox(val arr: Array<Array<Double>>) {
        override fun toString(): String {
            return arr.contentDeepToString()
        }
    }

    @Test
    fun testWriteDefaultPair() = parametrizedTest { useStreaming ->
        val pair = 42 to "foo"
        val serializer = PairSerializer(
            Int.serializer(),
            String.serializer()
        )
        val s = default.stringify(serializer, pair, useStreaming)
        assertEquals("""{"first":42,"second":"foo"}""", s)
        val restored = default.parse(serializer, s, useStreaming)
        assertEquals(pair, restored)
    }

    @Test
    fun testWritePlainTriple() = parametrizedTest { useStreaming ->
        val triple = Triple(42 , "foo", false)
        val serializer = TripleSerializer(
            Int.serializer(),
            String.serializer(),
            Boolean.serializer()
        )
        val s = unquoted.stringify(serializer, triple, useStreaming)
        assertEquals("{first:42,second:foo,third:false}", s)
        val restored = unquoted.parse(serializer, s, useStreaming)
        assertEquals(triple, restored)
    }

    @Test
    fun testRecursiveArrays() = parametrizedTest { useStreaming ->
        val arr = Array2DBox(arrayOf(arrayOf(2.1, 1.2), arrayOf(42.3, -3.4)))
        val str = default.stringify(Array2DBox.serializer(), arr, useStreaming)
        assertEquals("""{"arr":[[2.1,1.2],[42.3,-3.4]]}""", str)
        val restored = default.parse(Array2DBox.serializer(), str, useStreaming)
        assertTrue(arr.arr.contentDeepEquals(restored.arr))
    }
}
