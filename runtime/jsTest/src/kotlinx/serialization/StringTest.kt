/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.internal.InternalHexConverter
import kotlin.test.Test
import kotlin.test.assertEquals

class StringTest {
    @Test
    fun testCreateString() {
        val charArr = charArrayOf('a', 'b', 'c', 'd')
        val content = String(charArr, 0, 2)
        assertEquals("ab", content)
    }
}
