/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features.inline

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.test.*
import kotlin.test.*

@Serializable(WithUnsignedSerializer::class)
data class WithUnsigned(val u: UInt)

object WithUnsignedSerializer : KSerializer<WithUnsigned> {
    override fun serialize(encoder: Encoder, value: WithUnsigned) {
        val ce = encoder.beginStructure(descriptor)
        ce.encodeInlineElement(descriptor, 0).encodeInt(value.u.toInt())
        ce.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): WithUnsigned {
        val cd = decoder.beginStructure(descriptor)
        var u: UInt = 0.toUInt()
        loop@ while (true) {
            u = when (val i = cd.decodeElementIndex(descriptor)) {
                0 -> cd.decodeInlineElement(descriptor, i).decodeInt().toUInt()
                else -> break@loop
            }
        }
        cd.endStructure(descriptor)
        return WithUnsigned(u)
    }

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("WithUnsigned") {
        element("u", UInt.serializer().descriptor)
    }
}

class EncodeInlineElementTest {
    @Test
    fun wrapper() {
        val w = WithUnsigned(Int.MAX_VALUE.toUInt() + 1.toUInt())
        assertStringFormAndRestored<WithUnsigned>("""{"u":2147483648}""", w, WithUnsignedSerializer, printResult = true)
    }
}
