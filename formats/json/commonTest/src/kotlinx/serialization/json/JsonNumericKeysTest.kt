/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.serializer
import kotlin.test.*

class JsonNumericKeysTest : JsonTestBase() {
    @Serializable
    data class EntryWrapper(val e: Map.Entry<Int, Int>)

    @Serializable
    data class MapWrapper(val m: Map<Int, Int>)

    @Test
    fun testIntegerKeyInTopLevelEntry() {
        assertJsonFormAndRestored(MapEntrySerializer(Int.serializer(), Int.serializer()), getEntry(), """{"1":2}""")
    }

    @Test
    fun testIntegerKeyInEntry() {
        assertJsonFormAndRestored(EntryWrapper.serializer(), EntryWrapper(getEntry()), """{"e":{"1":2}}""")
    }

    @Test
    fun testIntegerKeyInTopLevelMap() = parametrizedTest {
        assertJsonFormAndRestored(serializer(), mapOf(1 to 2), """{"1":2}""")

    }

    @Test
    fun testIntegerKeyInMap() = parametrizedTest {
        assertJsonFormAndRestored(MapWrapper.serializer(), MapWrapper(mapOf(1 to 2)), """{"m":{"1":2}}""")
    }

    // Workaround equals on JS and Native
    fun getEntry(): Map.Entry<Int, Int> {
        val e = default.decodeFromString(MapEntrySerializer(Int.serializer(), Int.serializer()), """{"1":2}""")
        assertEquals(1, e.key)
        assertEquals(2, e.value)
        return e
    }
}
