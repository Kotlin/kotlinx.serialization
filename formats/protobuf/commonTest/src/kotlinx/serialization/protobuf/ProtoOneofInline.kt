/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import kotlinx.serialization.*
import kotlin.jvm.*
import kotlin.test.*

class ProtoInline {

    @Serializable
    data class OneOfDataNullable(
        @ProtoOneOf val i: ITypeWithInlineClass?,
        @ProtoNumber(3) val name: String
    )

    @Serializable
    sealed interface ITypeWithInlineClass

    @Serializable
    @JvmInline
    value class StringInlineType(@ProtoNumber(12) val s: String) : ITypeWithInlineClass

    @Test
    fun testOneOfStringTypeNullable() {
        val dataString = OneOfDataNullable(
            StringInlineType("bar"),
            "foo")
        ProtoBuf.encodeToHexString(OneOfDataNullable.serializer(), dataString).also {
            /**
             * 12: {"bar"}
             * 3: {"foo"}
             */
            assertEquals("62036261721a03666f6f", it)
        }
        ProtoBuf.decodeFromHexString<OneOfDataNullable>("62036261721a03666f6f").also {
            assertEquals(dataString, it)
        }
        val dataStringNull = OneOfDataNullable(null, "foo")
        ProtoBuf.encodeToHexString(OneOfDataNullable.serializer(), dataStringNull).also {
            /**
             * 3: {"foo"}
             */
            assertEquals("1a03666f6f", it)
        }
    }
}