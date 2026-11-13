/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.pinnedBugs

import kotlinx.serialization.json.*
import kotlin.test.*

/**
 * PINNED BUG. Related issue: https://github.com/Kotlin/kotlinx.serialization/issues/2971
 * Pins the current behavior that surfaced in the specific issue.
 *
 * decodeFromString can decode a signed numeric value containing an exponent as Int, but decoding
 * the same numeric-with-exponent literal as UInt fails with a JsonDecodingException.
 */
class Gh2971PinnedBugTest {

    @Test
    fun test() {
        val e = assertFailsWith<JsonDecodingException> {
            Json.decodeFromString<UInt>("1E2")
        }
        assertEquals(
            "Unexpected JSON token at offset 3: Failed to parse type 'UInt' for input '1E2' at path: \$\n" +
                "JSON input: 1E2",
            e.message
        )
    }
}
