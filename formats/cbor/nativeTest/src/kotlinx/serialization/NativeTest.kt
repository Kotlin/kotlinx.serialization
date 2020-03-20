/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.cbor.*
import kotlin.test.*

class CommonTest {
    @Test
    fun isomorphicCbor() {
        val zoo = shop
        val serial = Shop.serializer()
        val zoo2 = Cbor.load(serial, Cbor.dump(serial, zoo))
        assertTrue(zoo !== zoo2)
        assertEquals(zoo, zoo2)
    }
}
