/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlin.test.*

class ProtobufOptionalCollectionsTest {

    @Serializable
    data class ListWrapper(val dummyInt: Int, val list: List<Int>)

    @Serializable
    data class ListOfLists(val list: List<List<Int>>)

    @Serializable
    data class SetWrapper(val set: Set<Int>)

    @Serializable
    data class MapWrapper(val dummyInt: Int = 42, val list: Map<Int, Int>)

    @Serializable
    data class ArrayWrapper(val array: IntArray)

    @Test
    fun testEmptyList() {
        assertFailsWith<SerializationException> { ProtoBuf.dump(ListWrapper(2, emptyList())) }
        assertSerializedToBinaryAndRestored(listOf<Int>(), serializer())
    }

    @Test
    fun testEmptyListOfLists() {
        assertFailsWith<SerializationException> { ProtoBuf.dump(ListOfLists(listOf(emptyList()))) }
        assertSerializedToBinaryAndRestored(listOf<List<Int>>(emptyList(), emptyList()), serializer())
    }

    @Test
    fun testEmptySetSerializer() {
        assertFailsWith<SerializationException> { ProtoBuf.dump(SetWrapper(emptySet())) }
        assertSerializedToBinaryAndRestored(setOf<Int>(), serializer())
    }

    @Test
    fun testEmptyMap() {
        assertFailsWith<SerializationException> { ProtoBuf.dump(MapWrapper(3, emptyMap())) }
        assertSerializedToBinaryAndRestored(mapOf<Int, Int>(), serializer())
    }

    @Test
    fun testEmptyArray() {
        assertFailsWith<SerializationException> { ProtoBuf.dump(ArrayWrapper(intArrayOf())) }
        val data = ProtoBuf.dump(intArrayOf())
        assertTrue(ProtoBuf.load<IntArray>(data).isEmpty())
    }
}