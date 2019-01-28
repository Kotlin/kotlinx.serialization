/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:ContextualSerialization(Date::class)

package kotlinx.serialization.java

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.junit.*
import org.junit.Test
import java.util.*
import kotlin.test.*

class DateTest : JsonTestBase() {

    @Before
    fun setUp() {
        unquoted.install(JavaTypesModule)
    }

    @Serializable
    private data class Box(val date: Date)

    @Test
    fun testDate() = parametrizedTest { useStreaming ->
        val box = Box(Date(1548687745000L))
        val serialized = unquoted.stringify(box, useStreaming)
        assertEquals(box, unquoted.parse(serialized, useStreaming))
    }
}
