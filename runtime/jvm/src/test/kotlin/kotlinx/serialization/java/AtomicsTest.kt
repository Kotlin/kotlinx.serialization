/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:ContextualSerialization(AtomicInteger::class, AtomicLong::class, AtomicBoolean::class)

package kotlinx.serialization.java

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.junit.*
import org.junit.Test
import java.util.concurrent.atomic.*
import kotlin.test.*

class AtomicsTest : JsonTestBase() {

    @Serializable
    private data class Atomics(val i: AtomicInteger, val l: AtomicLong, val b: AtomicBoolean) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Atomics

            if (i.get() != other.i.get()) return false
            if (l.get() != other.l.get()) return false
            if (b.get() != other.b.get()) return false

            return true
        }

        override fun hashCode(): Int {
            var result = i.get().hashCode()
            result = 31 * result + l.get().hashCode()
            result = 31 * result + b.get().hashCode()
            return result
        }
    }

    @Before
    fun setUp() {
        unquoted.install(JavaTypesModule)
    }

    @Test
    fun testSerializer() = parametrizedTest { useStreaming ->
        val atomics = Atomics(AtomicInteger(1), AtomicLong(2), AtomicBoolean(true))
        val serialized = unquoted.stringify(Atomics.serializer(), atomics, useStreaming)
        assertEquals("{i:1,l:2,b:true}", serialized)
        assertEquals(atomics, unquoted.parse(serialized, useStreaming))
    }
}
