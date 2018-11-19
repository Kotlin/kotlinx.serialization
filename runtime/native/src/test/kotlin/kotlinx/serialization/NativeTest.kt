package kotlinx.serialization

import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.test.*

class CommonTest {
    @Test
    fun canSerialize() {
        val serializer = Shop.serializer()
        val jsonShop = Json.stringify(serializer, shop)
        assertTrue(jsonShop.isNotBlank())
    }

    @Test
    fun basicJson() {
        val serializer = SimpleData.serializer()
        val data = SimpleData("foo", 42)
        val json = Json.stringify(serializer, data)
        assertEquals("""{"foo":"foo","bar":42}""", json)
    }

    @Test
    fun isomorphicCbor() {
        val zoo = shop
        val serial = Shop.serializer()
        val zoo2 = Cbor.load(serial, Cbor.dump(serial, zoo))
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
