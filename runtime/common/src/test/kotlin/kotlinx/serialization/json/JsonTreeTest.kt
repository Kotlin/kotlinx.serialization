package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlin.test.*

class JsonTreeTest {
    @Serializable
    data class Data(val a: Int)

    @Serializable
    data class DataWrapper(val s: String, val d: Data?)

    @Serializable
    data class DataWrapperOptional(val s: String, @Optional val d: Data? = null)

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

    private fun prepare(s: String): JsonElement = JsonTreeParser(s).readFully()

    @Test
    fun dynamicSimpleTest() {
        val dyn = prepare("{a: 42}")
        val parsed = JsonTreeMapper().readTree(dyn, Data.serializer())
        assertEquals(Data(42), parsed)
    }

    @Test
    fun dynamicNestedTest() {
        val dyn = prepare("""{s:"foo", d:{a:42}}""")
        val parsed = JsonTreeMapper().readTree<DataWrapper>(dyn)
        val expected = DataWrapper("foo", Data(42))
        assertEquals(expected, parsed)
        assertEquals(3, parsed.s.length)
    }

    @Test
    fun dynamicAllTypesTest() {
        val dyn = prepare("""{ b: 1, s: 2, i: 3, f: 1.0, d: 42.0, c: "a", B: true, S: "str"}""")
        val kotlinObj = AllTypes(1, 2, 3, 1.0f, 42.0, 'a', true, "str")

        assertEquals(kotlinObj, JsonTreeMapper().readTree(dyn))
    }

    @Test
    fun dynamicNullableTest() {
        val dyn1 = prepare("""{s:"foo", d: null}""")
        val dyn2 = prepare("""{s:"foo"}""")

        assertEquals(DataWrapper("foo", null), JsonTreeMapper().readTree<DataWrapper>(dyn1))
        assertFailsWith(MissingFieldException::class) { JsonTreeMapper().readTree<DataWrapper>(dyn2) }
    }

    @Test
    fun dynamicOptionalTest() {
        val dyn1 = prepare("""{s:"foo", d: null}""")
        val dyn2 = prepare("""{s:"foo"}""")

        assertEquals(DataWrapperOptional("foo", null), JsonTreeMapper().readTree<DataWrapperOptional>(dyn1))
        assertEquals(DataWrapperOptional("foo", null), JsonTreeMapper().readTree<DataWrapperOptional>(dyn2))
    }

    @Test
    fun dynamicListTest() {
        val dyn1 = prepare("""{l:[1,2]}""")
        val dyn15 = prepare("""{l:[{a:42},{a:43}]}""")
        val dyn2 = prepare("""{l:[[],[{a:42}]]}""")

        assertEquals(IntList(listOf(1, 2)), JsonTreeMapper().readTree<IntList>(dyn1))
        assertEquals(DataList(listOf(Data(42), Data(43))), JsonTreeMapper().readTree<DataList>(dyn15))
        assertEquals(ListOfLists(listOf(listOf(), listOf(Data(42)))), JsonTreeMapper().readTree<ListOfLists>(dyn2))
    }

    @Test
    fun dynamicMapTest() {
        val dyn = prepare("{m : {\"a\": 1, \"b\" : 2}}")
        val m = MapWrapper(mapOf("a" to 1, "b" to 2))
        assertEquals(m, JsonTreeMapper().readTree<MapWrapper>(dyn))
    }

    @Test
    fun dynamicMapComplexTest() {
        val dyn = prepare("{m : {1: {a: 42}, 2: {a: 43}}}")
        val m = ComplexMapWrapper(mapOf("1" to Data(42), "2" to Data(43)))
        assertEquals(m, JsonTreeMapper().readTree<ComplexMapWrapper>(dyn))
    }
}
