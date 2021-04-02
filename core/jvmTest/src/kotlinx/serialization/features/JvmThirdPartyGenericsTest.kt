package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlin.test.*

class JvmThirdPartyGenericsTest : ThirdPartyGenericsTest() {
    @Test
    fun testSurrogateSerializerFoundForGenericWithJavaType() {
        val filledBox = ThirdPartyBox(contents = Item("Foo"))
        val serializer = serializersModule.serializer(filledBox::class.java)
        assertEquals(boxWithItemSerializer.descriptor, serializer.descriptor)
    }
}