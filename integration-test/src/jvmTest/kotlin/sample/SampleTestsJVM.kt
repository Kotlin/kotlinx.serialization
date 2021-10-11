/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package sample

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlin.test.*

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
}
