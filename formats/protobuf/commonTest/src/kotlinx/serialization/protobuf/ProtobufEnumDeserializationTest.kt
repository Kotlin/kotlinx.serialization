package kotlinx.serialization.protobuf

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.encodeToByteArray
import kotlin.test.Test
import kotlin.test.assertFailsWith

@Serializable
private enum class Fruit {
  @ProtoNumber(1)
  APPLE,
  @ProtoNumber(2)
  BANANA
}

@Serializable
private class Meal(
  @ProtoNumber(1)
  val fruit: Fruit,
)

class ProtobufEnumDeserializationTest {

  @Test
  fun deserializingUnknownEnumThrowsSerializationException() {
    val meal = Meal(Fruit.APPLE)
    val bytes = ProtoBuf.encodeToByteArray(meal)

    // Change the bytes to include an unrecognized value
    bytes[1] = 3 // Newer software allows ORANGE

    assertFailsWith<SerializationException> { ProtoBuf.decodeFromByteArray<Meal>(bytes) }
  }
}