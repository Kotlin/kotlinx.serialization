package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.internal.*
import kotlinx.serialization.test.*
import kotlin.random.*
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
        val data = UnicodeKeys(
            "\uD83E\uDD14", "\" \uD83E\uDD14", "\uD83E\uDD14",
            "slow-path-in-\"-the-middle\""
        )
        assertEquals(data, Json.decodeFromString(Json.encodeToString(data)))
    }

    @Serializable
    data class Wrapper(val s: String)

    @Test
    fun testLongEscapeSequence() {
        assertSerializedAndRestored(Wrapper("\"".repeat(100)), Wrapper.serializer())
        // #1456
        assertSerializedAndRestored(
            Wrapper("{\"status\":123,\"message\":\"content\",\"path\":\"/here/beeeeeeeeeeee/dragoons/d63574f-705c-49dd-a6bc-c8d1e524eefd/\"}"),
            Wrapper.serializer()
        )
        // #1460
        assertSerializedAndRestored(
            """{"avatar_url":"https://cdn.discordapp.com/avatars/384333349063491584/8adca1bddf8c5c46c7deed3edbd80d60.png","embeds":[{"color":1741274,"author":{"icon_url":"https://pbs.twimg.com/profile_images/1381321181719109633/4bpPMaer_normal.jpg","name":"Merlijn replied:","url":"https://twitter.com/@PixelHamster/status/1390719238155952129"},"description":"[@shroomizu](https://twitter.com/shroomizu) time for a pro controller","type":"rich","timestamp":"2021-05-07T17:24:39Z"}],"username":"Merijn"}""",
            String.serializer()
        )
    }

    @Test
    fun testRandomEscapeSequences() {
        repeat(10_000) {
            val s = generateRandomString()
            try {
                assertSerializedAndRestored(s, String.serializer())
            } catch (e: Throwable) {
                // Not assertion error to preserve cause
                throw IllegalStateException("Unexpectedly failed test, cause string: $s", e)
            }
        }
    }

    private fun generateRandomString(): String {
        val size = Random.nextInt(1, 2047)
        return buildString(size) {
            repeat(size) {
                val pickEscape = Random.nextBoolean()
                if (pickEscape) {
                    // Definitely escape symbol
                    // null can be appended as well, completely okay
                    append(ESCAPE_STRINGS.random())
                } else {
                    // Any symbol, including escaping one
                    append(Char(Random.nextInt(Char.MIN_VALUE.code..Char.MAX_VALUE.code)))
                }
            }
        }
    }
}
