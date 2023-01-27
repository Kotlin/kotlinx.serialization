/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package sample

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.json.Json
import kotlin.test.*
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

class SampleTestsJVM {
    @Test
    fun testHello() {
        assertTrue("JVM" in hello())
    }

    @Test
    @OptIn(ExperimentalSerializationApi::class)
    fun kindSimpleName() {
        val kind = Int.serializer().descriptor.kind
        val name = kind.toString()
        assertEquals("INT", name)
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun testSerializersAreIntrinsified() {
        val direct = measureTime {
            Json.encodeToString(IntData.serializer(), IntData(10))
        }
        val directMs = direct.inWholeMicroseconds
        val indirect = measureTime {
            Json.encodeToString(IntData(10))
        }
        val indirectMs = indirect.inWholeMicroseconds
        if (indirectMs > directMs + (directMs / 4)) error("Direct ($directMs) and indirect ($indirectMs) times are too far apart")
    }

}
