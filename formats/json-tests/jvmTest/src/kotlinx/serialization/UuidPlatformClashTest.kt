/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.test.*
import kotlin.uuid.*
import java.util.UUID as JUuid
import kotlin.uuid.Uuid as KUuid

@OptIn(ExperimentalUuidApi::class)
class UuidPlatformClashTest : JsonTestBase() {
    object JavaUuidSerializer : KSerializer<JUuid> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Uuid", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: JUuid) {
            encoder.encodeString(value.toString().uppercase())
        }

        override fun deserialize(decoder: Decoder): JUuid {
            return JUuid.fromString(decoder.decodeString())
        }
    }


    @Serializable
    data class UuidPair(
        @Contextual val jUuid: JUuid,
        @Contextual val kUuid: KUuid,
    )

    @Test
    fun testUuids() {
        val module = SerializersModule {
            contextual(JavaUuidSerializer)
            contextual(KUuid.serializer())
        }
        val json = Json { serializersModule = module }
        val pair = UuidPair(
            JUuid.fromString("252660b8-9a2b-44d1-a804-9a23f881cec5"),
            KUuid.parse("c8ad1526-4b7c-4b67-9f77-6d05e580ad71")
        )
        assertJsonFormAndRestored(
            UuidPair.serializer(),
            pair,
            """{"jUuid":"252660B8-9A2B-44D1-A804-9A23F881CEC5","kUuid":"c8ad1526-4b7c-4b67-9f77-6d05e580ad71"}""",
            json
        )
    }
}
