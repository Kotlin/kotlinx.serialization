/*
 * Copyright 2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlin.test.*
import kotlinx.serialization.builtins.*

class TuplesTest {
    @Test
    fun testSequentialDecodingKeyValue() {
        val decoder = DummySequentialDecoder()
        val serializer = MapEntrySerializer(Unit.serializer(), Unit.serializer())
        serializer.deserialize(decoder)
        assertEquals(decoder.beginStructureCalled, decoder.endStructureCalled)
    }
}
