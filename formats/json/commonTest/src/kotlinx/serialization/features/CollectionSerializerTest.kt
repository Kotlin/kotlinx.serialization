/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.test.*
import kotlin.test.*

class CollectionSerializerTest {

    @Serializable
    data class CollectionWrapper(
        val collection: Collection<String>
    )

    @Test
    fun testListJson() {
        val list = listOf("foo", "bar", "foo", "bar")

        val string = Json.encodeToString(CollectionWrapper(list))
        assertEquals("""{"collection":["foo","bar","foo","bar"]}""", string)

        val wrapper = Json.decodeFromString<CollectionWrapper>(string)
        assertEquals(list, wrapper.collection)
    }

    @Test
    fun testSetJson() {
        val set = setOf("foo", "bar", "foo", "bar")

        val string = Json.encodeToString(CollectionWrapper(set))
        assertEquals("""{"collection":["foo","bar"]}""", string)

        val wrapper = Json.decodeFromString<CollectionWrapper>(string)
        assertEquals(set.toList(), wrapper.collection)
    }
}
