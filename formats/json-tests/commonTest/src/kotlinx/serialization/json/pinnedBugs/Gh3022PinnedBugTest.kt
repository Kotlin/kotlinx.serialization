/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.pinnedBugs

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.jvm.*
import kotlin.test.*

/**
 * PINNED BUG. Related issue: https://github.com/Kotlin/kotlinx.serialization/issues/3022
 * Pins the current behavior that surfaced in the specific issue.
 *
 * With classDiscriminatorMode = ClassDiscriminatorMode.ALL_JSON_OBJECTS, encoding a value class
 * that wraps a List injects a "type" discriminator entry into the JSON array's own element list,
 * producing malformed/unexpected output such as ["type":"Test",3,4,5] instead of a plain [3,4,5].
 */
@OptIn(ExperimentalSerializationApi::class)
class Gh3022PinnedBugTest {
    @Serializable
    @JvmInline
    value class Wrapper(val value: List<Int>)

    private val json = Json {
        classDiscriminatorMode = ClassDiscriminatorMode.ALL_JSON_OBJECTS
    }

    @Test
    fun valueClassWrappingListGetsClassDiscriminatorInjectedIntoTheArray() {
        val result = json.encodeToString(Wrapper(listOf(3, 4, 5)))

        assertEquals("""["type":"kotlinx.serialization.json.pinnedBugs.Gh3022PinnedBugTest.Wrapper",3,4,5]""", result)
    }
}
