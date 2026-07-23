/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.properties

import kotlinx.serialization.*
import kotlin.test.*

@Serializable
internal data class IntegralHolder(
    val byte: Byte = 0,
    val short: Short = 0,
    val int: Int = 0,
    val long: Long = 0
)

class PropertiesNumericTest {

    @Test
    fun testRoundTripsIntegralValues() {
        val expected = IntegralHolder(Byte.MIN_VALUE, Short.MAX_VALUE, Int.MIN_VALUE, Long.MAX_VALUE)
        val encoded = Properties.encodeToMap(IntegralHolder.serializer(), expected)
        assertEquals(expected, Properties.decodeFromMap(IntegralHolder.serializer(), encoded))
    }
}
