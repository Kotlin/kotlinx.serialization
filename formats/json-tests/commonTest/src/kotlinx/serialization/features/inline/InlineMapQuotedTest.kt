/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features.inline

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlinx.serialization.test.*
import kotlin.jvm.*
import kotlin.test.*

class InlineMapQuotedTest : JsonTestBase() {
    @Serializable(with = CustomULong.Serializer::class)
    data class CustomULong(val value: ULong) {
        @OptIn(ExperimentalSerializationApi::class, ExperimentalUnsignedTypes::class)
        internal object Serializer : KSerializer<CustomULong> {
            override val descriptor: SerialDescriptor =
                @OptIn(ExperimentalUnsignedTypes::class) ULong.serializer().descriptor

            override fun deserialize(decoder: Decoder): CustomULong =
                CustomULong(decoder.decodeInline(descriptor).decodeSerializableValue(ULong.serializer()))

            override fun serialize(encoder: Encoder, value: CustomULong) {
                encoder.encodeInline(descriptor).encodeSerializableValue(ULong.serializer(), value.value)
            }
        }
    }

    @JvmInline
    @Serializable
    value class WrappedLong(val value: Long)

    @JvmInline
    @Serializable
    value class WrappedULong(val value: ULong)

    @Serializable
    data class Carrier(
        val mapLong: Map<Long, Long>,
        val mapULong: Map<ULong, Long>,
        val wrappedLong: Map<WrappedLong, Long>,
        val mapWrappedU: Map<WrappedULong, Long>,
        val mapCustom: Map<CustomULong, Long>
    )

    @Test
    fun testInlineClassAsMapKey() {
        println(Long.MAX_VALUE.toULong() + 2UL)
        val c = Carrier(
            mapOf(1L to 1L),
            mapOf(Long.MAX_VALUE.toULong() + 2UL to 2L),
            mapOf(WrappedLong(3L) to 3L),
            mapOf(WrappedULong(Long.MAX_VALUE.toULong() + 4UL) to 4L),
            mapOf(CustomULong(Long.MAX_VALUE.toULong() + 5UL) to 5L)
        )
        assertJsonFormAndRestored(
            serializer<Carrier>(),
            c,
            """{"mapLong":{"1":1},"mapULong":{"9223372036854775809":2},"wrappedLong":{"3":3},"mapWrappedU":{"9223372036854775811":4},"mapCustom":{"9223372036854775812":5}}"""
        )
    }
}
