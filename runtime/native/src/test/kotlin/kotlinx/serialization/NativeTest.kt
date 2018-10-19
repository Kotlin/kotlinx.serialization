package kotlinx.serialization

import kotlinx.serialization.cbor.CBOR
import kotlinx.serialization.json.JSON
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.test.*

class CommonTest {
    @Test
    fun canSerialize() {
        val serializer = Shop.serializer()
        val jsonShop = JSON.stringify(serializer, shop)
        assertTrue(jsonShop.isNotBlank())
    }

    @Test
    fun basicJson() {
        val serializer = SimpleData.serializer()
        val data = SimpleData("foo", 42)
        val json = JSON.stringify(serializer, data)
        assertEquals("""{"foo":"foo","bar":42}""", json)
    }

    @Test
    fun isomorphicCbor() {
        val zoo = shop
        val serial = Shop.serializer()
        val zoo2 = CBOR.load(serial, CBOR.dump(serial, zoo))
        assertTrue(zoo !== zoo2)
        assertEquals(zoo, zoo2)
    }

    @Test
    fun isomorphicProtobuf() {
        val country = russia
        val serial = CountryData.serializer()
        val country2 = ProtoBuf.load(serial, ProtoBuf.dump(serial, country))
        assertTrue(country !== country2)
        assertEquals(country, country2)
    }
}
