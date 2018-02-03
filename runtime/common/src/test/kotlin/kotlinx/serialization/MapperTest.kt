package kotlinx.serialization

import kotlin.test.Test
import kotlin.test.assertEquals

class MapperTest {

    @Serializable
    data class Data(val list: List<String>, val property: String)

    @Serializable
    data class Recursive(val data: Data, val property: String)

    @Serializable
    data class NullableData(val nullable: String?, val nullable2: String?, val property: String)

    @Test
    fun testListTagStack() {
        val data = Data(listOf("element1"), "property")

        val map = Mapper.map(data)
        val unmap = Mapper.unmap<Data>(map)

        assertEquals(data.list, unmap.list)
        assertEquals(data.property, unmap.property)
    }

    @Test
    fun testRecursiveBlockTag() {
        val recursive = Recursive(Data(listOf("l1"), "property"), "string")

        val map = Mapper.map(recursive)
        val unmap = Mapper.unmap<Recursive>(map)

        assertEquals(recursive.data.list, unmap.data.list)
        assertEquals(recursive.data.property, unmap.data.property)
        assertEquals(recursive.property, unmap.property)
    }

    @Test
    fun testNullableTagStack() {
        val data = NullableData(null, null, "property")

        val map = Mapper.mapNullable(data)
        val unmap = Mapper.unmapNullable<NullableData>(map)

        assertEquals(data.nullable, unmap.nullable)
        assertEquals(data.nullable2, unmap.nullable2)
        assertEquals(data.property, unmap.property)
    }
}
