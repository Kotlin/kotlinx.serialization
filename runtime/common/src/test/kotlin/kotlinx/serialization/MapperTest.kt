package kotlinx.serialization

import kotlin.test.Test
import kotlin.test.assertEquals

class MapperTest {

    @Serializable
    data class Data(val list: List<String>, val property: String)

    @Serializable
    data class NullableData(val nullable: String?, val property: String)

    @Test
    fun testListTagStack() {
        val data = Data(listOf("element1"), "property")

        val map = Mapper.map(data)
        val unmap = Mapper.unmap<Data>(map)

        assertEquals(data.list, unmap.list)
        assertEquals(data.property, unmap.property)
    }

    @Test
    fun testNullableTagStack() {
        val data = NullableData(null, "property")

        val map = Mapper.mapNullable(data)
        val unmap = Mapper.unmapNullable<NullableData>(map)

        assertEquals(data.nullable, unmap.nullable)
        assertEquals(data.property, unmap.property)
    }
}
