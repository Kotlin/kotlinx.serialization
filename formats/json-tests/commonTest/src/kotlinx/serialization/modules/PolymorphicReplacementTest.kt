/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.modules

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlinx.serialization.test.*
import kotlin.test.*

class PolymorphicReplacementTest: JsonTestBase() {

    interface IApiError {
        val code: Int
    }

    data object NotFound: IApiError {
        override val code: Int
            get() = 404
    }

    data object InternalError: IApiError {
        override val code: Int
            get() = 500
    }

    object MyApiErrorSerializer : KSerializer<IApiError> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("IApiError", PrimitiveKind.INT)

        override fun serialize(encoder: Encoder, value: IApiError) {
            encoder.encodeInt(value.code)
        }

        override fun deserialize(decoder: Decoder): IApiError {
            val code = decoder.decodeInt()
            return object : IApiError {
                override val code: Int = code
            }
        }
    }

    @Serializable
    data class Outer(
        val error: IApiError
    )

    @Test
    fun foobar() {
        if (isNative() || isWasm()) return // no .isInterface
        val module = SerializersModule {
            polymorphicReplacement(IApiError::class, MyApiErrorSerializer)
        }
        val j = Json { serializersModule = module }

        val ls = listOf(NotFound, InternalError)
        val o = Outer(InternalError)

        println(j.encodeToString(ls))
        println(j.encodeToString(o))
    }
}
