/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlin.test.*

class JsonOverwriteTest : JsonTestBase() {
    @Serializable
    data class Updatable1(val l: List<Int>)

    @Serializable
    data class Data(val a: Int)

    @Serializable
    data class Updatable2(val l: List<Data>)

    @Serializable
    data class NullableInnerIntList(val data: List<Int?>)

    @Serializable
    data class NullableUpdatable(val data: List<Data>?)

    @Test
    fun testCanUpdatePrimitiveList() = parametrizedTest { useStreaming ->
        val parsed =
            lenient.decodeFromString<Updatable1>(Updatable1.serializer(), """{"l":[1,2],"f":"foo","l":[3,4]}""", useStreaming)
        assertEquals(Updatable1(listOf(3, 4)), parsed)
    }

    @Test
    fun testCanUpdateObjectList() = parametrizedTest { useStreaming ->
        val parsed = lenient.decodeFromString<Updatable2>(
            Updatable2.serializer(),
            """{"f":"bar","l":[{"a":42}],"l":[{"a":43}]}""",
            useStreaming
        )
        assertEquals(Updatable2(listOf(Data(43))), parsed)
    }

    @Test
    fun testCanUpdateNullableValuesInside() = parametrizedTest { useStreaming ->
        val a1 = default.decodeFromString(NullableInnerIntList.serializer(), """{"data":[null],"data":[1]}""", useStreaming)
        assertEquals(NullableInnerIntList(listOf(1)), a1)
        val a2 = default.decodeFromString(NullableInnerIntList.serializer(), """{"data":[42],"data":[null]}""", useStreaming)
        assertEquals(NullableInnerIntList(listOf(null)), a2)
        val a3 = default.decodeFromString(NullableInnerIntList.serializer(), """{"data":[31],"data":[1]}""", useStreaming)
        assertEquals(NullableInnerIntList(listOf(1)), a3)
    }

    @Test
    fun testCanUpdateNullableValues() = parametrizedTest { useStreaming ->
        val a1 = lenient.decodeFromString(NullableUpdatable.serializer(), """{"data":null,"data":[{"a":42}]}""", useStreaming)
        assertEquals(NullableUpdatable(listOf(Data(42))), a1)
        val a2 = lenient.decodeFromString(NullableUpdatable.serializer(), """{"data":[{a:42}],"data":null}""", useStreaming)
        assertEquals(NullableUpdatable(null), a2)
        val a3 = lenient.decodeFromString(NullableUpdatable.serializer(), """{"data":[{a:42}],"data":[{"a":43}]}""", useStreaming)
        assertEquals(NullableUpdatable(listOf(Data(43))), a3)
    }
}
