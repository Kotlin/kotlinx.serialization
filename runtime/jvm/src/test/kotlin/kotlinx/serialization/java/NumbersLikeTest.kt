/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:ContextualSerialization(BigDecimal::class, BigInteger::class)

package kotlinx.serialization.java

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.junit.*
import org.junit.Test
import java.math.*
import kotlin.test.*

class NumbersLikeTest : JsonTestBase() {

    @Serializable
    private data class Numbers(val bd: BigDecimal, val bi: BigInteger)

    @Before
    fun setUp() {
        unquoted.install(JavaTypesModule)
    }

    @Test
    fun testSerializer() = parametrizedTest { useStreaming ->
        val longNumber = "9".repeat(64)
        val numbers = Numbers(BigDecimal.valueOf(123412345678901L, 2),
            BigInteger(longNumber))
        val serialized = unquoted.stringify(Numbers.serializer(), numbers, useStreaming)
        assertEquals("{bd:1234123456789.01,bi:$longNumber}", serialized)
        assertEquals(numbers, unquoted.parse(serialized, useStreaming))
    }
}
