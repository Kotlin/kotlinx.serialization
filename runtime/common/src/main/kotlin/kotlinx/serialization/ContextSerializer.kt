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

package kotlinx.serialization

import kotlinx.serialization.context.getByValueOrDefault
import kotlinx.serialization.context.getOrDefault
import kotlinx.serialization.internal.SerialClassDescImpl
import kotlin.reflect.KClass

@ImplicitReflectionSerializer
class ContextSerializer<T : Any>(val serializableClass: KClass<T>) : KSerializer<T> {
    override fun serialize(output: Encoder, obj: T) {
        val s = output.context.getByValueOrDefault(obj)
        output.encodeSerializableValue(s, obj)
    }

    override fun deserialize(input: Decoder): T {
        val s = input.context.getOrDefault(serializableClass)
        @Suppress("UNCHECKED_CAST")
        return input.decodeSerializableValue(s)
    }

    override val descriptor: SerialDescriptor = object : SerialClassDescImpl("CONTEXT") {} // todo: remove this crutch
}
