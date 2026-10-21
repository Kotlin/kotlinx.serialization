/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */


package kotlinx.serialization.properties

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlin.test.*

class StringPropertiesTest {

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

    @Serializable
    data class SharedPrefixNames(
        val first: String = "100",
        val firstSecond: String = "100"
    )

    @Serializable
    enum class TestEnum { ZERO, ONE }

    private inline fun <reified T : Any> assertMappedAndRestored(
        expectedString: String,
        obj: T,
        serializer: KSerializer<T>
    ) {
        val string = StringProperties.encodeToString(serializer, obj)
        assertEquals(expectedString, string)

        val unmap = StringProperties.decodeFromString(serializer, string)
        assertEquals(obj, unmap)
    }

    private inline fun <reified T : Any> assertStringDecodesInto(
        input: String,
        expectedObj: T,
        serializer: KSerializer<T>
    ) {
        val result = StringProperties.decodeFromString(serializer, input)
        assertEquals(expectedObj, result)
    }

    private inline fun <reified T : Any> assertMappedAndRestoredCustomFormat(
        expectedString: String,
        obj: T,
        serializer: KSerializer<T>
    ) {
        val stringProperties = StringProperties {
            lineSeparator = LineSeparator.CRLF
            keyValueSeparator = KeyValueSeparator.COLON
            spacesBeforeSeparator = 4
            spacesAfterSeparator = 2
        }
        val string = stringProperties.encodeToString(serializer, obj)
        assertEquals(expectedString, string)

        val unmap = StringProperties.decodeFromString(serializer, string)
        assertEquals(obj, unmap)
    }

    private inline fun <reified T : Any> assertMappedNullableAndRestored(
        expectedString: String,
        obj: T,
        serializer: KSerializer<T>
    ) {
        val map = StringProperties.encodeToString(serializer, obj)
        assertEquals(expectedString, map)
        val unmap = StringProperties.decodeFromString(serializer, map)
        assertEquals(obj, unmap)
    }

    @Test
    fun testMultipleTypes() {
        val data = MultiType(1, "2")
        val expectedString = """
            first=1
            second=2
            last=true

            """.trimIndent()
        assertMappedAndRestored(
            expectedString,
            data,
            MultiType.serializer()
        )
    }

    @Test
    fun testUnitIsEmptyString() {
        assertEquals("", StringProperties.encodeToString(Unit.serializer(), Unit))
    }

    @Test
    fun testList() {
        val data = Data(listOf("element1"), "property")
        val expectedString = """
            list.0=element1
            property=property

            """.trimIndent()
        assertMappedAndRestored(
            expectedString,
            data,
            Data.serializer()
        )
    }

    @Test
    fun testNestedStructure() {
        val recursive = Recursive(
            Data(
                listOf("l1", "l2"),
                "property"
            ), "string"
        )
        val expectedString = """
            data.list.0=l1
            data.list.1=l2
            data.property=property
            property=string

            """.trimIndent()
        assertMappedAndRestored(expectedString, recursive, Recursive.serializer())
    }

    @Test
    fun testCustomFormat() {
        val recursive = Recursive(
            Data(
                listOf("l1", "l2"),
                "property"
            ), "string"
        )
        val expectedString = listOf(
            "data.list.0    :  l1",
            "data.list.1    :  l2",
            "data.property    :  property",
            "property    :  string",
            ""
        ).joinToString("\r\n")
        assertMappedAndRestoredCustomFormat(expectedString, recursive, Recursive.serializer())
    }

    @Test
    fun testNullableProperties() {
        val data = NullableData("property", null, null)
        val expectedString = """
            property=property

            """.trimIndent()
        assertMappedNullableAndRestored(expectedString, data, NullableData.serializer())
    }

    @Test
    fun testNestedNull() {
        val category = Category(name = "Name")
        val expectedString = """
            name=Name

            """.trimIndent()
        assertMappedNullableAndRestored(expectedString, category, Category.serializer())
    }

    @Test
    fun testLoadOptionalProps() {
        val string = """
            name=Name

        """.trimIndent()
        val restored = StringProperties.decodeFromString(Category.serializer(), string)
        assertEquals(Category("Name"), restored)
    }

    @Test
    fun testLoadOptionalNestedProps() {
        val string = """
            name=Name
            subCategory.name=SubName

        """.trimIndent()
        val restored = StringProperties.decodeFromString<Category>(Category.serializer(), string)
        assertEquals(Category("Name", SubCategory("SubName")), restored)
    }

