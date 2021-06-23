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
    fun testWriteDefaultPair() = parametrizedTest { jsonTestingMode ->
        val pair = 42 to "foo"
        val serializer = PairSerializer(
            Int.serializer(),
            String.serializer()
        )
        val s = default.encodeToString(serializer, pair, jsonTestingMode)
        assertEquals("""{"first":42,"second":"foo"}""", s)
        val restored = default.decodeFromString(serializer, s, jsonTestingMode)
        assertEquals(pair, restored)
    }

    @Test
    fun testWritePlainTriple() = parametrizedTest { jsonTestingMode ->
        val triple = Triple(42, "foo", false)
        val serializer = TripleSerializer(
            Int.serializer(),
            String.serializer(),
            Boolean.serializer()
        )
        val s = default.encodeToString(serializer, triple, jsonTestingMode)
        assertEquals("""{"first":42,"second":"foo","third":false}""", s)
        val restored = default.decodeFromString(serializer, s, jsonTestingMode)
        assertEquals(triple, restored)
    }

    @Test
    fun testRecursiveArrays() = parametrizedTest { jsonTestingMode ->
        val arr = Array2DBox(arrayOf(arrayOf(2.1, 1.2), arrayOf(42.3, -3.4)))
        val str = default.encodeToString(Array2DBox.serializer(), arr, jsonTestingMode)
        assertEquals("""{"arr":[[2.1,1.2],[42.3,-3.4]]}""", str)
        val restored = default.decodeFromString(Array2DBox.serializer(), str, jsonTestingMode)
        assertTrue(arr.arr.contentDeepEquals(restored.arr))
    }
}
