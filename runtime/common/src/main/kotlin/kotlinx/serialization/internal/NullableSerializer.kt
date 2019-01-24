/*
 * Copyright 2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlin.reflect.KClass

fun <T : Any> makeNullable(actualSerializer: KSerializer<T>): KSerializer<T?> = NullableSerializer(actualSerializer)

class NullableSerializer<T : Any>(public val actualSerializer: KSerializer<T>) : KSerializer<T?> {
    private class SerialDescriptorForNullable(val original: SerialDescriptor): SerialDescriptor by original {
        override val isNullable: Boolean
            get() = true

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is SerialDescriptorForNullable) return false

            if (original != other.original) return false

            return true
        }

        override fun hashCode(): Int {
            return original.hashCode() * 31
        }
    }

    override val descriptor: SerialDescriptor = SerialDescriptorForNullable(actualSerializer.descriptor)

    override fun serialize(encoder: Encoder, obj: T?) {
        if (obj != null) {
            encoder.encodeNotNullMark()
            encoder.encodeSerializableValue(actualSerializer, obj)
        }
        else {
            encoder.encodeNull()
        }
    }

    override fun deserialize(decoder: Decoder): T? {
        return if (decoder.decodeNotNullMark()) decoder.decodeSerializableValue(actualSerializer) else decoder.decodeNull()
    }

    override fun patch(decoder: Decoder, old: T?): T? {
        return when {
            old == null -> deserialize(decoder)
            decoder.decodeNotNullMark() -> actualSerializer.patch(decoder, old)
            else -> decoder.decodeNull().let { old }
        }
    }
}

