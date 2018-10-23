package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.internal.IntSerializer
import kotlin.reflect.KClass
import kotlin.test.*

@Serializable
data class DataHolder(val data: String)

class ReifiedCollectionsTest {
    @Test
    fun listTest() {
        val data = listOf(DataHolder("data"), DataHolder("not data"))
        val json = JSON.stringify(data)
        val data2 = JSON.parseList<DataHolder>(json)
        assertEquals(data, data2)
    }

    @Test
    fun mapTest() {
        val data = mapOf("data" to DataHolder("data"), "smth" to DataHolder("not data"))
        val json = JSON.stringify(data)
        val data2 = JSON.parseMap<String, DataHolder>(json)
        assertEquals(data, data2)
    }

    @Test
    fun primitiveSerializerTest() {
        val intClass = Int::class
        val serial = intClass.serializer()
        assertSame(IntSerializer, serial)
    }
}
