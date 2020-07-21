/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlin.test.*

class JsonDefaultContextTest {

    @Test
    fun testRepeatedSerializer() {
        // #616
        val json = Json
        Json { serializersModule = json.serializersModule }
    }
}
