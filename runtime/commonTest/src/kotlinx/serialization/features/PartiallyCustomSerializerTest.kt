/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features

import kotlinx.serialization.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

@Serializable
data class WithNull(@SerialName("value") val nullable: String? = null) {
    @Serializer(forClass = WithNull::class)
    companion object : KSerializer<WithNull> {
        override fun serialize(encoder: Encoder, value: WithNull) {
            val elemOutput = encoder.beginStructure(descriptor)
            if (value.nullable != null) elemOutput.encodeStringElement(descriptor, 0, value.nullable)
            elemOutput.endStructure(descriptor)
        }
    }
}

class PartiallyCustomSerializerTest {
    @Test
    fun partiallyCustom() {
        assertEquals("""{"value":"foo"}""", Json.encodeToString(WithNull.serializer(), WithNull("foo")))
        assertEquals("""{}""", Json.encodeToString(WithNull.serializer(), WithNull()))
    }
}
