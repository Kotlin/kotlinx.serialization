/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */


package kotlinx.serialization.properties

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlin.test.*

class PropertiesTest {

    @Serializable
    data class Data(val list: List<String>, val property: String)

    @Serializable
    data class Recursive(val data: Data, val property: String)

    @Serializable
    data class NullableData(val property: String, val nullable: String? = null, val nullable2: String? = null)

    @Serializable
    data class Category(var name: String? = null, var subCategory: SubCategory? = null)

    @Serializable
    data class SubCategory(var name: String? = null, var option: String? = null)

    @Serializable
    data class DataWithMap(val map: Map<String, Int> = mapOf())

    @Serializable
    data class MultiType(
        val first: Int,
        val second: String,
        val unit: Unit = Unit,
        val last: Boolean = true
    )

    @Serializable
    data class TestWithSize(
        val p: String? = null,
        val size: String? = null
    )

    @Serializable
    data class EnumData(val data: TestEnum)

    @Serializable
    data class NullableEnumData(val data0: TestEnum?, val data1: TestEnum?)

    enum class TestEnum { ZERO, ONE }

    private inline fun <reified T : Any> assertMappedAndRestored(
        expectedMap: Map<String, Any>,
        obj: T,
        serializer: KSerializer<T>
    ) {
        val map = Properties.encodeToMap(serializer, obj)
        assertEquals(expectedMap, map)
        val unmap = Properties.decodeFromMap<T>(serializer, map)
        assertEquals(obj, unmap)
    }

    private inline fun <reified T : Any> assertMappedNullableAndRestored(
        expectedMap: Map<String, Any?>,
        obj: T,
        serializer: KSerializer<T>
    ) {
        val map = Properties.encodeToMap(serializer, obj)
        assertEquals(expectedMap, map)
        val unmap = Properties.decodeFromMap<T>(serializer, map)
        assertEquals(obj, unmap)
    }

    @Test
    fun testMultipleTypes() {
        val data = MultiType(1, "2")
        assertMappedAndRestored(
            mapOf("first" to 1, "second" to "2", "last" to true),
            data,
            MultiType.serializer()
        )
    }

    @Test
    fun testUnitIsEmptyMap() {
        assertEquals(emptyMap(), Properties.encodeToMap(Unit.serializer(), Unit))
    }

    @Test
    fun testList() {
        val data = Data(listOf("element1"), "property")
        assertMappedAndRestored(
            mapOf(
                "list.0" to "element1",
                "property" to "property"
            ),
            data,
            Data.serializer()
        )
    }

    @Test
    fun testNestedStructure() {
        val recursive = Recursive(
            Data(
                listOf("l1"),
                "property"
            ), "string"
        )
        val mapOf =
            mapOf("data.list.0" to "l1", "data.property" to "property", "property" to "string")
        assertMappedAndRestored(mapOf, recursive, Recursive.serializer())
    }

    @Test
    fun testNullableProperties() {
        val data = NullableData("property", null, null)
        val expectedMap = mapOf("property" to "property")
        assertMappedNullableAndRestored(expectedMap, data, NullableData.serializer())
    }

    @Test
    fun testNestedNull() {
        val category = Category(name = "Name")
        val expectedMap = mapOf("name" to "Name")
        assertMappedNullableAndRestored(expectedMap, category, Category.serializer())
    }

    @Test
    fun testLoadOptionalProps() {
        val map: Map<String, Any> = mapOf("name" to "Name")
        val restored = Properties.decodeFromMap<Category>(Category.serializer(), map)
        assertEquals(Category("Name"), restored)
    }

    @Test
    fun testLoadOptionalNestedProps() {
        val map: Map<String, Any> = mapOf("name" to "Name", "subCategory.name" to "SubName")
        val restored = Properties.decodeFromMap<Category>(Category.serializer(), map)
        assertEquals(Category("Name", SubCategory("SubName")), restored)
    }

    @Test
    fun testOmitsNullAndCanLoadBack() {
        val category = Category(name = "Name")
        val expectedMap = mapOf("name" to "Name")
        assertMappedAndRestored(expectedMap, category, Category.serializer())
    }

    @Test
    fun testThrowsOnIncorrectMaps() {
        val map: Map<String, Any> = mapOf("name" to "Name")
        assertFailsWith<SerializationException> {
            Properties.decodeFromMap(Data.serializer(), map)
        }
    }

    @Test
    fun testNestedMap() {
        val map0 = DataWithMap(mapOf())
        val map1 = DataWithMap(mapOf("one" to 1))
        val map2 = DataWithMap(mapOf("one" to 1, "two" to 2))

        fun doTest(testData: DataWithMap) {
            val map = Properties.encodeToMap(
                DataWithMap.serializer(),
                testData
            )
            val d2 = Properties.decodeFromMap(
                DataWithMap.serializer(),
                map
            )
            assertEquals(testData, d2)
        }

        doTest(map0)
        doTest(map1)
        doTest(map2)
    }

    @Test
    fun testEnumString() {
        val map = mapOf("data" to "ZERO")
        val loaded = Properties.decodeFromMap(EnumData.serializer(), map)
        assertEquals(EnumData(TestEnum.ZERO), loaded)
    }


    @Test
    fun testEnumInteger() {
        val map = mapOf("data" to 0)
        val loaded = Properties.decodeFromMap(EnumData.serializer(), map)
        assertEquals(EnumData(TestEnum.ZERO), loaded)
    }

    @Test
    fun testCanReadSizeProperty() {
        assertMappedAndRestored(mapOf("p" to "a", "size" to "b"), TestWithSize("a", "b"), TestWithSize.serializer())
    }
}
