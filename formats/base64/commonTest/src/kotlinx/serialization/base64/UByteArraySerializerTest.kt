package kotlinx.serialization.base64

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

@OptIn(ExperimentalUnsignedTypes::class)
class UByteArraySerializerTest {
  @Serializable
  class UE(
    @Serializable(with = UByteArrayAsBase64StringSerializer::class)
    val bytes: UByteArray
  )

  @Test
  fun serialize() {
    val element = Json.encodeToJsonElement(UE(ubyteArrayOf(0x31u, 0x32u, 0x33u))).jsonObject
    val base64 = element["bytes"]?.jsonPrimitive?.content!!

    assertEquals("MTIz", base64)
  }

  @Test
  fun deserialize() {
    val json = "{\"bytes\": \"MTIz\"}"
    val decoded = Json.decodeFromString<UE>(json).bytes

    assertContentEquals(ubyteArrayOf(0x31u, 0x32u, 0x33u), decoded)
  }
}
