/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonUnionEnumTest : JsonTestBase() {

    enum class SomeEnum { ALPHA, BETA, GAMMA }

    @Serializable
    data class WithUnions(@SerialId(5) val s: String,
                          @SerialId(6) val e: SomeEnum = SomeEnum.ALPHA,
                          @SerialId(7) val i: Int = 42)

    @Test
    fun testEnum() = parametrizedTest { useStreaming ->
        val data = WithUnions("foo", SomeEnum.BETA)
        val json = strict.stringify(WithUnions.serializer(), data, useStreaming)
        val restored = strict.parse(WithUnions.serializer(), json, useStreaming)
        assertEquals(data, restored)
    }
}
