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

open class EnumDescriptor(override val name: String, private val choices: Array<String>) : SerialClassDescImpl(name) {
    override val kind: SerialKind = UnionKind.ENUM_KIND

    init {
        choices.forEach { addElement(it) }
    }

    override fun getElementDescriptor(index: Int): SerialDescriptor {
        return this
    }
}

open class CommonEnumSerializer<T>(val serialName: String, val choices: Array<T>, choicesNames: Array<String>) :
    KSerializer<T> {
    override val descriptor: EnumDescriptor = EnumDescriptor(serialName, choicesNames)

    final override fun serialize(encoder: Encoder, obj: T) {
        val index = choices.indexOf(obj)
            .also { check(it != -1) { "$obj is not a valid enum $serialName, choices are $choices" } }
        encoder.encodeEnum(descriptor, index)
    }

    final override fun deserialize(decoder: Decoder): T {
        val index = decoder.decodeEnum(descriptor)
        check(index in choices.indices)
            { "$index is not among valid $serialName choices, choices size is ${choices.size}" }
        return choices[index]
    }
}

// Binary backwards-compatible with plugin
class EnumSerializer<T : Enum<T>>(serializableClass: KClass<T>) : CommonEnumSerializer<T>(
    serializableClass.enumClassName(),
    serializableClass.enumMembers(),
    serializableClass.enumMembers().map { it.name }.toTypedArray()
)
