/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlin.test.*

class JsonTreeTest : JsonTestBase() {
    @Serializable
    data class Data(val a: Int)

    @Serializable
    data class DataWrapper(val s: String, val d: Data?)

    @Serializable
    data class DataWrapperOptional(val s: String, val d: Data? = null)

    @Serializable
    data class IntList(val l: List<Int>)

    @Serializable
    data class DataList(val l: List<Data>)

    @Serializable
    data class ListOfLists(val l: List<List<Data>>)

    @Serializable
    data class MapWrapper(val m: Map<String, Int>)

    @Serializable
    data class ComplexMapWrapper(val m: Map<String, Data>)

    @Serializable
    data class AllTypes(
        val b: Byte,
        val s: Short,
        val i: Int,
        val f: Float,
        val d: Double,
        val c: Char,
        val B: Boolean,
        val S: String
    )

    private val json = Json
    private fun prepare(input: String): JsonElement = lenient.parseToJsonElement(input)

    @Test
    fun testReadTreeSimple() {
        val tree = prepare("{a: 42}")
        val parsed = lenient.decodeFromJsonElement(Data.serializer(), tree)
        assertEquals(Data(42), parsed)
    }

    @Test
    fun testReadTreeNested() {
        val tree = prepare("""{s:"foo", d:{a:42}}""")
        val parsed = lenient.decodeFromJsonElement(DataWrapper.serializer(), tree)
        val expected = DataWrapper("foo", Data(42))
        assertEquals(expected, parsed)
        assertEquals(3, parsed.s.length)
    }

    @Test
    fun testReadTreeAllTypes() {
        val tree = prepare("""{ b: 1, s: 2, i: 3, f: 1.0, d: 42.0, c: "a", B: true, S: "str"}""")
        val kotlinObj = AllTypes(1, 2, 3, 1.0f, 42.0, 'a', true, "str")

        assertEquals(kotlinObj, json.decodeFromJsonElement(AllTypes.serializer(), tree))
    }

    @Test
    fun testReadTreeNullable() {
        val tree1 = prepare("""{s:"foo", d: null}""")
        val tree2 = prepare("""{s:"foo"}""")

        assertEquals(DataWrapper("foo", null), lenient.decodeFromJsonElement(DataWrapper.serializer(), tree1))
        assertFailsWith(MissingFieldException::class) { lenient.decodeFromJsonElement(DataWrapper.serializer(), tree2) }
    }

    @Test
    fun testReadTreeOptional() {
        val tree1 = prepare("""{s:"foo", d: null}""")
        val tree2 = prepare("""{s:"foo"}""")

        assertEquals(DataWrapperOptional("foo", null), json.decodeFromJsonElement(DataWrapperOptional.serializer(), tree1))
        assertEquals(DataWrapperOptional("foo", null), json.decodeFromJsonElement(DataWrapperOptional.serializer(), tree2))
    }

    @Test
    fun testReadTreeList() {
        val tree1 = prepare("""{l:[1,2]}""")
        val tree2 = prepare("""{l:[{a:42},{a:43}]}""")
        val tree3 = prepare("""{l:[[],[{a:42}]]}""")

        assertEquals(IntList(listOf(1, 2)), lenient.decodeFromJsonElement(IntList.serializer(), tree1))
        assertEquals(DataList(listOf(Data(42), Data(43))), lenient.decodeFromJsonElement(DataList.serializer(), tree2))
        assertEquals(ListOfLists(listOf(listOf(), listOf(Data(42)))), json.decodeFromJsonElement(ListOfLists.serializer(), tree3))
    }

    @Test
    fun testReadTreeMap() {
        val dyn = prepare("{m : {\"a\": 1, \"b\" : 2}}")
        val m = MapWrapper(mapOf("a" to 1, "b" to 2))
        assertEquals(m, lenient.decodeFromJsonElement(MapWrapper.serializer(), dyn))
    }

    @Test
    fun testReadTreeComplexMap() {
        val dyn = prepare("{m : {1: {a: 42}, 2: {a: 43}}}")
        val m = ComplexMapWrapper(mapOf("1" to Data(42), "2" to Data(43)))
        assertEquals(m, lenient.decodeFromJsonElement(ComplexMapWrapper.serializer(), dyn))
    }

    private inline fun <reified T: Any> writeAndTest(obj: T, serial: KSerializer<T>, printDiagnostics: Boolean = false): Pair<JsonElement, T> {
        val tree = lenient.encodeToJsonElement(serial, obj)
        val str = tree.toString()
        if (printDiagnostics) println(str)
        val restored = lenient.decodeFromJsonElement(serial, lenient.parseToJsonElement(str))
        assertEquals(obj, restored)
        return tree to restored
    }

    @Test
    fun testSaveSimpleNestedTree() {
        writeAndTest(DataWrapper("foo", Data(42)), DataWrapper.serializer())
    }

    @Test
    fun testSaveComplexMapTree() {
        writeAndTest(ComplexMapWrapper(mapOf("foo" to Data(42), "bar" to Data(43))), ComplexMapWrapper.serializer())
    }

    @Test
    fun testSaveNestedLists() {
        writeAndTest(ListOfLists(listOf(listOf(), listOf(Data(1), Data(2)))), ListOfLists.serializer())
    }

    @Test
    fun testSaveOptional() {
        writeAndTest(DataWrapperOptional("foo", null), DataWrapperOptional.serializer())
    }

    @Test
    fun testSaveAllTypes() {
        writeAndTest(AllTypes(1, -2, 100500, 0.0f, 2048.2, 'a', true, "foobar"), AllTypes.serializer())
    }
}
