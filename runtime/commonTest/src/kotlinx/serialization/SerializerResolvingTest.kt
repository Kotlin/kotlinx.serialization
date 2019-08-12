/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.internal.IntSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.test.isJs
import kotlinx.serialization.test.isNative
import kotlin.reflect.typeOf
import kotlin.test.*

@UseExperimental(ImplicitReflectionSerializer::class)
class SerializerResolvingTest {

    private inline fun run(block: () -> Unit) {
        if (isJs()) return // JS does not support typeof() yet

        block()
    }

    private inline fun <reified T> assertSerializedWithType(
        expected: String,
        obj: T,
        json: StringFormat = Json.unquoted
    ) {
        val serial = serializer<T>()
        assertEquals(expected, json.stringify(serial, obj))
    }

    @Test
    fun testListWithDtoResolving() = run {
        val source = """[{"intV":42}]"""
        val serial = serializer<List<IntData>>()
        assertEquals(listOf(IntData(42)), Json.parse(serial, source))
    }

    @Test
    fun testListResolving() = run {
        val myArr = listOf("a", "b", "c")
        assertSerializedWithType("[a,b,c]", myArr)
    }

    @Test
    fun testSetResolving() = run {
        val mySet = setOf("a", "b", "c", "c")
        assertSerializedWithType("[a,b,c]", mySet)
    }

    @Test
    fun testMapResolving() = run {
        val myMap = mapOf("string" to StringData("foo"), "string2" to StringData("bar"))
        assertSerializedWithType("""{string:{data:foo},string2:{data:bar}}""", myMap)
    }

    @Test
    fun testNestedListResolving() = run {
        val myList = listOf(listOf(listOf(1, 2, 3)), listOf())
        assertSerializedWithType("[[[1,2,3]],[]]", myList)
    }

    @Test
    fun equalityOfListAndArrayList() = run {
        val myList = arrayListOf(1, 2, 3)
        assertSerializedWithType<ArrayList<Int>>("[1,2,3]", myList)
        assertSerializedWithType<List<Int>>("[1,2,3]", myList)
    }

    @Test
    fun equalityOfProjectedLists() = run {
        val myList = arrayListOf(1, 2, 3)
        assertSerializedWithType<List<Int>>("[1,2,3]", myList)
        assertSerializedWithType<MutableList<out Int>>("[1,2,3]", myList)
        assertSerializedWithType<ArrayList<in Int>>("[1,2,3]", myList)
    }

    @Test
    fun supportNullableTypes() = run {
        val myList: List<Int?> = listOf(1, null, 3)
        assertSerializedWithType("[1,null,3]", myList)
        assertSerializedWithType<List<Int?>?>("[1,null,3]", myList)
    }

    @Test
    fun objectTest() = run {
        val b = StringData("some string")
        assertSerializedWithType("""{data:"some string"}""", b)
    }

    @Test
    fun intResolve() = run {
        val token = typeOf<Int>()
        val serial = serializer(token)
        assertSame(IntSerializer as KSerializer<*>, serial)
        assertSerializedWithType("42", 42)
    }
}
