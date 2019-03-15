/*
 * Copyright 2019 JetBrains s.r.o.
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

class ObjectSerializer<T : Any>(private val serialName: String, private val theInstance: T) : KSerializer<T> {
    override fun serialize(encoder: Encoder, obj: T) {
        encoder.beginStructure(descriptor).endStructure(descriptor)
    }

    override fun deserialize(decoder: Decoder): T {
        decoder.beginStructure(descriptor).endStructure(descriptor)
        return theInstance
    }

    override val descriptor: SerialDescriptor = ObjectDescriptor(serialName)
}

/**
 * Object descriptor has UnionKind.OBJECT and consists of itself:
 * It has one element with name equals to object name
 * and [getElementDescriptor] returns the descriptor itself.
 */
class ObjectDescriptor(name: String) : SerialClassDescImpl(name) {
    override val kind: SerialKind = UnionKind.OBJECT

    init {
        addElement(name)
    }

    override fun getElementDescriptor(index: Int): SerialDescriptor {
        return this
    }
}
