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

    private inline fun <reified T : Any> assertMappedAndRestored(expectedMap: Map<String, Any>, obj: T) {
        val map = Properties.store(obj)
        assertEquals(expectedMap, map)
        val unmap = Properties.load<T>(map)
        assertEquals(obj, unmap)
    }

    private inline fun <reified T : Any> assertMappedNullableAndRestored(expectedMap: Map<String, Any?>, obj: T) {
        val map = Properties.storeNullable(obj)
        assertEquals(expectedMap, map)
        val unmap = Properties.loadNullable<T>(map)
        assertEquals(obj, unmap)
    }

    @Test
    @Ignore // todo: unignore after migration to 1.3.70-eap-3
    fun testMultipleTypes() {
        val data = MultiType(1, "2")
        assertMappedAndRestored(
            mapOf("first" to 1, "second" to "2", "unit" to Unit, "last" to true),
            data
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
            data
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
        assertMappedAndRestored(mapOf, recursive)
    }

    @Test
    fun testNullableProperties() {
        val data = NullableData(null, null, "property")
        val expectedMap = mapOf("nullable" to null, "nullable2" to null, "property" to "property")
        assertMappedNullableAndRestored(expectedMap, data)
    }

    @Test
    fun testNestedNull() {
        val category = Category(name = "Name")
        val expectedMap = mapOf("name" to "Name", "subCategory" to null)
        assertMappedNullableAndRestored(expectedMap, category)
    }

    @Test
    fun testNestedNullable() {
        val category = Category(
            name = "Name",
            subCategory = SubCategory()
        )
        val expectedMap = mapOf("name" to "Name", "subCategory.name" to null, "subCategory.option" to null)
        assertMappedNullableAndRestored(expectedMap, category)
    }

    @Test
    fun testLoadOptionalProps() {
        val map: Map<String, Any> = mapOf("name" to "Name")
        val restored = Properties.load<Category>(map)
        assertEquals(Category("Name"), restored)
    }

    @Test
    fun testLoadOptionalNestedProps() {
        val map: Map<String, Any> = mapOf("name" to "Name", "subCategory.name" to "SubName")
        val restored = Properties.load<Category>(map)
        assertEquals(Category("Name", SubCategory("SubName")), restored)
    }

    @Test
    fun testOmitsNullAndCanLoadBack() {
        val category = Category(name = "Name")
        val expectedMap = mapOf("name" to "Name")
        assertMappedAndRestored(expectedMap, category)
    }

    @Test
    fun testLoadNullableOptionalNestedProps() {
        val map: Map<String, Any?> = mapOf("name" to "Name", "subCategory.name" to null)
        val restored = Properties.loadNullable<Category>(map)
        assertEquals(Category("Name", SubCategory()), restored)
    }

    @Test
    fun testThrowsOnIncorrectMaps() {
        val map: Map<String, Any?> = mapOf("name" to "Name")
        assertFailsWith<MissingFieldException> {
            Properties.loadNullable<Data>(map)
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
}
