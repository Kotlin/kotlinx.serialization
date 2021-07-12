package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class ProtobufCollectionsTest {
    @Serializable
    data class ListWithNestedList(val l: List<List<Int>?>)

    @Serializable
    data class ListWithNestedMap(val l: List<Map<Int, Int>>)

    @Serializable
    data class MapWithNullableNestedLists(val m: Map<List<Int>?, List<Int>?>)

    @Serializable
    data class NullableListElement(val l: List<Int?>)

    @Serializable
    data class NullableMapKey(val m: Map<Int?, Int>)

    @Serializable
    data class NullableMapValue(val m: Map<Int, Int?>)

    @Test
    fun testEncodeNullAsListElement() {
        assertFailsWith(SerializationException::class) { ProtoBuf.encodeToByteArray(NullableListElement(listOf(null))) }
    }

    @Test
    fun testEncodeNullAsMapKey() {
        assertFailsWith(SerializationException::class) { ProtoBuf.encodeToByteArray(NullableMapKey(mapOf(null to 42))) }
    }

    @Test
    fun  testEmptyListIsNotListOfEmpty() {
        val emptyListBytes = ProtoBuf.encodeToByteArray(ListWithNestedList(emptyList()))
        val listOfEmptyBytes = ProtoBuf.encodeToByteArray(ListWithNestedList(listOf(emptyList())))
        val emptyList = ProtoBuf.decodeFromByteArray<ListWithNestedList>(emptyListBytes)
        val listOfEmpty = ProtoBuf.decodeFromByteArray<ListWithNestedList>(listOfEmptyBytes)

        assertNotEquals(emptyList, listOfEmpty)
    }

    @Test
    fun testEncodeMapWithNullableKey() {
        val map = NullableMapKey(mapOf(42 to 43))
        val bytes = ProtoBuf.encodeToByteArray(map)
        val decoded = ProtoBuf.decodeFromByteArray<NullableMapKey>(bytes)
        assertEquals(map, decoded)
    }

    @Test
    fun testEncodeNullAsMapValue() {
        assertFailsWith(SerializationException::class) { ProtoBuf.encodeToByteArray(NullableMapValue(mapOf(42 to null))) }
    }

    @Test
    fun testEncodeMapWithNullableValue() {
        val map = NullableMapValue(mapOf(42 to 43))
        val bytes = ProtoBuf.encodeToByteArray(map)
        val decoded = ProtoBuf.decodeFromByteArray<NullableMapValue>(bytes)
        assertEquals(map, decoded)
    }

    @Test
    fun testNestedList() {
        val lists = listOf(listOf(42, 0), emptyList(), listOf(43))
        val bytes = ProtoBuf.encodeToByteArray(ListWithNestedList(lists))
        val decoded = ProtoBuf.decodeFromByteArray<ListWithNestedList>(bytes)
        assertEquals(lists, decoded.l)
    }

    @Test
    fun testNestedListIsNull() {
        assertFailsWith(SerializationException::class) { ProtoBuf.encodeToByteArray(ListWithNestedList(listOf(null))) }
    }

    @Test
    fun testNestedMapInList() {
        val list = listOf(mapOf(1 to 2, 2 to 4), emptyMap(), mapOf(3 to 8))
        val bytes = ProtoBuf.encodeToByteArray(ListWithNestedMap(list))
        val decoded = ProtoBuf.decodeFromByteArray<ListWithNestedMap>(bytes)
        assertEquals(list, decoded.l)
    }

    @Test
    fun testNestedListsInMap() {
        val map = mapOf<List<Int>?, List<Int>?>(listOf(42, 0) to listOf(43, 1), listOf(5) to listOf(20, 11))
        val bytes = ProtoBuf.encodeToByteArray(MapWithNullableNestedLists(map))
        val decoded = ProtoBuf.decodeFromByteArray<MapWithNullableNestedLists>(bytes)
        assertEquals(map, decoded.m)
    }

    @Test
    fun testNestedListsAreNullInMap() {
        assertFailsWith(SerializationException::class) { ProtoBuf.encodeToByteArray(MapWithNullableNestedLists(mapOf(null to emptyList()))) }
        assertFailsWith(SerializationException::class) { ProtoBuf.encodeToByteArray(MapWithNullableNestedLists(mapOf(emptyList<Int>() to null))) }
        assertFailsWith(SerializationException::class) { ProtoBuf.encodeToByteArray(MapWithNullableNestedLists(mapOf(null to null))) }
    }
}
