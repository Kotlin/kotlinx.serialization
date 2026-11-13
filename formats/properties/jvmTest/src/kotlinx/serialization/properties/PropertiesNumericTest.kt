/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.properties

import kotlinx.serialization.*
import kotlin.test.*

class PropertiesJvmNumericTest {

    private fun assertRejected(property: String, value: Any) {
        assertFailsWith<SerializationException>("Expected ${value::class.simpleName} to be rejected for $property") {
            Properties.decodeFromMap(IntegralHolder.serializer(), mapOf(property to value))
        }
    }

    @Test
    fun testRejectsMismatchedIntegralRuntimeTypes() {
        assertRejected("byte", 1.toShort())
        assertRejected("byte", 1.0)
        assertRejected("short", 1)
        assertRejected("short", 1.0)
        assertRejected("int", 1L)
        assertRejected("int", 1.0)
        assertRejected("long", 1)
        assertRejected("long", 1.0)
    }
}
