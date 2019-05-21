/*
 * Copyright 2017-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.test.Test
import kotlin.test.assertEquals

class ProtobufUnionEnumTest {

    enum class SomeEnum { ALPHA, BETA, GAMMA }

    @Serializable
    data class WithUnions(@SerialId(5) val s: String,
                          @SerialId(6) val e: SomeEnum = SomeEnum.ALPHA,
                          @SerialId(7) val i: Int = 42)

    @Test
    fun testEnum() {
        val data = WithUnions("foo", SomeEnum.BETA)
        val hex = ProtoBuf.dumps(WithUnions.serializer(), data)
        val restored = ProtoBuf.loads<WithUnions>(WithUnions.serializer(), hex)
        assertEquals(data, restored)
    }
}
