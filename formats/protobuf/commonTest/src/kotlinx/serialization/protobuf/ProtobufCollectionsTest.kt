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
    data class MapWithNullableNestedMaps(val m: Map<Map<String, Int>?, Map<String, Int>?>)

    @Serializable
    data class NullableListElement(val l: List<Int?>)

    @Serializable
    data class NullableMapKey(val m: Map<Int?, Int>)

    @Serializable
    data class NullableMap(val m: Map<Int?, Int?>)

    @Test
    fun testEncodeNullAsListElement() {
        assertFailsWithMessage<SerializationException> ("'null' is not supported as the value of a list element in ProtoBuf") { ProtoBuf.encodeToByteArray(NullableListElement(listOf(null))) }
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
    fun testNullMap() {
        val keyNull = NullableMap(mapOf(null to 42))
        val valueNull = NullableMap(mapOf(42 to null))
        val bothNull = NullableMap(mapOf(null to null))

        val encodedKeyNull = ProtoBuf.encodeToHexString(keyNull)
        val encodedValueNull = ProtoBuf.encodeToHexString(valueNull)
        val encodedBothNull = ProtoBuf.encodeToHexString(bothNull)
        assertEquals(encodedKeyNull, "0a02102a")
        assertEquals(encodedValueNull, "0a02082a")
        assertEquals(encodedBothNull, "0a00")

        val decodedKeyNull = ProtoBuf.decodeFromHexString<NullableMap>(encodedKeyNull)
        val decodedValueNull = ProtoBuf.decodeFromHexString<NullableMap>(encodedValueNull)
        val decodedBothNull = ProtoBuf.decodeFromHexString<NullableMap>(encodedBothNull)
        assertEquals(decodedKeyNull, keyNull)
        assertEquals(decodedValueNull, valueNull)
        assertEquals(decodedBothNull, bothNull)
    }

    @Test
    fun testRepeatNullKeyInMap() {
        // there are two entries in message: (null to 42) and (null to 43), the last one is used
        val decoded = ProtoBuf.decodeFromHexString<NullableMap>("0a04102a102b")
        assertEquals(NullableMap(mapOf(null to 43)), decoded)
    }

    @Test
    fun testCollectionsInNullableMap() {
        assertFailsWithMessage<SerializationException> ("'null' is not supported as the value of collection types in ProtoBuf") { ProtoBuf.encodeToByteArray(MapWithNullableNestedLists(mapOf(null to listOf(42))) ) }
        assertFailsWithMessage<SerializationException> ("'null' is not supported as the value of collection types in ProtoBuf") { ProtoBuf.encodeToByteArray(MapWithNullableNestedLists(mapOf(listOf(42) to null)) ) }
        assertFailsWithMessage<SerializationException> ("'null' is not supported as the value of collection types in ProtoBuf") { ProtoBuf.encodeToByteArray(MapWithNullableNestedMaps(mapOf(null to mapOf("key" to 42))) ) }
        assertFailsWithMessage<SerializationException> ("'null' is not supported as the value of collection types in ProtoBuf") { ProtoBuf.encodeToByteArray(MapWithNullableNestedMaps(mapOf(mapOf("key" to 42) to null)) ) }
    }

    @Test
    fun testEncodeMapWithNullableValue() {
        val map = NullableMap(mapOf(42 to 43))
        val bytes = ProtoBuf.encodeToByteArray(map)
        val decoded = ProtoBuf.decodeFromByteArray<NullableMap>(bytes)
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
        assertFailsWithMessage<SerializationException>("'null' is not supported as the value of collection types in ProtoBuf") {
            ProtoBuf.encodeToByteArray(ListWithNestedList(listOf(null)))
        }
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
        assertFailsWithMessage<SerializationException> ("'null' is not supported as the value of collection types in ProtoBuf") { ProtoBuf.encodeToByteArray(MapWithNullableNestedLists(mapOf(null to emptyList()))) }
        assertFailsWithMessage<SerializationException> ("'null' is not supported as the value of collection types in ProtoBuf") { ProtoBuf.encodeToByteArray(MapWithNullableNestedLists(mapOf(emptyList<Int>() to null))) }
        assertFailsWithMessage<SerializationException> ("'null' is not supported as the value of collection types in ProtoBuf") { ProtoBuf.encodeToByteArray(MapWithNullableNestedLists(mapOf(null to null))) }
    }
}
