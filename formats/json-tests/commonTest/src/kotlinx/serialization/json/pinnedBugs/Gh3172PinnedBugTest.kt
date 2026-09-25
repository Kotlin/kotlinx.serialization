/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.pinnedBugs

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.jvm.*
import kotlin.test.*

/**
 * PINNED BUG. Related issue: https://github.com/Kotlin/kotlinx.serialization/issues/3172
 * Pins the current behavior that surfaced in the specific issue.
 *
 * Sealed value-class child wrapping a Map encodes type discriminator inside the map,
 * which leads to invalid json, that cannot be decoded.
 */
class Gh3172PinnedBugTest {
    @Serializable
    sealed interface Parent {
        @JvmInline
        @Serializable
        @SerialName("child")
        value class Child(val value: Map<Int, String>) : Parent
    }

    @Test
    fun decodingSealedValueClassMapChildFailsOnClassDiscriminator() {
        val value: Parent = Parent.Child(mapOf(1 to "one", 2 to "two"))

        val encoded = Json.encodeToString(Parent.serializer(), value)
        assertEquals("""{"type":"child","1":"one","2":"two"}""", encoded)

        val e = assertFailsWith<JsonDecodingException> {
            Json.decodeFromString(Parent.serializer(), encoded)
        }
        assertEquals(
            "Unexpected JSON token at offset 2: Unexpected symbol 't' in numeric literal at path: \$\n" +
                "JSON input: {\"type\":\"child\",\"1\":\"one\",\"2\":\"two\"}",
            e.message
        )
    }
}
