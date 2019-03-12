@file:UseSerializers(IntHolderSerializer::class, MultiplyingIntSerializer::class)

package kotlinx.serialization.features

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

@Serializable
data class Carrier2(
    val a: IntHolder,
    val i: Int
)

class UseSerializersTest {
    @Test
    fun testOnFile() {
        val str = Json.stringify(Carrier.serializer(), Carrier(IntHolder(42), 2))
        assertEquals("""{"a":{"data":42},"i":4}""", str)
    }
}
