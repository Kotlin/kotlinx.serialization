@file:OptIn(ExperimentalUnsignedTypes::class)
/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.features.inline

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.test.*
import kotlin.test.Test

// todo: fix such things in JSON (when descriptor = UIntDescriptor but inside there's List<Int>)
@Serializable(WithUnsignedSerializer::class)
data class WithUnsigned(val u: UInt)

object WithUnsignedSerializer : KSerializer<WithUnsigned> {
    override fun serialize(encoder: Encoder, obj: WithUnsigned) {
        val ce = encoder.beginStructure(descriptor)
        ce.encodeInlineElement(descriptor, 0, UInt.serializer().descriptor)?.encodeInt(obj.u.toInt())
        ce.endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): WithUnsigned {
        val cd = decoder.beginStructure(descriptor)
        var u: UInt = 0.toUInt()
        loop@ while (true) {
            u = when (val i = cd.decodeElementIndex(descriptor)) {
                0 -> cd.decodeInlineElement(descriptor, i, UInt.serializer().descriptor)?.decodeInt().toUInt()
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
    fun wrapper() = noLegacyJs {
        val w = WithUnsigned(Int.MAX_VALUE.toUInt() + 1.toUInt())
        assertStringFormAndRestored("""{"u":2147483648}""", w, WithUnsignedSerializer, printResult = true)
    }
}