    @Test
    fun testOmitsNullAndCanLoadBack() {
        val category = Category(name = "Name")
        val expectedString = """
            name=Name

        """.trimIndent()
        assertMappedAndRestored(expectedString, category, Category.serializer())
    }

    @Test
    fun testThrowsOnIncorrectMaps() {
        val string = """
            name=Name

        """.trimIndent()
        assertFailsWith<SerializationException> {
            StringProperties.decodeFromString(Data.serializer(), string)
        }
    }

    @Test
    fun testNestedMap() {
        val map0 = DataWithMap(mapOf())
        val string0 = ""

        val map1 = DataWithMap(mapOf("one" to 1))
        val string1 = """
            map.0=one
            map.1=1

            """.trimIndent()

        val map2 = DataWithMap(mapOf("one" to 1, "two" to 2))
        val string2 = """
            map.0=one
            map.1=1
            map.2=two
            map.3=2

            """.trimIndent()

        assertMappedAndRestored(string0, map0, DataWithMap.serializer())
        assertMappedAndRestored(string1, map1, DataWithMap.serializer())
        assertMappedAndRestored(string2, map2, DataWithMap.serializer())
    }

    @Test
    fun testEnumString() {
        val string = """
            data=ZERO
        """.trimIndent()
        val loaded = StringProperties.decodeFromString(EnumData.serializer(), string)
        assertEquals(EnumData(TestEnum.ZERO), loaded)
    }

    @Test
    fun testEnumInteger() {
        val string = """
            data=ZERO
        """.trimIndent()
        val loaded = StringProperties.decodeFromString(EnumData.serializer(), string)
        assertEquals(EnumData(TestEnum.ZERO), loaded)
    }

    @Test
    fun testCanReadSizeProperty() {
        val string = """
            p=a
            size=b

        """.trimIndent()
        assertMappedAndRestored(string, TestWithSize("a", "b"), TestWithSize.serializer())
    }

    @Test
    fun testSharedPrefixNames() {
        val string = """
            firstSecond=42
        """.trimIndent()
        val restored = StringProperties.decodeFromString(SharedPrefixNames.serializer(), string)
        assertEquals(SharedPrefixNames("100", "42"), restored)
    }

    @Test
    fun testEnumElementNotFound() {
        val wrongElementName = "wrong"
        val expectedMessage =
            "Enum '${TestEnum.serializer().descriptor.serialName}' does not contain element with name '${wrongElementName}'"
        val string = """
            data=${wrongElementName}
        """.trimIndent()
        val exception = assertFailsWith(SerializationException::class) {
            StringProperties.decodeFromString(EnumData.serializer(), string)
        }
        assertEquals(expectedMessage, exception.message)
    }

    @Test
    fun testCommentsDecode() {
        val map1 = Data(listOf("l1", "l2#this is not a comment"), "property")
        val string1 = """
            # This a hashtag comment
            ! This a exclamation point comment
            list.0=l1
            list.1=l2#this is not a comment
            #list.2=l3
            property=property
            """.trimIndent()

        assertStringDecodesInto(string1, map1, Data.serializer())
    }

    @Test
    fun testKeyValueSeparatorDecode() {
        val data = Data(listOf("l1", "l2"), "property")
        val string = """
            list.0:l1
            list.1:l2
            property:property
            """.trimIndent()

        assertStringDecodesInto(string, data, Data.serializer())
    }

    @Test
    fun testLogicalLinesDecode() {
        val data = Data(
            listOf(
                "apple, banana, pear, cantaloupe, watermelon, kiwi, mango",
                "Detroit,Chicago,Los Angeles"
            ),
            "property"
        )
        val string = """
            property=property
            list.1=\
                Detroit,\
                Chicago,\
                Los Angeles
            list.0=apple, banana, pear, \
                                   cantaloupe, watermelon, \
                                   kiwi, mango
            """.trimIndent()

        assertStringDecodesInto(string, data, Data.serializer())
    }

    @Test
    fun testWitheSpacesDecode() {
        val data = Data(listOf("l1    ", "l2"), "property")
        val string = """
            list.0=     l1    
            list.1=     l2
                  property=property
            """.trimIndent()

        assertStringDecodesInto(string, data, Data.serializer())
    }

    @Test
    fun testBackslashDecode() {
        val data = Data(listOf("l1", "l2\n\r\t"), "property")
        val string = """
            list.0\=l1
            list.1=l\2\n\r\t
            \property=property
            """.trimIndent()

        assertStringDecodesInto(string, data, Data.serializer())
    }

}
