/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EscapeMarkersTest {
    @Test
    fun isCodePointRequiringEscapeSequenceTest() {
        for (charCode in ESCAPE_MARKERS.indices) {
            if (ESCAPE_MARKERS[charCode] == 0.toByte()) {
                assertFalse(isCodePointRequiringEscapeSequence(charCode), "Unexpected escape marker for $charCode")
            } else {
                assertTrue(isCodePointRequiringEscapeSequence(charCode), "No escape marker for $charCode")
            }
        }
    }
}
