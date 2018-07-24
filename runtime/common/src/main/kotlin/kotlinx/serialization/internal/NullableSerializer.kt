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

fun <T : Any> makeNullable(element: KSerializer<T>): KSerializer<T?> = NullableSerializer(element)

class NullableSerializer<T : Any>(private val element: KSerializer<T>) : KSerializer<T?> {
    override val descriptor: SerialDescriptor = object : SerialDescriptor by element.descriptor {
        override val isNullable: Boolean
            get() = true
    }

    override fun serialize(output: Encoder, obj: T?) {
        if (obj != null) {
            output.encodeNotNullMark();
            element.serialize(output, obj)
        }
        else {
            output.encodeNull()
        }
    }

    override fun deserialize(input: Decoder): T? = if (input.decodeNotNullMark()) element.deserialize(input) else input.decodeNull()

    override fun patch(input: Decoder, old: T?): T? {
        return when {
            old == null -> deserialize(input)
            input.decodeNotNullMark() -> element.patch(input, old)
            else -> input.decodeNull().let { old }
        }
    }
}

