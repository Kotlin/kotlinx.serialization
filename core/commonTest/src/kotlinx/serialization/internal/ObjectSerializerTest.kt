/*
 * Copyright 2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlin.test.*
import kotlinx.serialization.*

class ObjectSerializerTest {
    @Test
    fun testSequentialDecoding() {
        SimpleObject.serializer().deserialize(DummySequentialDecoder())
    }

    @Serializable
    object SimpleObject
}
