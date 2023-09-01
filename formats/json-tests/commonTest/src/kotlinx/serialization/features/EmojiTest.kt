/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonTestBase
import kotlin.test.Test


class EmojiTest : JsonTestBase() {

    @Test
    fun testEmojiString() {
        assertJsonFormAndRestored(
            String.serializer(),
            "\uD83C\uDF34",
            "\"\uD83C\uDF34\""
        )
    }
}
