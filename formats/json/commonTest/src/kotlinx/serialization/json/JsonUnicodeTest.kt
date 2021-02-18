package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlin.test.*

class JsonUnicodeTest : JsonTestBase() {

    @Serializable
    data class UnicodeKeys(
        @SerialName("\uD83E\uDD14") val thinking: String,
        @SerialName("🤔?") val thinking2: String,
        @SerialName("\uD83E\uDD15") val bandage: String,
        @SerialName("\"") val escaped: String
    )

    @Test
    fun testUnicodeKeys() {
        val data = UnicodeKeys("1", "2", "3", "4")
        val s = """{"\uD83E\uDD14":"1","\uD83E\uDD14?":"2","\uD83E\uDD15":"3","\"":"4"}"""
        assertEquals(data, Json.decodeFromString(s))
    }

    @Test
    fun testUnicodeValues() {
        val data = UnicodeKeys("\uD83E\uDD14", "\" \uD83E\uDD14", "\uD83E\uDD14",
            "slow-path-in-\"-the-middle\"")
        assertEquals(data, Json.decodeFromString(Json.encodeToString(data)))
    }
}
