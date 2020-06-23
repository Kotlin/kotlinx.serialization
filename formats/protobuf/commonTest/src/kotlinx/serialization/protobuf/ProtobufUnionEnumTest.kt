/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlin.test.*

class ProtobufUnionEnumTest {

    enum class SomeEnum { ALPHA, BETA, GAMMA }

    @Serializable
    data class WithUnions(@ProtoId(5) val s: String,
                          @ProtoId(6) val e: SomeEnum = SomeEnum.ALPHA,
                          @ProtoId(7) val i: Int = 42)

    @Test
    fun testEnum() {
        val data = WithUnions("foo", SomeEnum.BETA)
        val hex = ProtoBuf.encodeToHexString(WithUnions.serializer(), data)
        val restored = ProtoBuf.decodeFromHexString(WithUnions.serializer(), hex)
        assertEquals(data, restored)
    }
}
