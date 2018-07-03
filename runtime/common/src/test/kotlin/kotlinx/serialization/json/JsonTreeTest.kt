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
    fun readTreeSimple() {
        val tree = prepare("{a: 42}")
        val parsed = JsonTreeMapper().readTree(tree, Data.serializer())
        assertEquals(Data(42), parsed)
    }

    @Test
    fun readTreeNested() {
        val tree = prepare("""{s:"foo", d:{a:42}}""")
        val parsed = JsonTreeMapper().readTree<DataWrapper>(tree)
        val expected = DataWrapper("foo", Data(42))
        assertEquals(expected, parsed)
        assertEquals(3, parsed.s.length)
    }

    @Test
    fun readTreeAllTypes() {
        val tree = prepare("""{ b: 1, s: 2, i: 3, f: 1.0, d: 42.0, c: "a", B: true, S: "str"}""")
        val kotlinObj = AllTypes(1, 2, 3, 1.0f, 42.0, 'a', true, "str")

        assertEquals(kotlinObj, JsonTreeMapper().readTree(tree))
    }

    @Test
    fun readTreeNullable() {
        val tree1 = prepare("""{s:"foo", d: null}""")
        val tree2 = prepare("""{s:"foo"}""")

        assertEquals(DataWrapper("foo", null), JsonTreeMapper().readTree<DataWrapper>(tree1))
        assertFailsWith(MissingFieldException::class) { JsonTreeMapper().readTree<DataWrapper>(tree2) }
    }

    @Test
    fun readTreeOptional() {
        val tree1 = prepare("""{s:"foo", d: null}""")
        val tree2 = prepare("""{s:"foo"}""")

        assertEquals(DataWrapperOptional("foo", null), JsonTreeMapper().readTree<DataWrapperOptional>(tree1))
        assertEquals(DataWrapperOptional("foo", null), JsonTreeMapper().readTree<DataWrapperOptional>(tree2))
    }

    @Test
    fun readTreeList() {
        val tree1 = prepare("""{l:[1,2]}""")
        val tree2 = prepare("""{l:[{a:42},{a:43}]}""")
        val tree3 = prepare("""{l:[[],[{a:42}]]}""")

        assertEquals(IntList(listOf(1, 2)), JsonTreeMapper().readTree<IntList>(tree1))
        assertEquals(DataList(listOf(Data(42), Data(43))), JsonTreeMapper().readTree<DataList>(tree2))
        assertEquals(ListOfLists(listOf(listOf(), listOf(Data(42)))), JsonTreeMapper().readTree<ListOfLists>(tree3))
    }

    @Test
    fun readTreeMap() {
        val dyn = prepare("{m : {\"a\": 1, \"b\" : 2}}")
        val m = MapWrapper(mapOf("a" to 1, "b" to 2))
        assertEquals(m, JsonTreeMapper().readTree<MapWrapper>(dyn))
    }

    @Test
    fun readTreeComplexMap() {
        val dyn = prepare("{m : {1: {a: 42}, 2: {a: 43}}}")
        val m = ComplexMapWrapper(mapOf("1" to Data(42), "2" to Data(43)))
        assertEquals(m, JsonTreeMapper().readTree<ComplexMapWrapper>(dyn))
    }

    private inline fun <reified T: Any> writeAndTest(obj: T, printDiagnostics: Boolean = false): Pair<JsonElement, T> {
        val serial = T::class.serializer()
        val tree = JsonTreeMapper().writeTree(obj, serial)
        val str = tree.toString()
        if (printDiagnostics) println(str)
        val restored = JsonTreeMapper().readTree(JsonTreeParser(str).readFully(), serial)
        assertEquals(obj, restored)
        return tree to restored
    }

    @Test
    fun saveSimpleNestedTree() {
        writeAndTest(DataWrapper("foo", Data(42)))
    }

    @Test
    fun saveComplexMapTree() {
        writeAndTest(ComplexMapWrapper(mapOf("foo" to Data(42), "bar" to Data(43))))
    }

    @Test
    fun saveNestedLists() {
        writeAndTest(ListOfLists(listOf(listOf(), listOf(Data(1), Data(2)))))
    }

    @Test
    fun saveOptional() {
        writeAndTest(DataWrapperOptional("foo", null))
    }

    @Test
    fun saveAllTypes() {
        writeAndTest(AllTypes(1, -2, 100500, 0.0f, 2048.2, 'a', true, "foobar"))
    }
}
