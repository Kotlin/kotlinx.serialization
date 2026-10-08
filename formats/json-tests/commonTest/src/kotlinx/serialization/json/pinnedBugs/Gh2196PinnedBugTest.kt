/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:UseSerializers(Gh2196PinnedBugTest.FirstSerializer::class)

package kotlinx.serialization.json.pinnedBugs

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*
import kotlin.test.*

/**
 * PINNED BUG. Related issue: https://github.com/Kotlin/kotlinx.serialization/issues/2196
 * Pins the current behavior that surfaced in the specific issue.
 *
 * @Contextual for generic type argument loses priority to a file-level
 * @UseSerializers serializer.
 * SecondSerializer from serializersModule is ignored, and FirstSerializer
 * from @UseSerializers is used instead.
 */
class Gh2196PinnedBugTest {
    class Data

    @Serializable
    class Holder(val b: List<@Contextual Data>)

    object FirstSerializer : KSerializer<Data> {
        override val descriptor: SerialDescriptor
            get() = TODO("descriptor1")

        override fun serialize(encoder: Encoder, value: Data) {
            TODO("serialize1")
        }

        override fun deserialize(decoder: Decoder): Data {
            TODO("deserialize1")
        }
    }

    object SecondSerializer : KSerializer<Data> {
        override val descriptor: SerialDescriptor
            get() = TODO("descriptor2")

        override fun serialize(encoder: Encoder, value: Data) {
            TODO("serialize2")
        }

        override fun deserialize(decoder: Decoder): Data {
            TODO("deserialize2")
        }
    }

    @Test
    fun contextualLosesPriorityOverFileLevelUseSerializers() {
        val h = Holder(listOf(Data()))
        val j = Json { serializersModule = serializersModuleOf(SecondSerializer) }
        val e = assertFailsWith<NotImplementedError> { j.encodeToString(h) }
        assertEquals("An operation is not implemented: descriptor1", e.message)
    }
}
