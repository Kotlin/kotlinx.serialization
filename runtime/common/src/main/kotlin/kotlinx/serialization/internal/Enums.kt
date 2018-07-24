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

open class EnumDesc(override val name: String, private val choices: List<String>) : SerialClassDescImpl(name) {
    override val kind: SerialKind = UnionKind.ENUM_KIND

    init {
        choices.forEach { addElement(it) }
    }

    override fun getElementDescriptor(index: Int): SerialDescriptor {
        return this
    }
}

// note, that it is instantiated in a special way
@Deprecated("Not supported in Native", replaceWith = ReplaceWith("ModernEnumSerializer()"))
class EnumSerializer<T : Enum<T>>(val serializableClass: KClass<T>) : KSerializer<T> {
    override val descriptor: SerialDescriptor = EnumDesc(serializableClass.enumClassName(), serializableClass.enumMembers().map { it.name })
    override fun serialize(output: Encoder, obj: T) = output.encodeEnum(serializableClass, obj)
    override fun deserialize(input: Decoder): T = input.decodeEnum(serializableClass)
}

class ModernEnumSerializer<T : Enum<T>>(className: String, val creator: EnumCreator<T>) : KSerializer<T> {
    override val descriptor: SerialDescriptor = EnumDesc(className, creator.choices().map { it.name })
    override fun serialize(output: Encoder, obj: T) = output.encodeEnum(obj)
    override fun deserialize(input: Decoder): T = input.decodeEnum(creator)

    companion object {
        inline operator fun <reified E : Enum<E>> invoke(): ModernEnumSerializer<E> {
            return ModernEnumSerializer(E::class.enumClassName(), object : EnumCreator<E> {
                override fun createFromOrdinal(ordinal: Int): E {
                    return enumValues<E>()[ordinal]
                }

                override fun createFromName(name: String): E {
                    return enumValueOf<E>(name)
                }

                override fun choices(): Array<E> {
                    return enumValues<E>()
                }
            })
        }
    }
}
