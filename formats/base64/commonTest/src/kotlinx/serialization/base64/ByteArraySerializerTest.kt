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

class ByteArraySerializerTest {
  @Serializable
  class E(
    @Serializable(with = ByteArrayAsBase64StringSerializer::class)
    val bytes: ByteArray
  )

  @Test
  fun serialize() {
    val element = Json.encodeToJsonElement(E(byteArrayOf(0x31, 0x32, 0x33))).jsonObject
    val base64 = element["bytes"]?.jsonPrimitive?.content!!

    assertEquals("MTIz", base64)
  }

  @Test
  fun deserialize() {
    val json = "{\"bytes\": \"MTIz\"}"
    val decoded = Json.decodeFromString<E>(json).bytes

    assertContentEquals(byteArrayOf(0x31, 0x32, 0x33), decoded)
  }
}
