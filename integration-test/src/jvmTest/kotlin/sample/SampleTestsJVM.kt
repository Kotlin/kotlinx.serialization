/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package sample

import kotlinx.serialization.IntSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SampleTestsJVM {
    @Test
    fun testHello() {
        assertTrue("JVM" in hello())
    }

    @Test
    fun kindSimpleName() {
        val kind = IntSerializer.descriptor.kind
        val name = kind.toString()
        assertEquals("INT", name)
    }

    @Test
    fun kotlinReflectNotInClasspath() {
        val klass = Json::class
        assertFailsWith<KotlinReflectionNotSupportedError> {
            println(klass.qualifiedName)
        }
    }
}
