/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */


package kotlinx.serialization

import kotlin.test.*

@UseExperimental(ImplicitReflectionSerializer::class)
class MapperTest {

    @Serializable
    data class Data(val list: List<String>, val property: String)

    @Serializable
    data class Recursive(val data: Data, val property: String)

    @Serializable
    data class NullableData(val nullable: String?, val nullable2: String?, val property: String)

    @Serializable
    data class Category(var name: String? = null, var subCategory: SubCategory? = null)

    @Serializable
    data class SubCategory(var name: String? = null)

    @Serializable
    data class DataWithMap(val map: Map<String, Int>)

    @Test
    fun testListTagStack() {
        val data = Data(listOf("element1"), "property")
        val map = Mapper.map(data)
        val unmap =
            Mapper.unmap<Data>(
                map
            )
        assertEquals(data.list, unmap.list)
        assertEquals(data.property, unmap.property)
    }

    @Test
    fun testRecursiveBlockTag() {
        val recursive = Recursive(
            Data(
                listOf("l1"),
                "property"
            ), "string"
        )

        val map = Mapper.map(recursive)
        val unmap =
            Mapper.unmap<Recursive>(
                map
            )

        assertEquals(recursive.data.list, unmap.data.list)
        assertEquals(recursive.data.property, unmap.data.property)
        assertEquals(recursive.property, unmap.property)
    }

    @Test
    fun testNullableTagStack() {
        val data = NullableData(null, null, "property")

        val map = Mapper.mapNullable(data)
        val unmap =
            Mapper.unmapNullable<NullableData>(
                map
            )

        assertEquals(data.nullable, unmap.nullable)
        assertEquals(data.nullable2, unmap.nullable2)
        assertEquals(data.property, unmap.property)
    }

    @Test
    fun testNestedNull() {
        val category = Category(name = "Name")
        val map = Mapper.mapNullable(category)
        val recreatedCategory =
            Mapper.unmapNullable<Category>(
                map
            )
        assertEquals(category, recreatedCategory)
    }

    @Test
    fun testNestedNullable() {
        val category = Category(
            name = "Name",
            subCategory = SubCategory()
        )
        val map = Mapper.mapNullable(category)
        val recreatedCategory =
            Mapper.unmapNullable<Category>(
                map
            )
        assertEquals(category, recreatedCategory)
    }

    @Test
    fun failsOnIncorrectMaps() {
        val map: Map<String, Any?> = mapOf("name" to "Name")
        assertFailsWith<NoSuchElementException> {
            Mapper.unmapNullable<Category>(
                map
            )
        }
    }

    @Test
    @Ignore // todo: fix map1 and map2 tests
    fun worksWithNestedMap() {
        val map0 = DataWithMap(mapOf())
        val map1 = DataWithMap(mapOf("one" to 1))
        val map2 = DataWithMap(mapOf("one" to 1, "two" to 2))

        fun doTest(testData: DataWithMap) {
            val map = Mapper.map(
                DataWithMap.serializer(),
                testData
            )
            val d2 = Mapper.unmap(
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
