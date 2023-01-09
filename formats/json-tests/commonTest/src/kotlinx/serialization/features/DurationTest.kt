/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonTestBase
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class DurationTest : JsonTestBase() {
    @Serializable
    data class DurationHolder(val duration: Duration)
    @Test
    fun testDuration() {
        assertJsonFormAndRestored(
            DurationHolder.serializer(),
            DurationHolder(1000.toDuration(DurationUnit.SECONDS)),
            """{"duration":"PT16M40S"}"""
        )
    }
}
