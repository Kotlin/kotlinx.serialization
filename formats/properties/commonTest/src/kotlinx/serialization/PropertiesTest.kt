/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */


package kotlinx.serialization

import kotlin.test.*

@OptIn(ImplicitReflectionSerializer::class)
class PropertiesTest {

    @Serializable
    data class Data(val list: List<String>, val property: String)

    @Serializable
    data class Recursive(val data: Data, val property: String)

    @Serializable
    data class NullableData(val nullable: String?, val nullable2: String?, val property: String)

    @Serializable
    data class Category(var name: String? = null, var subCategory: SubCategory? = null)

    @Serializable
    data class SubCategory(var name: String? = null, var option: String? = null)

    @Serializable
    data class DataWithMap(val map: Map<String, Int>)

    @Serializable
    data class MultiType(
        val first: Int,
        val second: String,
        val unit: Unit = Unit,
        val last: Boolean = true
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
        val map = Properties.store(serializer, obj)
        assertEquals(expectedMap, map)
        val unmap = Properties.load<T>(serializer, map)
        assertEquals(obj, unmap)
    }

    private inline fun <reified T : Any> assertMappedNullableAndRestored(
        expectedMap: Map<String, Any?>,
        obj: T,
        serializer: KSerializer<T>
    ) {
        val map = Properties.storeNullable(serializer, obj)
        assertEquals(expectedMap, map)
        val unmap = Properties.loadNullable<T>(serializer, map)
        assertEquals(obj, unmap)
    }

    @Test
    @Ignore // todo: unignore after migration to 1.3.70-eap-3
    fun testMultipleTypes() {
        val data = MultiType(1, "2")
        assertMappedAndRestored(
            mapOf("first" to 1, "second" to "2", "unit" to Unit, "last" to true),
            data,
            MultiType.serializer()
        )
    }

    @Test
    fun testList() {
        val data = Data(listOf("element1"), "property")
        assertMappedAndRestored(
            mapOf(
                "list.size" to 1,
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
            mapOf("data.list.size" to 1, "data.list.0" to "l1", "data.property" to "property", "property" to "string")
        assertMappedAndRestored(mapOf, recursive, Recursive.serializer())
    }

    @Test
    fun testNullableProperties() {
        val data = NullableData(null, null, "property")
        val expectedMap = mapOf("nullable" to null, "nullable2" to null, "property" to "property")
        assertMappedNullableAndRestored(expectedMap, data, NullableData.serializer())
    }

    @Test
    fun testNestedNull() {
        val category = Category(name = "Name")
        val expectedMap = mapOf("name" to "Name", "subCategory" to null)
        assertMappedNullableAndRestored(expectedMap, category, Category.serializer())
    }

    @Test
    fun testNestedNullable() {
        val category = Category(
            name = "Name",
            subCategory = SubCategory()
        )
        val expectedMap = mapOf("name" to "Name", "subCategory.name" to null, "subCategory.option" to null)
        assertMappedNullableAndRestored(expectedMap, category, Category.serializer())
    }

    @Test
    fun testLoadOptionalProps() {
        val map: Map<String, Any> = mapOf("name" to "Name")
        val restored = Properties.load<Category>(Category.serializer(), map)
        assertEquals(Category("Name"), restored)
    }

    @Test
    fun testLoadOptionalNestedProps() {
        val map: Map<String, Any> = mapOf("name" to "Name", "subCategory.name" to "SubName")
        val restored = Properties.load<Category>(Category.serializer(), map)
        assertEquals(Category("Name", SubCategory("SubName")), restored)
    }

    @Test
    fun testOmitsNullAndCanLoadBack() {
        val category = Category(name = "Name")
        val expectedMap = mapOf("name" to "Name")
        assertMappedAndRestored(expectedMap, category, Category.serializer())
    }

    @Test
    fun testLoadNullableOptionalNestedProps() {
        val map: Map<String, Any?> = mapOf("name" to "Name", "subCategory.name" to null)
        val restored = Properties.loadNullable<Category>(Category.serializer(), map)
        assertEquals(Category("Name", SubCategory()), restored)
    }

    @Test
    fun testThrowsOnIncorrectMaps() {
        val map: Map<String, Any?> = mapOf("name" to "Name")
        assertFailsWith<MissingFieldException> {
            Properties.loadNullable<Data>(Data.serializer(), map)
        }
    }

    @Test
    fun testNestedMap() {
        val map0 = DataWithMap(mapOf())
        val map1 = DataWithMap(mapOf("one" to 1))
        val map2 = DataWithMap(mapOf("one" to 1, "two" to 2))

        fun doTest(testData: DataWithMap) {
            val map = Properties.store(
                DataWithMap.serializer(),
                testData
            )
            val d2 = Properties.load(
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
    fun testEnum() {
        val obj = EnumData(TestEnum.ZERO)
        assertMappedAndRestored(
                mapOf("data" to 0),
                obj,
                EnumData.serializer()
        )
    }

    @Test
    fun testNullableEnum() {
        val obj = NullableEnumData(null, TestEnum.ONE)
        assertMappedNullableAndRestored(
                mapOf("data0" to null, "data1" to 1),
                obj,
                NullableEnumData.serializer()
        )
    }

    @Test
    fun testEnumString() {
        val map = mapOf("data" to "ZERO")
        val loaded = Properties.load(EnumData.serializer(), map)
        assertEquals(EnumData(TestEnum.ZERO), loaded)
    }

    @Test
    fun testNullableEnumString() {
        val map = mapOf("data0" to null, "data1" to "ONE")
        val loaded = Properties.loadNullable(NullableEnumData.serializer(), map)
        assertEquals(NullableEnumData(null, TestEnum.ONE), loaded)
    }
}
